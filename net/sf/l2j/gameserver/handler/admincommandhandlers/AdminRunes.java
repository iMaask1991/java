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

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

/**
 * Admin runes: based on inventory items only (no DB time columns).
 */
public class AdminRunes implements IAdminCommandHandler
{
	private static String[] _adminCommands =
	{
		"admin_setrune",
		"admin_removerune",
		"admin_rune_gift"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_setrune") || command.startsWith("admin_rune_gift"))
		{
			StringTokenizer str = new StringTokenizer(command);
			
			WorldObject target = activeChar.getTarget();
			Player player = (target instanceof Player) ? (Player) target : activeChar;
			
			try
			{
				str.nextToken();
				int runeTier = 1;
				if (str.hasMoreTokens())
				{
					final String first = str.nextToken();
					Player byName = World.getInstance().getPlayer(first);
					if (byName != null)
					{
						player = byName;
						if (str.hasMoreTokens())
							runeTier = Integer.parseInt(str.nextToken());
					}
					else
						runeTier = Integer.parseInt(first);
				}
				
				giveRune(activeChar, player, runeTier);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //setrune [char_name] [1|2|3]");
			}
			
			if (player != null)
				player.broadcastUserInfo();
			return true;
		}
		else if (command.startsWith("admin_removerune"))
		{
			StringTokenizer str = new StringTokenizer(command);
			WorldObject target = activeChar.getTarget();
			Player player = (target instanceof Player) ? (Player) target : activeChar;
			
			try
			{
				str.nextToken();
				if (str.hasMoreTokens())
				{
					String playername = str.nextToken();
					player = World.getInstance().getPlayer(playername);
					removeRune(activeChar, player, playername);
				}
				else
					removeRune(activeChar, player, player.getName());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //removerune [char_name]");
			}
			if (player != null)
				player.broadcastUserInfo();
			return true;
		}
		
		return false;
	}
	
	private static int getRuneItemId(int tier)
	{
		switch (tier)
		{
			case 2:
				return Config.RUNE_2_ID;
			case 3:
				return Config.RUNE_3_ID;
			case 1:
			default:
				return Config.RUNE_1_ID;
		}
	}
	
	private static boolean hasAnyRune(Player player)
	{
		return player.getInventory().getInventoryItemCount(Config.RUNE_1_ID, 0) >= 1
			|| player.getInventory().getInventoryItemCount(Config.RUNE_2_ID, 0) >= 1
			|| player.getInventory().getInventoryItemCount(Config.RUNE_3_ID, 0) >= 1;
	}
	
	public void giveRune(Player activeChar, Player player, int tier)
	{
		if (player == null)
		{
			activeChar.sendMessage("Character not found.");
			return;
		}
		
		removeAllRuneItems(player);
		final int itemId = getRuneItemId(tier);
		player.addItem("AdminRune", itemId, 1, player, true);
		player.sendPacket(new ItemList(player, false));
		activeChar.sendMessage("Runa " + tier + " (permanente) entregada a " + player.getName() + ".");
	}
	
	/** Kept for EnterWorld/legacy calls; now only strips items. */
	public void removeRune(Player activeChar, Player player, String playername)
	{
		if (player == null)
			return;
		
		if (!hasAnyRune(player) && activeChar != null)
		{
			activeChar.sendMessage("Player " + playername + " no tiene runa.");
			return;
		}
		
		removeAllRuneItems(player);
		player.sendPacket(new ItemList(player, false));
		if (activeChar != null)
			activeChar.sendMessage("Runa removida de " + playername + ".");
	}
	
	private static void removeAllRuneItems(Player player)
	{
		player.getInventory().destroyItemByItemId("1_Rune", Config.RUNE_1_ID, player.getInventory().getInventoryItemCount(Config.RUNE_1_ID, 0), player, null);
		player.getInventory().destroyItemByItemId("2_Rune", Config.RUNE_2_ID, player.getInventory().getInventoryItemCount(Config.RUNE_2_ID, 0), player, null);
		player.getInventory().destroyItemByItemId("3_Rune", Config.RUNE_3_ID, player.getInventory().getInventoryItemCount(Config.RUNE_3_ID, 0), player, null);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}
