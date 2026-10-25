package net.sf.l2j.gameserver.events.ctf;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author HorridoJoho
 */
public class CTFManager
{
	protected static final Logger _log = Logger.getLogger(CTFManager.class.getName());
	
	/** Task for event cycles<br> */
	private CTFStartTask _task;
	
	/**
	 * New instance only by getInstance()<br>
	 */
	protected CTFManager()
	{
		if (CTFConfig.CTF_EVENT_TEAM_1_NAME != CTFConfig.CTF_EVENT_TEAM_2_NAME)
			CTFEvent.init();
		
		if (CTFConfig.CTF_EVENT_ENABLED)
		{
			if (CTFConfig.CTF_EVENT_TEAM_1_NAME != CTFConfig.CTF_EVENT_TEAM_2_NAME)
			{
				scheduleEventStart();
				_log.info("Capture The Flag Engine: is Started.");
			}
			else
				_log.info("Capture The Flag Engine: is uninitiated. Cannot start if both teams have same name!");
		}
		else
			_log.info("Capture The Flag Engine: is disabled.");
	}
	
	/**
	 * Initialize new/Returns the one and only instance<br>
	 * <br>
	 * @return CTFManager<br>
	 */
	public static CTFManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Reloads CaptureTheFlag.properties and reschedules the next start if the event is idle.
	 */
	public void reloadSchedule()
	{
		CTFConfig.init();
		if (!CTFEvent.isInactive())
		{
			_log.info("CTF Event: config reloaded. New schedule applies after the current event.");
			return;
		}
		cancelPendingStart();
		if (CTFConfig.CTF_EVENT_ENABLED)
		{
			scheduleEventStart();
			_log.info("CTF Event: schedule reloaded.");
		}
		else
			_log.info("CTF Event: disabled after reload.");
	}
	
	private void cancelPendingStart()
	{
		if (_task != null && _task.nextRun != null)
			_task.nextRun.cancel(false);
		_task = null;
	}
	
	/**
	 * Starts CTFStartTask
	 */
	public void scheduleEventStart()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : CTFConfig.CTF_EVENT_INTERVAL)
			{
				// Creating a Calendar object from the specified interval value
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				// If the date is in the past, make it the next day (Example: Checking for "1:00", when the time is 23:57.)
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				// Check for the test date to be the minimum (smallest in the specified list)
				if ((nextStartTime == null) || (testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis()))
				{
					nextStartTime = testStartTime;
				}
			}
			if (nextStartTime != null)
			{
				_task = new CTFStartTask(nextStartTime.getTimeInMillis());
				 ThreadPool.execute(_task);
			}
		}
		catch (Exception e)
		{
			_log.warning("CTFEventEngine[CTFManager.scheduleEventStart()]: Error figuring out a start time. Check CTFEventInterval in config file.");
		}
	}
	
	/**
	 * Method to start participation
	 */
	public void startReg()
	{
		if (!CTFEvent.startParticipation())
		{
			Broadcast.gameAnnounceToOnlinePlayers("CTF Event was cancelled.");
			_log.warning("CTFEventEngine[CTFManager.run()]: Error spawning event npc for participation.");
			
			scheduleEventStart();
		}
		else
		{
			Broadcast.gameAnnounceToOnlinePlayers("CTF Joinable in " + CTFConfig.CTF_NPC_LOC_NAME + "!");

			if (CTFConfig.ALLOW_CTF_COMMANDS)
			{
				Broadcast.gameAnnounceToOnlinePlayers("CTF Command .ctfjoin / .ctfleave / .ctfinfo");
			}
				

	//		if (FakePlayerConfig.ALLOW_FAKE_PLAYER_CTF)
	//			CaptureTheFlagAI.spawnPhantoms();
			
			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + (60000L * CTFConfig.CTF_EVENT_PARTICIPATION_TIME));
			 ThreadPool.execute(_task);
		}
	}
	
	/**
	 * Method to start the fight
	 */
	public void startEvent()
	{
		if (!CTFEvent.startFight())
		{
			Broadcast.gameAnnounceToOnlinePlayers("CTF Event cancelled due to lack of Participation.");
			_log.info("CTFEventEngine[CTFManager.run()]: Lack of registration, abort event.");
			
			scheduleEventStart();
		}
		else
		{
			CTFEvent.sysMsgToAllParticipants("Teleporting in " + CTFConfig.CTF_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + (60000L * CTFConfig.CTF_EVENT_RUNNING_TIME));
			 ThreadPool.execute(_task);
		}
	}
	
	/**
	 * Method to end the event and reward
	 */
	public void endEvent()
	{
		Broadcast.gameAnnounceToOnlinePlayers(CTFEvent.calculateRewards());
		CTFEvent.sysMsgToAllParticipants("Teleporting back town in " + CTFConfig.CTF_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		CTFEvent.stopFight();

	//	if (FakePlayerConfig.ALLOW_FAKE_PLAYER_CTF)
	//		ThreadPoolManager.getInstance().scheduleAi(() -> CaptureTheFlagAI.unspawnPhantoms(), 25 * 1000);

		scheduleEventStart();
	}
	
	public void skipDelay()
	{
		if (_task == null)
		{
			_task = new CTFStartTask(System.currentTimeMillis());
			ThreadPool.execute(_task);
			return;
		}
		
		if (_task.nextRun == null)
		{
			_task.setStartTime(System.currentTimeMillis());
			ThreadPool.execute(_task);
			return;
		}
		
		if (_task.nextRun.cancel(false))
		{
			_task.setStartTime(System.currentTimeMillis());
			ThreadPool.execute(_task);
		}
	}
	
	/**
	 * Class for CTF cycles
	 */
	class CTFStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;
		
		public CTFStartTask(long startTime)
		{
			_startTime = startTime;
		}
		
		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}
		
		@Override
		public void run()
		{
			int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);
			
			if (delay > 0)
			{
				announce(delay);
			}
			
			int nextMsg = 0;
			if (delay > 3600)
			{
				nextMsg = delay - 3600;
			}
			else if (delay > 1800)
			{
				nextMsg = delay - 1800;
			}
			else if (delay > 900)
			{
				nextMsg = delay - 900;
			}
			else if (delay > 600)
			{
				nextMsg = delay - 600;
			}
			else if (delay > 300)
			{
				nextMsg = delay - 300;
			}
			else if (delay > 60)
			{
				nextMsg = delay - 60;
			}
			else if (delay > 5)
			{
				nextMsg = delay - 5;
			}
			else if (delay > 0)
			{
				nextMsg = delay;
			}
			else
			{
				// start
				if (CTFEvent.isInactive())
				{
					startReg();
				}
				else if (CTFEvent.isParticipating())
				{
					startEvent();
				}
				else
				{
					endEvent();
				}
			}
			
			if (delay > 0)
			{
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
			}
		}
		
		private void announce(long time)
		{
			if ((time >= 3600) && ((time % 3600) == 0))
			{
				if (CTFEvent.isParticipating())
				{
					Broadcast.gameAnnounceToOnlinePlayers("CTF " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (CTFEvent.isStarted())
				{
					CTFEvent.sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (CTFEvent.isParticipating())
				{
					Broadcast.gameAnnounceToOnlinePlayers("CTF " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (CTFEvent.isStarted())
				{
					CTFEvent.sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
				}
			}
			else
			{
				if (CTFEvent.isParticipating())
				{
					Broadcast.gameAnnounceToOnlinePlayers("CTF " + time + " second(s) until registration is closed!");
				}
				else if (CTFEvent.isStarted())
				{
					CTFEvent.sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final CTFManager _instance = new CTFManager();
	}
}