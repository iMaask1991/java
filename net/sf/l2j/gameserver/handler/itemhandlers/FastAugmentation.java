package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.AugmentationData;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Double-click Top-Grade Life Stone 76 (8762): apply random TOP augment on equipped S-grade weapon.
 * If the weapon is already augmented, removes it instead (does not consume the life stone).
 */
public class FastAugmentation implements IItemHandler
{
	private static final int GEMSTONE_C = 2131;
	private static final int GEMSTONE_COUNT = 25;
	private static final int LIFE_STONE_LEVEL = 9;
	private static final int LIFE_STONE_GRADE = 3;
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player player = (Player) playable;
		final ItemInstance weapon = player.getActiveWeaponInstance();
		if (weapon == null)
		{
			player.sendMessage("You have to equip a weapon first.");
			return;
		}
		if (!isSGradeWeapon(weapon, player))
			return;
		if (!canUseNow(player))
			return;
		
		if (weapon.isAugmented())
		{
			removeAug(player, weapon);
			return;
		}
		
		if (!payApplyCost(player))
			return;
		
		if (!player.destroyItem("FastAugment", item, 1, player, false))
			return;
		
		final boolean equipped = weapon.isEquipped();
		if (equipped)
			player.getInventory().unEquipItemInSlotAndRecord(weapon.getLocationSlot());
		
		final L2Augmentation augmentation = AugmentationData.getInstance().generateRandomAugmentation(LIFE_STONE_LEVEL, LIFE_STONE_GRADE, player);
		if (!weapon.setAugmentation(augmentation))
		{
			if (equipped)
				player.getInventory().equipItemAndRecord(weapon);
			sendWeaponUpdate(player, weapon);
			player.broadcastUserInfo();
			player.sendMessage("Augmentation failed.");
			return;
		}
		
		final L2Skill skill = weapon.getAugmentation().getSkill();
		final boolean jackpot = skill != null;
		
		if (equipped && !jackpot)
			player.getInventory().equipItemAndRecord(weapon);
		
		sendWeaponUpdate(player, weapon);
		player.broadcastUserInfo();
		player.sendPacket(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED);
		
		if (!jackpot)
			player.sendMessage("No luck, try again!");
		else
		{
			final String type = skill.isChance() ? "CHANCE" : skill.isActive() ? "ACTIVE" : "PASSIVE";
			final String msg = "[" + type + "] " + skill.getName();
			player.sendMessage(msg);
			player.sendPacket(new ExShowScreenMessage(msg, 4000));
			
			final MagicSkillUse fireworks = new MagicSkillUse(player, player, 2025, 1, 1, 0);
			player.sendPacket(fireworks);
			player.broadcastPacket(fireworks);
			player.broadcastPacket(new SocialAction(player, 3));
		}
	}
	
	private static boolean payApplyCost(Player player)
	{
		final ItemInstance gems = player.getInventory().getItemByItemId(GEMSTONE_C);
		if (gems == null || gems.getCount() < GEMSTONE_COUNT)
		{
			player.sendMessage("You do not have enough gemstones to augment. You need " + GEMSTONE_COUNT + " Gemstone C.");
			return false;
		}
		
		if (Config.FAST_AUGMENT_ADENA > 0 && player.getAdena() < Config.FAST_AUGMENT_ADENA)
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return false;
		}
		
		if (!player.destroyItemByItemId("FastAugment", GEMSTONE_C, GEMSTONE_COUNT, player, true))
			return false;
		
		if (Config.FAST_AUGMENT_ADENA > 0 && !player.reduceAdena("FastAugment", Config.FAST_AUGMENT_ADENA, player, true))
			return false;
		
		return true;
	}
	
	private static void removeAug(Player player, ItemInstance weapon)
	{
		if (Config.FAST_REMOVE_ADENA > 0 && player.getAdena() < Config.FAST_REMOVE_ADENA)
		{
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		
		if (Config.FAST_REMOVE_ADENA > 0 && !player.reduceAdena("FastAugmentRemove", Config.FAST_REMOVE_ADENA, player, true))
			return;
		
		final boolean equipped = weapon.isEquipped();
		if (equipped)
			player.getInventory().unEquipItemInSlotAndRecord(weapon.getLocationSlot());
		
		weapon.removeAugmentation();
		
		if (equipped)
			player.getInventory().equipItemAndRecord(weapon);
		
		sendWeaponUpdate(player, weapon);
		player.broadcastUserInfo();
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addItemName(weapon);
		player.sendPacket(sm);
	}
	
	private static boolean isSGradeWeapon(ItemInstance weapon, Player player)
	{
		if (weapon.getOwnerId() != player.getObjectId())
			return false;
		if (weapon.isHeroItem() || weapon.isShadowItem())
		{
			player.sendMessage("Hero weapons can't be augmented!");
			return false;
		}
		if (weapon.getItem().getCrystalType() != CrystalType.S)
		{
			player.sendMessage("Fast Augment is only available for S-grade weapons.");
			return false;
		}
		if (!(weapon.getItem() instanceof Weapon))
		{
			player.sendMessage("This item cannot be augmented.");
			return false;
		}
		
		final WeaponType type = ((Weapon) weapon.getItem()).getItemType();
		if (type == WeaponType.NONE || type == WeaponType.FISHINGROD)
		{
			player.sendMessage("This weapon cannot be augmented.");
			return false;
		}
		
		return true;
	}
	
	private static boolean canUseNow(Player player)
	{
		if (player.isInStoreMode() || player.getActiveTradeList() != null)
		{
			player.sendMessage("You can't use this while trading.");
			return false;
		}
		if (player.isDead() || player.isParalyzed() || player.isSitting())
		{
			player.sendMessage("You can't use this right now.");
			return false;
		}
		if (player.isInCombat())
		{
			player.sendMessage("You can't use this in combat!");
			return false;
		}
		if (player.isInOlympiadMode() || player.isInArenaEvent())
		{
			player.sendMessage("You can't use this in the Olympiad!");
			return false;
		}
		if (player.isCursedWeaponEquipped() || player.isProcessingTransaction())
			return false;
		
		return true;
	}
	
	private static void sendWeaponUpdate(Player player, ItemInstance weapon)
	{
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(weapon);
		player.sendPacket(iu);
	}
}
