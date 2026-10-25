package hwid.hwidmanager;

import java.util.StringTokenizer;

import hwid.HwidConfig;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class HWIDAdminBan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ban_hwid",
		"admin_unban_hwid",
		"admin_hwid_list"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (!HwidConfig.ALLOW_GUARD_SYSTEM)
		{
			activeChar.sendMessage("Guard/HWID system is disabled.");
			return false;
		}

		if (activeChar == null)
			return false;

		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		// ---- //hwid_list : muestra los HWID baneados ----
		if (command.startsWith("admin_hwid_list"))
		{
			final java.util.List<String> list = HWIDBan.getBannedHWIDs();
			activeChar.sendMessage("HWID bans (" + list.size() + "):");
			if (list.isEmpty())
				activeChar.sendMessage("  - (ninguno)");
			else
				for (String h : list)
					activeChar.sendMessage("  - " + h);
			return true;
		}

		// ---- //unban_hwid <hwid|player> : quita el ban y recarga la lista (sin restart) ----
		if (command.startsWith("admin_unban_hwid"))
		{
			String hwid = null;
			if (st.hasMoreTokens())
			{
				final String param = st.nextToken();
				final Player online = World.getInstance().getPlayer(param);
				hwid = (online != null) ? online.getHWID() : param;
			}
			else if (activeChar.getTarget() instanceof Player)
				hwid = ((Player) activeChar.getTarget()).getHWID();

			if (hwid == null || hwid.isEmpty())
			{
				activeChar.sendMessage("Usage: //unban_hwid <hwid | player> (or select a player target).");
				return false;
			}

			HWIDBan.removeHWIDBan(hwid);
			removeOtherHwidBans(hwid);
			activeChar.sendMessage("HWID unbanned: " + hwid);
			return true;
		}

		if (!command.startsWith("admin_ban_hwid"))
			return false;

		Player target = null;
		if (st.hasMoreTokens())
			target = World.getInstance().getPlayer(st.nextToken());
		else if (activeChar.getTarget() instanceof Player)
			target = (Player) activeChar.getTarget();

		if (target == null)
		{
			activeChar.sendMessage("Usage: //ban_hwid [player] (or select a player target).");
			return false;
		}

		if (target.equals(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
			return false;
		}

		final String hwid = target.getHWID();
		if (hwid == null || hwid.isEmpty())
		{
			activeChar.sendMessage(target.getName() + " has no HWID.");
			return false;
		}

		HWIDBan.addHWIDBan(target.getClient());

		for (Player p : World.getInstance().getPlayers())
		{
			if (p != null && p.isOnline() && hwid.equals(p.getHWID()))
				p.logout(true);
		}

		activeChar.sendMessage(target.getName() + " banned in HWID: " + hwid);
		return true;
	}

	/**
	 * Borra el HWID tambien de la tabla "banned_hwid" (la usa CharacterSelected.BanedHwid en el select de personaje).
	 */
	@SuppressWarnings("resource")
	private static void removeOtherHwidBans(String hwid)
	{
		try
		{
			final java.sql.Connection con = net.sf.l2j.L2DatabaseFactory.getInstance().getConnection();
			try
			{
				final java.sql.PreparedStatement statement = con.prepareStatement("DELETE FROM banned_hwid WHERE hwid=?");
				statement.setString(1, hwid);
				statement.execute();
				statement.close();
			}
			finally
			{
				con.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
