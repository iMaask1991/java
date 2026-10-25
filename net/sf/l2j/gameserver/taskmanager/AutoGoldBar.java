package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

public class AutoGoldBar implements Runnable
{
	@Override
	public final void run()
	{
		if (_players.isEmpty())
			return;
		
		for (Player player : _players.keySet())
			convert(player);
	}
	
	/**
	 * Converts every full chunk of BankingAdenaCount into gold bars.
	 * One pass, not one bar per tick — otherwise farm climbs to 2.1B.
	 */
	public static void convert(Player player)
	{
		if (player == null || !player.isOnline() || !player.isAutoGb())
			return;
		
		final int chunk = Config.BANKING_SYSTEM_ADENA;
		if (chunk <= 0)
			return;
		
		int converted = 0;
		while (player.getAdena() >= chunk && converted < 50)
		{
			if (!player.getInventory().reduceAdena("Adena Coin(s)", chunk, player, null))
				break;
			
			player.getInventory().addItem("Adena Coin(s)", Config.BANKING_SYSTEM_GOLDBAR_ID, Config.BANKING_SYSTEM_GOLDBARS, player, null);
			converted++;
		}
		
		if (converted <= 0)
			return;
		
		final ItemInstance adena = player.getInventory().getItemByItemId(57);
		if (adena != null)
			adena.updateDatabase();
		
		final ItemInstance bar = player.getInventory().getItemByItemId(Config.BANKING_SYSTEM_GOLDBAR_ID);
		if (bar != null)
			bar.updateDatabase();
		
		player.sendPacket(new ItemList(player, false));
	}
	
	private final Map<Player, Long> _players = new ConcurrentHashMap<>();
	
	protected AutoGoldBar()
	{
		final long intervalMs = Config.AUTO_GOLDBAR_INTERVAL * 1000L;
		ThreadPool.scheduleAtFixedRate(this, intervalMs, intervalMs);
	}
	
	public final void add(Player player)
	{
		_players.put(player, System.currentTimeMillis());
		convert(player);
	}
	
	public final void remove(Creature player)
	{
		_players.remove(player);
	}
	
	public static final AutoGoldBar getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoGoldBar _instance = new AutoGoldBar();
	}
}
