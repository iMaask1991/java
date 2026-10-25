package net.sf.l2j.gameserver.pvpfarm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;

/**
 * Programador del PvP Farm (mismo mecanismo que {@link net.sf.l2j.gameserver.partyfarm.InitialPartyFarm}): calcula el proximo horario de {@link Config#PVPFARM_START_TIME} y lanza el evento.
 */
public class InitialPvpFarm
{
	private static InitialPvpFarm _instance;
	protected static final Logger _log = Logger.getLogger(InitialPvpFarm.class.getName());
	private Calendar NextEvent;
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public static InitialPvpFarm getInstance()
	{
		if (_instance == null)
			_instance = new InitialPvpFarm();
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
			
			for (String timeOfDay : Config.PVPFARM_START_TIME)
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
			
			_log.info(Config.PVPFARM_NAME + ": Next event: " + NextEvent.getTime());
			ThreadPool.schedule(new StartEventTask(), flush2);
		}
		catch (Exception e)
		{
			_log.warning(Config.PVPFARM_NAME + ": Error reading event schedule. " + e.getMessage());
		}
	}
	
	class StartEventTask implements Runnable
	{
		@Override
		public void run()
		{
			_log.info(Config.PVPFARM_NAME + ": Event started.");
			PvpFarm.startEvent();
		}
	}
}
