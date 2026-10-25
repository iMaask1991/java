package net.sf.l2j.gameserver.communitybbs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.util.StatsSet;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.CustomEpicSpawnManager;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.RaidBossInfoManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * Raid/Grand boss list + drop viewer for Community Board.
 */
public final class HomeBoardBossInfo
{
	private static final String CB_PATH = "data/html/CommunityBoard/Custom/";
	private static final DecimalFormat PCT = new DecimalFormat("0.##");
	
	private HomeBoardBossInfo()
	{
	}
	
	public static String resolveHtml(String command)
	{
		return resolveHtml(command, "_bbsboss", CB_PATH + "raidboss/main.html", CB_PATH + "grandboss/main.html", CB_PATH + "boss/drops.html");
	}
	
	/**
	 * Same raid/grand/drop pages as CB, but with a custom bypass prefix and HTML templates
	 * (used by voiced .raid without enabling Community Board).
	 */
	public static String resolveHtml(String command, String bypassPrefix, String raidHtmlPath, String grandHtmlPath, String dropHtmlPath)
	{
		final String bypass = (bypassPrefix == null || bypassPrefix.isEmpty()) ? "_bbsboss" : bypassPrefix;
		final String[] parts = command.split(";");
		if (parts.length >= 2 && "grand".equals(parts[1]))
		{
			final int page = parts.length >= 3 ? parseInt(parts[2], 1) : 1;
			final String path = grandHtmlPath != null ? grandHtmlPath : (CB_PATH + "grandboss/main.html");
			return applyGrandBossPage(HtmCache.getInstance().getHtm(path), page, bypass);
		}
		
		if (parts.length >= 4 && "drop".equals(parts[1]))
		{
			final int npcId = parseInt(parts[2], 0);
			final int page = parseInt(parts[3], 1);
			final String tier = parts.length >= 5 ? normalizeTier(parts[4]) : "easy";
			final String back = "grand".equals(tier) ? (bypass + ";grand") : (bypass + ";" + tier);
			final String path = dropHtmlPath != null ? dropHtmlPath : (CB_PATH + "boss/drops.html");
			return applyDropPage(HtmCache.getInstance().getHtm(path), npcId, page, back, bypass);
		}
		
		final String tier = parts.length >= 2 ? normalizeTier(parts[1]) : "easy";
		final int page = parts.length >= 3 ? parseInt(parts[2], 1) : 1;
		final String path = raidHtmlPath != null ? raidHtmlPath : (CB_PATH + "raidboss/main.html");
		return applyRaidTierPage(HtmCache.getInstance().getHtm(path), tier, page, bypass);
	}
	
	public static String applyRaidBossPage(String html, int pageId)
	{
		return applyRaidTierPage(html, "easy", pageId, "_bbsboss");
	}
	
	public static String applyRaidTierPage(String html, String tier, int pageId)
	{
		return applyRaidTierPage(html, tier, pageId, "_bbsboss");
	}
	
	public static String applyRaidTierPage(String html, String tier, int pageId, String bypassPrefix)
	{
		if (html == null || html.isEmpty())
			return html;
		
		final String bypass = (bypassPrefix == null || bypassPrefix.isEmpty()) ? "_bbsboss" : bypassPrefix;
		final String safeTier = normalizeTier(tier);
		final List<Integer> ids = collectValidBossIds(getRaidIdsForTier(safeTier));
		html = html.replace("%raid_tier%", tierLabel(safeTier));
		html = html.replace("%boss_list%", buildBossList(ids, pageId, HomeBoardBossInfo::getRaidRespawnTime, safeTier, bypass));
		return html.replace("%pagination%", buildBossPages(pageId, ids.size(), safeTier, bypass));
	}
	
	public static String buildRaidBossListHtml()
	{
		final List<Integer> ids = collectValidBossIds(CommunityBoardConfig.COMMUNITY_RAID_EASY_IDS);
		return buildBossList(ids, 1, HomeBoardBossInfo::getRaidRespawnTime, "easy", "_bbsboss");
	}
	
	public static String applyGrandBossPage(String html, int pageId)
	{
		return applyGrandBossPage(html, pageId, "_bbsboss");
	}
	
	public static String applyGrandBossPage(String html, int pageId, String bypassPrefix)
	{
		if (html == null || html.isEmpty())
			return html;
		
		final String bypass = (bypassPrefix == null || bypassPrefix.isEmpty()) ? "_bbsboss" : bypassPrefix;
		final List<Integer> ids = getGrandBossIds();
		html = html.replace("%raid_tier%", "Grand");
		html = html.replace("%boss_list%", buildBossList(ids, pageId, HomeBoardBossInfo::getGrandRespawnTime, "grand", bypass));
		return html.replace("%pagination%", buildBossPages(pageId, ids.size(), "grand", bypass));
	}
	
	public static String applyDropPage(String html, int npcId, int page, String returnBypass)
	{
		return applyDropPage(html, npcId, page, returnBypass, "_bbsboss");
	}
	
	public static String applyDropPage(String html, int npcId, int page, String returnBypass, String bypassPrefix)
	{
		if (html == null)
			return html;
		
		final String bypass = (bypassPrefix == null || bypassPrefix.isEmpty()) ? "_bbsboss" : bypassPrefix;
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		final String name = template != null ? template.getName() : ("NPC " + npcId);
		html = html.replace("%boss_name%", name);
		html = html.replace("%return_bypass%", returnBypass);
		html = html.replace("%drop_list%", buildDropList(template, page));
		html = html.replace("%pagination%", buildDropPages(npcId, page, countDrops(template), returnBypass, bypass));
		return html;
	}
	
	private static List<Integer> collectValidBossIds(List<Integer> bossIds)
	{
		final List<Integer> valid = new ArrayList<>();
		if (bossIds == null)
			return valid;
		
		for (int bossId : bossIds)
		{
			if (NpcData.getInstance().getTemplate(bossId) != null)
				valid.add(bossId);
		}
		return valid;
	}
	
	private static List<Integer> getRaidIdsForTier(String tier)
	{
		if ("mid".equals(tier))
			return CommunityBoardConfig.COMMUNITY_RAID_MID_IDS;
		if ("hard".equals(tier))
			return CommunityBoardConfig.COMMUNITY_RAID_HARD_IDS;
		return CommunityBoardConfig.COMMUNITY_RAID_EASY_IDS;
	}
	
	private static String normalizeTier(String raw)
	{
		if (raw == null)
			return "easy";
		
		final String tier = raw.toLowerCase();
		if ("mid".equals(tier) || "hard".equals(tier) || "easy".equals(tier) || "grand".equals(tier))
			return tier;
		return "easy";
	}
	
	private static String tierLabel(String tier)
	{
		if ("mid".equals(tier))
			return "Mid";
		if ("hard".equals(tier))
			return "Hard";
		if ("grand".equals(tier))
			return "Grand";
		return "Easy";
	}
	
	private static int bossesPerPage()
	{
		final int limit = CommunityBoardConfig.COMMUNITY_RAID_PAGE_LIMIT;
		return limit > 0 ? limit : 10;
	}
	
	private static int dropsPerPage()
	{
		final int limit = CommunityBoardConfig.COMMUNITY_RAID_DROP_PAGE_LIMIT;
		return limit > 0 ? limit : 6;
	}
	
	private static String buildBossList(List<Integer> bossIds, int page, RespawnTimeProvider respawnProvider, String tier, String bypassPrefix)
	{
		if (bossIds == null || bossIds.isEmpty())
			return "<font color=696969>No bosses configured.</font>";
		
		final int perPage = bossesPerPage();
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
			
			final String name = template.getName();
			final long delay = respawnProvider.getRespawnTime(bossId);
			final String dropCmd = bypassPrefix + ";drop;" + bossId + ";1;" + tier;
			
			sb.append("<tr>");
			sb.append("<td width=220><font color=EEE8AA>").append(name).append("</font></td>");
			if (delay <= System.currentTimeMillis())
				sb.append("<td width=90 align=center><font color=ADFF2F>Alive</font></td>");
			else
			{
				final int hours = (int) ((delay - System.currentTimeMillis()) / 1000 / 60 / 60);
				final int mins = (int) (((delay - (hours * 60 * 60 * 1000)) - System.currentTimeMillis()) / 1000 / 60);
				sb.append("<td width=90 align=center><font color=FF9999>").append(hours).append("h ").append(mins).append("m</font></td>");
			}
			sb.append("<td width=90 align=center><button value=\"Drops\" action=\"bypass ").append(dropCmd).append("\" width=65 height=20 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></td>");
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	private static String buildBossPages(int page, int total, String tier, String bypassPrefix)
	{
		final int perPage = bossesPerPage();
		final int pages = Math.max(1, (total + perPage - 1) / perPage);
		if (pages <= 1)
			return "";
		
		final StringBuilder sb = new StringBuilder("<table align=center><tr>");
		for (int i = 1; i <= pages && i <= 8; i++)
		{
			final String label = (i == page) ? ("[" + i + "]") : String.valueOf(i);
			final String bypass = i == 1 ? (bypassPrefix + ";" + tier) : (bypassPrefix + ";" + tier + ";" + i);
			sb.append("<td align=center><button value=\"").append(label).append("\" action=\"bypass ").append(bypass).append("\" width=35 height=20 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private static String buildDropList(NpcTemplate template, int page)
	{
		if (template == null || template.getDropData() == null || template.getDropData().isEmpty())
			return "<font color=696969>No drops.</font>";
		
		final List<DropRow> rows = collectDrops(template);
		if (rows.isEmpty())
			return "<font color=696969>No drops.</font>";
		
		final int perPage = dropsPerPage();
		final int safePage = Math.max(1, page);
		final int from = (safePage - 1) * perPage;
		if (from >= rows.size())
			return "<font color=696969>No drops.</font>";
		
		final int to = Math.min(rows.size(), from + perPage);
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<table width=500 cellpadding=2 cellspacing=1>");
		
		for (int i = from; i < to; i++)
		{
			final DropRow row = rows.get(i);
			final Item item = ItemTable.getInstance().getTemplate(row.itemId);
			final String itemName = item != null ? item.getName() : ("Item " + row.itemId);
			final String icon = Item.getItemIcon(row.itemId);
			final String qty = row.min == row.max ? String.valueOf(row.min) : (row.min + "-" + row.max);
			final String type = row.sweep ? "<font color=00FF00>Sweep</font>" : "<font color=3BB9FF>Drop</font>";
			
			sb.append("<tr>");
			sb.append("<td width=36 align=center><button value=\" \" action=\"\" width=32 height=32 back=").append(icon).append(" fore=").append(icon).append("></td>");
			sb.append("<td width=280><font color=LEVEL>").append(itemName).append("</font><br1><font color=c1c1c1>x").append(qty).append("</font></td>");
			sb.append("<td width=80 align=center><font color=LEVEL>").append(PCT.format(row.chancePct)).append("%</font></td>");
			sb.append("<td width=70 align=center>").append(type).append("</td>");
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	private static String buildDropPages(int npcId, int page, int total, String returnBypass, String bypassPrefix)
	{
		final int pages = Math.max(1, (total + dropsPerPage() - 1) / dropsPerPage());
		if (pages <= 1)
			return "";
		
		String suffix = "";
		if (returnBypass.contains("grand"))
			suffix = ";grand";
		else if (returnBypass.contains("hard"))
			suffix = ";hard";
		else if (returnBypass.contains("mid"))
			suffix = ";mid";
		else if (returnBypass.contains("easy"))
			suffix = ";easy";
		final StringBuilder sb = new StringBuilder("<table align=center><tr>");
		for (int i = 1; i <= pages && i <= 8; i++)
		{
			final String label = (i == page) ? ("[" + i + "]") : String.valueOf(i);
			sb.append("<td align=center><button value=\"").append(label).append("\" action=\"bypass ").append(bypassPrefix).append(";drop;").append(npcId).append(";").append(i).append(suffix).append("\" width=35 height=20 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private static int countDrops(NpcTemplate template)
	{
		return collectDrops(template).size();
	}
	
	private static List<DropRow> collectDrops(NpcTemplate template)
	{
		final List<DropRow> rows = new ArrayList<>();
		if (template == null || template.getDropData() == null)
			return rows;
		
		for (DropCategory cat : template.getDropData())
		{
			if (cat == null || cat.getAllDrops() == null)
				continue;
			
			for (DropData drop : cat.getAllDrops())
			{
				if (drop == null)
					continue;
				
				final DropRow row = new DropRow();
				row.itemId = drop.getItemId();
				row.min = drop.getMinDrop();
				row.max = drop.getMaxDrop();
				row.chancePct = drop.getChance() / 10000.0;
				row.sweep = cat.isSweep();
				rows.add(row);
			}
		}
		return rows;
	}
	
	private static List<Integer> getGrandBossIds()
	{
		final List<Integer> ids = new ArrayList<>();
		
		for (int bossId : CommunityBoardConfig.COMMUNITY_GRAND_BOSS_IDS)
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
			if (template != null && template.isType("GrandBoss"))
				ids.add(bossId);
		}
		
		if (ids.isEmpty())
		{
			for (int bossId : new int[]
			{
				29001,
				29006,
				29014,
				29019,
				29020,
				29022,
				29028,
				29045,
				29046,
				29047
			})
			{
				final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
				if (template != null && template.isType("GrandBoss") && GrandBossManager.getInstance().getStatsSet(bossId) != null)
					ids.add(bossId);
			}
		}
		
		return ids;
	}
	
	private static long getRaidRespawnTime(int bossId)
	{
		return RaidBossInfoManager.getInstance().getRaidBossRespawnTime(bossId);
	}
	
	private static long getGrandRespawnTime(int bossId)
	{
		if (CustomEpicSpawnManager.getInstance().isCustomEpic(bossId))
			return CustomEpicSpawnManager.getInstance().getNextSpawnMillis(bossId);
		
		final StatsSet stats = GrandBossManager.getInstance().getStatsSet(bossId);
		return (stats != null) ? stats.getLong("respawn_time") : 0L;
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
	
	@FunctionalInterface
	private interface RespawnTimeProvider
	{
		long getRespawnTime(int bossId);
	}
	
	private static class DropRow
	{
		int itemId;
		int min;
		int max;
		double chancePct;
		boolean sweep;
	}
}
