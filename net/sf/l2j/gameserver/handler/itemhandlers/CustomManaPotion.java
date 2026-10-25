package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class CustomManaPotion implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		final Player activeChar = (Player) playable;

		activeChar.setCurrentMp(2000 + activeChar.getCurrentMp());
		StatusUpdate sump = new StatusUpdate(activeChar);
		sump.addAttribute(StatusUpdate.CUR_MP, (int) activeChar.getCurrentMp());
		activeChar.sendPacket(sump);
		playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_S2).addString("Restored MP."));
	}
	
}