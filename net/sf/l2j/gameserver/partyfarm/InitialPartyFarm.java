package net.sf.l2j.gameserver.partyfarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;

public class InitialPartyFarm
{
	private static InitialPartyFarm _instance;
	protected static final Logger _log = Logger.getLogger(InitialPartyFarm.class.getName());
	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public static InitialPartyFarm getInstance()
	{
		if (_instance == null)
			_instance = new InitialPartyFarm();
		return _instance;
	}
	
	public String getNextTime()
	{
		if (NextEvent != null && NextEvent.getTime() != null)
			return format.format(NextEvent.getTime());
		return "N/A";
	}
	
	public void StartCalculationOfNextEventTime()
	{
		try
		{
			final Calendar currentTime = Calendar.getInstance();
			Calendar testStartTime = null;
			long flush2 = 0L;
			long timeL = 0L;
			int count = 0;
			
			for (String timeOfDay : Config.EVENT_BEST_FARM_INTERVAL_BY_TIME_OF_DAY)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				final String[] splitTimeOfDay = timeOfDay.trim().split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 0);
				
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				
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
			
			_log.info(Config.NAME_EVENT + ": Next event: " + NextEvent.getTime());
			ThreadPool.schedule(new StartEventTask(), flush2);
		}
		catch (Exception e)
		{
			_log.warning(Config.NAME_EVENT + ": Error reading event schedule. " + e.getMessage());
		}
	}
	
	class StartEventTask implements Runnable
	{
		@Override
		public void run()
		{
			_log.info(Config.NAME_EVENT + ": Event started.");
			PartyFarm.startEvent();
		}
	}
}
