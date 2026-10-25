package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

/**
 * Right-click Gold Bar: consume 1 item → BankingAdenaCount adena.
 * Item XML: handler="GoldBar". ID from BankingGoldbarItemId.
 */
public class GoldBar implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player player = (Player) playable;
		
		if (item.getItemId() != Config.BANKING_SYSTEM_GOLDBAR_ID)
			return;
		
		if (Config.BANKING_SYSTEM_ADENA <= 0 || Config.BANKING_SYSTEM_GOLDBARS <= 0)
			return;
		
		final long currentAdena = player.getInventory().getInventoryItemCount(57, 0);
		if (currentAdena + Config.BANKING_SYSTEM_ADENA > Integer.MAX_VALUE)
		{
			player.sendMessage("You do not have enough space for all the adena in inventory!");
			return;
		}
		
		if (!player.destroyItem("GoldBar", item, Config.BANKING_SYSTEM_GOLDBARS, null, true))
			return;
		
		player.getInventory().addAdena("GoldBar", Config.BANKING_SYSTEM_ADENA, player, null);
		player.getInventory().updateDatabase();
		player.sendPacket(new ItemList(player, false));
	}
}
