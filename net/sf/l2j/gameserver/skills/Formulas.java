package net.sf.l2j.gameserver.skills;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.PveGearTier;
import net.sf.l2j.gameserver.instancemanager.ClassDamageManager;
import net.sf.l2j.gameserver.model.actor.instance.Cubic;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.zone.type.MotherTreeZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public final class Formulas
{
	protected static final CLogger LOGGER = new CLogger(Formulas.class.getName());
	
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	public static final byte SKILL_REFLECT_FAILED = 0; // no reflect
	public static final byte SKILL_REFLECT_SUCCEED = 1; // normal reflect, some damage reflected some other not
	public static final byte SKILL_REFLECT_VENGEANCE = 2; // 100% of the damage affect both
	
	private static final byte MELEE_ATTACK_RANGE = 40;
	
	public static final int MAX_STAT_VALUE = 100;
	
	private static final double[] STR_COMPUTE = new double[]
	{
		1.036,
		34.845
	};
	private static final double[] INT_COMPUTE = new double[]
	{
		1.020,
		31.375
	};
	private static final double[] DEX_COMPUTE = new double[]
	{
		1.009,
		19.360
	};
	private static final double[] WIT_COMPUTE = new double[]
	{
		1.050,
		20.000
	};
	private static final double[] CON_COMPUTE = new double[]
	{
		1.030,
		27.632
	};
	private static final double[] MEN_COMPUTE = new double[]
	{
		1.010,
		-0.060
	};
	
	public static final double[] WIT_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] MEN_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] INT_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] STR_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] DEX_BONUS = new double[MAX_STAT_VALUE];
	public static final double[] CON_BONUS = new double[MAX_STAT_VALUE];
	
	public static final double[] BASE_EVASION_ACCURACY = new double[MAX_STAT_VALUE];
	
	protected static final double[] SQRT_MEN_BONUS = new double[MAX_STAT_VALUE];
	protected static final double[] SQRT_CON_BONUS = new double[MAX_STAT_VALUE];
	
	static
	{
		for (int i = 0; i < STR_BONUS.length; i++)
			STR_BONUS[i] = Math.floor(Math.pow(STR_COMPUTE[0], i - STR_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < INT_BONUS.length; i++)
			INT_BONUS[i] = Math.floor(Math.pow(INT_COMPUTE[0], i - INT_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < DEX_BONUS.length; i++)
			DEX_BONUS[i] = Math.floor(Math.pow(DEX_COMPUTE[0], i - DEX_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < WIT_BONUS.length; i++)
			WIT_BONUS[i] = Math.floor(Math.pow(WIT_COMPUTE[0], i - WIT_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < CON_BONUS.length; i++)
			CON_BONUS[i] = Math.floor(Math.pow(CON_COMPUTE[0], i - CON_COMPUTE[1]) * 100 + .5d) / 100;
		for (int i = 0; i < MEN_BONUS.length; i++)
			MEN_BONUS[i] = Math.floor(Math.pow(MEN_COMPUTE[0], i - MEN_COMPUTE[1]) * 100 + .5d) / 100;
		
		for (int i = 0; i < BASE_EVASION_ACCURACY.length; i++)
			BASE_EVASION_ACCURACY[i] = Math.sqrt(i) * 6;
		
		// Precompute square root values
		for (int i = 0; i < SQRT_CON_BONUS.length; i++)
			SQRT_CON_BONUS[i] = Math.sqrt(CON_BONUS[i]);
		for (int i = 0; i < SQRT_MEN_BONUS.length; i++)
			SQRT_MEN_BONUS[i] = Math.sqrt(MEN_BONUS[i]);
	}
	
	private static final double[] karmaMods =
	{
		0,
		0.772184315,
		2.069377971,
		2.315085083,
		2.467800843,
		2.514219611,
		2.510075822,
		2.532083418,
		2.473028945,
		2.377178509,
		2.285526643,
		2.654635163,
		2.963434737,
		3.266100461,
		3.561112664,
		3.847320291,
		4.123878064,
		4.390194136,
		4.645886341,
		4.890745518,
		5.124704707,
		6.97914069,
		7.270620642,
		7.548951721,
		7.81438302,
		8.067235867,
		8.307889422,
		8.536768399,
		8.754332624,
		8.961068152,
		9.157479758,
		11.41901707,
		11.64989746,
		11.87007991,
		12.08015809,
		12.28072687,
		12.47237891,
		12.65570177,
		12.83127553,
		12.99967093,
		13.16144786,
		15.6563607,
		15.84513182,
		16.02782135,
		16.20501182,
		16.37727218,
		16.54515749,
		16.70920885,
		16.86995336,
		17.02790439,
		17.18356182,
		19.85792061,
		20.04235517,
		20.22556446,
		20.40806338,
		20.59035551,
		20.77293378,
		20.95628115,
		21.1408714,
		21.3271699,
		21.51563446,
		24.3895427,
		24.61486587,
		24.84389213,
		25.07711247,
		25.31501442,
		25.55808296,
		25.80680152,
		26.06165297,
		26.32312062,
		26.59168923,
		26.86784604,
		27.15208178,
		27.44489172,
		27.74677676,
		28.05824444,
		28.37981005,
		28.71199773,
		29.05534154,
		29.41038662,
		29.77769028
	};
	
	/**
	 * @param cha The character to make checks on.
	 * @return the period between 2 regenerations task (3s for Creature, 5 min for L2DoorInstance).
	 */
	public static int getRegeneratePeriod(Creature cha)
	{
		if (cha instanceof Door)
			return HP_REGENERATE_PERIOD * 100; // 5 mins
			
		return HP_REGENERATE_PERIOD; // 3s
	}
	
	/**
	 * @param cha The character to make checks on.
	 * @return the HP regen rate (base + modifiers).
	 */
	public static final double calcHpRegen(Creature cha)
	{
		double init = cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = (cha.isRaidRelated()) ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		
		if (cha.isChampion())
			hpRegenMultiplier *= Config.CHAMPION_HP_REGEN;
		
		if (cha instanceof Player)
		{
			Player player = (Player) cha;
			
			// Calculate correct baseHpReg value for certain level of PC
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;
			
			// SevenSigns Festival modifier
			if (FestivalOfDarknessManager.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			else if (calcSiegeRegenModifer(player))
				hpRegenMultiplier *= 1.5;
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getClanHallId();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHall(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
							hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
				hpRegenMultiplier *= 1.5; // Sitting
			else if (!player.isMoving())
				hpRegenMultiplier *= 1.1; // Staying
			else if (player.isRunning())
				hpRegenMultiplier *= 0.7; // Running
		}
		// Add CON bonus
		init *= cha.getLevelMod() * CON_BONUS[cha.getCON()];
		
		if (init < 1)
			init = 1;
		
		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}
	
	/**
	 * @param cha The character to make checks on.
	 * @return the MP regen rate (base + modifiers).
	 */
	public static final double calcMpRegen(Creature cha)
	{
		double init = cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = (cha.isRaidRelated()) ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		
		if (cha instanceof Player)
		{
			Player player = (Player) cha;
			
			// Calculate correct baseMpReg value for certain level of PC
			init += 0.3 * ((player.getLevel() - 1) / 10.0);
			
			// SevenSigns Festival modifier
			if (FestivalOfDarknessManager.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getClanHallId();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHall(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
							mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
				mpRegenMultiplier *= 1.5; // Sitting
			else if (!player.isMoving())
				mpRegenMultiplier *= 1.1; // Staying
			else if (player.isRunning())
				mpRegenMultiplier *= 0.7; // Running
		}
		// Add MEN bonus
		init *= cha.getLevelMod() * MEN_BONUS[cha.getMEN()];
		
		if (init < 1)
			init = 1;
		
		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}
	
	/**
	 * @param player The player to make checks on.
	 * @return the CP regen rate (base + modifiers).
	 */
	public static final double calcCpRegen(Player player)
	{
		// Calculate correct baseHpReg value for certain level of PC
		double init = player.getTemplate().getBaseHpReg() + ((player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5);
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		
		// Calculate Movement bonus
		if (player.isSitting())
			cpRegenMultiplier *= 1.5; // Sitting
		else if (!player.isMoving())
			cpRegenMultiplier *= 1.1; // Staying
		else if (player.isRunning())
			cpRegenMultiplier *= 0.7; // Running
			
		// Apply CON bonus
		init *= player.getLevelMod() * CON_BONUS[player.getCON()];
		
		if (init < 1)
			init = 1;
		
		return player.calcStat(Stats.REGENERATE_CP_RATE, init, null, null) * cpRegenMultiplier;
	}
	
	public static final double calcFestivalRegenModifier(Player player)
	{
		final int[] festivalInfo = FestivalOfDarknessManager.getInstance().getFestivalForPlayer(player);
		final int festivalId = festivalInfo[1];
		
		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
			return 1.;
		
		int[] festivalCenter;
		
		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		final CabalType oracle = CabalType.VALUES[festivalInfo[0]];
		if (oracle == CabalType.DAWN)
			festivalCenter = FestivalOfDarknessManager.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		else
			festivalCenter = FestivalOfDarknessManager.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		
		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = player.getPlanDistanceSq(festivalCenter[0], festivalCenter[1]);
		
		if (Config.DEVELOPER)
			LOGGER.info("calcFestivalRegenModifier() distance: {}, RegenMulti: {}.", distToCenter, String.format("%1.2f", 1.0 - (distToCenter * 0.0005)));
		
		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}
	
	/**
	 * @param player the player to test on.
	 * @return true if the player is near one of his clan HQ (+50% regen boost).
	 */
	public static final boolean calcSiegeRegenModifer(Player player)
	{
		if (player == null)
			return false;
		
		final Clan clan = player.getClan();
		if (clan == null)
			return false;
		
		final Siege siege = CastleManager.getInstance().getActiveSiege(player);
		if (siege == null || !siege.checkSide(clan, SiegeSide.ATTACKER))
			return false;
		
		return MathUtil.checkIfInRange(200, player, clan.getFlag(), true);
	}
	
	/**
	 * @param attacker The attacker, from where the blow comes from.
	 * @param target The victim of the blow.
	 * @param skill The skill used.
	 * @param shld True if victim was wearign a shield.
	 * @param ss True if ss were activated.
	 * @return blow damage based on cAtk
	 */
	public static double calcBlowDamage(Creature attacker, Creature target, L2Skill skill, byte shld, boolean ss)
	{
		double defence = target.getPDef(attacker);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		final boolean isPvP = attacker instanceof Playable && target instanceof Playable;
		
		double power = skill.getPower(attacker, target);
		double damage = 0;
		damage += calcValakasAttribute(attacker, target, skill);
		
		if (ss)
		{
			damage *= 2.;
			
			if (skill.getSSBoost() > 0)
				power *= skill.getSSBoost();
		}
		
		damage += power;
		damage *= attacker.calcStat(Stats.CRITICAL_DAMAGE, Config.PCRIT_POWER, target, skill);
		damage *= ((attacker.calcStat(Stats.CRITICAL_DAMAGE_POS, 1, target, skill) - 1) / 2 + 1);
		damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.5;
		damage *= target.calcStat(Stats.CRIT_VULN, 1, target, skill);
		
		// get the vulnerability for the instance due to skills (buffs, passives, toggles, etc)
		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		damage *= 70. / defence;
		
		// Bonus contra razas (H5): pAtk-insects/plants/... menos el pDef-* del NPC.
		damage = applyRaceBonus(attacker, target, damage);
		
		// Random weapon damage
		if (Config.RANDOM_DAMAGE)
		{
			if (attacker instanceof Player && target instanceof Player)
				damage += Rnd.nextDouble() * damage / 10;
			else
				damage *= attacker.getRandomDamageMultiplier();
		}
		
		// H5 physicalSkillPower
		if (skill != null)
			damage *= attacker.calcStat(Stats.PHYSICAL_SKILL_POWER, 1, null, null);
		
		// PvP / PvE bonuses
		if (isPvP)
		{
			damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			damage /= Math.max(0.05, target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null));
		}
		else if (appliesPveDamageBonus(attacker, target))
			damage *= attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
		
		// Atributos elementales (H5): puntos de ataque contra puntos de defensa
		damage *= calcElemental(attacker, target, skill);
		// Multiplicador global PvE (Balance.properties, //reload config).
		damage = applyPveMultiplier(attacker, target, damage, false);
		
		damage = applyClassDamageBalance(attacker, target, damage);
		damage = applyShieldDamageReduce(target, damage);
		
		if (!isPvP)
			damage = applyPveDamageGate(attacker, target, damage);
		
		return damage < 1 ? 1. : damage;
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target, called separatly for each weapon, if dual-weapon is used.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill skill used.
	 * @param shld target was using a shield or not.
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @return damage points
	 */
	public static final double calcPhysDam(Creature attacker, Creature target, L2Skill skill, byte shld, boolean crit, boolean ss)
	{
		if (attacker instanceof Player)
		{
			Player pcInst = (Player) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}
		
		double defence = target.getPDef(attacker);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}
		
		final boolean isPvP = attacker instanceof Playable && target instanceof Playable;
		double damage = attacker.getPAtk(target);
		
		damage += calcValakasAttribute(attacker, target, skill);
		
		if (ss)
			damage *= 2;
		
		if (skill != null)
		{
			double skillpower = skill.getPower(attacker, target);
			
			final float ssBoost = skill.getSSBoost();
			if (ssBoost > 0 && ss)
				skillpower *= ssBoost;
			
			damage += skillpower;
		}
		
		// defence modifier depending of the attacker weapon
		Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
			}
		}
		
		if (crit)
		{
			// Same model as magic: base from Balance (PCritPower / class custom), gear/skills via cAtk (add/mul).
			if (Config.ENABLE_CUSTOM_CRIT_POWER && attacker instanceof Player)
			{
				final ClassId classId = ((Player) attacker).getClassId();
				final float basePower = (classId == ClassId.PHANTOM_RANGER || classId == ClassId.GHOST_SENTINEL) ? Config.GHOSTSENTINEL_CRITICAL_POWER : Config.FIGHTER_CRITICAL_POWER;
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, basePower, target, skill) * target.calcStat(Stats.CRIT_VULN, 1, target, null) * (70 * damage / defence);
			}
			else
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, Config.PCRIT_POWER, target, skill) * attacker.calcStat(Stats.CRITICAL_DAMAGE_POS, 1, target, skill) * target.calcStat(Stats.CRIT_VULN, 1, target, null) * (70 * damage / defence);
			
			damage += (attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 70 / defence);
		}
		else
			damage = 70 * damage / defence;
		
		if (stat != null)
			damage = target.calcStat(stat, damage, target, null);
		
		// Weapon random damage ; invalid for CHARGEDAM skills.
		if ((skill == null || skill.getEffectType() != L2SkillType.CHARGEDAM) && Config.RANDOM_DAMAGE)
		{
			if (attacker instanceof Player && target instanceof Player)
				damage += Rnd.nextDouble() * damage / 10;
			else
				damage *= attacker.getRandomDamageMultiplier();
		}
		
		damage = applyRaceBonus(attacker, target, damage);
		
		if (damage > 0 && damage < 1)
			damage = 1;
		else if (damage < 0)
			damage = 0;
		
		// H5 physicalSkillPower
		if (skill != null)
			damage *= attacker.calcStat(Stats.PHYSICAL_SKILL_POWER, 1, null, null);
		
		// PvP / PvE bonuses
		if (isPvP)
		{
			if (skill == null)
			{
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
				damage /= Math.max(0.05, target.calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null));
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
				damage /= Math.max(0.05, target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null));
			}
		}
		else if (appliesPveDamageBonus(attacker, target))
			damage *= attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
		
		// Atributos elementales (H5): piedra del arma / buffs contra la defensa del objetivo
		damage *= calcElemental(attacker, target, skill);
		// Multiplicador global PvE (Balance.properties, //reload config).
		damage = applyPveMultiplier(attacker, target, damage, false);
		
		
		damage = applyClassDamageBalance(attacker, target, damage);
		damage = applyShieldDamageReduce(target, damage);
		
		if (!isPvP)
			damage = applyPveDamageGate(attacker, target, damage);
		
		if (Config.PVE_DAMAGE_DEBUG)
			debugPve(attacker, target, skill, "PHYS", attacker.getPAtk(target), defence, damage);
		
		return damage;
	}
	
	/** Ultimo desglose de daño contra NPC, por personaje (lo muestra //dmg <nombre>). */
	private static final Map<Integer, String> PVE_DEBUG_BY_PLAYER = new ConcurrentHashMap<>();
	/** Ultimo desglose global (compatibilidad). */
	public static volatile String LAST_PVE_DEBUG = "sin datos: pega un golpe a un NPC y despues usa //dmg";
	
	/**
	 * @param objectId : objectId del personaje.
	 * @return el ultimo desglose de ese personaje, o un texto por defecto.
	 */
	public static String getLastPveDebug(int objectId)
	{
		return PVE_DEBUG_BY_PLAYER.getOrDefault(objectId, "sin datos: ese pj no le pego a un NPC todavia (o PveDamageDebug esta en false)");
	}
	
	/**
	 * Guarda el desglose del ultimo golpe a un NPC para //dmg (solo si PveDamageDebug y el que pega es GM).
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param kind "MAGIC" o "PHYS"
	 * @param atk mAtk o pAtk usado en la formula
	 * @param def mDef o pDef del target usado en la formula
	 * @param damage daño final
	 */
	public static void debugPve(Creature attacker, Creature target, L2Skill skill, String kind, double atk, double def, double damage)
	{
		if (!Config.PVE_DAMAGE_DEBUG || !(attacker instanceof Player))
			return;
		
		final Player gm = (Player) attacker;
		final double power = (skill != null) ? skill.getPower(attacker, target) : 1;
		final double elem = (skill != null) ? calcElemental(attacker, target, skill) : 1;
		if (PVE_DEBUG_BY_PLAYER.size() > 2000)
			PVE_DEBUG_BY_PLAYER.clear();
		
		final String line = String.format(
			"%s skill=%d(%s) lvl=%d | atk=%.0f def=%.0f power=%.0f | pveMag=%.3f pvePhys=%.3f | elem=x%.2f | dano=%.0f | target=%s obj=%d lvl=%d hp=%d mDef=%d pDef=%d",
			kind, (skill != null) ? skill.getId() : 0, (skill != null) ? skill.getName() : "-", (skill != null) ? skill.getLevel() : 0,
			atk, def, power,
			gm.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null), gm.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null),
			elem, damage, target.getName(), target.getObjectId(), target.getLevel(), target.getMaxHp(),
			target.getMDef(attacker, skill), target.getPDef(attacker));
		
		LAST_PVE_DEBUG = line;
		PVE_DEBUG_BY_PLAYER.put(gm.getObjectId(), line);
	}
	
	/**
	 * H5 race bonus: pAtk-insects/plants/animals/monsters/magicCreature/giants/dragons
	 * against the pDef-* of the same race on the target. Used by hits and blows.
	 * @param attacker
	 * @param target
	 * @param damage
	 * @return damage multiplied by (1 + (pAtk-race - pDef-race) / 100) if the target is an NPC.
	 */
	public static double applyRaceBonus(Creature attacker, Creature target, double damage)
	{
		if (damage <= 0 || !(target instanceof Npc))
			return damage;
		
		final double bonus;
		switch (((Npc) target).getTemplate().getRace())
		{
			case BEAST:
				bonus = attacker.getPAtkMonsters(target) - target.getPDefMonsters(target);
				break;
			
			case ANIMAL:
				bonus = attacker.getPAtkAnimals(target) - target.getPDefAnimals(target);
				break;
			
			case PLANT:
				bonus = attacker.getPAtkPlants(target) - target.getPDefPlants(target);
				break;
			
			case DRAGON:
				bonus = attacker.getPAtkDragons(target) - target.getPDefDragons(target);
				break;
			
			case BUG:
				bonus = attacker.getPAtkInsects(target) - target.getPDefInsects(target);
				break;
			
			case GIANT:
				bonus = attacker.getPAtkGiants(target) - target.getPDefGiants(target);
				break;
			
			case MAGICCREATURE:
				bonus = attacker.getPAtkMagicCreatures(target) - target.getPDefMagicCreatures(target);
				break;
			
			default:
				return damage;
		}
		
		return damage * (1 + (bonus / 100));
	}
	
	/**
	 * Multiplicador global de dano PvE (jugador/playable -> NPC), configurable en Balance.properties.
	 * No toca PvP. Se lee en cada golpe, asi que //reload config lo aplica sin restart.
	 * @param attacker
	 * @param target
	 * @param damage
	 * @param magic true = usa PveMagicDamageMultiplier, false = PvePhysicalDamageMultiplier.
	 * @return damage multiplicado (o igual si no es PvE o el multiplicador es 1.0).
	 */
	public static double applyPveMultiplier(Creature attacker, Creature target, double damage, boolean magic)
	{
		if (damage <= 0 || !(attacker instanceof Playable) || !(target instanceof Npc))
			return damage;
		
		final double multiplier = magic ? Config.PVE_MAGIC_DAMAGE_MULTIPLIER : Config.PVE_PHYSICAL_DAMAGE_MULTIPLIER;
		if (multiplier != 1.0)
			damage *= multiplier;
		
		// Cap opcional por golpe (no se aplica a raid boss). 0 = desactivado.
		if (Config.PVE_MAX_HIT_PERCENT_OF_HP > 0 && !target.isRaidBoss() && target.getMaxHp() > 0)
			damage = Math.min(damage, target.getMaxHp() * Config.PVE_MAX_HIT_PERCENT_OF_HP / 100.0);
		
		return damage;
	}
	
	/**
	 * Flat physical damage reduce while a shield is equipped.
	 * Base reduce comes from config; skills add {@link Stats#SHIELD_DAMAGE_REDUCE}.
	 */
	public static double applyShieldDamageReduce(Creature target, double damage)
	{
		if (!Config.SHIELD_DAMAGE_REDUCE_ENABLED || damage <= 0)
			return damage;
		
		final Item item = target.getSecondaryWeaponItem();
		if (item == null || !(item instanceof Armor) || ((Armor) item).getItemType() != ArmorType.SHIELD)
			return damage;
		
		double reduce = Config.SHIELD_BASE_DAMAGE_REDUCE + target.calcStat(Stats.SHIELD_DAMAGE_REDUCE, 0, null, null);
		if (reduce <= 0)
			return damage;
		
		reduce = Math.min(Config.SHIELD_DAMAGE_REDUCE_MAX, reduce);
		return damage * (1.0 - reduce);
	}
	
	/** P.BonusDmg / M.BonusDmg vs farm NPCs only. Raid/Grand bosses are excluded. */
	private static boolean appliesPveDamageBonus(Creature attacker, Creature target)
	{
		return attacker instanceof Playable && target instanceof Npc && !target.isRaidBoss();
	}
	
	/**
	 * Progressive gear-tier gate.
	 * Effective tier = min(armor set tier, weapon gearTier, jewelry gearTier with all 5 jewels).
	 * NPC field {@code requiredPveDamage} is the required tier. If lower → damage becomes 1.
	 */
	public static double applyPveDamageGate(Creature attacker, Creature target, double damage)
	{
		if (damage <= 0 || !(attacker instanceof Playable) || !(target instanceof Npc))
			return damage;
		
		if (!hasRequiredPveDamage(attacker, target))
			return 1.0;
		
		return damage;
	}
	
	/**
	 * @return true if attacker may deal full damage to target (no gear-tier requirement, or tier is enough).
	 */
	public static boolean hasRequiredPveDamage(Creature attacker, Creature target)
	{
		if (!(attacker instanceof Playable) || !(target instanceof Npc))
			return true;
		
		final int required = ((Npc) target).getTemplate().getRequiredPveDamage();
		if (required <= 0)
			return true;
		
		final Player player = attacker.getActingPlayer();
		if (player == null)
			return false;
		
		return PveGearTier.getEffectiveTier(player) >= required;
	}
	
	public static final double calcMagicDam(Creature attacker, Creature target, L2Skill skill, byte shld, boolean ss, boolean bss, boolean mcrit)
	{
		if (attacker instanceof Player)
		{
			Player pcInst = (Player) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return 0;
		}
		
		double mDef = target.getMDef(attacker, skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}
		
		double mAtk = attacker.getMAtk(target, skill);
		
		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;
		
		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker, target);
		
		// Failure calculation
		if (Config.MAGIC_FAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker instanceof Player)
			{
				if (calcMagicSuccess(attacker, target, skill) && (target.getLevel() - attacker.getLevel()) <= 9)
				{
					if (skill.getSkillType() == L2SkillType.DRAIN)
						attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
					else
						attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
					
					damage /= 2;
				}
				else
				{
					attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					damage = 1;
				}
			}
			
			if (target instanceof Player)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(attacker));
				else
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(attacker));
			}
		}
	    else if (mcrit)
	    {
	        // Base from Balance (MCritPower / MagicCriticalPower), gear/skills via McAtk (add/mul).
	        if (Config.ENABLE_CUSTOM_CRIT_POWER && attacker instanceof Player)
	            damage *= attacker.calcStat(Stats.MCRITICAL_DAMAGE, Config.MAGIC_CRITICAL_POWER, target, skill);
	        else
	            damage *= attacker.calcStat(Stats.MCRITICAL_DAMAGE, Config.MCRIT_POWER, target, skill);
	        damage *= target.calcStat(Stats.MCRITICAL_VULN, 1, target, skill);
	    }
		
		// PvP / PvE bonuses
		if (attacker instanceof Playable && target instanceof Playable)
		{
			if (skill.isMagic())
			{
				damage *= attacker.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
				damage /= Math.max(0.05, target.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null));
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
				damage /= Math.max(0.05, target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null));
			}
		}
		else if (appliesPveDamageBonus(attacker, target))
		{
			if (skill.isMagic())
				damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			else
				damage *= attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
		}
		
		damage *= calcElemental(attacker, target, skill);
		// Multiplicador global PvE (Balance.properties, //reload config).
		damage = applyPveMultiplier(attacker, target, damage, true);
		
		if (attacker instanceof Player && target instanceof Player && Config.RANDOM_DAMAGE)
			damage += Rnd.nextDouble() * damage / 10;
		
		
		damage = applyClassDamageBalance(attacker, target, damage);
		
		if (!(attacker instanceof Playable && target instanceof Playable))
			damage = applyPveDamageGate(attacker, target, damage);
		
		if (Config.PVE_DAMAGE_DEBUG)
			debugPve(attacker, target, skill, "MAGIC", mAtk, mDef, damage);
		
		return damage;
	}
	
	private static double applyClassDamageBalance(Creature attacker, Creature target, double damage)
	{
		if (!Config.ENABLE_CLASS_DAMAGE_BALANCE)
			return damage;
		
		if (!(attacker instanceof Player) || !(target instanceof Player))
			return damage;
		
		final Player atk = (Player) attacker;
		final Player tgt = (Player) target;
		if (atk.isInOlympiadMode() || tgt.isInOlympiadMode())
			return damage;
		
		return damage * ClassDamageManager.getMultiplier(atk, tgt);
	}
	
	public static final double calcMagicDam(Cubic attacker, Creature target, L2Skill skill, boolean mcrit, byte shld)
	{
		double mDef = target.getMDef(attacker.getOwner(), skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		double damage = 91 / mDef * skill.getPower(attacker.getOwner(), target);
		Player owner = attacker.getOwner();
		
		// Failure calculation
		if (Config.MAGIC_FAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && (target.getLevel() - skill.getMagicLevel()) <= 9)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
				else
					owner.sendPacket(SystemMessageId.ATTACK_FAILED);
				
				damage /= 2;
			}
			else
			{
				owner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
				damage = 1;
			}
			
			if (target instanceof Player)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addCharName(owner));
				else
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addCharName(owner));
			}
		}
		else if (mcrit)
		{
			if (Config.ENABLE_CUSTOM_CRIT_POWER)
				damage *= Config.MAGIC_CRITICAL_POWER;
			else
				damage *= Config.MCRIT_POWER;
			damage *= target.calcStat(Stats.MCRITICAL_VULN, 1, target, skill);
		}
		
		// PvP / PvE bonuses (cubic uses owner stats)
		if (target instanceof Playable)
		{
			if (skill.isMagic())
			{
				damage *= owner.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
				damage /= Math.max(0.05, target.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null));
			}
			else
			{
				damage *= owner.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
				damage /= Math.max(0.05, target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null));
			}
		}
		else if (appliesPveDamageBonus(owner, target))
		{
			if (skill.isMagic())
				damage *= owner.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			else
				damage *= owner.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
		}
		
		damage *= calcElemental(owner, target, skill);
		
		if (!(target instanceof Playable))
			damage = applyPveDamageGate(owner, target, damage);
		
		return damage;
	}
	
	/**
	 * @param attacker The attacker.
	 * @param target The target.
	 * @return true in case of critical hit
	 */
	public static final boolean calcCustomPCrit(Creature attacker, Creature target)
	{
		return calcCrit(applyDefCrit(target, getEffectivePCritRate(attacker, target)));
	}
	
	/**
	 * H5 (Mobius): defCritRate / defCritRateAdd del que recibe el golpe (Light Armor Mastery, etc).
	 * @param target quien recibe el golpe.
	 * @param rate chance de critico (sobre 1000).
	 * @return la chance modificada.
	 */
	public static double applyDefCrit(Creature target, double rate)
	{
		if (target == null)
			return rate;
		return target.calcStat(Stats.DEFENCE_CRITICAL_RATE, rate, null, null) + target.calcStat(Stats.DEFENCE_CRITICAL_RATE_ADD, 0, null, null);
	}
	
	/**
	 * H5 (Mobius calcBlowSuccess): blowChance de la skill * bono DEX * posicion (frente 1, costado 1.5, espalda 2), despues blowRate.
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return true si entra el blow.
	 */
	public static boolean calcBlowH5(Creature attacker, Creature target, L2Skill skill)
	{
		final double dexMod = DEX_BONUS[Math.max(0, Math.min(MAX_STAT_VALUE - 1, attacker.getDEX()))];
		final double sideMod = attacker.isInFrontOfTarget() ? 1 : (attacker.isBehindTarget() ? 2 : 1.5);
		final double rate = attacker.calcStat(Stats.BLOW_RATE, skill.getBlowChance() * dexMod * sideMod, target, null);
		return Rnd.get(100) < rate;
	}
	
	/** Rate / 1000 used for physical crit (DEX + buffs, then optional class cap). */
	public static int getEffectivePCritRate(Creature attacker, Creature target)
	{
		final int rate = attacker.getStat().getCriticalHit(target, null);
		if (!Config.ENABLE_CUSTOM_CRIT || !(attacker instanceof Player))
			return rate;
		return Math.min(rate, getPCritCap(((Player) attacker).getClassId().getId()));
	}
	
	/** Max P.crit /1000 for this class (500 = 50%). Does not replace DEX/buffs. */
	public static int getPCritCap(int classId)
	{
		if (classId == 9 || classId == 92)
			return Config.PCRIT_RATE_ArcherHuman;
		if (classId == 24 || classId == 102)
			return Config.PCRIT_RATE_ArcherElfo;
		if (classId == 37 || classId == 109)
			return Config.PCRIT_RATE_ArcherDarkElfo;
		return Config.MAX_PCRIT_RATE;
	}
	
	/**
	 * @param activeChar The caster.
	 * @param target The target.
	 * @param skill The skill used.
	 * @return true in case of magic critical hit
	 */
	public static final boolean calcCustomMCrit(Creature activeChar, Creature target, L2Skill skill)
	{
		return calcMCrit(getEffectiveMCritRate(activeChar, target, skill));
	}
	
	/** Rate / 1000 used for magic crit (WIT + buffs, then optional class cap). */
	public static int getEffectiveMCritRate(Creature activeChar, Creature target, L2Skill skill)
	{
		final int rate = activeChar.getMCriticalHit(target, skill);
		if (!Config.ENABLE_CUSTOM_CRIT || !(activeChar instanceof Player))
			return rate;
		return Math.min(rate, getMCritCap(((Player) activeChar).getClassId().getId()));
	}
	
	/** Max M.crit /1000 for this class (500 = 50%). Does not replace WIT/buffs. */
	public static int getMCritCap(int classId)
	{
		if (classId == 12 || classId == 94)
			return Config.MCRIT_RATE_Archmage;
		if (classId == 13 || classId == 95)
			return Config.MCRIT_RATE_Soultaker;
		if (classId == 27 || classId == 103)
			return Config.MCRIT_RATE_Mystic_Muse;
		if (classId == 40 || classId == 110)
			return Config.MCRIT_RATE_Storm_Screamer;
		if (classId == 51 || classId == 52 || classId == 115 || classId == 116)
			return Config.MCRIT_RATE_Dominator;
		return Config.MAX_MCRIT_RATE;
	}
	
	/**
	 * @param rate The value to make check on.
	 * @return true in case of critical hit
	 */
	public static final boolean calcCrit(double rate)
	{
		return rate > Rnd.get(1000);
	}
	
	/**
	 * Calcul value of blow success
	 * @param attacker The character delaing the blow.
	 * @param target The victim.
	 * @param chance The base chance of landing a blow.
	 * @return true if successful, false otherwise
	 */
	public static final boolean calcBlow(Creature attacker, Creature target, int chance)
	{
		return attacker.calcStat(Stats.BLOW_RATE, chance * (1.0 + (attacker.getDEX() - 20) / 100), target, null) > Rnd.get(100);
	}
	
	/**
	 * Calcul value of lethal chance
	 * @param attacker The character delaing the blow.
	 * @param target The victim.
	 * @param baseLethal The base lethal chance of the skill.
	 * @param magiclvl
	 * @return
	 */
	public static final double calcLethal(Creature attacker, Creature target, int baseLethal, int magiclvl)
	{
		double chance = 0;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + attacker.getLevel()) / 2) - 1 - target.getLevel();
			
			if (delta >= -3)
				chance = (baseLethal * ((double) attacker.getLevel() / target.getLevel()));
			else if (delta < -3 && delta >= -9)
				chance = (-3) * (baseLethal / (delta));
			else
				chance = baseLethal / 15;
		}
		else
			chance = (baseLethal * ((double) attacker.getLevel() / target.getLevel()));
		
		chance = 10 * attacker.calcStat(Stats.LETHAL_RATE, chance, target, null);
		
		if (Config.DEVELOPER)
			LOGGER.info("Current calcLethal: {} / 1000.", chance);
		
		return chance;
	}
	
	public static final void calcLethalHit(Creature attacker, Creature target, L2Skill skill)
	{
		if (target.isRaidRelated() || target instanceof Door)
			return;
		
		// Insufficient flat PvE rating: block lethal against gated NPCs.
		if (!hasRequiredPveDamage(attacker, target))
			return;
		
		// If one of following IDs is found, return false (Tyrannosaurus x 3, Headquarters)
		if (target instanceof Npc)
		{
			switch (((Npc) target).getNpcId())
			{
				case 22215:
				case 22216:
				case 22217:
				case 35062:
					return;
			}
		}
		
		// Second lethal effect (hp to 1 for npc, cp/hp to 1 for player).
		if (skill.getLethalChance2() > 0 && Rnd.get(1000) < calcLethal(attacker, target, skill.getLethalChance2(), skill.getMagicLevel()))
		{
			if (target instanceof Npc)
				target.reduceCurrentHp(target.getCurrentHp() - 1, attacker, skill);
			else if (target instanceof Player) // If is a active player set his HP and CP to 1
			{
				Player player = (Player) target;
				if (!player.isInvul())
				{
					if (!(attacker instanceof Player && (((Player) attacker).isGM() && !((Player) attacker).getAccessLevel().canGiveDamage())))
					{
						player.setCurrentHp(1);
						player.setCurrentCp(1);
						player.sendPacket(SystemMessageId.LETHAL_STRIKE);
					}
				}
			}
			attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
		}
		// First lethal effect (hp/2 for npc, cp to 1 for player).
		else if (skill.getLethalChance1() > 0 && Rnd.get(1000) < calcLethal(attacker, target, skill.getLethalChance1(), skill.getMagicLevel()))
		{
			if (target instanceof Npc)
				target.reduceCurrentHp(target.getCurrentHp() / 2, attacker, skill);
			else if (target instanceof Player)
			{
				Player player = (Player) target;
				if (!player.isInvul())
				{
					if (!(attacker instanceof Player && (((Player) attacker).isGM() && !((Player) attacker).getAccessLevel().canGiveDamage())))
					{
						player.setCurrentCp(1);
						player.sendPacket(SystemMessageId.LETHAL_STRIKE);
					}
				}
			}
			attacker.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
		}
	}
	
	public static final boolean calcMCrit(int mRate)
	{
		if (Config.DEVELOPER)
			LOGGER.info("Current mCritRate: {} / 1000.", mRate);
		
		return mRate > Rnd.get(1000);
	}
	
	/**
	 * Check if casting process is canceled due to hit.
	 * @param target The target to make checks on.
	 * @param dmg The amount of dealt damages.
	 */
	public static final void calcCastBreak(Creature target, double dmg)
	{
		// Don't go further for invul characters or raid bosses.
		if (target.isRaidRelated() || target.isInvul())
			return;
		
		// Break automatically the skill cast if under attack.
		if (target instanceof Player && ((Player) target).getFusionSkill() != null)
		{
			target.breakCast();
			return;
		}
		
		// Initialization to 15% for magical skills ; don't go further for ppl casting a physical skill
		if (!target.isCastingNow() && (target.getLastSkillCast() != null && !target.getLastSkillCast().isMagic()))
			return;
		
		// Calculate all modifiers for ATTACK_CANCEL ; chance to break is higher with higher dmg, and is affected by target MEN.
		double rate = target.calcStat(Stats.ATTACK_CANCEL, 15 + Math.sqrt(13 * dmg) - (MEN_BONUS[target.getMEN()] * 100 - 100), null, null);
		
		// Adjust the rate to be between 1 and 99
		if (rate > 99)
			rate = 99;
		else if (rate < 1)
			rate = 1;
		
		if (Config.DEVELOPER)
			LOGGER.info("calcCastBreak rate: {}%.", (int) rate);
		
		if (Rnd.get(100) < rate)
			target.breakCast();
	}
	
	/**
	 * Calculate delay (in milliseconds) before next ATTACK.
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return delay in ms.
	 */
	public static final int calcPAtkSpd(Creature attacker, Creature target, double rate)
	{
		int delay;
		if (rate < 2)
			delay = 2700;
		else
			delay = Math.max(100, (int) (500000 / rate));
		
		// Solo players: multiplica el delay entre golpes (AltAttackDelay).
		if (attacker instanceof Player)
			delay = (int) (delay * Config.ALT_ATTACK_DELAY);
		
		return Math.max(100, delay);
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast.
	 * @param attacker
	 * @param skill used to know if skill is magic or no.
	 * @param skillTime
	 * @return delay in ms.
	 */
	public static final int calcAtkSpd(Creature attacker, L2Skill skill, double skillTime)
	{
		int spd = skill.isMagic() ? attacker.getMAtkSpd() : attacker.getPAtkSpd();
		
		// Solo players: tope de velocidad SOLO para el tiempo de casteo de skills (no se ve en Alt+T).
		// Ej: con 9999 de cast y SkillMCastSpdCap = 2100, el skill se castea como si tuviera 2100.
		if (attacker instanceof Player)
		{
			final int cap = skill.isMagic() ? Config.SKILL_MCAST_SPD_CAP : Config.SKILL_PCAST_SPD_CAP;
			if (cap > 0)
				spd = Math.min(spd, cap);
		}
		
		int delay = (int) (skillTime * 333 / Math.max(1, spd));
		
		// Solo players: multiplica hitTime/coolTime de skills (cast speed).
		if (attacker instanceof Player)
			delay = (int) (delay * Config.ALT_CAST_DELAY);
		
		return delay;
	}
	
	/**
	 * Calculate the hit/miss chance.
	 * @param attacker : The attacker to make checks on.
	 * @param target : The target to make checks on.
	 * @return true if hit is missed, false if it evaded.
	 */
	public static boolean calcHitMiss(Creature attacker, Creature target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;
		
		double modifier = 100;
		
		// Get high or low Z bonus.
		if (attacker.getZ() - target.getZ() > 50)
			modifier += 3;
		else if (attacker.getZ() - target.getZ() < -50)
			modifier -= 3;
		
		// Get weather bonus. TODO: rain support (-3%).
		if (GameTimeTaskManager.getInstance().isNight())
			modifier -= 10;
		
		// Get position bonus.
		if (attacker.isBehindTarget())
			modifier += 10;
		else if (!attacker.isInFrontOfTarget())
			modifier += 5;
		
		chance *= modifier / 100;
		
		if (Config.DEVELOPER)
			LOGGER.info("calcHitMiss rate: {}%, modifier : x{}.", chance / 10, +modifier / 100);
		
		return Math.max(Math.min(chance, 980), 200) < Rnd.get(1000);
	}
	
	/**
	 * Test the shield use.
	 * @param attacker The attacker.
	 * @param target The victim ; make check about his shield.
	 * @param skill The skill the attacker has used.
	 * @return 0 = shield defense doesn't succeed<br>
	 *         1 = shield defense succeed<br>
	 *         2 = perfect block
	 */
	public static byte calcShldUse(Creature attacker, Creature target, L2Skill skill)
	{
		// Custom balance: no chance-based shield block; DR is handled in applyShieldDamageReduce().
		if (!Config.SHIELD_BLOCK_ENABLED)
			return SHIELD_DEFENSE_FAILED;
		
		// Ignore shield skills types bypass the shield use.
		if (skill != null && skill.ignoreShield())
			return 0;
		
		Item item = target.getSecondaryWeaponItem();
		if (item == null || !(item instanceof Armor))
			return 0;
		
		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * DEX_BONUS[target.getDEX()];
		if (shldRate == 0.0)
			return 0;
		
		int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null);
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside)))
			return 0;
		
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		
		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		if (attacker.getAttackType() == WeaponType.BOW)
			shldRate *= 1.3;
		
		if (shldRate > 0 && 100 - Config.PERFECT_SHIELD_BLOCK_RATE < Rnd.get(100))
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		else if (shldRate > Rnd.get(100))
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		
		if (target instanceof Player)
		{
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					((Player) target).sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					((Player) target).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}
		
		return shldSuccess;
	}
	
	public static boolean calcMagicAffected(Creature actor, Creature target, L2Skill skill)
	{
		L2SkillType type = skill.getSkillType();
		if (target.isRaidRelated() && !calcRaidAffected(type))
			return false;
		
		double defence = 0;
		
		if (skill.isActive() && skill.isOffensive())
			defence = target.getMDef(actor, skill);
		
		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(actor, target, skill, type);
		double d = (attack - defence) / (attack + defence);
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	public static double calcSkillVulnerability(Creature attacker, Creature target, L2Skill skill, L2SkillType type)
	{
		double multiplier = 1;
		
		// Get the elemental damages.
		if (skill.getElement() > 0)
			multiplier *= calcElemental(attacker, target, skill);
		
		// Missing effectType in skill XML would NPE on switch(null).
		if (type == null)
			return multiplier;
		
		// Get the skillType to calculate its effect in function of base stats of the target.
		switch (type)
		{
			case BLEED:
				multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
				break;
			
			case POISON:
				multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
				break;
			
			case STUN:
				multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
				break;
			
			case PARALYZE:
				multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
				break;
			
			case ROOT:
				multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
				break;
			
			case SLEEP:
				multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
				break;
			
			case MUTE:
			case FEAR:
			case BETRAY:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
			case ERASE:
			case CONFUSION:
				multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
				break;
			
			case DEBUFF:
			case WEAKNESS:
				multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
				break;
			
			case CANCEL:
				multiplier = target.calcStat(Stats.CANCEL_VULN, multiplier, target, null);
				break;
		}
		
		// Return a multiplier (exemple with resist shock : 1 + (-0,4 stun vuln) = 0,6%
		return multiplier;
	}
	
	private static double calcSkillStatModifier(L2SkillType type, Creature target)
	{
		double multiplier = 1;
		
		switch (type)
		{
			case STUN:
			case BLEED:
			case POISON:
				multiplier = 2 - SQRT_CON_BONUS[target.getStat().getCON()];
				break;
			
			case SLEEP:
			case DEBUFF:
			case WEAKNESS:
			case ERASE:
			case ROOT:
			case MUTE:
			case FEAR:
			case BETRAY:
			case CONFUSION:
			case AGGREDUCE_CHAR:
			case PARALYZE:
				multiplier = 2 - SQRT_MEN_BONUS[target.getStat().getMEN()];
				break;
		}
		
		return Math.max(0, multiplier);
	}
	
	public static double getSTRBonus(Creature activeChar)
	{
		return STR_BONUS[activeChar.getSTR()];
	}
	
	private static double getLevelModifier(Creature attacker, Creature target, L2Skill skill)
	{
		if (skill.getLevelDepend() == 0)
			return 1;
		
		int delta = (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()) + skill.getLevelDepend() - target.getLevel();
		return 1 + ((delta < 0 ? 0.01 : 0.005) * delta);
	}
	
	private static double getMatkModifier(Creature attacker, Creature target, L2Skill skill, boolean bss)
	{
		double mAtkModifier = 1;
		
		if (skill.isMagic())
		{
			final double mAtk = attacker.getMAtk(target, skill);
			double val = mAtk;
			if (bss)
				val = mAtk * 4.0;
			
			mAtkModifier = (Math.sqrt(val) / target.getMDef(attacker, skill)) * 11.0;
		}
		return mAtkModifier;
	}
	
	public static boolean calcEffectSuccess(Creature attacker, Creature target, EffectTemplate effect, L2Skill skill, byte shld, boolean bss)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
			return false;
		
		final L2SkillType type = effect.effectType;
		final double baseChance = effect.effectPower;
		
		if (type == null)
			return Rnd.get(100) < baseChance;
		
		if (type.equals(L2SkillType.CANCEL)) // CANCEL type lands always
			return true;
		
		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker, target, skill, type);
		final double mAtkModifier = getMatkModifier(attacker, target, skill, bss);
		final double lvlModifier = getLevelModifier(attacker, target, skill);
		double rate = Math.max(1, Math.min((baseChance * statModifier * skillModifier * mAtkModifier * lvlModifier), 99));
		
		if (Config.ENABLE_CUSTOM_CHANCE_SKILL && !(target instanceof Monster))
			rate = Math.max(1, Math.min(rate * getChanceMultiplier(skill), 99));
		
		if (Config.DEVELOPER)
			LOGGER.info("calcEffectSuccess(): name:{} eff.type:{} power:{} statMod:{} skillMod:{} mAtkMod:{} lvlMod:{} total:{}%.", skill.getName(), type.toString(), baseChance, String.format("%1.2f", statModifier), String.format("%1.2f", skillModifier), String.format("%1.2f", mAtkModifier), String.format("%1.2f", lvlModifier), String.format("%1.2f", rate));
		
		return (Rnd.get(100) < rate);
	}
	
	public static boolean calcSkillSuccess(Creature attacker, Creature target, L2Skill skill, byte shld, boolean bss)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
			return false;
		
		final L2SkillType type = skill.getEffectType();
		
		if (target.isRaidRelated() && !calcRaidAffected(type))
			return false;
		
		final double baseChance = skill.getEffectPower();
		if (skill.ignoreResists())
			return (Rnd.get(100) < baseChance);
		
		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker, target, skill, type);
		final double mAtkModifier = getMatkModifier(attacker, target, skill, bss);
		final double lvlModifier = getLevelModifier(attacker, target, skill);
		double rate = Math.max(1, Math.min((baseChance * statModifier * skillModifier * mAtkModifier * lvlModifier), 99));
		
		if (Config.ENABLE_CUSTOM_CHANCE_SKILL && !(target instanceof Monster))
			rate = Math.max(1, Math.min(rate * getChanceMultiplier(skill), 99));
		
		if (Config.DEVELOPER)
			LOGGER.info("calcSkillSuccess(): name:{} type:{} power:{} statMod:{} skillMod:{} mAtkMod:{} lvlMod:{} total:{}%.", skill.getName(), skill.getSkillType().toString(), baseChance, String.format("%1.2f", statModifier), String.format("%1.2f", skillModifier), String.format("%1.2f", mAtkModifier), String.format("%1.2f", lvlModifier), String.format("%1.2f", rate));
		
		return (Rnd.get(100) < rate);
	}
	
	public static float getChanceMultiplier(L2Skill skill)
	{
		float multiplier = 1;
		
		if (skill == null)
			return multiplier;
		
		switch (skill.getId())
		{
			case 1083:
				multiplier *= Config.SURRENDER_TO_FIRE;
				break;
			case 1339:
				multiplier *= Config.VORTEX_TO_FIRE;
				break;
			case 1074:
				multiplier *= Config.SURRENDER_TO_WIND;
				break;
			case 1341:
				multiplier *= Config.WIND_VORTEX;
				break;
			case 1263:
				multiplier *= Config.CURSE_GLOOM;
				break;
			case 1343:
				multiplier *= Config.DARK_VORTEX;
				break;
			case 1064:
				multiplier *= Config.SILENCE;
				break;
			case 1069:
				multiplier *= Config.SLEEP;
				break;
			case 1169:
				multiplier *= Config.CURSE_FEAR;
				break;
			case 1170:
				multiplier *= Config.ANCHOR;
				break;
			case 1336:
				multiplier *= Config.CURSE_OF_DOOM;
				break;
			case 1337:
				multiplier *= Config.CURSE_OF_ABYSS;
				break;
			case 1056:
				multiplier *= Config.CANCELLATION;
				break;
			case 1298:
				multiplier *= Config.MASS_SLOW;
				break;
			case 1381:
				multiplier *= Config.MASS_FEAR;
				break;
			case 1382:
				multiplier *= Config.MASS_GLOOM;
				break;
			case 1072:
				multiplier *= Config.SLEEPING_CLOUD;
				break;
			case 1071:
				multiplier *= Config.SURRENDER_TO_WATER;
				break;
			case 1340:
				multiplier *= Config.ICE_VORTEX;
				break;
			case 1342:
				multiplier *= Config.LIGHT_VORTEX;
				break;
			case 1375:
				multiplier *= Config.HEROIC_GRANDEUR;
				break;
			case 1376:
				multiplier *= Config.HEROIC_DREAD;
				break;
			case 101:
				multiplier *= Config.STUNNING_SHOT;
				break;
			case 122:
				multiplier *= Config.HEX;
				break;
			case 452:
				multiplier *= Config.SHOCK_STOMP;
				break;
			case 48:
				multiplier *= Config.THUNDER_STORM;
				break;
			case 92:
				multiplier *= Config.SHIELD_STUN;
				break;
			case 353:
				multiplier *= Config.SHIELD_SLAM;
				break;
			case 403:
				multiplier *= Config.SHACKLE;
				break;
			case 404:
				multiplier *= Config.MASS_SHACKLING;
				break;
			case 402:
				multiplier *= Config.ARREST;
				break;
			case 358:
				multiplier *= Config.BLUFF;
				break;
			case 12:
				multiplier *= Config.SWITCH;
				break;
			case 120:
				multiplier *= Config.STUNNING_FIST;
				break;
			case 1092:
				multiplier *= Config.FEAR_OVER;
				break;
			case 1246:
				multiplier *= Config.SEAL_OF_SILENCE;
				break;
			case 1248:
				multiplier *= Config.SEAL_OF_SUSPENSION;
				break;
			case 100:
				multiplier *= Config.STUN_ATTACK;
				break;
			case 362:
				multiplier *= Config.ARMOR_CRUSH;
				break;
			case 1160:
				multiplier *= Config.SLOW;
				break;
			case 1366:
				multiplier *= Config.SEAL_OF_DESPAIR;
				break;
			case 1097:
				multiplier *= Config.DREAMING_SPIRIT;
				break;
			case 1208:
				multiplier *= Config.SEAL_OF_BINDING;
				break;
			case 1344:
				multiplier *= Config.MASS_WARRIOR_BANE;
				break;
			case 1345:
				multiplier *= Config.MASS_MAGE_BANE;
				break;
			case 352:
				multiplier *= Config.SHIELD_BASH;
				break;
			case 361:
				multiplier *= Config.SHOCK_BLAST;
				break;
		}
		
		return multiplier;
	}
	
	public static boolean calcCubicSkillSuccess(Cubic attacker, Creature target, L2Skill skill, byte shld, boolean bss)
	{
		// if target reflect this skill then the effect will fail
		if (calcSkillReflect(target, skill) != SKILL_REFLECT_FAILED)
			return false;
		
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK) // perfect block
			return false;
		
		final L2SkillType type = skill.getEffectType();
		
		if (target.isRaidRelated() && !calcRaidAffected(type))
			return false;
		
		final double baseChance = skill.getEffectPower();
		
		if (skill.ignoreResists())
			return Rnd.get(100) < baseChance;
		
		double mAtkModifier = 1;
		
		// Add Matk/Mdef Bonus
		if (skill.isMagic())
		{
			final double mAtk = attacker.getMAtk();
			double val = mAtk;
			if (bss)
				val = mAtk * 4.0;
			
			mAtkModifier = (Math.sqrt(val) / target.getMDef(null, null)) * 11.0;
		}
		
		final double statModifier = calcSkillStatModifier(type, target);
		final double skillModifier = calcSkillVulnerability(attacker.getOwner(), target, skill, type);
		final double lvlModifier = getLevelModifier(attacker.getOwner(), target, skill);
		final double rate = Math.max(1, Math.min((baseChance * statModifier * skillModifier * mAtkModifier * lvlModifier), 99));
		
		if (Config.DEVELOPER)
			LOGGER.info("calcCubicSkillSuccess(): name:{} type:{} power:{} statMod:{} skillMod:{} mAtkMod:{} lvlMod:{} total:{}%.", skill.getName(), skill.getSkillType().toString(), baseChance, String.format("%1.2f", statModifier), String.format("%1.2f", skillModifier), String.format("%1.2f", mAtkModifier), String.format("%1.2f", lvlModifier), String.format("%1.2f", rate));
		
		return (Rnd.get(100) < rate);
	}
	
	public static boolean calcMagicSuccess(Creature attacker, Creature target, L2Skill skill)
	{
		int lvlDifference = target.getLevel() - ((skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()) + skill.getLevelDepend());
		double rate = 100;
		
		if (lvlDifference > 0)
			rate = (Math.pow(1.166, lvlDifference)) * 100;
		
		if (attacker instanceof Player && ((Player) attacker).getExpertiseWeaponPenalty())
			rate += 6000;
		
		// H5 Deflect Magic (magicSuccRes): multiplica la chance de fallo de la magia.
		rate *= target.calcStat(Stats.MAGIC_SUCCESS_RES, 1, null, skill);
		
		if (Config.DEVELOPER)
			LOGGER.info("calcMagicSuccess(): name:{} lvlDiff:{} fail:{}%.", skill.getName(), lvlDifference, String.format("%1.2f", rate / 100));
		
		rate = Math.min(rate, 9900);
		
		return (Rnd.get(10000) > rate);
	}
	
	public static double calcManaDam(Creature attacker, Creature target, L2Skill skill, boolean ss, boolean bss)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		
		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;
		
		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker, target) * (mp / 97)) / mDef;
		damage *= calcSkillVulnerability(attacker, target, skill, skill.getSkillType());
		return damage;
	}
	
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, Creature caster)
	{
		if (baseRestorePercent == 0 || baseRestorePercent == 100)
			return baseRestorePercent;
		
		double restorePercent = baseRestorePercent * WIT_BONUS[caster.getWIT()];
		if (restorePercent - baseRestorePercent > 20.0)
			restorePercent += 20.0;
		
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		
		return restorePercent;
	}
	
	public static boolean calcPhysicalSkillEvasion(Creature target, L2Skill skill)
	{
		if (skill.isMagic())
			return false;
		
		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill);
	}
	
	public static boolean calcSkillMastery(Creature actor, L2Skill sk)
	{
		// Pointless check for Creature other than players, as initial value will stay 0.
		if (!(actor instanceof Player))
			return false;
		
		if (sk.getSkillType() == L2SkillType.FISHING)
			return false;
		
		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);
		
		if (((Player) actor).isMageClass())
			val *= INT_BONUS[actor.getINT()];
		else
			val *= STR_BONUS[actor.getSTR()];
		
		return Rnd.get(100) < val;
	}
	
	public static double calcValakasAttribute(Creature attacker, Creature target, L2Skill skill)
	{
		double calcPower = 0;
		double calcDefen = 0;
		
		if (skill != null && skill.getAttributeName().contains("valakas"))
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
		}
		else
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			if (calcPower > 0)
			{
				calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
				calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
			}
		}
		return calcPower - calcDefen;
	}
	
	/**
	 * Atributos elementales como High Five (ver ElementAttributes): ataque y defensa en puntos fijos.<br>
	 * Sirve para golpes normales (skill null), skills fisicas, magicas y la chance de los debuffs.
	 * @param attacker : el que pega.
	 * @param target : el que recibe.
	 * @param skill : la skill, o null si es golpe normal.
	 * @return el multiplicador (1 = sin cambio, maximo 2).
	 */
	public static double calcElemental(Creature attacker, Creature target, L2Skill skill)
	{
		return net.sf.l2j.gameserver.element.ElementAttributes.calcAttributeBonus(attacker, target, skill);
	}
	
	/**
	 * Calculate skill reflection according to these three possibilities:
	 * <ul>
	 * <li>Reflect failed</li>
	 * <li>Normal reflect (just effects).</li>
	 * <li>Vengeance reflect (100% damage reflected but damage is also dealt to actor).</li>
	 * </ul>
	 * @param target : The skill's target.
	 * @param skill : The skill to test.
	 * @return SKILL_REFLECTED_FAILED, SKILL_REFLECT_SUCCEED or SKILL_REFLECT_VENGEANCE
	 */
	public static byte calcSkillReflect(Creature target, L2Skill skill)
	{
		// Some special skills (like hero debuffs...) or ignoring resistances skills can't be reflected.
		if (skill.ignoreResists() || !skill.canBeReflected())
			return SKILL_REFLECT_FAILED;
		
		// Only magic and melee skills can be reflected.
		if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE))
			return SKILL_REFLECT_FAILED;
		
		byte reflect = SKILL_REFLECT_FAILED;
		
		// Check for non-reflected skilltypes, need additional retail check.
		switch (skill.getSkillType())
		{
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
				return SKILL_REFLECT_FAILED;
			
			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
				final double venganceChance = target.getStat().calcStat((skill.isMagic()) ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, target, skill);
				if (venganceChance > Rnd.get(100))
					reflect |= SKILL_REFLECT_VENGEANCE;
				break;
		}
		
		final double reflectChance = target.calcStat((skill.isMagic()) ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		if (Rnd.get(100) < reflectChance)
			reflect |= SKILL_REFLECT_SUCCEED;
		
		return reflect;
	}
	
	/**
	 * @param actor : The character affected.
	 * @param fallHeight : The height the NPC fallen.
	 * @return the damage, based on max HPs and falling height.
	 */
	public static double calcFallDam(Creature actor, int fallHeight)
	{
		if (!Config.ENABLE_FALLING_DAMAGE || fallHeight < 0)
			return 0;
		
		return actor.calcStat(Stats.FALL, fallHeight * actor.getMaxHp() / 1000, null, null);
	}
	
	/**
	 * @param type : The L2SkillType to test.
	 * @return true if the L2SkillType can affect a raid boss, false otherwise.
	 */
	public static boolean calcRaidAffected(L2SkillType type)
	{
		switch (type)
		{
			case MANADAM:
			case MDOT:
				return true;
			
			case CONFUSION:
			case ROOT:
			case STUN:
			case MUTE:
			case FEAR:
			case DEBUFF:
			case PARALYZE:
			case SLEEP:
			case AGGDEBUFF:
			case AGGREDUCE_CHAR:
				if (Rnd.get(1000) == 1)
					return true;
		}
		return false;
	}
	
	/**
	 * Calculates karma lost upon death.
	 * @param level : The level of the PKer.
	 * @param exp : The amount of xp earned.
	 * @return The amount of karma player has lost.
	 */
	public static int calculateKarmaLost(int level, long exp)
	{
		return (int) (exp / karmaMods[level] / 15);
	}
	
	/**
	 * Calculates karma gain upon player kill.
	 * @param pkCount : The current number of PK kills.
	 * @param isSummon : Does the victim is a summon or no (lesser karma gain if true).
	 * @return karma points that will be added to the player.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon)
	{
		int result = 14400;
		if (pkCount < 100)
			result = (int) (((((pkCount - 1) * 0.5) + 1) * 60) * 4);
		else if (pkCount < 180)
			result = (int) (((((pkCount + 1) * 0.125) + 37.5) * 60) * 4);
		
		if (isSummon)
			result = ((pkCount & 3) + result) >> 2;
		
		return result;
	}
}