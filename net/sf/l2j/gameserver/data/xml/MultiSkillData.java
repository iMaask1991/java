package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Config;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * MultiSkill catalog + persistence. Skills come from class templates; costs from config (short/long) + XML SP.
 */
public class MultiSkillData implements IXmlReader
{
	private static final String LOAD_SKILLS = "SELECT skill_id, skill_level FROM character_multiskills WHERE char_obj_id=?";
	private static final String UPSERT_SKILL = "INSERT INTO character_multiskills (char_obj_id, skill_id, skill_level) VALUES (?,?,?) ON DUPLICATE KEY UPDATE skill_level=VALUES(skill_level)";
	
	private static final int PAGE_SIZE = 8;
	private static final NumberFormat NUM = NumberFormat.getInstance(Locale.GERMANY);
	
	private final Map<ClassId, Map<Integer, TreeMap<Integer, LevelData>>> _catalog = new EnumMap<>(ClassId.class);
	private final Set<Integer> _blacklist = new HashSet<>();
	
	public static MultiSkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	protected MultiSkillData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_blacklist.clear();
		parseFile("./data/xml/multiskill_blacklist.xml");
		
		_catalog.clear();
		int skillEntries = 0;
		
		for (ClassId classId : ClassId.VALUES)
		{
			// Only 3rd classes: templates already include the full parent skill branch.
			if (classId.getRace() == null || classId.level() != 3)
				continue;
			
			final PlayerTemplate template = PlayerData.getInstance().getTemplate(classId);
			if (template == null)
				continue;
			
			final Map<Integer, TreeMap<Integer, LevelData>> bySkill = new LinkedHashMap<>();
			for (GeneralSkillNode node : template.getSkills())
			{
				final int skillId = node.getId();
				if (_blacklist.contains(skillId))
					continue;
				
				bySkill.computeIfAbsent(skillId, k -> new TreeMap<>()).put(node.getValue(), new LevelData(node.getCorrectedCost(), node.getMinLvl()));
			}
			
			if (!bySkill.isEmpty())
			{
				_catalog.put(classId, bySkill);
				skillEntries += bySkill.size();
			}
		}
		
		LOGGER.info("MultiSkillData: {} classes, {} skills, {} blacklisted.", _catalog.size(), skillEntries, _blacklist.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "skill", skillNode ->
		{
			final NamedNodeMap attrs = skillNode.getAttributes();
			final Node idNode = attrs.getNamedItem("id");
			if (idNode != null)
				_blacklist.add(Integer.parseInt(idNode.getNodeValue()));
		}));
	}
	
	public boolean isEnabled()
	{
		return Config.MULTISKILL_ENABLED;
	}
	
	public List<ClassId> getClassesForRace(ClassRace race)
	{
		final List<ClassId> list = new ArrayList<>();
		for (ClassId classId : ClassId.VALUES)
		{
			if (classId.getRace() == race && classId.level() == 3 && _catalog.containsKey(classId))
				list.add(classId);
		}
		return list;
	}
	
	public Map<Integer, TreeMap<Integer, LevelData>> getSkillsForClass(ClassId classId)
	{
		return _catalog.getOrDefault(classId, Collections.emptyMap());
	}
	
	public int getMaxLevel(ClassId classId, int skillId)
	{
		final TreeMap<Integer, LevelData> levels = getSkillsForClass(classId).get(skillId);
		return levels == null || levels.isEmpty() ? 0 : levels.lastKey();
	}
	
	/** Retail SP from class XML, multiplied by Config.MULTISKILL_SP_MULTIPLIER. */
	public int getSpCost(int retailSp)
	{
		if (retailSp <= 0)
			return 0;
		
		final long cost = Math.round(retailSp * Config.MULTISKILL_SP_MULTIPLIER);
		if (cost <= 0)
			return 0;
		return (int) Math.min(cost, Integer.MAX_VALUE);
	}
	
	public void apply(Player player)
	{
		if (!isEnabled() || player == null)
			return;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SKILLS))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int skillId = rs.getInt("skill_id");
					final int skillLevel = rs.getInt("skill_level");
					final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
					if (skill == null)
						continue;
					
					final L2Skill current = player.getSkills().get(skillId);
					if (current == null || current.getLevel() < skillLevel)
						player.addSkill(skill, false);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("MultiSkillData: could not apply skills for {}.", e, player.getName());
		}
	}
	
	public void store(Player player, int skillId, int skillLevel)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT_SKILL))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, skillId);
			ps.setInt(3, skillLevel);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("MultiSkillData: could not store skill {} for {}.", e, skillId, player.getName());
		}
	}
	
	public boolean learnNextLevel(Player player, ClassId classId, int skillId)
	{
		if (!isEnabled())
		{
			player.sendMessage("MultiSkill is disabled.");
			return false;
		}
		
		if (classId == null || classId.level() != 3)
			return false;
		
		final TreeMap<Integer, LevelData> levels = getSkillsForClass(classId).get(skillId);
		if (levels == null || levels.isEmpty())
		{
			player.sendMessage("That skill is not available.");
			return false;
		}
		
		final int current = player.getSkillLevel(skillId);
		final Integer nextLevel = levels.higherKey(Math.max(0, current));
		if (nextLevel == null)
		{
			player.sendMessage("You already own the maximum level of that skill.");
			return false;
		}
		
		final LevelData data = levels.get(nextLevel);
		if (player.getLevel() < data.minLvl)
		{
			player.sendMessage("You need level " + data.minLvl + " to learn this skill.");
			return false;
		}
		
		final int spCost = getSpCost(data.sp);
		
		if (player.getSp() < spCost)
			return false;
		
		final L2Skill skill = SkillTable.getInstance().getInfo(skillId, nextLevel);
		if (skill == null)
			return false;
		
		if (spCost > 0)
			player.removeExpAndSp(0, spCost);
		
		player.addSkill(skill, true);
		store(player, skillId, nextLevel);
		player.sendSkillList();
		player.sendMessage("Learned " + skill.getName() + " Lv " + nextLevel + ".");
		return true;
	}
	
	public void showMain(Player player, int npcObjectId)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npcObjectId);
		html.setFile("data/html/mods/MultiSkill/main.htm");
		html.replace("%objectId%", npcObjectId);
		player.sendPacket(html);
	}
	
	public void showRace(Player player, int npcObjectId, ClassRace race)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npcObjectId);
		html.setFile("data/html/mods/MultiSkill/" + raceFile(race));
		html.replace("%objectId%", npcObjectId);
		player.sendPacket(html);
	}
	
	public void showClass(Player player, int npcObjectId, ClassId classId, int page)
	{
		if (classId == null || classId.level() != 3)
		{
			showMain(player, npcObjectId);
			return;
		}
		
		final Map<Integer, TreeMap<Integer, LevelData>> skills = getSkillsForClass(classId);
		if (skills.isEmpty())
		{
			player.sendMessage("No skills for that class.");
			return;
		}
		
		// Only next levels the player can learn at current character level (retail minLvl).
		final List<Integer> ids = new ArrayList<>();
		for (Map.Entry<Integer, TreeMap<Integer, LevelData>> entry : skills.entrySet())
		{
			final int current = Math.max(0, player.getSkillLevel(entry.getKey()));
			final Integer nextLevel = entry.getValue().higherKey(current);
			if (nextLevel == null)
				continue;
			
			final LevelData data = entry.getValue().get(nextLevel);
			if (player.getLevel() >= data.minLvl)
				ids.add(entry.getKey());
		}
		
		if (ids.isEmpty())
		{
			player.sendMessage("No skills available at your level for this class.");
			showRace(player, npcObjectId, classId.getRace());
			return;
		}
		
		final int maxPage = Math.max(0, (ids.size() - 1) / PAGE_SIZE);
		if (page < 0)
			page = 0;
		if (page > maxPage)
			page = maxPage;
		
		final int from = page * PAGE_SIZE;
		final int to = Math.min(from + PAGE_SIZE, ids.size());
		final StringBuilder skillList = new StringBuilder(2048);
		
		for (int i = from; i < to; i++)
		{
			final int skillId = ids.get(i);
			final TreeMap<Integer, LevelData> levels = skills.get(skillId);
			final int maxLevel = levels.lastKey();
			final int current = Math.max(0, player.getSkillLevel(skillId));
			final int nextLevel = levels.higherKey(current);
			final LevelData data = levels.get(nextLevel);
			final String icon = String.format("icon.skill%04d", skillId);
			
			skillList.append("<tr>");
			skillList.append("<td width=40 align=center><img src=\"").append(icon).append("\" width=32 height=32></td>");
			skillList.append("<td width=170>Lv ").append(current).append("/").append(maxLevel).append(" (Req ").append(data.minLvl).append(")<br1>");
			skillList.append(format(getSpCost(data.sp))).append(" SP</td>");
			skillList.append("<td width=81 align=center>");
			skillList.append("<button value=\"Learn\" action=\"bypass npc_").append(npcObjectId);
			skillList.append("_multiskill learn ").append(classId.getId()).append(" ").append(skillId).append(" ").append(page);
			skillList.append("\" width=81 height=24 back=L2UI_NOVA.btn_i_down fore=L2UI_NOVA.btn_i>");
			skillList.append("</td></tr>");
		}
		
		final String prev = page > 0
			? "<button value=\"Prev\" action=\"bypass npc_" + npcObjectId + "_multiskill class " + classId.getId() + " " + (page - 1) + "\" width=81 height=24 back=L2UI_NOVA.btn_i_down fore=L2UI_NOVA.btn_i>"
			: "";
		final String next = page < maxPage
			? "<button value=\"Next\" action=\"bypass npc_" + npcObjectId + "_multiskill class " + classId.getId() + " " + (page + 1) + "\" width=81 height=24 back=L2UI_NOVA.btn_i_down fore=L2UI_NOVA.btn_i>"
			: "";
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npcObjectId);
		html.setFile("data/html/mods/MultiSkill/class.htm");
		html.replace("%objectId%", npcObjectId);
		html.replace("%skill_list%", skillList.toString());
		html.replace("%button_prev%", prev);
		html.replace("%button_next%", next);
		html.replace("%race%", classId.getRace().name());
		player.sendPacket(html);
	}
	
	private static String raceFile(ClassRace race)
	{
		switch (race)
		{
			case HUMAN:
				return "human.htm";
			case ELF:
				return "elf.htm";
			case DARK_ELF:
				return "dark_elf.htm";
			case ORC:
				return "orc.htm";
			case DWARF:
				return "dwarf.htm";
			default:
				return "main.htm";
		}
	}
	
	private static String format(long value)
	{
		return NUM.format(value);
	}
	
	private static final class LevelData
	{
		private final int sp;
		private final int minLvl;
		
		private LevelData(int sp, int minLvl)
		{
			this.sp = sp;
			this.minLvl = minLvl;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final MultiSkillData INSTANCE = new MultiSkillData();
	}
}
