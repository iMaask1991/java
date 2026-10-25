package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.alchemy.AttributeAlchemyData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ChooseInventoryItem;

/**
 * Attribute Alchemy (Sockets) - doble click al orbe: abre la ventana de enchant del cliente.<br>
 * Cuando el jugador elige el item, RequestEnchantItem le pasa el trabajo a AttributeAlchemyManager.
 */
public class AttributeAlchemyOrb implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player player = (Player) playable;
		if (player.isCastingNow())
			return;
		
		if (!AttributeAlchemyData.getInstance().isEnabled())
		{
			player.sendMessage("El sistema de Sockets esta desactivado.");
			return;
		}
		
		player.sendMessage("Elegi el item (arma, armadura o joya grado S) al que le queres aplicar el orbe.");
		player.setActiveEnchantItem(item);
		player.sendPacket(new ChooseInventoryItem(item.getItemId()));
	}
}
