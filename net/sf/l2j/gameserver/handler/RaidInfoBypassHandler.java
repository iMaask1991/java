package net.sf.l2j.gameserver.handler;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.communitybbs.CommunityBoardConfig;
import net.sf.l2j.gameserver.communitybbs.HomeBoardBossInfo;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.RaidBossInfoManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * Bypass handler for voiced .raid window (works with Community Board disabled).
 */
public final class RaidInfoBypassHandler
{
	private static final String BYPASS = "raidinfo";
	private static final String HTML_MAIN = "data/html/mods/RaidInfo/main.html";
	private static final String HTML_DROPS = "data/html/mods/RaidInfo/drops.html";
	
	private RaidInfoBypassHandler()
	{
	}
	
	public static boolean isRaidInfoCommand(String command)
	{
		return command != null && (command.equals(BYPASS) || command.startsWith(BYPASS + ";"));
	}
	
	public static boolean handleBypass(String command, Player player)
	{
		if (!isRaidInfoCommand(command) || player == null)
			return false;
		
		show(player, command);
		return true;
	}
	
	public static void show(Player player, String command)
	{
		String cmd = command;
		if (cmd == null || cmd.isEmpty() || cmd.equals(BYPASS))
			cmd = BYPASS + ";easy";
		
		final String[] parts = cmd.split(";");
		if (parts.length >= 2 && "drop".equals(parts[1]))
		{
			final String html = HomeBoardBossInfo.resolveHtml(cmd, BYPASS, HTML_MAIN, null, HTML_DROPS);
			if (html == null || html.isEmpty())
			{
				player.sendMessage("Raid info is unavailable.");
				return;
			}
			BaseBBSManager.separateAndSend(html, player);
			return;
		}
		
		final String tier = parts.length >= 2 ? normalizeTier(parts[1]) : "easy";
		final int page = parts.length >= 3 ? parseInt(parts[2], 1) : 1;
		
		String html = HtmCache.getInstance().getHtm(HTML_MAIN);
		if (html == null || html.isEmpty())
		{
			player.sendMessage("Raid info is unavailable.");
			return;
		}
		
		final List<Integer> ids = collectIds(tier);
		html = html.replace("%raid_tier%", tierLabel(tier));
		html = html.replace("%boss_list%", buildBossList(ids, page, tier));
		html = html.replace("%pagination%", buildPages(ids.size(), page, tier));
		BaseBBSManager.separateAndSend(html, player);
	}
	
	private static List<Integer> collectIds(String tier)
	{
		final List<Integer> source;
		if ("mid".equals(tier))
			source = CommunityBoardConfig.COMMUNITY_RAID_MID_IDS;
		else if ("hard".equals(tier))
			source = CommunityBoardConfig.COMMUNITY_RAID_HARD_IDS;
		else
			source = CommunityBoardConfig.COMMUNITY_RAID_EASY_IDS;
		
		final List<Integer> ids = new ArrayList<>();
		if (source == null)
			return ids;
		
		for (int id : source)
		{
			if (NpcData.getInstance().getTemplate(id) != null)
				ids.add(id);
		}
		return ids;
	}
	
	private static String buildBossList(List<Integer> bossIds, int page, String tier)
	{
		if (bossIds.isEmpty())
			return "<font color=696969>No bosses configured.</font>";
		
		final int perPage = Math.max(1, CommunityBoardConfig.COMMUNITY_RAID_PAGE_LIMIT);
		final int safePage = Math.max(1, page);
		final int from = (safePage - 1) * perPage;
		if (from >= bossIds.size())
			return "<font color=696969>No bosses configured.</font>";
		
		final int to = Math.min(bossIds.size(), from + perPage);
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<table width=500 cellpadding=2 cellspacing=1>");
		
		for (int i = from; i < to; i++)
		{
			final int bossId = bossIds.get(i);
			final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
			if (template == null)
				continue;
			
			final long delay = RaidBossInfoManager.getInstance().getRaidBossRespawnTime(bossId);
			sb.append("<tr>");
			sb.append("<td width=220><font color=EEE8AA>").append(template.getName()).append("</font></td>");
			if (delay <= System.currentTimeMillis())
				sb.append("<td width=90 align=center><font color=ADFF2F>Alive</font></td>");
			else
			{
				final int hours = (int) ((delay - System.currentTimeMillis()) / 1000 / 60 / 60);
				final int mins = (int) (((delay - (hours * 60 * 60 * 1000)) - System.currentTimeMillis()) / 1000 / 60);
				sb.append("<td width=90 align=center><font color=FF9999>").append(hours).append("h ").append(mins).append("m</font></td>");
			}
			sb.append("<td width=90 align=center><button value=\"Drops\" action=\"bypass ").append(BYPASS).append(";drop;").append(bossId).append(";1;").append(tier).append("\" width=65 height=20 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></td>");
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	private static String buildPages(int total, int page, String tier)
	{
		final int perPage = Math.max(1, CommunityBoardConfig.COMMUNITY_RAID_PAGE_LIMIT);
		final int pages = Math.max(1, (total + perPage - 1) / perPage);
		if (pages <= 1)
			return "";
		
		final StringBuilder sb = new StringBuilder("<table align=center><tr>");
		for (int i = 1; i <= pages && i <= 8; i++)
		{
			final String label = (i == page) ? ("[" + i + "]") : String.valueOf(i);
			final String bypass = i == 1 ? (BYPASS + ";" + tier) : (BYPASS + ";" + tier + ";" + i);
			sb.append("<td align=center><button value=\"").append(label).append("\" action=\"bypass ").append(bypass).append("\" width=35 height=20 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private static String normalizeTier(String raw)
	{
		if (raw == null)
			return "easy";
		final String tier = raw.toLowerCase();
		if ("mid".equals(tier) || "hard".equals(tier) || "easy".equals(tier))
			return tier;
		return "easy";
	}
	
	private static String tierLabel(String tier)
	{
		if ("mid".equals(tier))
			return "Mid";
		if ("hard".equals(tier))
			return "Hard";
		return "Easy";
	}
	
	private static int parseInt(String s, int def)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch (Exception e)
		{
			return def;
		}
	}
}
