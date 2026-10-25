package net.sf.l2j.gameserver.network.serverpackets;

import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SellBuffsManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.tradelist.TradeItem;

public class PrivateStoreListSell extends L2GameServerPacket
{
	private final int _playerAdena;
	private final Player _storePlayer;
	private final Player _player;
	private final List<TradeItem> _items;
	private final boolean _packageSale;
	
	public PrivateStoreListSell(Player player, Player storePlayer)
	{
		_player = player;
		_playerAdena = player.getAdena();
		_storePlayer = storePlayer;
		_items = _storePlayer.getSellList().getItems();
		_packageSale = _storePlayer.getSellList().isPackaged();
	}
	
	@Override
	protected final void writeImpl()
	{
		if (Config.SELLBUFF_ENABLED && _storePlayer.isSellBuff())
		{
			SellBuffsManager.getInstance().sendBuffMenu(_player, _storePlayer, 0);
			return;
		}
		
		writeC(0x9b);
		writeD(_storePlayer.getObjectId());
		writeD(_packageSale ? 1 : 0);
		writeD(_playerAdena);
		writeD(_items.size());
		
		for (TradeItem item : _items)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(item.getAugmentationId() & 0xFFFF);
			writeH(item.getEnchant());
			writeH(item.getAugmentationId() >> 16);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice()); // your price
			writeD(item.getItem().getReferencePrice()); // store price
		}
	}
}