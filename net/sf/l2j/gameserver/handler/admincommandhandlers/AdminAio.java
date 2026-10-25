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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;

/**
 *  "//setaio" give aio status to player Changes name color and title color if enabled Uses: setaio [<player_name>] [<time_duration in days>]
 *  "//removeaio"  Uses: removeaio [<player_name>] if <player_name> is not specified, the current target player is used.
 * @author KhayrusS, SweeTs, Reynald0
 */
public class AdminAio implements IAdminCommandHandler
{
   private static String[] _adminCommands =
   {
       "admin_setaio",
       "admin_aio",
       "admin_removeaio",
       "admin_gift"
   };
   private static Logger _log = Logger.getLogger(MapRegionData.class.getName());
   
   @Override
   public boolean useAdminCommand(String command, Player activeChar)
   {
       if (command.startsWith("admin_setaio") || command.startsWith("admin_aio"))
       {
           if(!Config.ENABLE_AIO_SYSTEM)
           {
               activeChar.sendMessage("AIO system disabled");
               return false;
           }
           
           StringTokenizer str = new StringTokenizer(command);
           
           WorldObject target = activeChar.getTarget();
           Player player = null;
           
           if (target != null && target instanceof Player)
               player = (Player) target;
           else
               player = activeChar;
           
           try
           {
               str.nextToken();
               String time = str.nextToken();
               if (str.hasMoreTokens())
               {
                   String playername = time;
                   time = str.nextToken();
                   player = World.getInstance().getPlayer(playername);
                   doAio(activeChar, player, playername, time);
               }
               else
               {
                   String playername = player.getName();
                   doAio(activeChar, player, playername, time);
               }
           }
           catch (Exception e)
           {
               activeChar.sendMessage("Usage: //setaio <char_name> [time](in days)");
           }
           
           player.broadcastUserInfo();
           
           if (player.isAio())
               return true;
       }
       else if (command.startsWith("admin_removeaio"))
       {
           StringTokenizer str = new StringTokenizer(command);
           WorldObject target = activeChar.getTarget();
           Player player = null;
           
           if (target instanceof Player)
               player = (Player) target;
           else
               player = activeChar;
           
           try
           {
               str.nextToken();
               
               if (str.hasMoreTokens())
               {
                   String playername = str.nextToken();
                   player = World.getInstance().getPlayer(playername);
                   removeAio(activeChar, player, playername);
               }
               else
               {
                   String playername = player.getName();
                   removeAio(activeChar, player, playername);
               }
           }
           catch (Exception e)
           {
               activeChar.sendMessage("Usage: //removeaio <char_name>");
           }
           player.broadcastUserInfo();
       }

       return false;
   }
   
   public void doAio(Player activeChar, Player _player, String _playername, String _time)
   {
       int days = Integer.parseInt(_time);
       
       if (_player == null)
       {
           activeChar.sendMessage("Character not found.");
           return;
       }
       if (_player.isAio())
       {
           activeChar.sendMessage("Player " + _playername + " is already an AIO.");
           return;
       }
       
       if (days > 0)
       {
           _player.getStat().addExp(_player.getStat().getExpForLevel(81));
           _player.setClassId(_player.getClassId().getFirstClass().getId());
           _player.lostAioSkills();
           _player.setAio(true);
           applyAioName(_player);
           _player.setEndTimeAio("aio", days);
           
           try (Connection con = L2DatabaseFactory.getInstance().getConnection())
           {
               PreparedStatement ps = con.prepareStatement("UPDATE characters SET aio=1, aio_end=? WHERE obj_id=?");
               ps.setLong(1, _player.getAioEndTime());
               ps.setInt(2, _player.getObjectId());
               ps.execute();
               ps.close();
               
               if (Config.ALLOW_AIO_NCOLOR)
                   _player.getAppearance().setNameColor(Config.AIO_NCOLOR);
               
               if (Config.ALLOW_AIO_TCOLOR)
                   _player.getAppearance().setTitleColor(Config.AIO_TCOLOR);
               
               _player.rewardAioSkills();

		       if (Config.ALLOW_AIO_EQUIP_ITEM)
			       giveAioItems(_player); 
		          
               updateChar(_player);

           }
           catch (Exception e)
           {
               _log.log(Level.WARNING, "Something went wrong, check log folder for details", e);
           }
       }
   }
   
   public void removeAio(Player activeChar, Player _player, String _playername)
   {
       if (_player == null)
       {
           if (activeChar != null)
               activeChar.sendMessage("Character not found.");
           return;
       }
       
       // Si no es AIO, igual limpia nick huérfano ([ AIO ] / *Aio-*).
       if (!_player.isAio())
       {
           removeAioName(_player);
           if (activeChar != null)
               activeChar.sendMessage("Player " + _playername + " is not an AIO.");
           return;
       }
       
       //Paralyze player
       _player.startAbnormalEffect(0x0800);
       _player.setIsParalyzed(true);
       _player.broadcastPacket(new StopMove(_player));
       _player.lostAioSkills();
       _player.setAioEndTime(0);
       _player.setAio(false);
       
       // Sacar nick YA (aunque falle el SQL abajo)
       removeAioName(_player);
       
       try (Connection con = L2DatabaseFactory.getInstance().getConnection())
       {
           PreparedStatement ps = con.prepareStatement("UPDATE characters SET Aio=0, Aio_end=0 WHERE obj_id=?");
           ps.setInt(1, _player.getObjectId());
           ps.execute();
           ps.close();
           
           _player.removeExpAndSp(6299994999L, 0);
           _player.setTitle("");
           _player.getAppearance().setNameColor(0xFFFFFF);
           _player.getAppearance().setTitleColor(0xFFFFFF);
           updateChar(_player);
       }
       catch (Exception e)
       {
           _log.log(Level.WARNING, "Something went wrong, check log folder for details", e);
           // Igual broadcast/store del nick limpio
           updateChar(_player);
       }
       //Unparalyze player
       _player.stopAbnormalEffect(0x0800);
       _player.setIsParalyzed(false);
   }

   public static void giveAioItems(Player activeChar) 
   {
		for (int[] reward : Config.AIO_ITEMS) {
		   ItemInstance PhewPew1 = activeChar.getInventory().addItem("Aio Items", reward[0], reward[1], activeChar, null);
		   activeChar.getInventory().equipItemAndRecord(PhewPew1);
		} 
	}
	
	/**
	 * Limpia prefijos: *Aio-Nick*, [AIO], [ AIO ], y Config.AioNamePrefix.
	 */
	public static String stripAioNamePrefix(String name)
	{
		if (name == null || name.isEmpty())
			return name;
		
		String clean = name;
		
		// Formato viejo: *Aio-Nick*
		if (clean.startsWith("*Aio-") && clean.endsWith("*") && clean.length() > 6)
			clean = clean.substring(5, clean.length() - 1);
		else if (clean.startsWith("*Aio-"))
			clean = clean.substring(5);
		
		// Prefijo del properties (con o sin espacios raros al borde)
		String prefix = Config.AIO_NAME_PREFIX;
		if (prefix != null)
		{
			prefix = prefix.trim();
			if (!prefix.isEmpty() && clean.regionMatches(true, 0, prefix, 0, prefix.length()))
				clean = clean.substring(prefix.length());
		}
		
		// Cualquier variante [AIO] / [ AIO ] / [AIO ] al inicio
		clean = clean.replaceFirst("(?i)^\\[\\s*AIO\\s*\\]\\s*", "");
		
		return clean;
	}
	
	private static void commitNameChange(Player player, String newName)
	{
		if (player == null || newName == null || newName.equals(player.getName()))
			return;
		
		player.setName(newName);
		PlayerInfoTable.getInstance().updatePlayerData(player, false);
		player.broadcastUserInfo();
		player.store();
		if (player.getClan() != null)
			player.getClan().broadcastClanStatus();
	}
	
	/** Aplica AioNamePrefix al nick si ChangeAioName esta activo. */
	public static void applyAioName(Player player)
	{
		if (player == null)
			return;
		
		final String clean = stripAioNamePrefix(player.getName());
		if (!Config.CHANGE_AIO_NAME || Config.AIO_NAME_PREFIX == null || Config.AIO_NAME_PREFIX.trim().isEmpty())
		{
			commitNameChange(player, clean);
			return;
		}
		
		commitNameChange(player, Config.AIO_NAME_PREFIX + clean);
	}
	
	/** Quita cualquier prefijo AIO del nick. */
	public static void removeAioName(Player player)
	{
		if (player == null)
			return;
		
		commitNameChange(player, stripAioNamePrefix(player.getName()));
	}
	  
   public void updateChar(Player player)
   {
       player.sendPacket(new InventoryUpdate());
       player.broadcastUserInfo();
       player.broadcastCharInfo();
       player.sendSkillList();
       player.store();
   }

   @Override
   public String[] getAdminCommandList()
   {
       return _adminCommands;
   }
}