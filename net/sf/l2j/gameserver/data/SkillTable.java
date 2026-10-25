package net.sf.l2j.gameserver.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.DocumentSkill;

public class SkillTable
{
	private static final Logger _log = Logger.getLogger(SkillTable.class.getName());
	
	private static final Map<Integer, L2Skill> _skills = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> _skillMaxLevel = new ConcurrentHashMap<>();
	
	/** Ids de skills que se lanzan por chance (ChanceSkillTrigger). Estilo H5 tienen su propia fila de buffs. */
	private static volatile java.util.Set<Integer> _triggeredSkills = java.util.Collections.emptySet();
	
	private static final L2Skill[] _heroSkills = new L2Skill[5];
	private static final int[] _heroSkillsId =
	{
		395,
		396,
		1374,
		1375,
		1376
	};
	
	private static final L2Skill[] _nobleSkills = new L2Skill[8];
	private static final int[] _nobleSkillsId =
	{
		325,
		326,
		327,
		1323,
		1324,
		1325,
		1326,
		1327
	};
	
	public static SkillTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected SkillTable()
	{
		load();
	}
	
	private void load()
	{
		applySkills(parseSkills());
	}
	
	private static Map<Integer, L2Skill> parseSkills()
	{
		final Map<Integer, L2Skill> skills = new HashMap<>();
		final File dir = new File("./data/xml/skills");
		final File[] files = dir.listFiles();
		if (files == null)
			throw new IllegalStateException("Skill XML folder not found: " + dir.getAbsolutePath());
		
		for (File file : files)
		{
			if (!file.isFile())
				continue;
			
			DocumentSkill doc = new DocumentSkill(file);
			doc.parse();
			
			for (L2Skill skill : doc.getSkills())
				skills.put(getSkillHashCode(skill), skill);
		}
		return skills;
	}
	
	private void applySkills(Map<Integer, L2Skill> skills)
	{
		_skills.keySet().removeIf(k -> !skills.containsKey(k));
		_skills.putAll(skills);
		
		_log.info("SkillTable: Loaded " + _skills.size() + " skills.");
		
		_skillMaxLevel.clear();
		for (final L2Skill skill : _skills.values())
		{
			final int skillLvl = skill.getLevel();
			if (skillLvl < 99)
			{
				final int skillId = skill.getId();
				final int maxLvl = getMaxLevel(skillId);
				
				if (skillLvl > maxLvl)
					_skillMaxLevel.put(skillId, skillLvl);
			}
		}
		
		final java.util.Set<Integer> triggered = new java.util.HashSet<>();
		for (final L2Skill skill : _skills.values())
		{
			if (skill.isChance() && skill.getTriggeredChanceId() > 0)
				triggered.add(skill.getTriggeredChanceId());
			
			for (net.sf.l2j.gameserver.skills.effects.EffectTemplate et : skill.getEffectTemplateList())
			{
				if (et.triggeredId > 0)
					triggered.add(et.triggeredId);
			}
		}
		_triggeredSkills = triggered;
		_log.info("SkillTable: Loaded " + triggered.size() + " triggered skills.");
		
		for (FrequentSkill sk : FrequentSkill.values())
			sk._skill = getInfo(sk._id, sk._level);
		
		for (int i = 0; i < _heroSkillsId.length; i++)
			_heroSkills[i] = getInfo(_heroSkillsId[i], 1);
		
		for (int i = 0; i < _nobleSkills.length; i++)
			_nobleSkills[i] = getInfo(_nobleSkillsId[i], 1);
	}
	
	public void reload()
	{
		final Map<Integer, L2Skill> skills = parseSkills();
		if (skills.isEmpty())
			throw new IllegalStateException("Skill reload aborted: 0 skills parsed.");
		
		applySkills(skills);
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(L2Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		// 1024: los niveles de enchant llegan hasta la ruta 9 (901-930).
		return skillId * 1024 + skillLevel;
	}
	
	public static boolean isTriggeredSkill(int skillId)
	{
		return _triggeredSkills.contains(skillId);
	}
	
	public L2Skill getInfo(int skillId, int level)
	{
		return _skills.get(getSkillHashCode(skillId, level));
	}
	
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _skillMaxLevel.get(skillId);
		return (maxLevel != null) ? maxLevel : 0;
	}
	
	/**
	 * @param addNoble if true, will add also Advanced headquarters.
	 * @return an array with siege skills.
	 */
	public L2Skill[] getSiegeSkills(boolean addNoble)
	{
		L2Skill[] temp = new L2Skill[2 + (addNoble ? 1 : 0)];
		int i = 0;
		
		temp[i++] = _skills.get(SkillTable.getSkillHashCode(246, 1));
		temp[i++] = _skills.get(SkillTable.getSkillHashCode(247, 1));
		
		if (addNoble)
			temp[i++] = _skills.get(SkillTable.getSkillHashCode(326, 1));
		
		return temp;
	}
	
	public static L2Skill[] getHeroSkills()
	{
		return _heroSkills;
	}
	
	public static boolean isHeroSkill(int skillid)
	{
		for (int id : _heroSkillsId)
			if (id == skillid)
				return true;
			
		return false;
	}
	
	public static L2Skill[] getNobleSkills()
	{
		return _nobleSkills;
	}
	
	/**
	 * Enum to hold some important references to frequently used (hardcoded) skills in core
	 * @author DrHouse
	 */
	public static enum FrequentSkill
	{
		LUCKY(194, 1),
		BLESSING_OF_PROTECTION(5182, 1),
		
		SEAL_OF_RULER(246, 1),
		BUILD_HEADQUARTERS(247, 1),
		STRIDER_SIEGE_ASSAULT(325, 1),
		
		DWARVEN_CRAFT(1321, 1),
		COMMON_CRAFT(1322, 1),
		
		FIREWORK(5965, 1),
		LARGE_FIREWORK(2025, 1),
		SPECIAL_TREE_RECOVERY_BONUS(2139, 1),
		
		ANTHARAS_JUMP(4106, 1),
		ANTHARAS_TAIL(4107, 1),
		ANTHARAS_FEAR(4108, 1),
		ANTHARAS_DEBUFF(4109, 1),
		ANTHARAS_MOUTH(4110, 1),
		ANTHARAS_BREATH(4111, 1),
		ANTHARAS_NORMAL_ATTACK(4112, 1),
		ANTHARAS_NORMAL_ATTACK_EX(4113, 1),
		ANTHARAS_SHORT_FEAR(5092, 1),
		ANTHARAS_METEOR(5093, 1),
		
		QUEEN_ANT_BRANDISH(4017, 1),
		QUEEN_ANT_STRIKE(4018, 1),
		QUEEN_ANT_SPRINKLE(4019, 1),
		NURSE_HEAL_1(4020, 1),
		NURSE_HEAL_2(4024, 1),
		
		ZAKEN_TELE(4216, 1),
		ZAKEN_MASS_TELE(4217, 1),
		ZAKEN_DRAIN(4218, 1),
		ZAKEN_HOLD(4219, 1),
		ZAKEN_DUAL_ATTACK(4220, 1),
		ZAKEN_MASS_DUAL_ATTACK(4221, 1),
		ZAKEN_SELF_TELE(4222, 1),
		ZAKEN_NIGHT_TO_DAY(4223, 1),
		ZAKEN_DAY_TO_NIGHT(4224, 1),
		ZAKEN_REGEN_NIGHT(4227, 1),
		ZAKEN_REGEN_DAY(4242, 1),
		
		RAID_CURSE(4215, 1),
		RAID_CURSE2(4515, 1),
		RAID_ANTI_STRIDER_SLOW(4258, 1),
		
		WYVERN_BREATH(4289, 1),
		ARENA_CP_RECOVERY(4380, 1),
		VARKA_KETRA_PETRIFICATION(4578, 1),
		FAKE_PETRIFICATION(4616, 1),
		
		THE_VICTOR_OF_WAR(5074, 1),
		THE_VANQUISHED_OF_WAR(5075, 1);
		
		protected final int _id;
		protected final int _level;
		protected L2Skill _skill = null;
		
		private FrequentSkill(int id, int level)
		{
			_id = id;
			_level = level;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final SkillTable _instance = new SkillTable();
	}
}