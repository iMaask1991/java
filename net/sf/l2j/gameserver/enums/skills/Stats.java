package net.sf.l2j.gameserver.enums.skills;

import java.util.NoSuchElementException;

public enum Stats
{
	// HP & MP
	MAX_HP("maxHp"),
	MAX_MP("maxMp"),
	MAX_CP("maxCp"),
	REGENERATE_HP_RATE("regHp"),
	REGENERATE_CP_RATE("regCp"),
	REGENERATE_MP_RATE("regMp"),
	RECHARGE_MP_RATE("gainMp"),
	HEAL_EFFECTIVNESS("gainHp"),
	HEAL_PROFICIENCY("giveHp"),
	
	// Atk & Def
	POWER_DEFENCE("pDef"),
	MAGIC_DEFENCE("mDef"),
	POWER_ATTACK("pAtk"),
	MAGIC_ATTACK("mAtk"),
	POWER_ATTACK_SPEED("pAtkSpd"),
	MAGIC_ATTACK_SPEED("mAtkSpd"),
	MAGIC_REUSE_RATE("mReuse"),
	P_REUSE("pReuse"),
	SHIELD_DEFENCE("sDef"),
	SHIELD_DEFENCE_ANGLE("shieldDefAngle"),
	SHIELD_RATE("rShld"),
	/** Extra physical damage reduce while a shield is equipped (0.10 = 10%). */
	SHIELD_DAMAGE_REDUCE("shieldDmgReduce"),
	
	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_POS("cAtkPos"),
	CRITICAL_DAMAGE_ADD("cAtkAdd"),
	MCRITICAL_DAMAGE("McAtk"),
	
	PVP_PHYSICAL_DMG("pvpPhysDmg"),
	PVP_MAGICAL_DMG("pvpMagicalDmg"),
	PVP_PHYS_SKILL_DMG("pvpPhysSkillsDmg"),
	PVP_PHYS_SKILL_DEF("pvpPhysSkillsDef"),
	PVP_PHYSICAL_DEF("pvpPhysDef"),
	PVP_MAGICAL_DEF("pvpMagicalDef"),
	
	/** % bonus damage vs NPCs (physical). */
	PVE_PHYSICAL_DMG("pvePhysDmg"),
	/** % bonus damage vs NPCs (magical). */
	PVE_MAGICAL_DMG("pveMagicalDmg"),
	/** Legacy flat rating (unused by gear-tier gate). */
	PVE_DAMAGE("pveDamage"),
	/** Armor-set progressive tier (full set skill). */
	PVE_ARMOR_TIER("pveArmorTier"),
	
	// Atk & Def rates
	EVASION_RATE("rEvas"),
	P_SKILL_EVASION("pSkillEvas"),
	CRITICAL_RATE("rCrit"),
	BLOW_RATE("blowRate"),
	LETHAL_RATE("lethalRate"),
	MCRITICAL_RATE("mCritRate"),
	ATTACK_CANCEL("cancel"),
	
	// Accuracy and range
	ACCURACY_COMBAT("accCombat"),
	POWER_ATTACK_RANGE("pAtkRange"),
	POWER_ATTACK_ANGLE("pAtkAngle"),
	ATTACK_COUNT_MAX("atkCountMax"),
	
	// Run speed
	RUN_SPEED("runSpd"),
	
	// Player-only stats
	STAT_STR("STR"),
	STAT_CON("CON"),
	STAT_DEX("DEX"),
	STAT_INT("INT"),
	STAT_WIT("WIT"),
	STAT_MEN("MEN"),
	
	// stats of various abilities
	BREATH("breath"),
	FALL("fall"),
	
	// Abnormal effects
	AGGRESSION("aggression"),
	BLEED("bleed"),
	POISON("poison"),
	STUN("stun"),
	ROOT("root"),
	MOVEMENT("movement"),
	CONFUSION("confusion"),
	SLEEP("sleep"),
	
	VALAKAS("valakas"),
	VALAKAS_RES("valakasRes"),
	
	// Elemental resistances/vulnerabilities
	FIRE_RES("fireRes"),
	WATER_RES("waterRes"),
	WIND_RES("windRes"),
	EARTH_RES("earthRes"),
	HOLY_RES("holyRes"),
	DARK_RES("darkRes"),
	
	// Elemental power (used for skills such as Holy blade)
	FIRE_POWER("firePower"),
	WATER_POWER("waterPower"),
	WIND_POWER("windPower"),
	EARTH_POWER("earthPower"),
	HOLY_POWER("holyPower"),
	DARK_POWER("darkPower"),
	
	// Vulnerabilities
	BLEED_VULN("bleedVuln"),
	POISON_VULN("poisonVuln"),
	STUN_VULN("stunVuln"),
	PARALYZE_VULN("paralyzeVuln"),
	ROOT_VULN("rootVuln"),
	SLEEP_VULN("sleepVuln"),
	DAMAGE_ZONE_VULN("damageZoneVuln"),
	CRIT_VULN("critVuln"), // Resistance to Crit DMG.
	MCRITICAL_VULN("mCritVuln"), // Resistance to Magic Crit DMG.
	CANCEL_VULN("cancelVuln"),
	DERANGEMENT_VULN("derangementVuln"),
	DEBUFF_VULN("debuffVuln"),
	
	// Weapons vuln
	SWORD_WPN_VULN("swordWpnVuln"),
	BLUNT_WPN_VULN("bluntWpnVuln"),
	DAGGER_WPN_VULN("daggerWpnVuln"),
	BOW_WPN_VULN("bowWpnVuln"),
	POLE_WPN_VULN("poleWpnVuln"),
	DUAL_WPN_VULN("dualWpnVuln"),
	DUALFIST_WPN_VULN("dualFistWpnVuln"),
	BIGSWORD_WPN_VULN("bigSwordWpnVuln"),
	BIGBLUNT_WPN_VULN("bigBluntWpnVuln"),
	
	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_SKILL_MAGIC("reflectSkillMagic"),
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic"),
	VENGEANCE_SKILL_MAGIC_DAMAGE("vengeanceMdam"),
	VENGEANCE_SKILL_PHYSICAL_DAMAGE("vengeancePdam"),
	ABSORB_DAMAGE_PERCENT("absorbDam"),
	TRANSFER_DAMAGE_PERCENT("transDam"),
	
	PATK_PLANTS("pAtk-plants"),
	PATK_INSECTS("pAtk-insects"),
	PATK_ANIMALS("pAtk-animals"),
	PATK_MONSTERS("pAtk-monsters"),
	PATK_DRAGONS("pAtk-dragons"),
	PATK_GIANTS("pAtk-giants"),
	PATK_MCREATURES("pAtk-magicCreature"),
	
	PDEF_PLANTS("pDef-plants"),
	PDEF_INSECTS("pDef-insects"),
	PDEF_ANIMALS("pDef-animals"),
	PDEF_MONSTERS("pDef-monsters"),
	PDEF_DRAGONS("pDef-dragons"),
	PDEF_GIANTS("pDef-giants"),
	PDEF_MCREATURES("pDef-magicCreature"),
	
	// ExSkill :)
	MAX_LOAD("maxLoad"),
	INV_LIM("inventoryLimit"),
	WH_LIM("whLimit"),
	FREIGHT_LIM("FreightLimit"),
	P_SELL_LIM("PrivateSellLimit"),
	P_BUY_LIM("PrivateBuyLimit"),
	REC_D_LIM("DwarfRecipeLimit"),
	REC_C_LIM("CommonRecipeLimit"),
	
	// C4 Stats
	PHYSICAL_MP_CONSUME_RATE("PhysicalMpConsumeRate"),
	MAGICAL_MP_CONSUME_RATE("MagicalMpConsumeRate"),
	DANCE_MP_CONSUME_RATE("DanceMpConsumeRate"),
	
	/** Per-player multipliers (skills/items). Stack on top of Config/VIP/Rune rates. 1.0 = nothing, 1.5 = x1.5. */
	RATE_XP("RateXp"),
	RATE_SP("RateSp"),
	RATE_DROP_ADENA("RateDropAdena"),
	RATE_DROP_ITEMS("RateDropItems"),
	RATE_RAID_DROP_ITEMS("RateRaidDropItems"),
	RATE_DROP_SPOIL("RateDropSpoil"),
	RATE_ENCHANT("RateEnchant"),
	RATE_AUGMENT_SKILL("RateAugmentSkill"),
	
	// Skill mastery
	SKILL_MASTERY("skillMastery"),
	
	// H5: physical skill power (Spirit Sharing, Final Secret...)
	PHYSICAL_SKILL_POWER("physicalSkillPower"),
	
	// H5 (Mobius): Arcane Shield, Seal of Limit, Silent Mind, Deflect Magic, Light Armor Mastery
	MANA_SHIELD("manaShield"),
	MAX_RECOVERABLE_HP("maxRecoverableHp"),
	MAX_RECOVERABLE_MP("maxRecoverableMp"),
	MAX_RECOVERABLE_CP("maxRecoverableCp"),
	BOW_MP_CONSUME_RATE("bowMpConsumeRate"),
	MAGIC_SUCCESS_RES("magicSuccRes"),
	DEFENCE_CRITICAL_RATE("defCritRate"),
	DEFENCE_CRITICAL_RATE_ADD("defCritRateAdd");
	
	public static final int NUM_STATS = values().length;
	
	private String _value;
	
	public String getValue()
	{
		return _value;
	}
	
	private Stats(String s)
	{
		_value = s;
	}
	
	public static Stats valueOfXml(String name)
	{
		name = name.intern();
		for (Stats s : values())
		{
			if (s.getValue().equals(name))
				return s;
		}
		
		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}
}