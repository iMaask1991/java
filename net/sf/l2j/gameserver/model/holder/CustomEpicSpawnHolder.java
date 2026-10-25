package net.sf.l2j.gameserver.model.holder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Config entry for a custom epic/grandboss scheduled spawn.
 * DaysAndHours accepts Spanish: "Lunes 22:00 hs" or English: "Monday,22:00" (multiple with ;).
 */
public class CustomEpicSpawnHolder
{
	private static final TimeZone TZ = TimeZone.getTimeZone("America/Argentina/Buenos_Aires");
	
	private final int _npcId;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	private final boolean _announce;
	/** Each entry: [dayOfWeek, hour, minute] */
	private final List<int[]> _schedules;
	
	public CustomEpicSpawnHolder(int npcId, int x, int y, int z, int heading, boolean announce, List<int[]> schedules)
	{
		_npcId = npcId;
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
		_announce = announce;
		_schedules = Collections.unmodifiableList(new ArrayList<>(schedules));
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public int getX()
	{
		return _x;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public boolean isAnnounce()
	{
		return _announce;
	}
	
	public List<int[]> getSchedules()
	{
		return _schedules;
	}
	
	/**
	 * Next spawn calendar strictly in the future.
	 */
	public Calendar getNextSpawnTime()
	{
		final Calendar now = Calendar.getInstance(TZ);
		Calendar nearest = null;
		
		for (int[] sch : _schedules)
		{
			final Calendar cal = Calendar.getInstance(TZ);
			cal.set(Calendar.DAY_OF_WEEK, sch[0]);
			cal.set(Calendar.HOUR_OF_DAY, sch[1]);
			cal.set(Calendar.MINUTE, sch[2]);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			if (cal.getTimeInMillis() <= now.getTimeInMillis())
				cal.add(Calendar.DAY_OF_YEAR, 7);
			
			if (nearest == null || cal.getTimeInMillis() < nearest.getTimeInMillis())
				nearest = cal;
		}
		
		return nearest;
	}
	
	/**
	 * Parse DaysAndHours. Supports:
	 * <ul>
	 * <li>Spanish: Lunes 22:00 hs</li>
	 * <li>English retail: Monday,22:00</li>
	 * <li>Multiple entries separated by ;</li>
	 * </ul>
	 */
	public static List<int[]> parseDaysAndHours(String raw)
	{
		final List<int[]> list = new ArrayList<>();
		if (raw == null || raw.trim().isEmpty())
			return list;
		
		String normalized = raw.trim().toLowerCase(Locale.ROOT);
		normalized = normalized.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u");
		normalized = normalized.replace(" hs", "").replace("hs", "").trim();
		
		for (String part : normalized.split(";"))
		{
			part = part.trim();
			if (part.isEmpty())
				continue;
			
			String dayToken;
			String timeToken;
			
			if (part.contains(","))
			{
				final String[] tokens = part.split(",");
				if (tokens.length < 2)
					continue;
				dayToken = tokens[0].trim();
				timeToken = tokens[1].trim();
			}
			else
			{
				final String[] tokens = part.split("\\s+");
				if (tokens.length < 2)
					continue;
				dayToken = tokens[0].trim();
				timeToken = tokens[1].trim();
			}
			
			final int day = dayFromString(dayToken);
			final String[] hm = timeToken.split(":");
			if (hm.length < 2)
				continue;
			
			list.add(new int[]
			{
				day,
				Integer.parseInt(hm[0].trim()),
				Integer.parseInt(hm[1].trim())
			});
		}
		
		return list;
	}
	
	private static int dayFromString(String day)
	{
		switch (day)
		{
			case "sunday":
			case "domingo":
				return Calendar.SUNDAY;
			case "monday":
			case "lunes":
				return Calendar.MONDAY;
			case "tuesday":
			case "martes":
				return Calendar.TUESDAY;
			case "wednesday":
			case "miercoles":
				return Calendar.WEDNESDAY;
			case "thursday":
			case "jueves":
				return Calendar.THURSDAY;
			case "friday":
			case "viernes":
				return Calendar.FRIDAY;
			case "saturday":
			case "sabado":
				return Calendar.SATURDAY;
			default:
				return Calendar.MONDAY;
		}
	}
}
