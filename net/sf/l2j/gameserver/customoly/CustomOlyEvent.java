package net.sf.l2j.gameserver.customoly;

import java.util.Calendar;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class CustomOlyEvent
{
	private static final Logger LOGGER = Logger.getLogger(CustomOlyEvent.class.getName());
	
	private L2Spawn _spawn;
	private boolean _sessionOpen;
	private boolean _manual;
	
	public void startScheduler()
	{
		if (!Config.CUSTOM_OLY_ENABLED)
			return;
		
		forceStart();
		scheduleNext();
	}
	
	public void scheduleNext()
	{
		if (!Config.CUSTOM_OLY_ENABLED || Config.CUSTOM_OLY_SPAWN_TIMES == null || Config.CUSTOM_OLY_SPAWN_TIMES.length == 0)
			return;
		
		try
		{
			Calendar now = Calendar.getInstance();
			Calendar next = null;
			long best = Long.MAX_VALUE;
			for (String timeOfDay : Config.CUSTOM_OLY_SPAWN_TIMES)
			{
				Calendar test = Calendar.getInstance();
				String[] split = timeOfDay.trim().split(":");
				test.set(Calendar.HOUR_OF_DAY, Integer.parseInt(split[0]));
				test.set(Calendar.MINUTE, Integer.parseInt(split[1]));
				test.set(Calendar.SECOND, 0);
				if (test.getTimeInMillis() <= now.getTimeInMillis())
					test.add(Calendar.DAY_OF_MONTH, 1);
				long diff = test.getTimeInMillis() - now.getTimeInMillis();
				if (diff < best)
				{
					best = diff;
					next = test;
				}
			}
			if (next != null)
			{
				LOGGER.info("Custom Olympiad: next NPC window " + next.getTime());
				ThreadPool.schedule(new Runnable()
				{
					@Override
					public void run()
					{
						startSession();
					}
				}, best);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Custom Olympiad: scheduler " + e);
		}
	}
	
	public synchronized void startSession()
	{
		if (!Config.CUSTOM_OLY_ENABLED)
			return;
		if (CustomOlyManager.getInstance().getPhase() != CustomOlyPhase.COMPETITION)
		{
			if (!_manual)
				scheduleNext();
			return;
		}
		if (_sessionOpen)
			return;
		
		spawnNpc();
		_sessionOpen = true;
		Announcement.AnnounceEvents(Config.CUSTOM_OLY_NAME + " abierto. NPC en Giran. Duracion: " + Config.CUSTOM_OLY_DURATION_MINUTES + " min.");
		LOGGER.info("Custom Olympiad: NPC " + Config.CUSTOM_OLY_NPC_ID + " spawned at " + Config.CUSTOM_OLY_NPC_X + "," + Config.CUSTOM_OLY_NPC_Y + "," + Config.CUSTOM_OLY_NPC_Z);
		
		final boolean reschedule = !_manual;
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				stopSession(reschedule);
			}
		}, Config.CUSTOM_OLY_DURATION_MINUTES * 60 * 1000L);
	}
	
	public synchronized void stopSession(boolean reschedule)
	{
		if (!_sessionOpen && _spawn == null)
		{
			if (reschedule && !_manual)
				scheduleNext();
			return;
		}
		_sessionOpen = false;
		unspawnNpc();
		CustomOlyArena.getInstance().clearQueues();
		Announcement.AnnounceEvents(Config.CUSTOM_OLY_NAME + " cerrado.");
		if (reschedule && !_manual)
			scheduleNext();
		_manual = false;
	}
	
	public void forceStart()
	{
		_manual = true;
		startSession();
	}
	
	public void forceStop()
	{
		_manual = true;
		stopSession(false);
	}
	
	public boolean isSessionOpen()
	{
		return _sessionOpen;
	}
	
	private void spawnNpc()
	{
		if (_spawn != null)
			return;
		
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.CUSTOM_OLY_NPC_ID);
		if (tmpl == null)
		{
			LOGGER.warning("Custom Olympiad: NPC id " + Config.CUSTOM_OLY_NPC_ID + " not found in xml.");
			return;
		}
		try
		{
			_spawn = new L2Spawn(tmpl);
			_spawn.setLoc(Config.CUSTOM_OLY_NPC_X, Config.CUSTOM_OLY_NPC_Y, Config.CUSTOM_OLY_NPC_Z, Config.CUSTOM_OLY_NPC_HEADING);
			_spawn.setRespawnDelay(1);
			SpawnTable.getInstance().addSpawn(_spawn, false);
			_spawn.setRespawnState(true);
			_spawn.doSpawn(false);
			_spawn.getNpc().getStatus().setCurrentHp(999999999);
			_spawn.getNpc().decayMe();
			_spawn.getNpc().spawnMe(_spawn.getNpc().getX(), _spawn.getNpc().getY(), _spawn.getNpc().getZ());
			_spawn.getNpc().broadcastPacket(new MagicSkillUse(_spawn.getNpc(), _spawn.getNpc(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			LOGGER.warning("Custom Olympiad: spawn failed " + e);
			e.printStackTrace();
		}
	}
	
	private void unspawnNpc()
	{
		if (_spawn == null)
			return;
		try
		{
			_spawn.getNpc().deleteMe();
			_spawn.setRespawnState(false);
			SpawnTable.getInstance().deleteSpawn(_spawn, true);
		}
		catch (Exception e)
		{
		}
		_spawn = null;
	}
	
	public static CustomOlyEvent getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final CustomOlyEvent INSTANCE = new CustomOlyEvent();
	}
}
