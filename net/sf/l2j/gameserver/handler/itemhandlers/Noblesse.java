package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

public class Noblesse implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		final Player activeChar = (Player) playable;
		if (!activeChar.isNoble())
		{
			activeChar.setNoble(true, true);
			activeChar.sendPacket(new UserInfo(activeChar));
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
			activeChar.sendMessage("Congratulations new noblesse");
		}
		else
		{
			activeChar.sendMessage("Already noblesse");
		}
	}
	
}