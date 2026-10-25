package net.sf.l2j.gameserver.events.rewardsoloevent;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;


public class InitialSoloEvent
{
	private static InitialSoloEvent _instance = null;
	protected static final Logger _log = Logger.getLogger(InitialSoloEvent.class.getName());
	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public static InitialSoloEvent getInstance()
	{
		if (_instance == null)
		{
			_instance = new InitialSoloEvent();
		}
		return _instance;
	}
	
	public String getRestartNextTime()
	{
		if (NextEvent != null && NextEvent.getTime() != null)
			return format.format(NextEvent.getTime());
		
		return "Erro";
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
			for (String timeOfDay : Config.EVENT_SOLO_FARM_INTERVAL_BY_TIME_OF_DAY)
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
			_log.info("[Happy Hour]: Proximo Evento: " + NextEvent.getTime().toString());
			ThreadPool.schedule(new StartEventTask(), flush2);
		}
		catch (Exception e)
		{
			System.out.println("[Happy Hour]: Algum erro nas config foi encontrado!");
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
			InitialSoloEvent._log.info("[Happy Hour]: Event Started.");
			RewardSoloEvent.StartedEvent();
		}
	}
}
