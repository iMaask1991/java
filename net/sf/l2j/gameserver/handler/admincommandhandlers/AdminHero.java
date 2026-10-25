package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

/**
 * //sethero [&lt;player_name&gt;] &lt;days&gt;
 * //removehero [&lt;player_name&gt;]
 */
public class AdminHero implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_sethero",
		"admin_removehero"
	};
	
	private static final Logger _log = Logger.getLogger(AdminHero.class.getName());
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_sethero"))
		{
			StringTokenizer str = new StringTokenizer(command);
			
			WorldObject target = activeChar.getTarget();
			Player player = (target instanceof Player) ? (Player) target : activeChar;
			
			try
			{
				str.nextToken();
				String time = str.nextToken();
				if (str.hasMoreTokens())
				{
					String playername = time;
					time = str.nextToken();
					player = World.getInstance().getPlayer(playername);
					doHero(activeChar, player, playername, time);
				}
				else
				{
					doHero(activeChar, player, player.getName(), time);
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //sethero <char_name> [time](in days)");
			}
			
			if (player != null)
				player.broadcastUserInfo();
			
			return true;
		}
		else if (command.startsWith("admin_removehero"))
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
					removeHero(activeChar, player, playername);
				}
				else
				{
					removeHero(activeChar, player, player.getName());
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //removehero <char_name>");
			}
			
			if (player != null)
				player.broadcastUserInfo();
			
			return true;
		}
		
		return false;
	}
	
	public void doHero(Player activeChar, Player player, String playername, String time)
	{
		int days = Integer.parseInt(time);
		
		if (player == null)
		{
			activeChar.sendMessage("Character not found.");
			return;
		}
		
		final long now = System.currentTimeMillis();
		if (player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > now))
		{
			activeChar.sendMessage("Player " + playername + " ya tiene Hero activo.");
			return;
		}
		
		if (days <= 0)
			return;
		
		player.setHero(true, true);
		player.setEndTime("hero", days);
		player.broadcastPacket(new SocialAction(player, 16));
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET custom_hero=1, hero_end=? WHERE obj_id=?"))
		{
			ps.setLong(1, player.getHeroEndTime());
			ps.setInt(2, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error al guardar Hero custom", e);
		}
		
		player.broadcastUserInfo();
		player.store();
		activeChar.sendMessage("Hero otorgado a " + playername + " por " + days + " dia(s).");
	}
	
	public void removeHero(Player activeChar, Player player, String playername)
	{
		if (player == null)
		{
			if (activeChar != null)
				activeChar.sendMessage("Character not found.");
			return;
		}
		
		if (!player.isHero() && player.getHeroEndTime() <= 0 && activeChar != null)
		{
			activeChar.sendMessage("Player " + playername + " no tiene Hero.");
			return;
		}
		
		player.setHeroEndTime(0);
		
		final boolean olyHero = HeroManager.getInstance().isActiveHero(player.getObjectId());
		if (!olyHero)
			player.setHero(false, true);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET custom_hero=0, hero_end=0 WHERE obj_id=?"))
		{
			ps.setInt(1, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error al remover Hero", e);
		}
		
		player.broadcastUserInfo();
		player.store();
		
		if (activeChar != null)
			activeChar.sendMessage("Hero removido de " + playername + (olyHero ? " (sigue Hero Oly)." : "."));
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
