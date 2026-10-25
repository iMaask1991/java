package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.type.TimeFarmZone;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class TimeZoneManager
{
	private static final Logger _log = Logger.getLogger(TimeZoneManager.class.getName());
	
	public TimeZoneManager()
	{
		_log.log(Level.INFO, "TimeZoneManager - Loaded.");
	}
	
	public boolean checkPlayersArea(Player activeChar, Integer numberBox, TimeFarmZone zone)
	{
		int count = 0;
		final String hwid = activeChar.getHWID();
		if (hwid == null)
			return false;
		
		for (Player plr : World.getInstance().getPlayers())
		{
			if (plr == null || plr == activeChar || plr.getHWID() == null)
				continue;
			
			if (!hwid.equals(plr.getHWID()))
				continue;
			
			if (!zone.isInsideZone(plr))
				continue;
			
			count++;
		}
		
		if (count >= numberBox)
		{
			activeChar.sendPacket(new CreatureSay(0, Say2.TELL, "SYS", "Allowed only " + numberBox + " client in Time Farm Zone!"));
			
			for (Player gm : AdminData.getInstance().getAllGms(true))
			{
				if (!activeChar.isGM())
					gm.sendPacket(new CreatureSay(0, Say2.TELL, "[Double HWID]", activeChar.getName() + " in Time Farm Zone!"));
			}
			
			randomTeleport(activeChar);
			return true;
		}
		
		return false;
	}
	
	public boolean checkPlayersAreaIp(Player activeChar, Integer numberBox, Collection<Player> world, boolean forcedLogOut)
	{
		if (checkPlayersKickTaskIp(activeChar, numberBox, world))
		{
			if (forcedLogOut)
			{
				activeChar.sendPacket(new CreatureSay(0, Say2.TELL, "SYS", "Double box not allowed in Time Farm Zone!"));
				for (Player gm : AdminData.getInstance().getAllGms(true))
				{
					if (!activeChar.isGM())
						gm.sendPacket(new CreatureSay(0, Say2.TELL, "[Double IP]", activeChar.getName() + " Time Farm Zone!"));
				}
				randomTeleport(activeChar);
			}
			return true;
		}
		return false;
	}
	
	private static boolean checkPlayersKickTaskIp(Player activeChar, Integer numberBox, Collection<Player> world)
	{
		final Map<String, List<Player>> ipMap = new HashMap<>();
		
		for (Player player : world)
		{
			if (!player.isInsideZone(ZoneId.TIME_FARM_ZONE) || player.getHWID() == null || player.getClient().isDetached())
				continue;
			
			final String playerIp = player.getHWID();
			if (IPProtection(playerIp))
			{
				ipMap.computeIfAbsent(playerIp, k -> new ArrayList<>()).add(player);
				if (ipMap.get(playerIp).size() >= numberBox)
					return true;
			}
		}
		return false;
	}
	
	public static synchronized boolean IPProtection(final String ip)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT ip FROM ip_protect_timezone WHERE ip=?"))
		{
			statement.setString(1, ip);
			try (ResultSet rset = statement.executeQuery())
			{
				return rset.next();
			}
		}
		catch (SQLException e)
		{
			_log.warning("TimeZoneManager -> IPProtection: " + e.getMessage());
		}
		return true;
	}
	
	public void randomTeleport(Player activeChar)
	{
		switch (Rnd.get(5))
		{
			case 0:
				activeChar.teleportTo(82533 + Rnd.get(100), 149122 + Rnd.get(100), -3474, 0);
				break;
			case 1:
				activeChar.teleportTo(82571 + Rnd.get(100), 148060 + Rnd.get(100), -3467, 0);
				break;
			case 2:
				activeChar.teleportTo(81376 + Rnd.get(100), 148042 + Rnd.get(100), -3474, 0);
				break;
			case 3:
				activeChar.teleportTo(81359 + Rnd.get(100), 149218 + Rnd.get(100), -3474, 0);
				break;
			default:
				activeChar.teleportTo(82862 + Rnd.get(100), 148606 + Rnd.get(100), -3474, 0);
				break;
		}
	}
	
	public static TimeZoneManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TimeZoneManager INSTANCE = new TimeZoneManager();
	}
}
