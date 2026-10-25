package net.sf.l2j.gameserver.model.actor.player;

import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

/**
 * Progressive PvE gear tier:
 * <pre>
 * effective = min(armorTier, weaponTier, jewelryTier)
 * </pre>
 * Jewelry requires all 5 slots filled; tier is the lowest among them.
 */
public final class PveGearTier
{
	private PveGearTier()
	{
	}
	
	public static int getEffectiveTier(Player player)
	{
		if (player == null)
			return 0;
		
		final int armor = getArmorTier(player);
		final int weapon = getWeaponTier(player);
		final int jewelry = getJewelryTier(player);
		if (armor <= 0 || weapon <= 0 || jewelry <= 0)
			return 0;
		
		return Math.min(armor, Math.min(weapon, jewelry));
	}
	
	public static int getArmorTier(Player player)
	{
		return (int) player.calcStat(Stats.PVE_ARMOR_TIER, 0, null, null);
	}
	
	public static int getWeaponTier(Player player)
	{
		final ItemInstance weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon == null)
			return 0;
		
		final Item item = weapon.getItem();
		return item != null ? item.getGearTier() : 0;
	}
	
	/**
	 * Requires necklace + both earrings + both rings. Returns the minimum gearTier among the 5, or 0 if incomplete.
	 */
	public static int getJewelryTier(Player player)
	{
		final Inventory inv = player.getInventory();
		final ItemInstance[] jewels =
		{
			inv.getPaperdollItem(Inventory.PAPERDOLL_NECK),
			inv.getPaperdollItem(Inventory.PAPERDOLL_REAR),
			inv.getPaperdollItem(Inventory.PAPERDOLL_LEAR),
			inv.getPaperdollItem(Inventory.PAPERDOLL_RFINGER),
			inv.getPaperdollItem(Inventory.PAPERDOLL_LFINGER)
		};
		
		int min = Integer.MAX_VALUE;
		for (ItemInstance jewel : jewels)
		{
			if (jewel == null || jewel.getItem() == null)
				return 0;
			
			final int tier = jewel.getItem().getGearTier();
			if (tier <= 0)
				return 0;
			
			if (tier < min)
				min = tier;
		}
		
		return min == Integer.MAX_VALUE ? 0 : min;
	}
}
