package net.sf.l2j.gameserver.handler.admincommandhandlers;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 *  "//setvip" give vip status to player Changes name color and title color if enabled Uses: setvip [<player_name>] [<time_duration in days>]
 *  "//removevip"  Uses: removevip [<player_name>] if <player_name> is not specified, the current target player is used.
 * @author KhayrusS, SweeTs, Reynald0
 */
public class AdminVip implements IAdminCommandHandler
{
   private static String[] _adminCommands =
   {
       "admin_setvip",
       "admin_removevip",
       "admin_showvip",
       "admin_gift"
   };
 
   private static Logger _log = Logger.getLogger(MapRegionData.class.getName());
   
   @Override
   public boolean useAdminCommand(String command, Player activeChar)
   {
       if (command.startsWith("admin_setvip"))
       {
           StringTokenizer str = new StringTokenizer(command);
           
           final WorldObject target = activeChar.getTarget();
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
                   doVip(activeChar, player, playername, time);
               }
               else
               {
                   String playername = player.getName();
                   doVip(activeChar, player, playername, time);
               }
           }
           catch (Exception e)
           {
               activeChar.sendMessage("Usage: //setvip <char_name> [time](in days)");
           }
           
           player.broadcastUserInfo();
           
           if (player.isVip())
               return true;
       }
       else if (command.startsWith("admin_removevip"))
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
                   removeVip(activeChar, player, playername);
               }
               else
               {
                   String playername = player.getName();
                   removeVip(activeChar, player, playername);
               }
           }
           catch (Exception e)
           {
               activeChar.sendMessage("Usage: //removevip <char_name>");
           }
           player.broadcastUserInfo();
       }
       else if (command.startsWith("admin_showvip"))
           showAllVip(activeChar);
       return false;
   }
   
   public void doVip(Player activeChar, Player _player, String _playername, String _time)
   {
       int days = Integer.parseInt(_time);
       
       if (_player == null)
       {
           activeChar.sendMessage("Character not found.");
           return;
       }
       if (_player.isVip())
       {
           activeChar.sendMessage("Player " + _playername + " is already an Vip.");
           return;
       }
       
       if (days > 0)
       {
    	   _player.setVip(true);
           _player.setEndTime("vip", days);
           
           if (Config.ALLOW_VIP_TCOLOR)
        	   _player.getAppearance().setTitleColor(Config.VIP_TCOLOR);
           
           try (Connection con = L2DatabaseFactory.getInstance().getConnection())
           {
               @SuppressWarnings("resource")
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET vip=1, vip_end=? WHERE obj_id=?");
               ps.setLong(1, _player.getVipEndTime());
               ps.setInt(2, _player.getObjectId());
               ps.execute();
               ps.close();

               updateChar(_player);
           }
           catch (Exception e)
           {
               _log.log(Level.WARNING, "Something went wrong, check log folder for details", e);
           }
       }
   }
   
   public void removeVip(Player activeChar, Player _player, String _playername)
   {
       if (!_player.isVip() && activeChar != null)
       {
           activeChar.sendMessage("Player " + _playername + " is not an Vip.");
           return;
       }
       _player.setVipEndTime(0);
       _player.setVip(false);
       
       // Quitar color de title VIP (igual que AIO al expirar)
       if (Config.ALLOW_VIP_TCOLOR)
    	   _player.getAppearance().setTitleColor(0xFFFFFF);
       
       try (Connection con = L2DatabaseFactory.getInstance().getConnection())
       {
           @SuppressWarnings("resource")
		PreparedStatement ps = con.prepareStatement("UPDATE characters SET vip=0, vip_end=0 WHERE obj_id=?");
           ps.setInt(1, _player.getObjectId());
           ps.execute();
           ps.close();
           
           updateChar(_player);
       }
       catch (Exception e)
       {
           _log.log(Level.WARNING, "Something went wrong, check log folder for details", e);
       }
   }
   
   public void updateChar(Player player)
   {
       player.sendPacket(new InventoryUpdate());
       player.broadcastUserInfo();
       player.broadcastCharInfo();
       player.sendSkillList();
       player.store();
       net.sf.l2j.gameserver.vip.VipBar.update(player); // barra VIP de la interface
   }
   
   public void showAllVip(Player activeChar)
   {
       short num = 0;
       NpcHtmlMessage html = new NpcHtmlMessage(0);
       html.setFile("data/html/admin/Vip.htm");
       StringBuilder sb = new StringBuilder();
       for (Player player : World.getInstance().getPlayers())
       {
           if (player.isVip())
           {
               num++;
               sb.append("<tr><td width=10></td>" +
                   "<td width=20>" + num + "</td>" +
                   "<td width=80><font color=\"LEVEL\">" + player.getName() + "</font></td>" +
                   "<td width=110>" + MapRegionData.getInstance().getClosestTownName(player.getX(), player.getY()) + "</td>" +
                   "<td width=50><font color=\"00FF00\">ON</font></td></tr>");
           }
       }
       html.replace("%show_vip%", num > 0 ? sb.toString() : "");
       activeChar.sendPacket(html);
   } 
   
   @Override
   public String[] getAdminCommandList()
   {
       return _adminCommands;
   }
}