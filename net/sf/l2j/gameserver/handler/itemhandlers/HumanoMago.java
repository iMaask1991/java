package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.CambioDeSkin;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class HumanoMago implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		final Player activeChar = (Player) playable;

		CambioDeSkin.HumanoMago(activeChar);
		playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
	}
	
}