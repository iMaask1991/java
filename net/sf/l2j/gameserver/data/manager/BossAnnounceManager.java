package net.sf.l2j.gameserver.data.manager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;

public class BossAnnounceManager
{
	private static final TimeZone ARGENTINA_TZ = TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
	
	private final ConcurrentHashMap<Integer, Set<Integer>> _announcedThresholds = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, List<ScheduledFuture<?>>> _spawnWarnings = new ConcurrentHashMap<>();
	
	protected BossAnnounceManager()
	{
	}
	
	public void clearAnnouncements(int objectId)
	{
		_announcedThresholds.remove(objectId);
	}
	
	public void checkHpAnnounce(Monster boss)
	{
		if (boss == null || boss.isDead())
			return;
		
		final int npcId = boss.getNpcId();
		final boolean isGrandBoss = boss instanceof GrandBoss;
		
		if (isGrandBoss)
		{
			if (!Config.ANNOUNCE_GRAND_BOSS_HP_IDS.contains(npcId))
				return;
		}
		else if (!Config.ANNOUNCE_RAID_BOSS_HP || !(boss instanceof RaidBoss) || !Config.ANNOUNCE_RAID_BOSS_HP_IDS.contains(npcId))
			return;
		
		final double maxHp = boss.getMaxHp();
		if (maxHp <= 0)
			return;
		
		final int hpPercent = (int) Math.floor(boss.getCurrentHp() * 100.0 / maxHp);
		final Set<Integer> announced = _announcedThresholds.computeIfAbsent(boss.getObjectId(), key -> ConcurrentHashMap.newKeySet());
		
		boolean crossed = false;
		for (int threshold : Config.BOSS_HP_ANNOUNCE_THRESHOLDS)
		{
			if (hpPercent <= threshold && announced.add(threshold))
				crossed = true;
		}
		
		if (crossed)
		{
			final String type = isGrandBoss ? "Grand Boss" : "Raid Boss";
			World.announceToOnlinePlayers("[" + type + "] " + boss.getName() + " tiene " + hpPercent + "% de HP restante.");
		}
	}
	
	public void onBossKill(Monster boss, Player killer)
	{
		final int npcId = boss.getNpcId();
		final boolean isGrandBoss = boss instanceof GrandBoss;
		
		if (isGrandBoss)
		{
			if (!Config.ANNOUNCE_GRAND_BOSS_KILL || !Config.ANNOUNCE_GRAND_BOSS_HP_IDS.contains(npcId))
				return;
		}
		else if (!(boss instanceof RaidBoss) || !Config.ANNOUNCE_RAID_BOSS_KILL || !Config.ANNOUNCE_RAID_BOSS_HP_IDS.contains(npcId))
			return;
		
		final String killerName = (killer != null) ? killer.getName() : "Unknown";
		final String type = isGrandBoss ? "Grand Boss" : "Raid Boss";
		World.announceToOnlinePlayers("[" + type + "] " + boss.getName() + " fue derrotado por " + killerName + ".");
		
		clearAnnouncements(boss.getObjectId());
	}
	
	public void cancelSpawnWarnings(int npcId)
	{
		final List<ScheduledFuture<?>> tasks = _spawnWarnings.remove(npcId);
		if (tasks == null)
			return;
		
		for (ScheduledFuture<?> task : tasks)
		{
			if (task != null)
				task.cancel(false);
		}
	}
	
	public void scheduleSpawnWarnings(int npcId, String bossName, long spawnTimeMillis, boolean grandBoss)
	{
		cancelSpawnWarnings(npcId);
		
		if (!shouldAnnounceBoss(npcId, grandBoss))
			return;
		
		if (grandBoss)
		{
			if (!Config.ANNOUNCE_GRAND_BOSS_SPAWN_WARNING)
				return;
		}
		else if (!Config.ANNOUNCE_RAID_BOSS_SPAWN_WARNING)
			return;
		
		final List<ScheduledFuture<?>> tasks = new ArrayList<>();
		for (int minutes : Config.BOSS_SPAWN_WARNING_MINUTES)
		{
			final long warningTime = spawnTimeMillis - (minutes * 60L * 1000L);
			final long delay = warningTime - System.currentTimeMillis();
			if (delay <= 0)
				continue;
			
			final int warnMinutes = minutes;
			tasks.add(ThreadPool.schedule(() ->
			{
				final String type = grandBoss ? "Epic Boss" : "Raid Boss";
				String message = "[" + type + "] " + bossName + " spawneara en " + warnMinutes + " minuto(s)";
				
				if (Config.BOSS_SPAWN_WARNING_SHOW_TIME)
				{
					final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
					dateFormat.setTimeZone(ARGENTINA_TZ);
					message += " (" + dateFormat.format(spawnTimeMillis) + " ARG)";
				}
				
				message += ".";
				World.announceToOnlinePlayers(message);
			}, delay));
		}
		
		if (!tasks.isEmpty())
			_spawnWarnings.put(npcId, tasks);
	}
	
	public boolean onBossSpawn(int npcId, String bossName)
	{
		if (!shouldAnnounceBoss(npcId, Config.ANNOUNCE_GRAND_BOSS_HP_IDS.contains(npcId)))
			return false;
		
		final String type = Config.ANNOUNCE_GRAND_BOSS_HP_IDS.contains(npcId) ? "Grand Boss" : "Raid Boss";
		World.announceToOnlinePlayers("[" + type + "] " + bossName + " ha aparecido.");
		return true;
	}
	
	public static boolean shouldAnnounceBoss(int npcId, boolean grandBoss)
	{
		return grandBoss ? Config.ANNOUNCE_GRAND_BOSS_HP_IDS.contains(npcId) : Config.ANNOUNCE_RAID_BOSS_HP_IDS.contains(npcId);
	}
	
	public boolean shouldAnnounceBoss(int npcId)
	{
		return Config.ANNOUNCE_GRAND_BOSS_HP_IDS.contains(npcId) || Config.ANNOUNCE_RAID_BOSS_HP_IDS.contains(npcId);
	}
	
	public static BossAnnounceManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BossAnnounceManager INSTANCE = new BossAnnounceManager();
	}
}
