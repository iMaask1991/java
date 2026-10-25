package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.element.ItemElementData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ChooseInventoryItem;

/**
 * Elementos de items - doble click a la piedra: abre la ventana de enchant del cliente.<br>
 * Cuando el jugador elige el item, RequestEnchantItem le pasa el trabajo a ItemElementManager.
 */
public class ElementStone implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player player = (Player) playable;
		if (player.isCastingNow())
			return;
		
		if (!ItemElementData.getInstance().isEnabled())
		{
			player.sendMessage("El sistema de elementos esta desactivado.");
			return;
		}
		
		player.sendMessage("Elegi el arma o armadura grado " + ItemElementData.getInstance().getMinGrade() + " a la que le queres poner el elemento.");
		player.setActiveEnchantItem(item);
		player.sendPacket(new ChooseInventoryItem(item.getItemId()));
	}
}
