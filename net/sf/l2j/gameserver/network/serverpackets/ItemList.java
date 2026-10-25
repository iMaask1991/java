package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Set;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class ItemList extends L2GameServerPacket
{
	private final Set<ItemInstance> _items;
	private final boolean _showWindow;
	
	public ItemList(Player cha, boolean showWindow)
	{
		_items = cha.getInventory().getItems();
		_showWindow = showWindow;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x1b);
		writeH(_showWindow ? 0x01 : 0x00);
		writeH(_items.size());
		
		for (ItemInstance temp : _items)
		{
			Item item = temp.getItem();
			
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(net.sf.l2j.gameserver.alchemy.AttributeAlchemyManager.getInstance().getClientType1(temp)); // customType1
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(item.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(net.sf.l2j.gameserver.alchemy.AttributeAlchemyManager.getInstance().getClientType2(temp)); // customType2 / Socket 3
			writeD((temp.isAugmented()) ? temp.getAugmentation().getAugmentationId() : 0x00);
			writeD(net.sf.l2j.gameserver.alchemy.AttributeAlchemyManager.getInstance().getClientMana(temp)); // mana / Sockets
		}
	}
}