package net.sf.l2j.gameserver.events.soloboss;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;

public class InitialSoloBossEvent
{
	private static InitialSoloBossEvent _instance = null;
	protected static final Logger _log = Logger.getLogger(InitialSoloBossEvent.class.getName());
	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public static InitialSoloBossEvent getInstance()
	{
		if (_instance == null)
		{
			_instance = new InitialSoloBossEvent();
		}
		return _instance;
	}
	
	public String getRestartNextTime()
	{
		if (NextEvent.getTime() != null)
		{
			return format.format(NextEvent.getTime());
		}
		return "Erro";
	}
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
			Calendar testStartTime = null;
			long flush2 = 0, timeL = 0;
			int count = 0;
			Calendar nextEvent = null;
			for (String timeOfDay : Config.SOLO_BOSS_EVENT_INTERVAL_BY_TIME_OF_DAY)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 00);
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
				
				if (count == 0)
				{
					flush2 = timeL;
					nextEvent = testStartTime;
				}
				
				if (timeL < flush2)
				{
					flush2 = timeL;
					nextEvent = testStartTime;
				}
				count++;
			}
			//_log.info("Tournament: Next Event " + nextEvent.getTime().toString());
			return nextEvent;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Solo Boss]: "+e);
			return null;
		}
		
	}

	private InitialSoloBossEvent()
	{
	}
	public void StartCalculationOfNextEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar testStartTime = null;
			long flush2 = 0L;
			long timeL = 0L;
			int count = 0;
			for (String timeOfDay : Config.SOLO_BOSS_EVENT_INTERVAL_BY_TIME_OF_DAY)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(11, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(12, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(13, 0);
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(5, 1);
				}
				timeL = testStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
				if (count == 0)
				{
					flush2 = timeL;
					NextEvent = testStartTime;
				}
				if (timeL < flush2)
				{
					flush2 = timeL;
					NextEvent = testStartTime;
				}
				count++;
			}
			_log.info("[Solo Boss]: Proximo Evento: " + NextEvent.getTime().toString());
			ThreadPool.schedule(new StartEventTask(), flush2);
		}
		catch (Exception e)
		{
			System.out.println("[Solo Boss]: Algum erro nas config foi encontrado!");
		}
	}
	
	class StartEventTask implements Runnable
	{
		StartEventTask()
		{
		}
		
		@Override
		public void run()
		{
			InitialSoloBossEvent._log.info("[Solo Boss]: Event Started.");
			SoloBoss.bossSpawnMonster();
		}
	}
}