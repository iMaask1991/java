package net.sf.l2j.gameserver.model.actor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoardConfig;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Shift+click drop viewer for Attackable NPCs (monster / raid / grand).
 * Same visual language as Community Board drops, but via NpcHtmlMessage.
 * <br>
 * Adena rows show the viewer's real amount, using the same formula as Monster drops:
 * server rate x VIP x rune x RateDropAdena stat (skills/buffs) x champion x happy hour.
 * Deep blue level penalty is intentionally NOT applied here.
 */
public final class NpcDropViewer
{
	private static final String HTML_PATH = "data/html/mods/npc_drops.htm";
	private static final DecimalFormat PCT = new DecimalFormat("0.00");
	private static final DecimalFormat MUL = new DecimalFormat("0.##");
	private static final DecimalFormat QTY = new DecimalFormat("#,##0");
	private static final int ADENA_ID = 57;

	private NpcDropViewer()
	{
	}

	public static void show(Player player, Npc npc, int page)
	{
		if (player == null || npc == null)
			return;

		show(player, npc.getTemplate(), npc, page);
	}

	public static void show(Player player, int npcId, int page)
	{
		if (player == null)
			return;

		// Pagination: reuse the targeted NPC instance (keeps champion / happy hour state) when it matches.
		Npc npc = null;
		if (player.getTarget() instanceof Npc && ((Npc) player.getTarget()).getNpcId() == npcId)
			npc = (Npc) player.getTarget();

		show(player, npc != null ? npc.getTemplate() : NpcData.getInstance().getTemplate(npcId), npc, page);
	}

	public static void show(Player player, NpcTemplate template, int page)
	{
		show(player, template, null, page);
	}

	private static void show(Player player, NpcTemplate template, Npc npc, int page)
	{
		if (player == null)
			return;

		String html = HtmCache.getInstance().getHtm(HTML_PATH);
		if (html == null || html.isEmpty())
		{
			player.sendMessage("Drop info is temporarily unavailable.");
			return;
		}

		final int npcId = template != null ? template.getNpcId() : 0;
		final String name = template != null ? template.getName() : ("NPC " + npcId);
		final int safePage = Math.max(1, page);
		final AdenaRates rates = new AdenaRates(player, npc);

		html = html.replace("%npc_name%", name);
		html = html.replace("%drop_list%", buildDropList(template, safePage, rates));
		html = html.replace("%pagination%", buildDropPages(npcId, safePage, countDrops(template, rates)));

		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	private static int dropsPerPage()
	{
		final int limit = CommunityBoardConfig.COMMUNITY_RAID_DROP_PAGE_LIMIT;
		return limit > 0 ? limit : 8;
	}

	private static String buildDropList(NpcTemplate template, int page, AdenaRates rates)
	{
		if (template == null || template.getDropData() == null || template.getDropData().isEmpty())
			return "<font color=696969>No drops.</font>";

		final List<DropRow> rows = collectDrops(template, rates);
		if (rows.isEmpty())
			return "<font color=696969>No drops.</font>";

		final int perPage = dropsPerPage();
		final int from = (page - 1) * perPage;
		if (from >= rows.size())
			return "<font color=696969>No drops.</font>";

		final int to = Math.min(rows.size(), from + perPage);
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<table width=280 cellpadding=2 cellspacing=1>");

		boolean hasAdena = false;
		for (int i = from; i < to; i++)
		{
			final DropRow row = rows.get(i);
			final Item item = ItemTable.getInstance().getTemplate(row.itemId);
			final String itemName = item != null ? item.getName() : ("Item " + row.itemId);
			final String icon = Item.getItemIcon(row.itemId);
			final String qty = row.min == row.max ? QTY.format(row.min) : (QTY.format(row.min) + "-" + QTY.format(row.max));
			final String type = row.sweep ? "<font color=00FF00>Sweep</font>" : "<font color=3BB9FF>Drop</font>";
			if (row.itemId == ADENA_ID)
				hasAdena = true;

			sb.append("<tr>");
			sb.append("<td width=36 align=center><button value=\" \" action=\"\" width=32 height=32 back=").append(icon).append(" fore=").append(icon).append("></td>");
			sb.append("<td width=140><font color=LEVEL>").append(itemName).append("</font><br1><font color=c1c1c1>").append(qty).append("</font></td>");
			sb.append("<td width=50 align=center><font color=LEVEL>").append(PCT.format(row.chancePct)).append("%</font></td>");
			sb.append("<td width=40 align=center>").append(type).append("</td>");
			sb.append("</tr>");
		}

		sb.append("</table>");


		return sb.toString();
	}

	private static String buildDropPages(int npcId, int page, int total)
	{
		final int pages = Math.max(1, (total + dropsPerPage() - 1) / dropsPerPage());
		if (pages <= 1)
			return "";

		final StringBuilder sb = new StringBuilder("<table align=center><tr>");
		for (int i = 1; i <= pages && i <= 8; i++)
		{
			final String label = (i == page) ? ("[" + i + "]") : String.valueOf(i);
			sb.append("<td align=center><button value=\"").append(label);
			sb.append("\" action=\"bypass -h custom_droplist ").append(npcId).append(" ").append(i);
			sb.append("\" width=35 height=20 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}

	private static int countDrops(NpcTemplate template, AdenaRates rates)
	{
		return collectDrops(template, rates).size();
	}

	private static List<DropRow> collectDrops(NpcTemplate template, AdenaRates rates)
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

				if (row.itemId == ADENA_ID)
					applyAdenaRates(row, drop, cat, rates);

				rows.add(row);
			}
		}
		return rows;
	}

	/**
	 * Mirrors Monster.calculateCategorizedRewardItem (drop) and Monster.calculateRewardItem (sweep) for adena, without deep blue penalty.
	 */
	private static void applyAdenaRates(DropRow row, DropData drop, DropCategory cat, AdenaRates rates)
	{
		final double max = DropData.MAX_CHANCE;
		final double base = drop.getChance();

		double quantityChance;
		double showChance;

		if (row.sweep)
		{
			// Sweep: no happy hour. Chance and quantity come from the same roll.
			quantityChance = base * rates.adenaMultiplier(false);
			showChance = Math.min(1.0, quantityChance / max);
		}
		else
		{
			// Category roll uses item rates (+ champion); quantity uses adena rates, min 1 roll.
			final int catChance = Math.max(1, cat.getCategoryChance());
			final double catRoll = Math.min(1.0, catChance * rates.categoryMultiplier() / max);
			showChance = catRoll * Math.min(1.0, base / catChance);
			quantityChance = Math.max(max, base * rates.adenaMultiplier(true));
		}

		final double rolls = quantityChance / max;
		final long minRolls = Math.max(1, (long) Math.floor(rolls));
		final long maxRolls = Math.max(1, (long) Math.ceil(rolls));

		row.min = drop.getMinDrop() * minRolls * rates.championAdena;
		row.max = drop.getMaxDrop() * maxRolls * rates.championAdena;
		row.chancePct = showChance * 100.0;
	}

	/**
	 * Viewer-specific adena multipliers (same sources as Monster drop code).
	 */
	private static final class AdenaRates
	{
		final double server = Config.RATE_DROP_ADENA;
		final double vip;
		final double rune;
		final double stat;
		final double champion;
		final long championAdena;
		final double happyHour;
		final boolean raid;

		AdenaRates(Player player, Npc npc)
		{
			vip = player.isVip() ? Config.VIP_ADENA_RATES : 1.0;

			if (player.getInventory().getInventoryItemCount(Config.RUNE_1_ID, 0) >= 1)
				rune = Config.RUNE_1_ADENA;
			else if (player.getInventory().getInventoryItemCount(Config.RUNE_2_ID, 0) >= 1)
				rune = Config.RUNE_2_ADENA;
			else if (player.getInventory().getInventoryItemCount(Config.RUNE_3_ID, 0) >= 1)
				rune = Config.RUNE_3_ADENA;
			else
				rune = 1.0;

			stat = player.calcStat(Stats.RATE_DROP_ADENA, 1, null, null);

			final Monster monster = (npc instanceof Monster) ? (Monster) npc : null;
			final boolean isChampion = monster != null && monster.isChampion();
			champion = isChampion ? Config.CHAMPION_REWARDS : 1.0;
			championAdena = isChampion ? Math.max(1, Config.CHAMPION_ADENAS_REWARDS) : 1;
			happyHour = (monster != null && monster.isHappyHourDropTarget()) ? Config.SOLO_EVENT_DROP_RATE_MULTIPLIER : 1.0;
			raid = monster != null && monster.isRaidBoss();
		}

		double adenaMultiplier(boolean withHappyHour)
		{
			return server * vip * rune * stat * champion * (withHappyHour ? happyHour : 1.0);
		}

		double categoryMultiplier()
		{
			return (raid ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS) * champion;
		}

		String describe()
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("Rate x").append(MUL.format(server));
			if (vip != 1.0)
				sb.append(" | VIP x").append(MUL.format(vip));
			if (rune != 1.0)
				sb.append(" | Runa x").append(MUL.format(rune));
			if (stat != 1.0)
				sb.append(" | Skill x").append(MUL.format(stat));
			if (champion != 1.0)
				sb.append(" | Champion x").append(MUL.format(champion * championAdena));
			if (happyHour != 1.0)
				sb.append(" | Happy Hour x").append(MUL.format(happyHour));
			sb.append(" = <font color=LEVEL>x").append(MUL.format(adenaMultiplier(true) * championAdena)).append("</font>");
			return sb.toString();
		}
	}

	private static class DropRow
	{
		int itemId;
		long min;
		long max;
		double chancePct;
		boolean sweep;
	}
}
