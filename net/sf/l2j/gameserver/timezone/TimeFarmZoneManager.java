package net.sf.l2j.gameserver.timezone;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.zone.type.TimeFarmZone;
import net.sf.l2j.gameserver.util.Broadcast;

public class TimeFarmZoneManager
{
	private static final CLogger LOGGER = new CLogger(TimeFarmZoneManager.class.getName());
	
	private static final String SELECT_STATE = "SELECT zone_name FROM timefarm_zone_state WHERE id=1";
	private static final String UPSERT_STATE = "REPLACE INTO timefarm_zone_state (id, zone_name, updated_at) VALUES (1, ?, ?)";
	
	private static final List<TimeFarmZone> _zones = new ArrayList<>();
	private static final List<L2Spawn> _activeSpawns = new ArrayList<>();
	
	private static TimeFarmZone _currentZone = null;
	private static TimeFarmZone _nextZone = null;
	
	private static ScheduledFuture<?> _rotateTask;
	private static ScheduledFuture<?> _announceTask;
	
	private static long _intervalMs;
	private static long _nextRotateTime;
	private static boolean _tasksStarted = false;
	
	public static void registerZone(TimeFarmZone zone)
	{
		if (zone == null || !zone.isEnabled())
			return;
		
		if (!_zones.contains(zone))
			_zones.add(zone);
	}
	
	public static List<TimeFarmZone> getZones()
	{
		final List<TimeFarmZone> enabledZones = new ArrayList<>();
		for (TimeFarmZone zone : _zones)
		{
			if (zone.isEnabled())
				enabledZones.add(zone);
		}
		return enabledZones;
	}
	
	public static TimeFarmZone getCurrentZone()
	{
		return _currentZone;
	}
	
	public static TimeFarmZone getNextZone()
	{
		return _nextZone;
	}
	
	public static void teleportToCurrentZone(Player player)
	{
		if (_currentZone == null)
		{
			player.sendMessage("There is no active Time Farm Zone at the moment.");
			return;
		}
		
		final List<Location> spawns = _currentZone.getLocs();
		final Location loc = (spawns != null && !spawns.isEmpty()) ? spawns.get(Rnd.get(spawns.size())) : _currentZone.getZoneCenter();
		
		if (player.isDead())
			player.doRevive();
		
		player.teleportTo(loc, 20);
	}
	
	public static void init()
	{
		final List<TimeFarmZone> enabledZones = getZones();
		if (enabledZones.isEmpty())
		{
			LOGGER.warn("TimeFarmZoneManager: No enabled zones to start.");
			return;
		}
		
		final String savedName = loadCurrentZoneName();
		_currentZone = findZoneByName(savedName);
		if (_currentZone == null)
			_currentZone = enabledZones.get(0);
		
		LOGGER.info("TimeFarmZoneManager: Active zone set to {}.", _currentZone.getZoneName());
		spawnCurrentZoneMobs();
		selectNextZone();
		saveCurrentZoneName();
		
		if (Config.TIME_FARM_AUTO_ROTATE)
			startTasks();
		else
			LOGGER.info("TimeFarmZoneManager: Auto-rotate OFF. Use //next_zone to change zone.");
	}
	
	/** Manual advance: //next_zone */
	public static boolean advanceZone()
	{
		final List<TimeFarmZone> enabledZones = getZones();
		if (enabledZones.isEmpty())
			return false;
		
		if (_nextZone == null)
			selectNextZone();
		
		if (_nextZone == null)
			return false;
		
		rotateZone(true);
		selectNextZone();
		
		if (Config.TIME_FARM_AUTO_ROTATE)
		{
			updateIntervalByCurrentZone();
			_nextRotateTime = System.currentTimeMillis() + _intervalMs;
			restartRotateTask();
		}
		return true;
	}
	
	private static void startTasks()
	{
		if (_zones.isEmpty() || _tasksStarted)
			return;
		
		_tasksStarted = true;
		stopTasks();
		updateIntervalByCurrentZone();
		_nextRotateTime = System.currentTimeMillis() + _intervalMs;
		
		_rotateTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			rotateZone(true);
			selectNextZone();
			updateIntervalByCurrentZone();
			_nextRotateTime = System.currentTimeMillis() + _intervalMs;
		}, _intervalMs, _intervalMs);
		
		_announceTask = ThreadPool.scheduleAtFixedRate(TimeFarmZoneManager::announceRemainingTime, 0, 1000L);
	}
	
	private static void restartRotateTask()
	{
		if (!Config.TIME_FARM_AUTO_ROTATE)
			return;
		
		_tasksStarted = false;
		stopTasks();
		startTasks();
	}
	
	private static void stopTasks()
	{
		if (_rotateTask != null)
		{
			_rotateTask.cancel(false);
			_rotateTask = null;
		}
		
		if (_announceTask != null)
		{
			_announceTask.cancel(false);
			_announceTask = null;
		}
	}
	
	private static void updateIntervalByCurrentZone()
	{
		if (_currentZone == null)
			return;
		
		final TimeFarmZoneData.ZoneInfo info = TimeFarmZoneData.getZoneInfo(_currentZone.getZoneName());
		_intervalMs = (info != null) ? info.intervalMinutes * 60L * 1000L : 10L * 60L * 1000L;
	}
	
	private static void rotateZone(boolean announce)
	{
		if (_nextZone == null || getZones().isEmpty())
			return;
		
		despawnAllMobs();
		_currentZone = _nextZone;
		saveCurrentZoneName();
		
		final int spawned = spawnCurrentZoneMobs();
		if (announce)
		{
			if (spawned <= 0)
				Broadcast.gameAnnounceToOnlinePlayers("Farm Zone Moved to " + _currentZone.getZoneName() + " (no mobs configured).");
			else
				Broadcast.gameAnnounceToOnlinePlayers("Farm Zone Moved to " + _currentZone.getZoneName() + ".");
		}
		
		teleportPlayersInFarmZone();
		LOGGER.info("TimeFarmZoneManager: Active zone now {} ({} mobs).", _currentZone.getZoneName(), spawned);
		
		final TimeFarmZoneData.ZoneInfo zoneInfo = TimeFarmZoneData.getZoneInfo(_currentZone.getZoneName());
		if (zoneInfo != null)
		{
			_nextRotateTime = System.currentTimeMillis() + (zoneInfo.intervalMinutes * 60L * 1000L);
			_intervalMs = zoneInfo.intervalMinutes * 60L * 1000L;
		}
	}
	
	private static int spawnCurrentZoneMobs()
	{
		if (_currentZone == null)
			return 0;
		
		final TimeFarmZoneData.ZoneInfo zoneInfo = TimeFarmZoneData.getZoneInfo(_currentZone.getZoneName());
		if (zoneInfo == null || zoneInfo.spawns.isEmpty())
		{
			LOGGER.warn("TimeFarmZoneManager: No mob spawns defined for {}.", _currentZone.getZoneName());
			return 0;
		}
		
		int count = 0;
		for (TimeFarmZoneData.MobSpawn spawn : zoneInfo.spawns)
		{
			final L2Spawn l2spawn = spawnMob(spawn.mobId, spawn.loc);
			if (l2spawn != null)
			{
				_activeSpawns.add(l2spawn);
				count++;
			}
		}
		return count;
	}
	
	private static void teleportPlayersInFarmZone()
	{
		if (_currentZone == null)
			return;
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player == null || !player.isOnline())
				continue;
			
			if (player.isInsideZone(ZoneId.TIME_FARM_ZONE))
				teleportToCurrentZone(player);
		}
	}
	
	private static void selectNextZone()
	{
		final List<TimeFarmZone> enabledZones = getZones();
		if (enabledZones.isEmpty())
		{
			_nextZone = null;
			return;
		}
		
		if (enabledZones.size() == 1 || _currentZone == null)
		{
			_nextZone = enabledZones.get(0);
			return;
		}
		
		int index = enabledZones.indexOf(_currentZone);
		if (index < 0)
			index = 0;
		
		_nextZone = enabledZones.get((index + 1) % enabledZones.size());
	}
	
	private static TimeFarmZone findZoneByName(String name)
	{
		if (name == null || name.isEmpty())
			return null;
		
		for (TimeFarmZone zone : getZones())
		{
			if (name.equalsIgnoreCase(zone.getZoneName()))
				return zone;
		}
		return null;
	}
	
	private static String loadCurrentZoneName()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_STATE);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				final String name = rs.getString("zone_name");
				return name == null ? "" : name;
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("TimeFarmZoneManager: Could not load zone state from SQL (run timefarm_zone_state.sql). {}", e.getMessage());
		}
		return "";
	}
	
	private static void saveCurrentZoneName()
	{
		if (_currentZone == null)
			return;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT_STATE))
		{
			ps.setString(1, _currentZone.getZoneName());
			ps.setLong(2, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warn("TimeFarmZoneManager: Could not save zone state to SQL. {}", e.getMessage());
		}
	}
	
	private static void despawnAllMobs()
	{
		for (L2Spawn spawn : _activeSpawns)
		{
			if (spawn != null && spawn.getNpc() != null)
			{
				spawn.getNpc().deleteMe();
				spawn.setRespawnState(false);
				SpawnTable.getInstance().deleteSpawn(spawn, true);
			}
		}
		_activeSpawns.clear();
	}
	
	public static void despawnExact(int x, int y, int z)
	{
		final Iterator<L2Spawn> it = _activeSpawns.iterator();
		while (it.hasNext())
		{
			final L2Spawn spawn = it.next();
			if (spawn == null)
				continue;
			
			if (spawn.getLocX() != x || spawn.getLocY() != y || spawn.getLocZ() != z)
				continue;
			
			if (spawn.getNpc() != null)
				spawn.getNpc().deleteMe();
			
			spawn.setRespawnState(false);
			SpawnTable.getInstance().deleteSpawn(spawn, true);
			it.remove();
			break;
		}
	}
	
	private static L2Spawn spawnMob(int mobId, Location loc)
	{
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(mobId);
			if (template == null)
				return null;
			
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(loc.getX(), loc.getY(), loc.getZ(), 0);
			spawn.setRespawnDelay(10);
			spawn.setRespawnState(true);
			SpawnTable.getInstance().addSpawn(spawn, false);
			spawn.doSpawn(false);
			return spawn;
		}
		catch (Exception e)
		{
			LOGGER.error("TimeFarmZoneManager: Failed to spawn mob {}.", e, mobId);
		}
		return null;
	}
	
	private static void announceRemainingTime()
	{
		if (_nextZone == null || !Config.TIME_FARM_AUTO_ROTATE)
			return;
		
		final long remainingMs = _nextRotateTime - System.currentTimeMillis();
		if (remainingMs <= 0)
			return;
		
		final int remainingMinutes = (int) (remainingMs / (60 * 1000L));
		final int remainingSeconds = (int) ((remainingMs / 1000L) % 60);
		
		if (remainingMinutes == 5 && remainingSeconds == 0)
			Broadcast.gameAnnounceToOnlinePlayers("Farm Zone Next move to " + _nextZone.getZoneName() + " in 5 minute(s).");
	}
}
