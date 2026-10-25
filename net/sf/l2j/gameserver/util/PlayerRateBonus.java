package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Multipliers for VIP + Great Rune + skill Stats (artifacts/dolls).
 * Same semantics as drop rates: 1.0 = nothing, 1.5 = x1.5.
 */
public final class PlayerRateBonus
{
	private PlayerRateBonus()
	{
	}
	
	public static double getEnchantMul(Player player)
	{
		if (player == null)
			return 1.0;
		
		double mul = 1.0;
		if (player.isVip())
			mul *= Config.VIP_ENCHANT_RATES;
		
		mul *= getRuneMul(player, Config.RUNE_1_ENCHANT, Config.RUNE_2_ENCHANT, Config.RUNE_3_ENCHANT);
		mul *= player.calcStat(Stats.RATE_ENCHANT, 1, null, null);
		return mul;
	}
	
	/** Augment skill chance only (not glow). */
	public static double getAugmentSkillMul(Player player)
	{
		if (player == null)
			return 1.0;
		
		double mul = 1.0;
		if (player.isVip())
			mul *= Config.VIP_AUGMENT_SKILL_RATES;
		
		mul *= getRuneMul(player, Config.RUNE_1_AUGMENT_SKILL, Config.RUNE_2_AUGMENT_SKILL, Config.RUNE_3_AUGMENT_SKILL);
		mul *= player.calcStat(Stats.RATE_AUGMENT_SKILL, 1, null, null);
		return mul;
	}
	
	private static double getRuneMul(Player player, double rune1, double rune2, double rune3)
	{
		if (player.getInventory().getInventoryItemCount(Config.RUNE_1_ID, 0) >= 1)
			return rune1;
		if (player.getInventory().getInventoryItemCount(Config.RUNE_2_ID, 0) >= 1)
			return rune2;
		if (player.getInventory().getInventoryItemCount(Config.RUNE_3_ID, 0) >= 1)
			return rune3;
		return 1.0;
	}
}
