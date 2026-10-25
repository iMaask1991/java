package net.sf.l2j.gameserver.pvpfarm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * PvP Farm.<br>
 * <br>
 * Copia del Party Farm (evento por horario + spawn de mobs por coordenadas) con dos diferencias:<br>
 * <ul>
 * <li>El flag PvP se da <b>por rango alrededor de los mobs spawneados</b>, con el mismo patron que Flag_Raids/Flag_Range del {@link net.sf.l2j.gameserver.model.actor.instance.RaidBoss}. Asi el area de PvP se mueve con los bichos y no se desborda a otras zonas (castillo, etc).</li>
 * <li>El reward es <b>individual</b> (ver {@link PvpFarmReward}), no de party.</li>
 * </ul>
 */
public class PvpFarm
{
	public static volatile boolean _started = false;
	public static volatile boolean _aborted = false;
	private static boolean _finish = false;
	
	protected static final List<L2Spawn> monsters = new CopyOnWriteArrayList<>();
	
	private static ScheduledFuture<?> _flagTask;
	
	public static void startEvent()
	{
		if (Config.PVPFARM_SPAWN_MONSTERS)
			spawnMonsters();
		
		Announcement.AnnounceEvents(Config.PVPFARM_NAME + " Teleport Now!");
		Announcement.AnnounceEvents(Config.PVPFARM_NAME + " Duration: " + Config.PVPFARM_EVENT_TIME + " minute(s)!");
		
		_aborted = false;
		_started = true;
		_finish = false;
		
		startFlagTask();
		
		waiter(Config.PVPFARM_EVENT_TIME * 60 * 1000L);
		
		if (!_aborted)
			finishEvent();
	}
	
	public static void finishEvent()
	{
		stopFlagTask();
		
		if (Config.PVPFARM_SPAWN_MONSTERS)
			unSpawnMonsters();
		
		_started = false;
		_finish = true;
		
		Announcement.AnnounceEvents(Config.PVPFARM_NAME + " Finished!");
		
		if (!AdminPvpFarm._pvpfarm_manual)
			InitialPvpFarm.getInstance().StartCalculationOfNextEventTime();
		else
			AdminPvpFarm._pvpfarm_manual = false;
	}
	
	public static void spawnMonsters()
	{
		if (Config.PVPFARM_SPAWN_POINTS.isEmpty() || Config.PVPFARM_MONSTER_IDS.isEmpty())
			return;
		
		final int numberOfMonsters = Config.PVPFARM_SPAWN_AMOUNT;
		final int spawnRadius = Config.PVPFARM_SPAWN_RADIUS;
		final int minDistance = 400;
		final List<int[]> spawnPoints = Config.PVPFARM_SPAWN_POINTS;
		final List<int[]> usedLocations = new ArrayList<>();
		
		for (int i = 0; i < numberOfMonsters; i++)
		{
			final int monsterId = Config.PVPFARM_MONSTER_IDS.get(Rnd.get(Config.PVPFARM_MONSTER_IDS.size()));
			final int spawnPointIndex = Rnd.get(spawnPoints.size());
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
					final L2Spawn spawn = spawnNPC(xPos, yPos, zPos, monsterId);
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
	
	/**
	 * Flag PvP por rango, igual que Flag_Raids / Flag_Range de los raid bosses: cada Config.PVPFARM_FLAG_INTERVAL ms se flaguea a todos los players que estan alrededor de cada mob spawneado.
	 */
	private static void startFlagTask()
	{
		if (!Config.PVPFARM_FLAG_MONSTERS)
			return;
		
		stopFlagTask();
		
		_flagTask = ThreadPool.scheduleAtFixedRate(PvpFarm::flagPlayersAroundMonsters, 1000L, Config.PVPFARM_FLAG_INTERVAL);
	}
	
	private static void flagPlayersAroundMonsters()
	{
		if (monsters.isEmpty())
			return;
		
		for (L2Spawn spawn : monsters)
		{
			if (spawn == null)
				continue;
			
			final Npc npc = spawn.getNpc();
			if (npc == null || npc.isDead())
				continue;
			
			// Flag_MonstersIds: si esta configurado, solo estos mobs flaguean.
			if (!Config.PVPFARM_FLAG_MONSTER_IDS.isEmpty() && !Config.PVPFARM_FLAG_MONSTER_IDS.contains(npc.getNpcId()))
				continue;
			
			for (Player player : npc.getKnownTypeInRadius(Player.class, Config.PVPFARM_FLAG_RANGE))
			{
				if (player == null || player.isDead() || !player.isOnline())
					continue;
				
				player.updatePvPStatus();
			}
		}
	}
	
	private static void stopFlagTask()
	{
		if (_flagTask != null)
		{
			_flagTask.cancel(false);
			_flagTask = null;
		}
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
			spawn.setRespawnDelay(Config.PVPFARM_MONSTER_DELAY);
			
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
					Announcement.AnnounceEvents(Config.PVPFARM_NAME + " " + (seconds / 60 / 60) + " hour(s) till event finish!");
				else if (seconds == 60 || seconds == 120 || seconds == 180 || seconds == 240 || seconds == 300 || seconds == 600 || seconds == 900 || seconds == 1800)
					Announcement.AnnounceEvents(Config.PVPFARM_NAME + " " + (seconds / 60) + " minute(s) till event finish!");
				else if (seconds == 1 || seconds == 2 || seconds == 3 || seconds == 10 || seconds == 15 || seconds == 30)
					Announcement.AnnounceEvents(Config.PVPFARM_NAME + " " + seconds + " second(s) till event finish!");
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
