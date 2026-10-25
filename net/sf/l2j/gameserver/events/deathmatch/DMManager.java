package net.sf.l2j.gameserver.events.deathmatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.util.Broadcast;

public class DMManager
{
	protected static final Logger _log = Logger.getLogger(DMManager.class.getName());

	private DMStartTask _task;

	private DMManager()
	{
		DMEvent.init();
		
		if (DMConfig.DM_EVENT_ENABLED)
		{
			this.scheduleEventStart();
			_log.info("Deathmatch Engine: is Started.");
		}
		else
			_log.info("Deathmatch Engine: Engine is disabled.");
	}

	public static DMManager getInstance()
	{
		return SingletonHolder._instance;
	}

	/**
	 * Reloads DeathMatch.properties and reschedules the next start if the event is idle.
	 */
	public void reloadSchedule()
	{
		DMConfig.init();
		if (!DMEvent.isInactive())
		{
			_log.info("DM Event: config reloaded. New schedule applies after the current event.");
			return;
		}
		cancelPendingStart();
		if (DMConfig.DM_EVENT_ENABLED)
		{
			scheduleEventStart();
			_log.info("DM Event: schedule reloaded.");
		}
		else
			_log.info("DM Event: disabled after reload.");
	}
	
	private void cancelPendingStart()
	{
		if (_task != null && _task.nextRun != null)
			_task.nextRun.cancel(false);
		_task = null;
	}
	
	public void scheduleEventStart()
	{
	    try
	    {
	        if (DMConfig.DM_EVENT_INTERVAL == null || DMConfig.DM_EVENT_INTERVAL.length == 0)
	        {
	            _log.warning("DMEventEngine: DM_EVENT_INTERVAL is null or empty in config.");
	            return;
	        }

	        Calendar currentTime = Calendar.getInstance();
	        Calendar nextStartTime = null;
	        Calendar testStartTime;

	        for (String timeOfDay : DMConfig.DM_EVENT_INTERVAL)
	        {
	            testStartTime = Calendar.getInstance();
	            testStartTime.setLenient(true);
	            String[] splitTimeOfDay = timeOfDay.split(":");
	            testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
	            testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));

	            if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
	                testStartTime.add(Calendar.DAY_OF_MONTH, 1);

	            if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
	                nextStartTime = testStartTime;
	        }

	        if (nextStartTime != null)
	        {
	            _task = new DMStartTask(nextStartTime.getTimeInMillis());
	             ThreadPool.execute(_task);
	        }
	        else
	        {
	            _log.warning("DMEventEngine: No valid start time found. Check DM_EVENT_INTERVAL in config.");
	        }
	    }
	    catch (Exception e)
	    {
	        _log.warning("DMEventEngine: Error figuring out a start time. Check DM_EVENT_INTERVAL in config.");
	    }
	}


	public void restrictionAddEventArea()
	{
		DoorData.getInstance().getDoor(23170002).openMe();
		DoorData.getInstance().getDoor(23170003).openMe();
		DoorData.getInstance().getDoor(23170004).openMe();
		DoorData.getInstance().getDoor(23170005).openMe();
		DoorData.getInstance().getDoor(23170008).openMe();
		DoorData.getInstance().getDoor(23170009).openMe();
		DoorData.getInstance().getDoor(23170010).openMe();
		DoorData.getInstance().getDoor(23170011).openMe();
	}
	
	public void restrictionRemoveEventArea()
	{
		DoorData.getInstance().getDoor(23170002).closeMe();
		DoorData.getInstance().getDoor(23170003).closeMe();
		DoorData.getInstance().getDoor(23170004).closeMe();
		DoorData.getInstance().getDoor(23170005).closeMe();
		DoorData.getInstance().getDoor(23170008).closeMe();
		DoorData.getInstance().getDoor(23170009).closeMe();
		DoorData.getInstance().getDoor(23170010).closeMe();
		DoorData.getInstance().getDoor(23170011).closeMe();
	}
	
	public void startReg()
	{
		if (!DMEvent.startParticipation())
		{
			Broadcast.gameAnnounceToOnlinePlayers("DM Event was cancelled.");
			_log.warning("DMEventEngine: Error spawning event npc for participation.");

			restrictionRemoveEventArea();
			scheduleEventStart();
		}
		else
		{
			Broadcast.gameAnnounceToOnlinePlayers("DM Joinable in " + DMConfig.DM_NPC_LOC_NAME + "!");

			if (DMConfig.ALLOW_DM_COMMANDS)
			{
				Broadcast.gameAnnounceToOnlinePlayers("DM Command .dmjoin / .dmleave / .dminfo");
			}
	
		//	if (FakePlayerConfig.ALLOW_FAKE_PLAYER_DM)
		//		DeathMatchAI.spawnPhantoms();
			
			// schedule registration end
			_task.setStartTime(System.currentTimeMillis() + DMConfig.DM_EVENT_PARTICIPATION_TIME);
			 ThreadPool.execute(_task);
		}
	}

	public void startEvent()
	{
		if (!DMEvent.startFight())
		{
			Broadcast.gameAnnounceToOnlinePlayers("DM Event cancelled due to lack of Participation.");
			_log.info("DMEventEngine: Lack of registration, abort event.");

			scheduleEventStart();
		}
		else
		{
			DMEvent.sysMsgToAllParticipants("Teleporting in " + DMConfig.DM_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
			_task.setStartTime(System.currentTimeMillis() + 60000 * DMConfig.DM_EVENT_RUNNING_TIME);
			 ThreadPool.execute(_task);
		}
	}

	public void endEvent()
	{
		Broadcast.gameAnnounceToOnlinePlayers(DMEvent.calculateRewards());
		DMEvent.sysMsgToAllParticipants("Teleporting back town in " + DMConfig.DM_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
		DMEvent.stopFight();

	//	if (FakePlayerConfig.ALLOW_FAKE_PLAYER_DM)
	//	{
	//		ThreadPoolManager.getInstance().scheduleAi(() -> DeathMatchAI.unspawnPhantoms(), 25 * 1000);
	//	}
		
		scheduleEventStart();
	}

	public void skipDelay()
	{
		if (_task == null)
		{
			_task = new DMStartTask(System.currentTimeMillis());
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

	class DMStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;

		public DMStartTask(long startTime)
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
				this.announce(delay);
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
				if (DMEvent.isInactive())
				{
					DMManager.this.startReg();
				}
				else if (DMEvent.isParticipating())
				{
					DMManager.this.startEvent();
				}
				else
				{
					DMManager.this.endEvent();
				}
			}

			if (delay > 0)
			{
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
			}
		}

		private void announce(long time)
		{
			if (time >= 3600 && time % 3600 == 0)
			{
				if (DMEvent.isParticipating())
				{
					Broadcast.gameAnnounceToOnlinePlayers("DM " + (time / 60 / 60) + " hour(s) until registration is closed!");
				}
				else if (DMEvent.isStarted())
				{
					DMEvent.sysMsgToAllParticipants("" + (time / 60 / 60) + " hour(s) until event is finished!");
				}
			}
			else if (time >= 60)
			{
				if (DMEvent.isParticipating())
				{
					Broadcast.gameAnnounceToOnlinePlayers("DM " + (time / 60) + " minute(s) until registration is closed!");
				}
				else if (DMEvent.isStarted())
				{
					DMEvent.sysMsgToAllParticipants("" + (time / 60) + " minute(s) until the event is finished!");
				}
			}
			else
			{
				if (DMEvent.isParticipating())
				{
					Broadcast.gameAnnounceToOnlinePlayers("DM " + time + " second(s) until registration is closed!");
				}
				else if (DMEvent.isStarted())
				{
					DMEvent.sysMsgToAllParticipants("" + time + " second(s) until the event is finished!");
				}
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final DMManager _instance = new DMManager();
	}
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	public String getNextTime()
	{
		if (getNextEventTime().getTime() != null)
			return format.format(getNextEventTime().getTime());
		return "Erro";
	}
	public Calendar getNextEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : DMConfig.DM_EVENT_INTERVAL)
			{
				// Creating a Calendar object from the specified interval value
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				// If the date is in the past, make it the next day (Example: Checking for "1:00", when the time is 23:57.)
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				// Check for the test date to be the minimum (smallest in the specified list)
				if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
					nextStartTime = testStartTime;
			}
			return nextStartTime;
		}
		catch (Exception e)
		{
			_log.warning("DMEventEngine: Error figuring out a start time. Check DMEventInterval in config file.");
			return null;
		}
	}
}