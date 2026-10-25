package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.skillnode.ClanSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.EnchantSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.FishingSkillNode;
import net.sf.l2j.gameserver.model.pledge.Clan;

import org.w3c.dom.Document;

/**
 * Loads and stores skill trees: fishing, rebirth, class master, clan and enchant.
 */
public class SkillTreeData implements IXmlReader
{
	private final List<FishingSkillNode> _fishingSkills = new LinkedList<>();
	private final List<FishingSkillNode> _rebirthSkills = new LinkedList<>();
	private final List<FishingSkillNode> _classMasterSkills = new LinkedList<>();
	private final List<ClanSkillNode> _clanSkills = new LinkedList<>();
	private final List<EnchantSkillNode> _enchantSkills = new LinkedList<>();
	
	protected SkillTreeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_fishingSkills.clear();
		_rebirthSkills.clear();
		_classMasterSkills.clear();
		_clanSkills.clear();
		_enchantSkills.clear();
		
		parseFile("./data/xml/skillstrees");
		LOGGER.info("Loaded {} fishing skills.", _fishingSkills.size());
		LOGGER.info("Loaded {} rebirth skills.", _rebirthSkills.size());
		LOGGER.info("Loaded {} class master skills.", _classMasterSkills.size());
		LOGGER.info("Loaded {} clan skills.", _clanSkills.size());
		LOGGER.info("Loaded {} enchant skills.", _enchantSkills.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "clanSkill", clanSkillNode -> _clanSkills.add(new ClanSkillNode(parseAttributes(clanSkillNode))));
			forEach(listNode, "fishingSkill", fishingSkillNode -> _fishingSkills.add(new FishingSkillNode(parseAttributes(fishingSkillNode))));
			forEach(listNode, "rebirthSkill", rebirthSkillNode -> _rebirthSkills.add(new FishingSkillNode(parseAttributes(rebirthSkillNode))));
			forEach(listNode, "classMasterSkill", classMasterSkillNode -> _classMasterSkills.add(new FishingSkillNode(parseAttributes(classMasterSkillNode))));
			forEach(listNode, "enchantSkill", enchantSkillNode -> _enchantSkills.add(new EnchantSkillNode(parseAttributes(enchantSkillNode))));
		});
	}
	
	/**
	 * @return true if this skill id is listed in rebirth tree (protected from removal).
	 *         Class Master skills are NOT protected — admin/delete must work.
	 */
	public boolean isProtectedSkill(int skillId)
	{
		return containsSkillId(_rebirthSkills, skillId);
	}
	
	private static boolean containsSkillId(List<FishingSkillNode> skills, int skillId)
	{
		for (FishingSkillNode skill : skills)
		{
			if (skill.getId() == skillId)
				return true;
		}
		return false;
	}
	
	public List<FishingSkillNode> getFishingSkillsFor(Player player)
	{
		return getAvailableFrom(_fishingSkills, player, true);
	}
	
	public List<FishingSkillNode> getRebirthSkillsFor(Player player)
	{
		return getAvailableFrom(_rebirthSkills, player, false);
	}
	
	public List<FishingSkillNode> getClassMasterSkillsFor(Player player)
	{
		// Same as Rebirth: sequential (next level only).
		return getAvailableFrom(_classMasterSkills, player, false);
	}
	
	private static List<FishingSkillNode> getAvailableFrom(List<FishingSkillNode> source, Player player, boolean checkDwarven)
	{
		return getAvailableFrom(source, player, checkDwarven, false);
	}
	
	private static List<FishingSkillNode> getAvailableFrom(List<FishingSkillNode> source, Player player, boolean checkDwarven, boolean allowJumpToListedLevel)
	{
		final List<FishingSkillNode> result = new ArrayList<>();
		
		for (FishingSkillNode s : source)
		{
			if (s.getMinLvl() > player.getLevel())
				continue;
			
			if (checkDwarven && s.isDwarven() && !player.hasDwarvenCraft())
				continue;
			
			final int current = player.getSkillLevel(s.getId());
			if (allowJumpToListedLevel)
			{
				if (current < s.getValue())
					result.add(s);
			}
			else if (current == s.getValue() - 1)
				result.add(s);
		}
		
		return result;
	}
	
	public FishingSkillNode getFishingSkillFor(Player player, int skillId, int skillLevel)
	{
		return getSkillFrom(_fishingSkills, player, skillId, skillLevel, true);
	}
	
	public FishingSkillNode getRebirthSkillFor(Player player, int skillId, int skillLevel)
	{
		return getSkillFrom(_rebirthSkills, player, skillId, skillLevel, false);
	}
	
	public FishingSkillNode getClassMasterSkillFor(Player player, int skillId, int skillLevel)
	{
		// Same as Rebirth: exact next level only.
		return getSkillFrom(_classMasterSkills, player, skillId, skillLevel, false);
	}
	
	private static FishingSkillNode getSkillFrom(List<FishingSkillNode> source, Player player, int skillId, int skillLevel, boolean checkDwarven)
	{
		return getSkillFrom(source, player, skillId, skillLevel, checkDwarven, false);
	}
	
	private static FishingSkillNode getSkillFrom(List<FishingSkillNode> source, Player player, int skillId, int skillLevel, boolean checkDwarven, boolean allowJumpToListedLevel)
	{
		final FishingSkillNode fsn = source.stream().filter(s -> s.getId() == skillId && s.getValue() == skillLevel && (!checkDwarven || !s.isDwarven() || player.hasDwarvenCraft())).findFirst().orElse(null);
		if (fsn == null)
			return null;
		
		if (fsn.getMinLvl() > player.getLevel())
			return null;
		
		final int current = player.getSkillLevel(skillId);
		if (allowJumpToListedLevel)
		{
			if (current < fsn.getValue())
				return fsn;
			return null;
		}
		
		if (current == fsn.getValue() - 1)
			return fsn;
		
		return null;
	}
	
	public int getRequiredLevelForNextFishingSkill(Player player)
	{
		return getRequiredLevelForNext(_fishingSkills, player, true);
	}
	
	public int getRequiredLevelForNextRebirthSkill(Player player)
	{
		return getRequiredLevelForNext(_rebirthSkills, player, false);
	}
	
	public int getRequiredLevelForNextClassMasterSkill(Player player)
	{
		return getRequiredLevelForNext(_classMasterSkills, player, false);
	}
	
	private static int getRequiredLevelForNext(List<FishingSkillNode> source, Player player, boolean checkDwarven)
	{
		return source.stream().filter(s -> s.getMinLvl() > player.getLevel() && (!checkDwarven || !s.isDwarven() || player.hasDwarvenCraft())).min((s1, s2) -> Integer.compare(s1.getMinLvl(), s2.getMinLvl())).map(s -> s.getMinLvl()).orElse(0);
	}
	
	public List<ClanSkillNode> getClanSkillsFor(Player player)
	{
		final Clan clan = player.getClan();
		if (clan == null)
			return Collections.emptyList();
		
		final List<ClanSkillNode> result = new ArrayList<>();
		
		_clanSkills.stream().filter(s -> s.getMinLvl() <= clan.getLevel()).forEach(s ->
		{
			final L2Skill clanSkill = clan.getClanSkills().get(s.getId());
			if ((clanSkill == null && s.getValue() == 1) || (clanSkill != null && clanSkill.getLevel() == s.getValue() - 1))
				result.add(s);
		});
		
		return result;
	}
	
	public ClanSkillNode getClanSkillFor(Player player, int skillId, int skillLevel)
	{
		final Clan clan = player.getClan();
		if (clan == null)
			return null;
		
		final ClanSkillNode csn = _clanSkills.stream().filter(s -> s.getId() == skillId && s.getValue() == skillLevel).findFirst().orElse(null);
		if (csn == null)
			return null;
		
		if (csn.getMinLvl() > clan.getLevel())
			return null;
		
		final L2Skill clanSkill = clan.getClanSkills().get(skillId);
		if ((clanSkill == null && csn.getValue() == 1) || (clanSkill != null && clanSkill.getLevel() == csn.getValue() - 1))
			return csn;
		
		return null;
	}
	
	public List<EnchantSkillNode> getEnchantSkillsFor(Player player)
	{
		final List<EnchantSkillNode> result = new ArrayList<>();
		
		for (EnchantSkillNode esn : _enchantSkills)
		{
			if (EnchantSkillNode.getEnchantStep(esn.getValue()) > Config.SKILL_ENCHANT_MAX)
				continue;
			
			final L2Skill skill = player.getSkill(esn.getId());
			if (skill != null && ((skill.getLevel() == SkillTable.getInstance().getMaxLevel(skill.getId()) && EnchantSkillNode.isRouteStart(esn.getValue())) || (skill.getLevel() == esn.getValue() - 1)))
			{
				// Guard against duplicated nodes in the XML (would show the same skill twice).
				boolean alreadyAdded = false;
				for (EnchantSkillNode added : result)
				{
					if (added.getId() == esn.getId() && added.getValue() == esn.getValue())
					{
						alreadyAdded = true;
						break;
					}
				}
				
				if (!alreadyAdded)
					result.add(esn);
			}
		}
		return result;
	}
	
	/**
	 * Rutas de enchant de una skill (para el Donation Manager - Skill Enchant).
	 * @param skillId : la skill.
	 * @return ruta (1, 2, 3...) -> nivel mas alto de esa ruta en el arbol (ej 130), respetando SkillEnchantMax. Vacio si no se enchantea.
	 */
	public Map<Integer, Integer> getEnchantRoutes(int skillId)
	{
		final Map<Integer, Integer> routes = new TreeMap<>();
		for (EnchantSkillNode esn : _enchantSkills)
		{
			if (esn.getId() != skillId || EnchantSkillNode.getEnchantStep(esn.getValue()) > Config.SKILL_ENCHANT_MAX)
				continue;
			
			routes.merge(esn.getValue() / 100, esn.getValue(), Math::max);
		}
		return routes;
	}
	
	public EnchantSkillNode getEnchantSkillFor(Player player, int skillId, int skillLevel)
	{
		final EnchantSkillNode esn = _enchantSkills.stream().filter(s -> s.getId() == skillId && s.getValue() == skillLevel).findFirst().orElse(null);
		if (esn == null)
			return null;
		
		if (EnchantSkillNode.getEnchantStep(skillLevel) > Config.SKILL_ENCHANT_MAX)
			return null;
		
		final int currentSkillLevel = player.getSkillLevel(skillId);
		if ((currentSkillLevel == SkillTable.getInstance().getMaxLevel(skillId) && EnchantSkillNode.isRouteStart(skillLevel)) || (currentSkillLevel == skillLevel - 1))
			return esn;
		
		return null;
	}
	
	public static SkillTreeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillTreeData INSTANCE = new SkillTreeData();
	}
}
