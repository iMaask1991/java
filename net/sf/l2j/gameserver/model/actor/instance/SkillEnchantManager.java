package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.skillnode.EnchantSkillNode;
import net.sf.l2j.gameserver.util.PlayerRateBonus;

/**
 * Config-driven skill enchant helper.<br>
 * Enchant is unified: every {@link Folk} that opens the enchant window (town trainers, warehouse
 * keepers, MultiServiceNpc, etc.) uses the exact same rates and Safe item defined in
 * SkillEnchant.properties. There is no separate "retail XML" branch anymore.
 */
public final class SkillEnchantManager
{
	private SkillEnchantManager()
	{
	}
	
	/**
	 * @return true if this {@link Folk} may use the config-driven enchant system. Enchant is unified,
	 *         so any Folk qualifies.
	 */
	public static boolean isCustom(Folk folk)
	{
		return folk != null;
	}
	
	public static boolean isSafe(Player player, Folk folk)
	{
		return Config.SKILL_ENCHANT_CUSTOM_ENABLED && isCustom(folk) && player.isSkillEnchantSafe();
	}
	
	public static int getChance(Player player, EnchantSkillNode esn)
	{
		int chance;
		if (isSafe(player, player.getCurrentFolk()))
			chance = Config.SKILL_ENCHANT_SAFE_CHANCE;
		else if (Config.SKILL_ENCHANT_NORMAL_CHANCE >= 0)
			chance = Config.SKILL_ENCHANT_NORMAL_CHANCE;
		else
		{
			int idx = player.getLevel() - 76;
			if (idx < 0)
				idx = 0;
			else if (idx > 4)
				idx = 4;
			
			if (Config.SKILL_ENCHANT_NORMAL_BY_LEVEL[idx] >= 0)
				chance = Config.SKILL_ENCHANT_NORMAL_BY_LEVEL[idx];
			else
				chance = esn.getEnchantRate(player.getLevel());
		}
		
		return (int) Math.min(100, Math.round(chance * PlayerRateBonus.getEnchantMul(player)));
	}
}
