/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.util.CloseUtil;

/**
 * Give / remove Vip status to player Changes name color and title color if enabled Uses: setvip [<player_name>] [<time_duration in days>] If <player_name> is not specified, the current target player is used.
 * @author KhayrusS && SweeTs
 */
public class AdminBanHWID implements IAdminCommandHandler
{

	private static String[] _adminCommands =
	{
		"admin_ban_ip"
	};
	protected static final Logger _log = Logger.getLogger(AdminBanHWID.class.getName());

	@SuppressWarnings("null")
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		String player = "";
		Player targetPlayer = null;
		// One parameter, player name
		if (st.hasMoreTokens())
		{
			player = st.nextToken();
			targetPlayer = World.getInstance().getPlayer(player);
		}
		else
		{
			// If there is no name, select target
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof Player)
				targetPlayer = (Player) activeChar.getTarget();
		}

		// Can't ban yourself
		if (targetPlayer != null && targetPlayer.equals(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
			return false;
		}

		if (command.startsWith("admin_ban_ip"))
		{
			String hwid = targetPlayer.getHWID();
			if (hwid != null)
			{
				updateDatabase(targetPlayer);
				for (Player p : World.getInstance().getPlayers())
				{
					String hwidz = p.getHWID();

					if (p.isOnline())
					{
						if (hwidz.equals(targetPlayer.getHWID()))
							p.logout(true);
					}
				}
				activeChar.sendMessage(new StringBuilder().append("HWID : ").append(hwid).append(" Banned").toString());
			}

		}

		return false;
	}

	@SuppressWarnings("resource")
	public static void updateDatabase(Player player)
	{
		// prevents any NPE.
		if (player == null)
			return;

		Connection con = null;
		try
		{
			// Database Connection
			// --------------------------------
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);

			stmt.setString(1, player.getName());
			stmt.setString(2, player.getHWID());
			stmt.execute();
			stmt.close();
			stmt = null;

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}

	// Updates That Will be Executed by MySQL
	// ----------------------------------------
	static String INSERT_DATA = "REPLACE INTO banned_hwid (char_name, hwid) VALUES (?,?)";

	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}

}