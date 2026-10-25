package net.sf.l2j.gameserver.partyfarm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class PartyFarm
{
	public static boolean _started = false;
	public static boolean _aborted = false;
	private static boolean _finish = false;
	
	protected static final List<L2Spawn> monsters = new CopyOnWriteArrayList<>();
	
	public static void startEvent()
	{
		if (Config.ENABLE_SPAWN_PARTYFARM_MONSTERS)
			spawnMonsters();
		
		Announcement.AnnounceEvents(Config.NAME_EVENT + " Teleport Now!");
		Announcement.AnnounceEvents(Config.NAME_EVENT + " Duration: " + Config.EVENT_BEST_FARM_TIME + " minute(s)!");
		
		_aborted = false;
		_started = true;
		_finish = false;
		
		waiter(Config.EVENT_BEST_FARM_TIME * 60 * 1000L);
		
		if (!_aborted)
			finishEvent();
	}
	
	public static void finishEvent()
	{
		if (Config.ENABLE_SPAWN_PARTYFARM_MONSTERS)
			unSpawnMonsters();
		
		_started = false;
		_finish = true;
		
		Announcement.AnnounceEvents(Config.NAME_EVENT + " Finished!");
		
		if (!AdminPartyFarm._partyfarm_manual)
			InitialPartyFarm.getInstance().StartCalculationOfNextEventTime();
		else
			AdminPartyFarm._partyfarm_manual = false;
	}
	
	public static void spawnMonsters()
	{
		if (Config.CUSTOM_SPAWN_PARTYFARM.isEmpty())
			return;
		
		final int numberOfMonsters = Config.AMOUNT_MONSTROS_SPAWN_PT;
		final int spawnRadius = Config.RADIUS_SPAWN_LOC_PTFARM;
		final int minDistance = 400;
		final List<int[]> spawnPoints = Config.CUSTOM_SPAWN_PARTYFARM;
		final List<int[]> usedLocations = new ArrayList<>();
		
		for (int i = 0; i < numberOfMonsters; i++)
		{
			final int spawnPointIndex = (int) (Math.random() * spawnPoints.size());
			final int centerX = spawnPoints.get(spawnPointIndex)[0];
			final int centerY = spawnPoints.get(spawnPointIndex)[1];
			final int centerZ = spawnPoints.get(spawnPointIndex)[2];
			
			boolean validSpawn = false;
			int tryCount = 0;
			final int maxTries = 40;
			
			while (!validSpawn && tryCount < maxTries)
			{
				final double angle = Math.random() * 2 * Math.PI;
				final double distance = Math.sqrt(Math.random()) * spawnRadius;
				
				final int xPos = centerX + (int) (Math.cos(angle) * distance);
				final int yPos = centerY + (int) (Math.sin(angle) * distance);
				final int zPos = GeoEngine.getInstance().getHeight(xPos, yPos, centerZ);
				
				if (Math.abs(zPos - centerZ) > 150)
				{
					tryCount++;
					continue;
				}
				
				boolean tooClose = false;
				for (int[] loc : usedLocations)
				{
					if (Math.hypot(loc[0] - xPos, loc[1] - yPos) < minDistance)
					{
						tooClose = true;
						break;
					}
				}
				
				if (!tooClose)
				{
					final L2Spawn spawn = spawnNPC(xPos, yPos, zPos, Config.monsterId);
					if (spawn != null)
					{
						monsters.add(spawn);
						usedLocations.add(new int[]
						{
							xPos,
							yPos
						});
					}
					validSpawn = true;
				}
				
				tryCount++;
			}
		}
	}
	
	public static boolean is_started()
	{
		return _started;
	}
	
	public static boolean is_finish()
	{
		return _finish;
	}
	
	protected static L2Spawn spawnNPC(int xPos, int yPos, int zPos, int npcId)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
			return null;
		
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(xPos, yPos, zPos, 0);
			spawn.setRespawnDelay(Config.PARTY_FARM_MONSTER_DALAY);
			
			SpawnTable.getInstance().addSpawn(spawn, false);
			
			spawn.setRespawnState(true);
			spawn.doSpawn(false);
			spawn.getNpc().isAggressive();
			spawn.getNpc().decayMe();
			spawn.getNpc().spawnMe(spawn.getNpc().getX(), spawn.getNpc().getY(), spawn.getNpc().getZ());
			spawn.getNpc().broadcastPacket(new MagicSkillUse(spawn.getNpc(), spawn.getNpc(), 1034, 1, 1, 1));
			return spawn;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	protected static void unSpawnMonsters()
	{
		if (monsters.isEmpty())
			return;
		
		final List<L2Spawn> toRemove = new ArrayList<>(monsters);
		for (L2Spawn spawn : toRemove)
		{
			if (spawn == null || spawn.getNpc() == null)
				continue;
			
			spawn.getNpc().deleteMe();
			spawn.setRespawnState(false);
			SpawnTable.getInstance().deleteSpawn(spawn, true);
		}
		monsters.clear();
	}
	
	protected static void waiter(long interval)
	{
		final long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000L);
		
		while ((startWaiterTime + interval > System.currentTimeMillis()) && !_aborted)
		{
			seconds--;
			
			if (_started)
			{
				if (seconds == 3600)
					Announcement.AnnounceEvents(Config.NAME_EVENT + " " + (seconds / 60 / 60) + " hour(s) till event finish!");
				else if (seconds == 60 || seconds == 120 || seconds == 180 || seconds == 240 || seconds == 300 || seconds == 600 || seconds == 900 || seconds == 1800)
					Announcement.AnnounceEvents(Config.NAME_EVENT + " " + (seconds / 60) + " minute(s) till event finish!");
				else if (seconds == 1 || seconds == 2 || seconds == 3 || seconds == 10 || seconds == 15 || seconds == 30)
					Announcement.AnnounceEvents(Config.NAME_EVENT + " " + seconds + " second(s) till event finish!");
			}
			
			try
			{
				Thread.sleep(1000L);
			}
			catch (InterruptedException ie)
			{
			}
		}
	}
}
