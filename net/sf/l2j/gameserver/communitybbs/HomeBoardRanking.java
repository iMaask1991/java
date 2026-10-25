package net.sf.l2j.gameserver.communitybbs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;

/**
 * Community Board rankings: PvP, PK and highest enchants (weapon / armor / jewelry).
 */
public final class HomeBoardRanking
{
	private static final String CB_PATH = "data/html/CommunityBoard/Custom/";
	private static final int LIMIT = 12;
	private static final long CACHE_MS = 60_000L;
	
	private static final String PVP_SQL = "SELECT char_name,pvpkills,online FROM characters WHERE pvpkills>0 AND accesslevel=0 ORDER BY pvpkills DESC LIMIT ?";
	private static final String PK_SQL = "SELECT char_name,pkkills,online FROM characters WHERE pkkills>0 AND accesslevel=0 ORDER BY pkkills DESC LIMIT ?";
	private static final String ENCHANT_SQL = "SELECT c.char_name,c.online,i.item_id,i.enchant_level FROM items i INNER JOIN characters c ON c.obj_Id=i.owner_id WHERE i.enchant_level>0 AND c.accesslevel=0 AND i.loc IN ('PAPERDOLL','INVENTORY') ORDER BY i.enchant_level DESC LIMIT 250";
	
	private static final int JEWEL_SLOTS = Item.SLOT_NECK | Item.SLOT_R_EAR | Item.SLOT_L_EAR | Item.SLOT_LR_EAR | Item.SLOT_R_FINGER | Item.SLOT_L_FINGER | Item.SLOT_LR_FINGER;
	
	private enum RankType
	{
		PVP,
		PK,
		WEAPON,
		ARMOR,
		JEWEL
	}
	
	private static volatile long _cacheTime;
	private static volatile String _pvpHtml = "";
	private static volatile String _pkHtml = "";
	private static volatile String _weaponHtml = "";
	private static volatile String _armorHtml = "";
	private static volatile String _jewelHtml = "";
	
	private HomeBoardRanking()
	{
	}
	
	public static void warmup()
	{
		refreshCacheIfNeeded();
	}
	
	public static String resolveHtml(String command)
	{
		String html = HtmCache.getInstance().getHtm(CB_PATH + "ranking/main.html");
		if (html == null)
			return html;
		
		final RankType type = parseType(command);
		refreshCacheIfNeeded();
		
		html = html.replace("%rank_title%", titleOf(type));
		html = html.replace("%rank_list%", listOf(type));
		return html;
	}
	
	private static RankType parseType(String command)
	{
		if (command == null)
			return RankType.PVP;
		
		final String[] parts = command.split(";");
		if (parts.length < 2)
			return RankType.PVP;
		
		switch (parts[1].toLowerCase())
		{
			case "pk":
				return RankType.PK;
			case "weapon":
				return RankType.WEAPON;
			case "armor":
				return RankType.ARMOR;
			case "jewel":
			case "jewelry":
				return RankType.JEWEL;
			default:
				return RankType.PVP;
		}
	}
	
	private static String titleOf(RankType type)
	{
		switch (type)
		{
			case PK:
				return "Ranking PK";
			case WEAPON:
				return "Ranking Enchant Weapon";
			case ARMOR:
				return "Ranking Enchant Armor";
			case JEWEL:
				return "Ranking Enchant Jewelry";
			default:
				return "Ranking PvP";
		}
	}
	
	private static String listOf(RankType type)
	{
		switch (type)
		{
			case PK:
				return _pkHtml;
			case WEAPON:
				return _weaponHtml;
			case ARMOR:
				return _armorHtml;
			case JEWEL:
				return _jewelHtml;
			default:
				return _pvpHtml;
		}
	}
	
	private static void refreshCacheIfNeeded()
	{
		final long now = System.currentTimeMillis();
		if (now - _cacheTime < CACHE_MS && !_pvpHtml.isEmpty())
			return;
		
		synchronized (HomeBoardRanking.class)
		{
			if (now - _cacheTime < CACHE_MS && !_pvpHtml.isEmpty())
				return;
			
			_pvpHtml = loadKillRank(PVP_SQL, "pvpkills");
			_pkHtml = loadKillRank(PK_SQL, "pkkills");
			loadEnchantRanks();
			_cacheTime = System.currentTimeMillis();
		}
	}
	
	private static String loadKillRank(String sql, String column)
	{
		final StringBuilder tb = new StringBuilder(1024);
		tb.append("<table width=520 cellpadding=1 cellspacing=1>");
		tb.append("<tr><td width=40 align=center><font color=LEVEL>#</font></td><td width=180 align=center><font color=LEVEL>Player</font></td><td width=140 align=center><font color=LEVEL>Score</font></td><td width=140 align=center><font color=LEVEL>Status</font></td></tr>");
		
		int pos = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, LIMIT);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					pos++;
					tb.append("<tr>");
					tb.append("<td width=40 align=center>").append(pos).append("</td>");
					tb.append("<td width=180 align=center><font color=00FFFF>").append(safe(rs.getString("char_name"))).append("</font></td>");
					tb.append("<td width=140 align=center>").append(rs.getInt(column)).append("</td>");
					tb.append("<td width=140 align=center>").append(status(rs.getInt("online"))).append("</td>");
					tb.append("</tr>");
				}
			}
		}
		catch (Exception e)
		{
			tb.append("<tr><td align=center colspan=4>No data.</td></tr>");
		}
		
		if (pos == 0)
			tb.append("<tr><td align=center colspan=4>No data.</td></tr>");
		
		tb.append("</table>");
		return tb.toString();
	}
	
	private static void loadEnchantRanks()
	{
		final List<String> weapons = new ArrayList<>();
		final List<String> armors = new ArrayList<>();
		final List<String> jewels = new ArrayList<>();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(ENCHANT_SQL);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final Item item = ItemTable.getInstance().getTemplate(rs.getInt("item_id"));
				if (item == null)
					continue;
				
				if (item instanceof Weapon)
				{
					if (weapons.size() < LIMIT)
						weapons.add(enchantRow(weapons.size() + 1, rs.getString("char_name"), item.getName(), rs.getInt("enchant_level"), rs.getInt("online")));
				}
				else if (item instanceof Armor)
				{
					if (isJewelry(item))
					{
						if (jewels.size() < LIMIT)
							jewels.add(enchantRow(jewels.size() + 1, rs.getString("char_name"), item.getName(), rs.getInt("enchant_level"), rs.getInt("online")));
					}
					else if (armors.size() < LIMIT)
						armors.add(enchantRow(armors.size() + 1, rs.getString("char_name"), item.getName(), rs.getInt("enchant_level"), rs.getInt("online")));
				}
				
				if (weapons.size() >= LIMIT && armors.size() >= LIMIT && jewels.size() >= LIMIT)
					break;
			}
		}
		catch (Exception e)
		{
			// keep empty lists
		}
		
		_weaponHtml = wrapEnchant(weapons);
		_armorHtml = wrapEnchant(armors);
		_jewelHtml = wrapEnchant(jewels);
	}
	
	private static boolean isJewelry(Item item)
	{
		return (item.getBodyPart() & JEWEL_SLOTS) != 0;
	}
	
	private static String enchantRow(int pos, String name, String itemName, int enchant, int online)
	{
		final StringBuilder tb = new StringBuilder(160);
		tb.append("<tr>");
		tb.append("<td width=40 align=center>").append(pos).append("</td>");
		tb.append("<td width=150 align=center><font color=00FFFF>").append(safe(name)).append("</font></td>");
		tb.append("<td width=190 align=center>+").append(enchant).append(" ").append(safe(itemName)).append("</td>");
		tb.append("<td width=120 align=center>").append(status(online)).append("</td>");
		tb.append("</tr>");
		return tb.toString();
	}
	
	private static String wrapEnchant(List<String> rows)
	{
		final StringBuilder tb = new StringBuilder(1024);
		tb.append("<table width=520 cellpadding=1 cellspacing=1>");
		tb.append("<tr><td width=40 align=center><font color=LEVEL>#</font></td><td width=150 align=center><font color=LEVEL>Player</font></td><td width=190 align=center><font color=LEVEL>Item</font></td><td width=120 align=center><font color=LEVEL>Status</font></td></tr>");
		if (rows.isEmpty())
			tb.append("<tr><td align=center colspan=4>No data.</td></tr>");
		else
		{
			for (String row : rows)
				tb.append(row);
		}
		tb.append("</table>");
		return tb.toString();
	}
	
	private static String status(int online)
	{
		return online == 1 ? "<font color=00FF00>Online</font>" : "<font color=FF0000>Offline</font>";
	}
	
	private static String safe(String value)
	{
		if (value == null || value.isEmpty())
			return "-";
		
		String clean = value.replace("<", "").replace(">", "");
		if (clean.length() > 22)
			clean = clean.substring(0, 22);
		return clean;
	}
}
