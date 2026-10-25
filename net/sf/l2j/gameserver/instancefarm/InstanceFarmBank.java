package net.sf.l2j.gameserver.instancefarm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Banco del Instance Farm POR PERSONAJE (lo que compra un personaje queda solo en ese personaje).<br>
 * Guarda los SEGUNDOS de TIME que quedan y las KILLS compradas que quedan.<br>
 * El HWID (resolveKey) se usa aparte, solo para limitar 1 personaje por PC adentro al mismo tiempo.
 */
public final class InstanceFarmBank
{
	private static final CLogger LOGGER = new CLogger(InstanceFarmBank.class.getName());
	
	private static final String CREATE = "CREATE TABLE IF NOT EXISTS instance_farm_hwid_bank (hwid VARCHAR(128) NOT NULL, time_seconds INT NOT NULL DEFAULT 0, kill_count INT NOT NULL DEFAULT 0, updated_at BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (hwid)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	private static final String SELECT = "SELECT time_seconds, kill_count FROM instance_farm_hwid_bank WHERE hwid=?";
	private static final String UPSERT = "REPLACE INTO instance_farm_hwid_bank (hwid, time_seconds, kill_count, updated_at) VALUES (?,?,?,?)";
	
	// Estadisticas del Kill Farm (.instance): kills compradas y mobs matados en total.
	private static final String CREATE_STATS = "CREATE TABLE IF NOT EXISTS instance_farm_kill_stats (hwid VARCHAR(128) NOT NULL, total_bought INT NOT NULL DEFAULT 0, total_killed INT NOT NULL DEFAULT 0, PRIMARY KEY (hwid)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	private static final String SELECT_STATS = "SELECT total_bought, total_killed FROM instance_farm_kill_stats WHERE hwid=?";
	private static final String UPSERT_STATS = "REPLACE INTO instance_farm_kill_stats (hwid, total_bought, total_killed) VALUES (?,?,?)";
	
	private static final Map<String, InstanceFarmBank> CACHE = new ConcurrentHashMap<>();
	
	private final String _key;
	private int _timeSeconds;
	private int _killCount;
	private int _totalBought;
	private int _totalKilled;
	
	private InstanceFarmBank(String key)
	{
		_key = key;
	}
	
	public static void createTable()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement())
		{
			st.executeUpdate(CREATE);
			st.executeUpdate(CREATE_STATS);
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmBank: no se pudo crear la tabla instance_farm_hwid_bank.", e);
		}
	}
	
	/**
	 * @param player : el jugador.
	 * @return la clave de la PC (HWID, o la cuenta si el cliente no manda HWID). Se usa para limitar 1 personaje por PC.
	 */
	public static String resolveKey(Player player)
	{
		if (player == null)
			return null;
		
		final String hwid = player.getHWID();
		if (hwid != null && !hwid.trim().isEmpty())
			return "hwid:" + hwid.trim();
		
		final String account = player.getAccountName();
		if (account != null && !account.isEmpty())
			return "acc:" + account.toLowerCase();
		
		return null;
	}
	
	/**
	 * @param player : el jugador.
	 * @return el saldo (tiempo / kills) de ESE personaje.
	 */
	public static InstanceFarmBank get(Player player)
	{
		if (player == null)
			return null;
		
		final String key = "char:" + player.getObjectId();
		
		return CACHE.computeIfAbsent(key, InstanceFarmBank::loadFromDb);
	}
	
	private static InstanceFarmBank loadFromDb(String key)
	{
		final InstanceFarmBank bank = new InstanceFarmBank(key);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT))
		{
			ps.setString(1, key);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					bank._timeSeconds = Math.max(0, rs.getInt("time_seconds"));
					bank._killCount = Math.max(0, rs.getInt("kill_count"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmBank: error cargando {}.", e, key);
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_STATS))
		{
			ps.setString(1, key);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					bank._totalBought = Math.max(0, rs.getInt("total_bought"));
					bank._totalKilled = Math.max(0, rs.getInt("total_killed"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmBank: error cargando estadisticas de {}.", e, key);
		}
		return bank;
	}
	
	public synchronized void save()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT))
		{
			ps.setString(1, _key);
			ps.setInt(2, Math.max(0, _timeSeconds));
			ps.setInt(3, Math.max(0, _killCount));
			ps.setLong(4, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmBank: error guardando {}.", e, _key);
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT_STATS))
		{
			ps.setString(1, _key);
			ps.setInt(2, Math.max(0, _totalBought));
			ps.setInt(3, Math.max(0, _totalKilled));
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmBank: error guardando estadisticas de {}.", e, _key);
		}
	}
	
	public String getKey()
	{
		return _key;
	}
	
	public synchronized int getTimeSeconds()
	{
		return _timeSeconds;
	}
	
	public synchronized void setTimeSeconds(int seconds)
	{
		_timeSeconds = Math.max(0, seconds);
	}
	
	/**
	 * Resta 1 segundo.
	 * @return los segundos que quedan.
	 */
	public synchronized int consumeSecond()
	{
		if (_timeSeconds > 0)
			_timeSeconds--;
		return _timeSeconds;
	}
	
	public synchronized int getKillCount()
	{
		return _killCount;
	}
	
	public synchronized void addKills(int amount)
	{
		_killCount = Math.max(0, _killCount + amount);
		if (amount > 0)
			_totalBought += amount;
	}
	
	/** @return kills compradas en total (estadistica). */
	public synchronized int getTotalBought()
	{
		return _totalBought;
	}
	
	/** @return mobs matados en total en el Kill Farm (estadistica). */
	public synchronized int getTotalKilled()
	{
		return _totalKilled;
	}
	
	/**
	 * Resta 1 kill.
	 * @return las kills que quedan, o -1 si ya no tenia.
	 */
	public synchronized int consumeKill()
	{
		if (_killCount <= 0)
			return -1;
		_totalKilled++;
		return --_killCount;
	}
}
