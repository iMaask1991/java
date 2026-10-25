package net.sf.l2j.gameserver.communitybbs;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ArenaEvent;
import net.sf.l2j.gameserver.partyfarm.InitialPartyFarm;
import net.sf.l2j.gameserver.partyfarm.PartyFarm;
import net.sf.l2j.gameserver.pvpfarm.InitialPvpFarm;
import net.sf.l2j.gameserver.pvpfarm.PvpFarm;
import net.sf.l2j.gameserver.events.bossevent.KTBConfig;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.bossevent.KTBManager;
import net.sf.l2j.gameserver.events.ctf.CTFConfig;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMConfig;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMManager;
import net.sf.l2j.gameserver.events.lastman.LMConfig;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.lastman.LMManager;
import net.sf.l2j.gameserver.events.rewardsoloevent.InitialSoloEvent;
import net.sf.l2j.gameserver.events.rewardsoloevent.RewardSoloEvent;
import net.sf.l2j.gameserver.events.soloboss.InitialSoloBossEvent;
import net.sf.l2j.gameserver.events.soloboss.SoloBoss;
import net.sf.l2j.gameserver.events.teamvsteam.TvTConfig;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.tournament.ArenaTask;

/**
 * Builds event schedule/status rows for the Community Board events page.
 */
public final class HomeBoardEventInfo
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final SimpleDateFormat NEXT_TIME_FORMAT = new SimpleDateFormat("HH:mm");
	
	private static final String COLOR_ACTIVE = "00FF00";
	private static final String COLOR_REG = "FFFF00";
	private static final String COLOR_NEXT = "CDB67F";
	private static final String COLOR_DISABLED = "696969";
	
	private HomeBoardEventInfo()
	{
	}
	
	public static String applyPlaceholders(String html)
	{
		if (html == null || html.isEmpty())
			return html;
		
		if (html.contains("%event_list%"))
			html = html.replace("%event_list%", buildEventList());
		
		if (html.contains("%server_date%") || html.contains("%server_time%"))
		{
			final Calendar now = Calendar.getInstance();
			if (html.contains("%server_date%"))
				html = html.replace("%server_date%", DATE_FORMAT.format(now.getTime()));
			if (html.contains("%server_time%"))
				html = html.replace("%server_time%", TIME_FORMAT.format(now.getTime()));
		}
		
		return html;
	}
	
	public static String buildEventList()
	{
		final StringBuilder sb = new StringBuilder(2048);
		int index = 0;
		
		sb.append("<table width=500 cellpadding=2 cellspacing=1>");
		sb.append("<tr>");
		sb.append("<td width=320 align=left bgcolor=333333><font color=FFFFFF>Events</font></td>");
		sb.append("<td width=180 align=center bgcolor=333333><font color=FFFFFF>Status / Next</font></td>");
		sb.append("</tr>");
		
		index = appendRow(sb, index, "TvT", TvTConfig.TVT_EVENT_ENABLED, TvTEvent.isStarted(), TvTEvent.isParticipating(), TvTEvent.isStarting(), computeNextTime(TvTConfig.TVT_EVENT_INTERVAL));
		
		index = appendRow(sb, index, "CTF", CTFConfig.CTF_EVENT_ENABLED, CTFEvent.isStarted(), CTFEvent.isParticipating(), CTFEvent.isStarting(), computeNextTime(CTFConfig.CTF_EVENT_INTERVAL));
		index = appendRow(sb, index, "Death Match", DMConfig.DM_EVENT_ENABLED, DMEvent.isStarted(), DMEvent.isParticipating(), DMEvent.isStarting(), safeNextTime(DMManager.getInstance().getNextTime()));
		index = appendRow(sb, index, "Last Man", LMConfig.LM_EVENT_ENABLED, LMEvent.isStarted(), LMEvent.isParticipating(), LMEvent.isStarting(), safeNextTime(LMManager.getInstance().getNextTime()));
		index = appendRow(sb, index, "Kill The Boss", KTBConfig.KTB_EVENT_ENABLED, KTBEvent.isStarted(), KTBEvent.isParticipating(), KTBEvent.isStarting(), safeNextTime(KTBManager.getInstance().getNextTime()));
		index = appendRow(sb, index, "Party Farm", Config.PARTY_FARM_EVENT_ENABLED, PartyFarm.is_started(), false, false, safeEventNextTime(() -> InitialPartyFarm.getInstance().getNextTime(), Config.EVENT_BEST_FARM_INTERVAL_BY_TIME_OF_DAY));
		index = appendRow(sb, index, "PvP Farm", Config.PVPFARM_EVENT_ENABLED, PvpFarm.is_started(), false, false, safeEventNextTime(() -> InitialPvpFarm.getInstance().getNextTime(), Config.PVPFARM_START_TIME));
		index = appendRow(sb, index, "Tournament", Config.TOURNAMENT_EVENT_TIME || Config.TOURNAMENT_EVENT_START, ArenaTask.is_started(), false, false, safeEventNextTime(() -> ArenaEvent.getInstance().getNextTime(), Config.TOURNAMENT_EVENT_INTERVAL_BY_TIME_OF_DAY));
		index = appendRow(sb, index, "Solo Boss", Config.SOLO_BOSS_EVENT, SoloBoss.is_started(), false, false, safeEventNextTime(() -> InitialSoloBossEvent.getInstance().getNextTime(), Config.SOLO_BOSS_EVENT_INTERVAL_BY_TIME_OF_DAY));
		appendRow(sb, index, "Happy Hour", Config.SOLO_FARM_BY_TIME_OF_DAY, RewardSoloEvent.is_started(), false, false, safeEventNextTime(() -> InitialSoloEvent.getInstance().getRestartNextTime(), Config.EVENT_SOLO_FARM_INTERVAL_BY_TIME_OF_DAY));
		
		sb.append("</table>");
		
		return sb.toString();
	}
	
	private static int appendRow(StringBuilder sb, int index, String name, boolean enabled, boolean started, boolean participating, boolean starting, String nextTime)
	{
		final String rowBg = (index % 2 == 0) ? "111111" : "0A0A0A";
		sb.append("<tr>");
		sb.append("<td width=320 bgcolor=").append(rowBg).append("><font color=EEE8AA>").append(name).append("</font></td>");
		sb.append("<td width=180 align=center bgcolor=").append(rowBg).append("><font color=").append(getStatusColor(enabled, started, participating, starting)).append(">").append(getStatusText(enabled, started, participating, starting, nextTime)).append("</font></td>");
		sb.append("</tr>");
		return index + 1;
	}
	
	private static String getStatusText(boolean enabled, boolean started, boolean participating, boolean starting, String nextTime)
	{
		if (!enabled)
			return "Off";
		
		if (started)
			return "Live";
		
		if (participating || starting)
			return "Register";
		
		return nextTime;
	}
	
	private static String getStatusColor(boolean enabled, boolean started, boolean participating, boolean starting)
	{
		if (!enabled)
			return COLOR_DISABLED;
		
		if (started)
			return COLOR_ACTIVE;
		
		if (participating || starting)
			return COLOR_REG;
		
		return COLOR_NEXT;
	}
	
	private static String safeNextTime(String nextTime)
	{
		return (nextTime == null || nextTime.isEmpty() || "Erro".equalsIgnoreCase(nextTime)) ? "N/A" : nextTime;
	}
	
	private static String safeEventNextTime(java.util.function.Supplier<String> supplier, String[] intervals)
	{
		try
		{
			final String nextTime = safeNextTime(supplier.get());
			if (!"N/A".equals(nextTime))
				return nextTime;
		}
		catch (Exception e)
		{
			// Event manager not initialized yet.
		}
		
		return computeNextTime(intervals);
	}
	
	private static String computeNextTime(String[] intervals)
	{
		if (intervals == null || intervals.length == 0)
			return "N/A";
		
		try
		{
			final Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			
			for (String timeOfDay : intervals)
			{
				final Calendar testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				final String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				testStartTime.set(Calendar.SECOND, 0);
				testStartTime.set(Calendar.MILLISECOND, 0);
				
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				
				if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
					nextStartTime = testStartTime;
			}
			
			return (nextStartTime != null) ? NEXT_TIME_FORMAT.format(nextStartTime.getTime()) : "N/A";
		}
		catch (Exception e)
		{
			return "N/A";
		}
	}
}
