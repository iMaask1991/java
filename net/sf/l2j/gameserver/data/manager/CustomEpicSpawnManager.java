package net.sf.l2j.gameserver.data.manager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.CustomEpicSpawnHolder;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;

/**
 * Spawns grand/epic bosses at fixed Loc + DaysAndHours (CustomEpic_N_*).
 * Retail GrandBoss AI scripts must remain disabled while this manager is enabled.
 */
public class CustomEpicSpawnManager
{
	private static final CLogger LOGGER = new CLogger(CustomEpicSpawnManager.class.getName());
	
	private static final int ALIVE = 0;
	private static final int DEAD = 1;
	
	private final Map<Integer, CustomEpicSpawnHolder> _holders = new HashMap<>();
	private final Map<Integer, ScheduledFuture<?>> _tasks = new ConcurrentHashMap<>();
	private final Map<Integer, Npc> _alive = new ConcurrentHashMap<>();
	private final Map<Integer, Long> _nextSpawnAt = new ConcurrentHashMap<>();
	
	protected CustomEpicSpawnManager()
	{
		if (!Config.CUSTOM_EPIC_SPAWN_ENABLED)
		{
			LOGGER.info("CustomEpic spawn disabled.");
			return;
		}
		
		for (CustomEpicSpawnHolder holder : Config.CUSTOM_EPIC_SPAWNS)
			_holders.put(holder.getNpcId(), holder);
		
		for (CustomEpicSpawnHolder holder : _holders.values())
			scheduleNext(holder, true);
		
		LOGGER.info("Loaded {} custom epic spawn(s).", _holders.size());
	}
	
	public static CustomEpicSpawnManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * @return true if this manager handled the death of its tracked instance.
	 */
	public boolean onDeath(Monster boss)
	{
		final CustomEpicSpawnHolder holder = _holders.get(boss.getNpcId());
		if (holder == null)
			return false;
		
		final Npc tracked = _alive.get(boss.getNpcId());
		if (tracked == null || tracked.getObjectId() != boss.getObjectId())
			return false;
		
		_alive.remove(boss.getNpcId());
		
		final Calendar nextSpawn = holder.getNextSpawnTime();
		syncGrandBossDead(boss.getNpcId(), nextSpawn != null ? nextSpawn.getTimeInMillis() : 0L);
		scheduleNext(holder, false);
		return true;
	}
	
	public boolean isCustomEpic(int npcId)
	{
		return _holders.containsKey(npcId);
	}
	
	/**
	 * Next scheduled spawn from DaysAndHours. 0 if the boss is currently alive.
	 */
	public long getNextSpawnMillis(int npcId)
	{
		if (_alive.containsKey(npcId))
			return 0L;
		return _nextSpawnAt.getOrDefault(npcId, 0L);
	}
	
	private void scheduleNext(CustomEpicSpawnHolder holder, boolean onStartup)
	{
		cancelTask(holder.getNpcId());
		
		final Calendar nextSpawn = holder.getNextSpawnTime();
		if (nextSpawn == null)
		{
			LOGGER.warn("Custom epic {} has no valid schedule.", holder.getNpcId());
			return;
		}
		
		final long nextMillis = nextSpawn.getTimeInMillis();
		final long delay = Math.max(1000L, nextMillis - System.currentTimeMillis());
		final NpcTemplate template = NpcData.getInstance().getTemplate(holder.getNpcId());
		final String bossName = (template != null) ? template.getName() : String.valueOf(holder.getNpcId());
		
		_nextSpawnAt.put(holder.getNpcId(), nextMillis);
		syncGrandBossDead(holder.getNpcId(), nextMillis);
		
		if (holder.isAnnounce())
			BossAnnounceManager.getInstance().scheduleSpawnWarnings(holder.getNpcId(), bossName, nextMillis, true);
		
		_tasks.put(holder.getNpcId(), ThreadPool.schedule(() -> spawnEpic(holder), delay));
		
		LOGGER.info("Custom epic {} scheduled for {}{}.", bossName, new SimpleDateFormat("dd-MM-yyyy HH:mm").format(nextSpawn.getTime()), onStartup ? " (startup)" : " (respawn)");
	}
	
	private void spawnEpic(CustomEpicSpawnHolder holder)
	{
		try
		{
			cleanupExisting(holder.getNpcId());
			
			final NpcTemplate template = NpcData.getInstance().getTemplate(holder.getNpcId());
			if (template == null)
			{
				LOGGER.warn("Custom epic template {} missing.", holder.getNpcId());
				return;
			}
			
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(holder.getX(), holder.getY(), holder.getZ(), holder.getHeading());
			spawn.setRespawnState(false);
			
			final Npc npc = spawn.doSpawn(false);
			if (npc == null)
			{
				LOGGER.warn("Failed to spawn custom epic {}.", holder.getNpcId());
				return;
			}
			
			_alive.put(holder.getNpcId(), npc);
			syncGrandBossAlive(npc);
			
			if (holder.isAnnounce())
				World.announceToOnlinePlayers("[Grand Boss] " + npc.getName() + " ha aparecido.");
			
			LOGGER.info("Custom epic {} spawned at {},{},{}.", npc.getName(), holder.getX(), holder.getY(), holder.getZ());
		}
		catch (Exception e)
		{
			LOGGER.error("Error spawning custom epic {}.", e, holder.getNpcId());
		}
	}
	
	private void cleanupExisting(int npcId)
	{
		final Npc existing = _alive.remove(npcId);
		if (existing != null && !existing.isDecayed())
		{
			LOGGER.info("Custom epic {} leftover removed before respawn.", existing.getName());
			existing.deleteMe();
		}
	}
	
	private void syncGrandBossAlive(Npc npc)
	{
		if (!(npc instanceof GrandBoss))
			return;
		
		final GrandBoss boss = (GrandBoss) npc;
		final GrandBossManager gbm = GrandBossManager.getInstance();
		if (gbm.getStatsSet(boss.getNpcId()) == null)
			return;
		
		gbm.addBoss(boss);
		gbm.setBossStatus(boss.getNpcId(), ALIVE);
	}
	
	private void syncGrandBossDead(int npcId, long respawnTime)
	{
		final GrandBossManager gbm = GrandBossManager.getInstance();
		final StatsSet info = gbm.getStatsSet(npcId);
		if (info == null)
			return;
		
		gbm.setBossStatus(npcId, DEAD);
		info.set("respawn_time", respawnTime);
		gbm.setStatsSet(npcId, info);
	}
	
	private void cancelTask(int npcId)
	{
		final ScheduledFuture<?> task = _tasks.remove(npcId);
		if (task != null)
			task.cancel(false);
	}
	
	private static class SingletonHolder
	{
		protected static final CustomEpicSpawnManager INSTANCE = new CustomEpicSpawnManager();
	}
}
