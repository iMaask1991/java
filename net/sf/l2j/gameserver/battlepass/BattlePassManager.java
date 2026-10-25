package net.sf.l2j.gameserver.battlepass;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.HomeBBSManager;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.memo.PlayerMemo;

/**
 * Free and VIP battle pass tracks. Same kill/level/claim logic; VIP track requires active VIP to farm and claim.
 */
public class BattlePassManager implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(BattlePassManager.class.getName());
	private static final NumberFormat NUM = NumberFormat.getInstance(Locale.US);
	private static final int SLOTS_PER_PAGE = 35;
	private static final int BAR_WIDTH = 400;
	private static final int BAR_HEIGHT = 16;
	
	private final boolean _vip;
	private final String _html;
	private final String _xml;
	private final String _bypass;
	private final String _label;
	private final String _table;
	private final String _memoKills;
	private final String _memoClaimed;
	private final String _memoDoubled;
	
	private final Map<Integer, IntIntHolder> _rewards = new HashMap<>();
	private final Map<Integer, Long> _killsForLevel = new HashMap<>();
	private long[] _neededToReach = new long[0];
	private final Map<Integer, BattlePassData> _data = new ConcurrentHashMap<>();
	private long _xmlTime;
	
	protected BattlePassManager(boolean vip)
	{
		_vip = vip;
		if (vip)
		{
			_html = "data/html/CommunityBoard/Custom/battlepassvip/main.html";
			_xml = "./data/xml/custom/BattlePassVip.xml";
			_bypass = "_bbspassvip";
			_label = "VIP Battle Pass";
			_table = "character_battlepass_vip";
			_memoKills = "bpVipKills";
			_memoClaimed = "bpVipClaimed";
			_memoDoubled = "bpVipDoubled";
		}
		else
		{
			_html = "data/html/CommunityBoard/Custom/battlepass/main.html";
			_xml = "./data/xml/custom/BattlePass.xml";
			_bypass = "_bbspass";
			_label = "Battle Pass";
			_table = "character_battlepass";
			_memoKills = "bpKills";
			_memoClaimed = "bpClaimed";
			_memoDoubled = "bpDoubled";
		}
		load();
	}
	
	private boolean isEnabled()
	{
		return _vip ? Config.BATTLEPASS_VIP_ENABLED : Config.BATTLEPASS_ENABLED;
	}
	
	private int maxLevel()
	{
		return _vip ? Config.BATTLEPASS_VIP_MAX_LEVEL : Config.BATTLEPASS_MAX_LEVEL;
	}
	
	@Override
	public void load()
	{
		_rewards.clear();
		_killsForLevel.clear();
		parseFile(_xml);
		rebuildNeededCache();
		_xmlTime = new File(_xml).lastModified();
		LOGGER.info("Loaded {} " + _label + " rewards ({} kill entries).", _rewards.size(), _killsForLevel.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", list -> forEach(list, "level", this::parseLevel));
	}
	
	private void parseLevel(Node node)
	{
		final int id = parseInteger(node.getAttributes(), "id", 0);
		final int item = parseInteger(node.getAttributes(), "item", 0);
		final int count = parseInteger(node.getAttributes(), "count", 0);
		final int kills = parseInteger(node.getAttributes(), "kills", 0);
		if (id < 1 || item < 1 || count < 1)
			return;
		
		_rewards.put(id, new IntIntHolder(item, count));
		if (kills > 0)
			_killsForLevel.put(id, (long) kills);
	}
	
	private void rebuildNeededCache()
	{
		final int max = maxLevel();
		_neededToReach = new long[max + 1];
		long total = 0;
		for (int level = 1; level <= max; level++)
		{
			total += killsForLevel(level);
			_neededToReach[level] = total;
		}
	}
	
	private long killsForLevel(int level)
	{
		final Long xml = _killsForLevel.get(level);
		if (xml != null && xml > 0)
			return xml;
		LOGGER.warn(_label + " level {} has no kills in XML.", level);
		return Long.MAX_VALUE / 2;
	}
	
	public void onKill(Player player)
	{
		if (!isEnabled() || player == null)
			return;
		if (_vip && !isVip(player))
			return;
		
		final BattlePassData data = getData(player);
		final long before = data.kills;
		data.kills = before + 1;
		data.dirty = true;
		
		final int oldLevel = getUnlockedLevel(before);
		final int newLevel = getUnlockedLevel(data.kills);
		if (newLevel > oldLevel)
			player.sendMessage(_label + " level " + newLevel + " unlocked.");
	}
	
	public void store(Player player)
	{
		if (player == null)
			return;
		
		final BattlePassData data = _data.get(player.getObjectId());
		if (data != null && data.dirty)
			save(player.getObjectId(), data);
	}
	
	public void unload(Player player)
	{
		if (player == null)
			return;
		
		store(player);
		_data.remove(player.getObjectId());
	}
	
	public void parseCmd(String command, Player player)
	{
		if (!isEnabled())
		{
			player.sendMessage(_label + " is disabled.");
			return;
		}
		
		final String[] args = command.split(";");
		final BattlePassData data = getData(player);
		
		if (args.length >= 3 && "page".equals(args[1]))
			data.page = clampPage(parseInt(args[2], 0));
		else if (args.length >= 3 && ("sel".equals(args[1]) || "s".equals(args[1])))
		{
			final int level = parseInt(args[2], 1);
			if (level >= 1 && level <= maxLevel())
			{
				data.selected = level;
				data.page = pageOf(level);
			}
		}
		else if (args.length >= 2 && "claim".equals(args[1]))
		{
			if (args.length >= 3)
			{
				final int level = parseInt(args[2], 0);
				if (level >= 1 && level <= maxLevel())
					data.selected = level;
			}
			claim(player, data);
		}
		else
			data.page = pageOf(Math.max(1, getUnlockedLevel(data.kills)));
		
		show(player);
	}
	
	private void claim(Player player, BattlePassData data)
	{
		synchronized (data)
		{
			if (_vip && !isVip(player))
			{
				player.sendMessage("Your VIP expired. You can't claim VIP Battle Pass rewards.");
				return;
			}
			
			final int level = Math.max(1, data.selected);
			final int unlocked = getUnlockedLevel(data.kills);
			if (level > unlocked)
			{
				player.sendMessage("Unlock this " + _label + " level first.");
				return;
			}
			
			final IntIntHolder reward = _rewards.get(level);
			if (reward == null)
			{
				player.sendMessage("No reward configured for this level.");
				return;
			}
			
			if (isClaimed(data, level))
			{
				player.sendMessage("Already claimed.");
				return;
			}
			
			setClaimed(data, level, true);
			data.dirty = true;
			if (!save(player.getObjectId(), data))
			{
				setClaimed(data, level, false);
				player.sendMessage(_label + " save failed. Try again.");
				return;
			}
			
			if (player.addItem("BattlePass", reward.getId(), reward.getValue(), player, true) == null)
				player.sendMessage("Invalid reward item. Claim is already marked; contact a GM.");
			else
				player.sendMessage(_label + " reward claimed.");
		}
	}
	
	public void show(Player player)
	{
		refreshRewards();
		final BattlePassData data = getData(player);
		data.page = clampPage(data.page);
		
		String html = readHtml(data.page);
		if (html == null)
			return;
		
		final long kills = data.kills;
		final int unlocked = getUnlockedLevel(kills);
		if (data.selected < 1 || data.selected > maxLevel())
			data.selected = Math.max(1, unlocked == 0 ? 1 : unlocked);
		
		html = applyCells(html, player, data, unlocked);
		html = applyProgressBar(html, kills, unlocked);
		html = html.replace("%level%", String.valueOf(unlocked));
		html = html.replace("%maxLevel%", String.valueOf(maxLevel()));
		html = html.replace("%kills%", NUM.format(kills));
		html = html.replace("%vip%", isVip(player) ? "VIP" : "Normal");
		html = HomeBBSManager.applyPlayerStats(html, player);
		
		if (html.length() >= 12270)
			LOGGER.warn(_label + " HTML too long: {} chars.", html.length());
		
		BaseBBSManager.separateAndSend(html, player);
	}
	
	private void refreshRewards()
	{
		final long time = new File(_xml).lastModified();
		if (time != 0 && time != _xmlTime)
			load();
	}
	
	private String readHtml(int page)
	{
		String path = _html;
		if (page == 1)
			path = path.replace("main.html", "page2.html");
		else if (page >= 2)
			path = path.replace("main.html", "page3.html");
		
		try
		{
			return Files.readString(Path.of(path));
		}
		catch (Exception e)
		{
			return HtmCache.getInstance().getHtm(path);
		}
	}
	
	private String applyProgressBar(String html, long kills, int unlocked)
	{
		final int max = maxLevel();
		int fill = 0;
		int empty = BAR_WIDTH;
		String progress;
		if (unlocked >= max)
			progress = "MAX";
		else
		{
			final int next = unlocked + 1;
			final long start = killsToReach(unlocked);
			final long end = killsToReach(next);
			final long span = Math.max(1L, end - start);
			final long done = Math.max(0L, Math.min(span, kills - start));
			fill = (int) (BAR_WIDTH * done / span);
			if (fill < 1 && done > 0)
				fill = 1;
			if (fill > BAR_WIDTH)
				fill = BAR_WIDTH;
			empty = BAR_WIDTH - fill;
			progress = NUM.format(done) + " / " + NUM.format(span);
		}
		
		final StringBuilder bar = new StringBuilder(256);
		bar.append("<table width=").append(BAR_WIDTH).append(" cellpadding=0 cellspacing=0><tr>");
		if (fill > 0)
			bar.append("<td><img src=L2UI_VK.Big_Center_GaugeCP width=").append(fill).append(" height=").append(BAR_HEIGHT).append("></td>");
		if (empty > 0)
			bar.append("<td><img src=L2UI_VK.Big_Center_GaugeBG width=").append(empty).append(" height=").append(BAR_HEIGHT).append("></td>");
		bar.append("</tr></table>");
		
		html = html.replace("%bp_bar%", bar.toString());
		return html.replace("%bp_progress%", progress);
	}
	
	private String applyCells(String html, Player player, BattlePassData data, int unlocked)
	{
		final boolean vip = isVip(player);
		final int max = Math.max(maxLevel(), 105);
		for (int level = 1; level <= max; level++)
		{
			final String n = String.valueOf(level);
			if (level > maxLevel())
			{
				html = html.replace("%a" + n + "%", "");
				html = html.replace("%i" + n + "%", "icon.skill0000");
				html = html.replace("%c" + n + "%", "-");
				html = html.replace("%s" + n + "%", "");
				continue;
			}
			
			final IntIntHolder reward = _rewards.get(level);
			html = html.replace("%i" + n + "%", iconOf(reward));
			html = html.replace("%c" + n + "%", compactCount(reward == null ? 0 : reward.getValue()));
			html = html.replace("%a" + n + "%", actionOf(level, unlocked, data, vip));
			html = html.replace("%s" + n + "%", statusOf(level, unlocked, data));
		}
		return html;
	}
	
	private String actionOf(int level, int unlocked, BattlePassData data, boolean vip)
	{
		if (level > unlocked)
			return "";
		if (_vip && !vip)
			return "";
		if (isClaimed(data, level))
			return "";
		return "bypass " + _bypass + ";claim;" + level;
	}
	
	private String statusOf(int level, int unlocked, BattlePassData data)
	{
		if (level > unlocked)
			return compactCount(killsToReach(level));
		if (isClaimed(data, level))
			return "<font color=00ffff>(Received)</font>";
		return "<font color=LEVEL>(REWARD)</font>";
	}
	
	private static String compactCount(long value)
	{
		if (value >= 1000000)
		{
			if (value % 1000000 == 0)
				return (value / 1000000) + "M";
			return String.format(Locale.US, "%.1fM", value / 1000000.0);
		}
		if (value >= 1000)
		{
			if (value % 1000 == 0)
				return (value / 1000) + "K";
			return String.format(Locale.US, "%.1fK", value / 1000.0);
		}
		return String.valueOf(value);
	}
	
	private BattlePassData getData(Player player)
	{
		return _data.computeIfAbsent(player.getObjectId(), id -> loadFromDb(player));
	}
	
	private BattlePassData loadFromDb(Player player)
	{
		final BattlePassData data = new BattlePassData();
		final int objectId = player.getObjectId();
		if (loadRow(objectId, data, true) || loadRow(objectId, data, false))
			return data;
		
		final PlayerMemo memos = player.getMemos();
		data.kills = memos.getLong(_memoKills, 0);
		data.claimed = memos.getInteger(_memoClaimed, 0);
		data.doubled = memos.getInteger(_memoDoubled, 0);
		if (data.kills > 0 || data.claimed != 0 || data.claimed2 != 0 || data.doubled != 0)
		{
			data.dirty = true;
			save(objectId, data);
			memos.remove(_memoKills);
			memos.remove(_memoClaimed);
			memos.remove(_memoDoubled);
			memos.storeMe();
		}
		return data;
	}
	
	private boolean loadRow(int objectId, BattlePassData data, boolean withClaimed2)
	{
		final String sql = withClaimed2
			? "SELECT kills, claimed, claimed2, doubled FROM " + _table + " WHERE char_id=?"
			: "SELECT kills, claimed, doubled FROM " + _table + " WHERE char_id=?";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, objectId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (!rs.next())
					return false;
				
				data.kills = rs.getLong("kills");
				data.claimed = rs.getLong("claimed");
				data.claimed2 = withClaimed2 ? rs.getLong("claimed2") : 0L;
				data.doubled = rs.getInt("doubled");
				return true;
			}
		}
		catch (Exception e)
		{
			if (withClaimed2)
				return false;
			LOGGER.error(_label + ": load failed for {}.", e, objectId);
			return false;
		}
	}
	
	private boolean save(int objectId, BattlePassData data)
	{
		if (saveRow(objectId, data, true) || saveRow(objectId, data, false))
		{
			data.dirty = false;
			return true;
		}
		return false;
	}
	
	private boolean saveRow(int objectId, BattlePassData data, boolean withClaimed2)
	{
		final String sql = withClaimed2
			? "INSERT INTO " + _table + " (char_id, kills, claimed, claimed2, doubled) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE kills=VALUES(kills), claimed=VALUES(claimed), claimed2=VALUES(claimed2), doubled=VALUES(doubled)"
			: "INSERT INTO " + _table + " (char_id, kills, claimed, doubled) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE kills=VALUES(kills), claimed=VALUES(claimed), doubled=VALUES(doubled)";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, objectId);
			ps.setLong(2, data.kills);
			ps.setLong(3, data.claimed);
			if (withClaimed2)
			{
				ps.setLong(4, data.claimed2);
				ps.setInt(5, data.doubled);
			}
			else
				ps.setInt(4, data.doubled);
			ps.executeUpdate();
			return true;
		}
		catch (Exception e)
		{
			if (withClaimed2)
				return false;
			LOGGER.error(_label + ": save failed for {}.", e, objectId);
			return false;
		}
	}
	
	private static String iconOf(IntIntHolder reward)
	{
		if (reward == null)
			return "icon.noimage";
		
		final String mapped = Item.getItemIcon(reward.getId());
		if (isRealIcon(mapped))
			return mapped;
		
		final Item item = ItemTable.getInstance().getTemplate(reward.getId());
		if (item != null && isRealIcon(item.getIcon()))
			return item.getIcon();
		
		switch (reward.getId())
		{
			case 57:
				return "icon.etc_adena_i00";
			case 1539:
				return "icon.etc_reagent_white_i00";
			case 10001:
				return "icon.etc_coins_gold_i00";
			case 6622:
				return "icon.etc_spell_books_red_i00";
			case 6577:
				return "icon.etc_scroll_of_enchant_weapon_i05";
			default:
				return "icon.noimage";
		}
	}
	
	private static boolean isRealIcon(String icon)
	{
		return icon != null && !icon.isEmpty() && !"icon.noimage".equalsIgnoreCase(icon);
	}
	
	public int getUnlockedLevel(long kills)
	{
		int unlocked = 0;
		final int max = maxLevel();
		for (int level = 1; level <= max; level++)
		{
			if (kills < killsToReach(level))
				break;
			unlocked = level;
		}
		return unlocked;
	}
	
	public long killsToReach(int level)
	{
		if (level < 1)
			return 0;
		if (_neededToReach != null && level < _neededToReach.length)
			return _neededToReach[level];
		
		long needed = 0;
		for (int i = 1; i <= level; i++)
			needed += killsForLevel(i);
		return needed;
	}
	
	private int pageOf(int level)
	{
		return Math.max(0, (level - 1) / SLOTS_PER_PAGE);
	}
	
	private int clampPage(int page)
	{
		final int last = Math.max(0, (maxLevel() - 1) / SLOTS_PER_PAGE);
		if (page < 0)
			return 0;
		return page > last ? last : page;
	}
	
	private static boolean isClaimed(BattlePassData data, int level)
	{
		if (level < 1 || level > 128)
			return false;
		if (level <= 64)
			return (data.claimed & (1L << (level - 1))) != 0;
		return (data.claimed2 & (1L << (level - 65))) != 0;
	}
	
	private static void setClaimed(BattlePassData data, int level, boolean value)
	{
		if (level < 1 || level > 128)
			return;
		if (level <= 64)
		{
			final long bit = 1L << (level - 1);
			data.claimed = value ? (data.claimed | bit) : (data.claimed & ~bit);
			return;
		}
		final long bit = 1L << (level - 65);
		data.claimed2 = value ? (data.claimed2 | bit) : (data.claimed2 & ~bit);
	}
	
	private static boolean isVip(Player player)
	{
		return player.isVip() && player.getVipEndTime() > System.currentTimeMillis();
	}
	
	private static int parseInt(String value, int fallback)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
			return fallback;
		}
	}
	
	public static BattlePassManager getInstance()
	{
		return SingletonHolder.FREE;
	}
	
	public static BattlePassManager getVipInstance()
	{
		return SingletonHolder.VIP;
	}
	
	private static class SingletonHolder
	{
		private static final BattlePassManager FREE = new BattlePassManager(false);
		private static final BattlePassManager VIP = new BattlePassManager(true);
	}
	
	private static class BattlePassData
	{
		long kills;
		long claimed;
		long claimed2;
		int doubled;
		int selected;
		int page;
		boolean dirty;
	}
}
