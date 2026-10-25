package net.sf.l2j.gameserver.farmhwid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Una PC = una cuenta de drop. No depende de .farm.
 * Misma PC si coincide HWID o IP (cada ventana puede mandar HWID distinto).
 */
public final class FarmHwidService
{
	private static final CLogger LOGGER = new CLogger(FarmHwidService.class.getName());
	
	private static final String SELECT_ALL_OWNERS = "SELECT hwid, account_name, char_id, bound_at FROM farm_hwid_owner";
	private static final String SELECT_OWNER = "SELECT hwid, account_name, char_id, bound_at FROM farm_hwid_owner WHERE hwid=?";
	private static final String INSERT_OWNER = "INSERT INTO farm_hwid_owner (hwid, account_name, char_id, bound_at) VALUES (?,?,?,?)";
	private static final String DELETE_OWNER = "DELETE FROM farm_hwid_owner WHERE hwid=?";
	private static final String SELECT_ALL_CD = "SELECT hwid, unbound_at FROM farm_hwid_cooldown";
	private static final String UPSERT_CD = "REPLACE INTO farm_hwid_cooldown (hwid, unbound_at) VALUES (?,?)";
	private static final String DELETE_CD = "DELETE FROM farm_hwid_cooldown WHERE hwid=?";
	
	private static final String CREATE_IP_OWNER = "CREATE TABLE IF NOT EXISTS farm_ip_owner (ip VARCHAR(45) NOT NULL, account_name VARCHAR(45) NOT NULL, bound_at BIGINT NOT NULL, PRIMARY KEY (ip), KEY account_name (account_name)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	private static final String SELECT_ALL_IPS = "SELECT ip, account_name FROM farm_ip_owner";
	private static final String INSERT_IP = "INSERT INTO farm_ip_owner (ip, account_name, bound_at) VALUES (?,?,?)";
	private static final String DELETE_IP = "DELETE FROM farm_ip_owner WHERE ip=?";
	private static final String DELETE_IP_BY_ACCOUNT = "DELETE FROM farm_ip_owner WHERE account_name=?";
	
	private static final ConcurrentHashMap<String, Owner> OWNERS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> IP_OWNERS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Long> COOLDOWNS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Integer, Long> DROP_DENY_MSG = new ConcurrentHashMap<>();
	private static final long DROP_DENY_MSG_INTERVAL = 15_000L;
	
	private FarmHwidService()
	{
	}
	
	public static void init()
	{
		OWNERS.clear();
		IP_OWNERS.clear();
		COOLDOWNS.clear();
		
		if (!Config.FARM_HWID_ENABLED)
		{
			LOGGER.info("FarmHwidService: disabled.");
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(CREATE_IP_OWNER))
			{
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_OWNERS);
				ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					OWNERS.put(rs.getString("hwid"), readOwner(rs));
			}
			
			try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_IPS);
				ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					IP_OWNERS.put(rs.getString("ip"), rs.getString("account_name"));
			}
			
			try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_CD);
				ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
					COOLDOWNS.put(rs.getString("hwid"), rs.getLong("unbound_at"));
			}
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: failed loading tables. Run sql/10_farm_hwid_owner.sql", e);
		}
		
		LOGGER.info("FarmHwidService: loaded {} hwid owner(s), {} ip owner(s), {} cooldown(s).", OWNERS.size(), IP_OWNERS.size(), COOLDOWNS.size());
	}
	
	/**
	 * Solo bloquea OTRA VENTANA online de otra cuenta en la misma PC.
	 * Si la cuenta dueña se desloguea, esta cuenta puede dropear.
	 */
	public static boolean canReceiveDrop(Player player)
	{
		try
		{
			return evaluate(player, false);
		}
		catch (Exception e)
		{
			LOGGER.warn("FarmHwidService: drop check failed, allowing drop.", e);
			return true;
		}
	}
	
	private static boolean evaluate(Player player, boolean notifyAlways)
	{
		if (!Config.FARM_HWID_ENABLED)
			return true;
		
		if (player == null)
			return false;
		
		if (Config.FARM_HWID_IGNORE_GM && player.isGM())
			return true;
		
		final String account = player.getAccountName();
		if (account == null || account.isEmpty())
			return true;
		
		final String hwid = firstId(normalizeHwid(player), VoicedAutoFarm.getSafeHwid(player));
		final String ip = firstId(getIp(player), VoicedAutoFarm.getSafeIp(player));
		if (hwid == null && ip == null)
			return true;
		
		final Player rival = findSamePcOtherAccount(player, hwid, ip);
		if (rival != null)
		{
			final String ipOwner = ip == null ? null : IP_OWNERS.get(ip);
			String winner = null;
			if (ipOwner != null && (ipOwner.equalsIgnoreCase(account) || ipOwner.equalsIgnoreCase(rival.getAccountName())))
				winner = ipOwner;
			if (winner == null)
				winner = connectionStart(rival) <= connectionStart(player) ? rival.getAccountName() : account;
			
			if (!winner.equalsIgnoreCase(account))
			{
				notify(player, "Este PC solo dropea en " + rival.getName() + " (otra ventana).", notifyAlways);
				return false;
			}
			
			claimPc(player, account);
			return true;
		}
		
		claimPc(player, account);
		return true;
	}
	
	private static String getHwidOwnerAccount(String hwid)
	{
		if (hwid == null)
			return null;
		
		Owner owner = OWNERS.get(hwid);
		if (owner == null)
		{
			owner = loadOwner(hwid);
			if (owner != null)
				OWNERS.putIfAbsent(hwid, owner);
		}
		return owner == null ? null : owner.accountName;
	}
	
	private static Player findSamePcOtherAccount(Player player, String hwid, String ip)
	{
		Player best = null;
		long bestStart = Long.MAX_VALUE;
		for (Player other : World.getInstance().getPlayers())
		{
			if (other == null || other == player || other.getClient() == null)
				continue;
			if (Config.FARM_HWID_IGNORE_GM && other.isGM())
				continue;
			final String otherAcc = other.getAccountName();
			if (otherAcc == null || otherAcc.equalsIgnoreCase(player.getAccountName()))
				continue;
			if (!isSamePc(hwid, ip, other))
				continue;
			final long start = connectionStart(other);
			if (start < bestStart)
			{
				best = other;
				bestStart = start;
			}
		}
		return best;
	}
	
	private static boolean isSamePc(String hwid, String ip, Player other)
	{
		final String otherHwid = firstId(normalizeHwid(other), VoicedAutoFarm.getSafeHwid(other));
		if (hwid != null && otherHwid != null && hwid.equalsIgnoreCase(otherHwid))
			return true;
		final String otherIp = firstId(getIp(other), VoicedAutoFarm.getSafeIp(other));
		return ip != null && otherIp != null && ip.equals(otherIp);
	}
	
	private static void claimPc(Player player, String account)
	{
		if (player == null || account == null)
			return;
		
		final String hwid = firstId(normalizeHwid(player), VoicedAutoFarm.getSafeHwid(player));
		final String ip = firstId(getIp(player), VoicedAutoFarm.getSafeIp(player));
		if (hwid != null)
			rebindHwid(hwid, account, player.getObjectId());
		if (ip != null)
			rebindIp(ip, account);
	}
	
	private static void rebindHwid(String hwid, String account, int charId)
	{
		final Owner existing = OWNERS.get(hwid);
		if (existing != null && existing.accountName.equalsIgnoreCase(account))
			return;
		if (existing != null)
			unbind(hwid, false);
		tryClaim(hwid, account, charId);
	}
	
	private static void rebindIp(String ip, String account)
	{
		if (ip == null || account == null)
			return;
		
		final String existing = IP_OWNERS.get(ip);
		if (existing != null && existing.equalsIgnoreCase(account))
			return;
		
		IP_OWNERS.put(ip, account);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement del = con.prepareStatement(DELETE_IP))
			{
				del.setString(1, ip);
				del.executeUpdate();
			}
			try (PreparedStatement ins = con.prepareStatement(INSERT_IP))
			{
				ins.setString(1, ip);
				ins.setString(2, account);
				ins.setLong(3, System.currentTimeMillis());
				ins.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: ip rebind failed for {}", e, ip);
		}
	}
	
	public static boolean unbindPlayer(Player player, boolean applyCooldown)
	{
		if (player == null)
			return false;
		
		boolean ok = false;
		final String hwid = firstId(normalizeHwid(player), VoicedAutoFarm.getSafeHwid(player));
		final String ip = firstId(getIp(player), VoicedAutoFarm.getSafeIp(player));
		final String account = player.getAccountName();
		
		if (hwid != null)
			ok |= unbind(hwid, applyCooldown);
		if (ip != null)
			ok |= unbindIp(ip);
		if (account != null)
			ok |= unbindAccount(account);
		return ok;
	}
	
	public static boolean unbindIp(String ip)
	{
		if (ip == null || ip.isEmpty())
			return false;
		
		IP_OWNERS.remove(ip);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_IP))
		{
			ps.setString(1, ip);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: ip unbind failed for {}", e, ip);
			return false;
		}
		return true;
	}
	
	public static boolean unbindAccount(String account)
	{
		if (account == null || account.isEmpty())
			return false;
		
		IP_OWNERS.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(account));
		OWNERS.entrySet().removeIf(e -> e.getValue().accountName.equalsIgnoreCase(account));
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_IP_BY_ACCOUNT))
		{
			ps.setString(1, account);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: account ip unbind failed for {}", e, account);
		}
		return true;
	}
	
	private static long connectionStart(Player player)
	{
		try
		{
			if (player.getClient() != null)
				return player.getClient().getConnectionStartTime();
		}
		catch (Exception e)
		{
		}
		return Long.MAX_VALUE;
	}
	
	private static String firstId(String a, String b)
	{
		return a != null ? a : b;
	}
	
	private static String getIp(Player player)
	{
		try
		{
			if (player.getClient() == null || player.getClient().getConnection() == null)
				return null;
			final java.net.InetAddress addr = player.getClient().getConnection().getInetAddress();
			if (addr == null)
				return null;
			final String ip = addr.getHostAddress();
			if (ip == null || ip.isEmpty())
				return null;
			return ip;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	private static boolean bindIp(String ip, String account)
	{
		if (ip == null || account == null)
			return false;
		
		final String existing = IP_OWNERS.putIfAbsent(ip, account);
		if (existing != null)
			return existing.equalsIgnoreCase(account);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_IP))
		{
			ps.setString(1, ip);
			ps.setString(2, account);
			ps.setLong(3, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			if (!isDuplicate(e))
				LOGGER.error("FarmHwidService: ip bind failed for {}", e, ip);
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: ip bind failed for {}", e, ip);
		}
		return true;
	}
	
	private static void notify(Player player, String message, boolean always)
	{
		if (player == null || message == null)
			return;
		
		if (!always)
		{
			final long now = System.currentTimeMillis();
			final Long last = DROP_DENY_MSG.get(player.getObjectId());
			if (last != null && now - last < DROP_DENY_MSG_INTERVAL)
				return;
			DROP_DENY_MSG.put(player.getObjectId(), now);
		}
		player.sendMessage(message);
	}
	
	public static String normalizeHwid(Player player)
	{
		if (player == null)
			return null;
		
		String hwid = null;
		try
		{
			if (player.getClient() != null)
				hwid = player.getClient().getHWID();
		}
		catch (Exception e)
		{
			hwid = null;
		}
		if (hwid == null)
			hwid = VoicedAutoFarm.getSafeHwid(player);
		if (hwid == null)
			return null;
		
		hwid = hwid.trim();
		if (hwid.isEmpty())
			return null;
		if (hwid.equalsIgnoreCase("NoGuard") || hwid.regionMatches(true, 0, "NoHWID", 0, 6))
			return null;
		
		return hwid.toUpperCase();
	}
	
	public static Owner getOwner(String hwid)
	{
		if (hwid == null || hwid.isEmpty())
			return null;
		return OWNERS.get(hwid);
	}
	
	public static Owner getOwner(Player player)
	{
		return getOwner(normalizeHwid(player));
	}
	
	public static long remainingCooldown(String hwid)
	{
		if (hwid == null)
			return 0;
		
		final Long unboundAt = COOLDOWNS.get(hwid);
		if (unboundAt == null)
			return 0;
		
		final long cooldownMs = Config.FARM_HWID_UNBIND_COOLDOWN_HOURS * 60L * 60L * 1000L;
		if (cooldownMs <= 0)
			return 0;
		
		final long remain = (unboundAt + cooldownMs) - System.currentTimeMillis();
		return Math.max(0, remain);
	}
	
	public static boolean unbind(String hwid, boolean applyCooldown)
	{
		if (hwid == null || hwid.isEmpty())
			return false;
		
		hwid = hwid.toUpperCase();
		final Owner removed = OWNERS.remove(hwid);
		if (removed != null)
		{
			IP_OWNERS.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(removed.accountName));
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_IP_BY_ACCOUNT))
			{
				ps.setString(1, removed.accountName);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("FarmHwidService: ip unbind failed for {}", e, removed.accountName);
			}
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_OWNER))
		{
			ps.setString(1, hwid);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: unbind delete failed for {}", e, hwid);
			return false;
		}
		
		if (applyCooldown)
		{
			final long now = System.currentTimeMillis();
			COOLDOWNS.put(hwid, now);
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(UPSERT_CD))
			{
				ps.setString(1, hwid);
				ps.setLong(2, now);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("FarmHwidService: cooldown save failed for {}", e, hwid);
			}
		}
		else
			clearCooldown(hwid);
		
		return true;
	}
	
	public static void clearCooldown(String hwid)
	{
		if (hwid == null || hwid.isEmpty())
			return;
		
		hwid = hwid.toUpperCase();
		COOLDOWNS.remove(hwid);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_CD))
		{
			ps.setString(1, hwid);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: clear cooldown failed for {}", e, hwid);
		}
	}
	
	public static String formatRemain(long ms)
	{
		long totalMin = Math.max(1, ms / 60000L);
		final long days = totalMin / (60 * 24);
		totalMin %= (60 * 24);
		final long hours = totalMin / 60;
		final long mins = totalMin % 60;
		if (days > 0)
			return days + "d " + hours + "h";
		if (hours > 0)
			return hours + "h " + mins + "m";
		return mins + "m";
	}
	
	private static boolean tryClaim(String hwid, String account, int charId)
	{
		final long now = System.currentTimeMillis();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_OWNER))
		{
			ps.setString(1, hwid);
			ps.setString(2, account);
			ps.setInt(3, charId);
			ps.setLong(4, now);
			ps.executeUpdate();
			OWNERS.put(hwid, new Owner(hwid, account, charId, now));
			return true;
		}
		catch (SQLException e)
		{
			if (isDuplicate(e))
				return false;
			
			LOGGER.error("FarmHwidService: claim failed for {}", e, hwid);
			return false;
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: claim failed for {}", e, hwid);
			return false;
		}
	}
	
	private static Owner loadOwner(String hwid)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_OWNER))
		{
			ps.setString(1, hwid);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
					return readOwner(rs);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("FarmHwidService: load owner failed for {}", e, hwid);
		}
		return null;
	}
	
	private static Owner readOwner(ResultSet rs) throws SQLException
	{
		return new Owner(rs.getString("hwid"), rs.getString("account_name"), rs.getInt("char_id"), rs.getLong("bound_at"));
	}
	
	private static boolean isDuplicate(SQLException e)
	{
		SQLException cur = e;
		while (cur != null)
		{
			if (cur.getErrorCode() == 1062)
				return true;
			final String msg = cur.getMessage();
			if (msg != null && msg.toLowerCase().contains("duplicate"))
				return true;
			cur = cur.getNextException();
		}
		return false;
	}
	
	public static final class Owner
	{
		public final String hwid;
		public final String accountName;
		public final int charId;
		public final long boundAt;
		
		private Owner(String hwid, String accountName, int charId, long boundAt)
		{
			this.hwid = hwid;
			this.accountName = accountName;
			this.charId = charId;
			this.boundAt = boundAt;
		}
	}
}
