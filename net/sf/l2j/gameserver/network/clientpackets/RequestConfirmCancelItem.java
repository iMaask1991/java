package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExConfirmCancelItem;

public final class RequestConfirmCancelItem extends L2GameClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player activeChar = getClient().getPlayer();
		if (activeChar == null)
			return;
		
		final ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;
		
		if (item.getOwnerId() != activeChar.getObjectId())
			return;
		
		if (!item.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}
		
		// Flat remove-augment fee for all grades (shown in interface via ExConfirmCancelItem).
		int price;
		switch (item.getItem().getCrystalType())
		{
			case C:
			case B:
			case A:
			case S:
				price = 250000;
				break;
			// any other item type is not augmentable
			default:
				return;
		}
		
		activeChar.sendPacket(new ExConfirmCancelItem(item, price));
	}
}