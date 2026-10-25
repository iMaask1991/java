package net.sf.l2j.gameserver.events.lastman;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.l2j.gameserver.events.ctf.CTFConfig;
import net.sf.l2j.gameserver.events.teamvsteam.TvTConfig;

public class CheckNextEvent 
{
	private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
	
	public String getNextLMTime()
	{
		if (getNextLMEventTime().getTime() != null)
			return format.format(getNextLMEventTime().getTime());
		
		return "Erro";
	}
	
	public Calendar getNextLMEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : LMConfig.LM_EVENT_INTERVAL)
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
			
			return nextStartTime;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static CheckNextEvent getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final CheckNextEvent INSTANCE = new CheckNextEvent();
	}

	public String getNextTvTTime()
	{
		if (getNextTvTEventTime().getTime() != null)
			return format.format(getNextTvTEventTime().getTime());
		
		return "Erro";
	}
	public Calendar getNextTvTEventTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : TvTConfig.TVT_EVENT_INTERVAL)
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
			
			return nextStartTime;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	public String getNextCTFTime()
	{
		if (getNextCTFEventTime().getTime() != null)
			return format.format(getNextCTFEventTime().getTime());
		
		return "Erro";
	}
	public Calendar getNextCTFEventTime()
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
			
			return nextStartTime;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
}