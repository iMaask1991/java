package net.sf.l2j.gameserver.timezone;

import java.util.Iterator;

import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.zone.ZoneType;
import net.sf.l2j.gameserver.model.zone.type.TimeFarmZone;

public class AdminSpawnTimeZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_loc_timezone",
		"admin_loc_delete",
		"admin_next_zone"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equalsIgnoreCase("admin_loc_timezone"))
			return addLoc(activeChar);
		
		if (command.equalsIgnoreCase("admin_loc_delete"))
			return deleteLoc(activeChar);
		
		if (command.equalsIgnoreCase("admin_next_zone"))
			return nextZone(activeChar);
		
		return false;
	}
	
	private static boolean nextZone(Player activeChar)
	{
		final TimeFarmZone before = TimeFarmZoneManager.getCurrentZone();
		if (!TimeFarmZoneManager.advanceZone())
		{
			activeChar.sendMessage("Time Farm: no hay zonas enabled para rotar.");
			return true;
		}
		
		final TimeFarmZone current = TimeFarmZoneManager.getCurrentZone();
		final TimeFarmZone next = TimeFarmZoneManager.getNextZone();
		activeChar.sendMessage("Time Farm zone: " + (before != null ? before.getZoneName() : "?") + " -> " + (current != null ? current.getZoneName() : "?"));
		if (next != null)
			activeChar.sendMessage("Proxima zona: " + next.getZoneName());
		return true;
	}
	
	private static boolean addLoc(Player activeChar)
	{
		for (ZoneType zone : ZoneManager.getInstance().getZones(activeChar))
		{
			if (!(zone instanceof TimeFarmZone))
				continue;
			
			final TimeFarmZone tfz = (TimeFarmZone) zone;
			final String zoneName = tfz.getZoneName();
			final TimeFarmZoneData.ZoneInfo zoneInfo = TimeFarmZoneData.getZoneInfo(zoneName);
			
			if (zoneInfo != null)
			{
				final Location loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				zoneInfo.spawns.add(new TimeFarmZoneData.MobSpawn(0, loc));
				TimeFarmZoneData.save();
				activeChar.sendMessage("Location saved for zone: " + zoneName);
				activeChar.sendMessage("Coordinates: " + loc.getX() + " " + loc.getY() + " " + loc.getZ());
			}
			else
				activeChar.sendMessage("Zone '" + zoneName + "' not found in TimeFarmZones.xml.");
			
			return true;
		}
		
		activeChar.sendMessage("You are not inside a Time Farm Zone.");
		return true;
	}
	
	private static boolean deleteLoc(Player activeChar)
	{
		for (ZoneType zone : ZoneManager.getInstance().getZones(activeChar))
		{
			if (!(zone instanceof TimeFarmZone))
				continue;
			
			final TimeFarmZone tfz = (TimeFarmZone) zone;
			final String zoneName = tfz.getZoneName();
			final TimeFarmZoneData.ZoneInfo zoneInfo = TimeFarmZoneData.getZoneInfo(zoneName);
			
			if (zoneInfo == null)
			{
				activeChar.sendMessage("Zone '" + zoneName + "' not found in TimeFarmZones.xml.");
				return true;
			}
			
			final int[] xyz = resolveExactCoords(activeChar);
			if (xyz == null)
			{
				activeChar.sendMessage("No valid coordinates. Target a farm NPC or stand on the exact spawn.");
				return true;
			}
			
			final int x = xyz[0];
			final int y = xyz[1];
			final int z = xyz[2];
			
			boolean removed = false;
			final Iterator<TimeFarmZoneData.MobSpawn> it = zoneInfo.spawns.iterator();
			while (it.hasNext())
			{
				final TimeFarmZoneData.MobSpawn spawn = it.next();
				if (spawn.loc.getX() == x && spawn.loc.getY() == y && spawn.loc.getZ() == z)
				{
					it.remove();
					removed = true;
					break;
				}
			}
			
			if (!removed)
			{
				activeChar.sendMessage("No spawn with exact coords: " + x + " " + y + " " + z);
				return true;
			}
			
			TimeFarmZoneData.save();
			TimeFarmZoneManager.despawnExact(x, y, z);
			activeChar.sendMessage("Spawn deleted from zone: " + zoneName);
			activeChar.sendMessage("Coordinates: " + x + " " + y + " " + z);
			return true;
		}
		
		activeChar.sendMessage("You are not inside a Time Farm Zone.");
		return true;
	}
	
	/** Target NPC spawn loc if available; otherwise player position. */
	private static int[] resolveExactCoords(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		if (target instanceof Npc)
		{
			final Npc npc = (Npc) target;
			final L2Spawn spawn = npc.getSpawn();
			if (spawn != null)
				return new int[]
				{
					spawn.getLocX(),
					spawn.getLocY(),
					spawn.getLocZ()
				};
			
			return new int[]
			{
				npc.getX(),
				npc.getY(),
				npc.getZ()
			};
		}
		
		return new int[]
		{
			activeChar.getX(),
			activeChar.getY(),
			activeChar.getZ()
		};
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
