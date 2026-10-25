package net.sf.l2j.gameserver.model.spawn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.CommunityBoardConfig;
import net.sf.l2j.gameserver.data.manager.BossAnnounceManager;
import net.sf.l2j.gameserver.data.manager.RaidBossInfoManager;
import net.sf.l2j.gameserver.enums.BossStatus;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;

/**
 * A data holder keeping informations related to the {@link L2Spawn} of a RaidBoss.
 */
public class BossSpawn
{
	protected static final CLogger LOGGER = new CLogger(BossSpawn.class.getName());
	
	private static final String DELETE_RAIDBOSS = "DELETE FROM raidboss_spawnlist WHERE boss_id=?";
	private static final String UPDATE_RAIDBOSS = "UPDATE raidboss_spawnlist SET respawn_time = ?, currentHP = ?, currentMP = ? WHERE boss_id = ?";
	
	private L2Spawn _spawn;
	private BossStatus _status = BossStatus.UNDEFINED;
	
	private ScheduledFuture<?> _task;
	
	private double _currentHp;
	private double _currentMp;
	
	private long _respawnTime;
	
	public L2Spawn getSpawn()
	{
		return _spawn;
	}
	
	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}
	
	public BossStatus getStatus()
	{
		return _status;
	}
	
	public void setStatus(BossStatus status)
	{
		_status = status;
	}
	
	public ScheduledFuture<?> getTask()
	{
		return _task;
	}
	
	public void setTask(ScheduledFuture<?> task)
	{
		_task = task;
	}
	
	/**
	 * Cancel the {@link ScheduledFuture} and drop the reference, if any.
	 */
	public void cancelTask()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
	}
	
	public double getCurrentHp()
	{
		return _currentHp;
	}
	
	public void setCurrentHp(double currentHp)
	{
		_currentHp = currentHp;
	}
	
	public double getCurrentMp()
	{
		return _currentMp;
	}
	
	public void setCurrentMp(double currentMp)
	{
		_currentMp = currentMp;
	}
	
	public long getRespawnTime()
	{
		return _respawnTime;
	}
	
	public void setRespawnTime(long respawnTime)
	{
		_respawnTime = respawnTime;
	}
	
	public Npc getBoss()
	{
		return _spawn.getNpc();
	}
	
	/**
	 * A method called upon {@link Npc} death.
	 */
	public void onDeath()
	{
		final int respawnDelay = _spawn.getRespawnMinDelay() + Rnd.get(-_spawn.getRespawnMaxDelay(), _spawn.getRespawnMaxDelay());
		final long respawnTime = System.currentTimeMillis() + (respawnDelay * 3600000L);
		
		_status = BossStatus.DEAD;
		_currentHp = 0;
		_currentMp = 0;
		_respawnTime = respawnTime;
		
		cancelTask();
		_task = ThreadPool.schedule(this::onSpawn, respawnDelay * 3600000L);
		updateOnDb();
		
		if (CommunityBoardConfig.isTrackedBoss(_spawn.getNpcId()))
			RaidBossInfoManager.getInstance().updateRaidBossInfo(_spawn.getNpcId(), respawnTime);
		
		final Npc deadNpc = _spawn.getNpc();
		final String bossName = deadNpc != null ? deadNpc.getName() : String.valueOf(_spawn.getNpcId());
		BossAnnounceManager.getInstance().scheduleSpawnWarnings(_spawn.getNpcId(), bossName, respawnTime, false);
		
		LOGGER.info("Raid boss: {} - {} ({}h).", _spawn.getNpc().getName(), new SimpleDateFormat("dd-MM-yyyy HH:mm").format(respawnTime), respawnDelay);
	}
	
	/**
	 * A method called upon {@link Npc} spawn.
	 */
	public void onSpawn()
	{
		final Npc npc = _spawn.doSpawn(false);
		
		_status = BossStatus.ALIVE;
		_currentHp = npc.getMaxHp();
		_currentMp = npc.getMaxMp();
		_respawnTime = 0L;
		
		cancelTask();
		updateOnDb();
		
		if (CommunityBoardConfig.isTrackedBoss(npc.getNpcId()))
			RaidBossInfoManager.getInstance().updateRaidBossInfo(npc.getNpcId(), 0);
		
		LOGGER.info("{} raid boss has spawned.", npc.getName());
		
		BossAnnounceManager.getInstance().cancelSpawnWarnings(npc.getNpcId());
		
		if (BossAnnounceManager.getInstance().onBossSpawn(npc.getNpcId(), npc.getName()))
			return;
		
		World.announceToOnlinePlayers("[Server] " + npc.getName() + " Spawneo!.");
	}
	
	/**
	 * A method called upon {@link Npc} despawn.
	 */
	public void onDespawn()
	{
		cancelTask();
		BossAnnounceManager.getInstance().cancelSpawnWarnings(_spawn.getNpcId());
		
		final Npc npc = _spawn.getNpc();
		if (npc != null && !npc.isDecayed())
			npc.deleteMe();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_RAIDBOSS))
		{
			ps.setInt(1, _spawn.getNpcId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't delete raid boss #{}.", e, _spawn.getNpcId());
		}
	}
	
	private void updateOnDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_RAIDBOSS))
		{
			ps.setLong(1, _respawnTime);
			ps.setDouble(2, _currentHp);
			ps.setDouble(3, _currentMp);
			ps.setInt(4, _spawn.getNpcId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't update raid boss #{}.", e, _spawn.getNpcId());
		}
	}
}
