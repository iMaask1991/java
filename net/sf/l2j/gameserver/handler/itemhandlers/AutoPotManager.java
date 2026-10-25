package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Auto potions (HP / MP / CP). Toggle with .on / .off.
 */
public final class AutoPotManager
{
	private static final int[] HP_IDS =
	{
		1540
	};
	private static final int[] MP_IDS =
	{
		728
	};
	private static final int[] CP_IDS =
	{
		5592
	};
	
	private AutoPotManager()
	{
	}
	
	public static boolean isPotion(int itemId)
	{
		return itemId == 728 || itemId == 1540 || itemId == 5592;
	}
	
	public static boolean isOn(Player player)
	{
		for (int id : HP_IDS)
		{
			if (player.isAutoPot(id))
				return true;
		}
		for (int id : MP_IDS)
		{
			if (player.isAutoPot(id))
				return true;
		}
		for (int id : CP_IDS)
		{
			if (player.isAutoPot(id))
				return true;
		}
		return false;
	}
	
	public static void toggle(Player player, int itemId)
	{
		if (!isPotion(itemId))
			return;
		
		if (player.isAutoPot(itemId))
			stopOne(player, itemId);
		else
			startOne(player, itemId);
	}
	
	public static void startOne(Player player, int itemId)
	{
		if (!isPotion(itemId) || player.isAutoPot(itemId))
			return;
		
		if (startGroup(player, new int[]
		{
			itemId
		}, interval(itemId)) == 0)
		{
			player.sendMessage("No potion in inventory: " + itemId + ".");
			return;
		}
		
		player.sendMessage("Auto potion ON: " + itemId + ".");
	}
	
	public static void stopOne(Player player, int itemId)
	{
		if (!isPotion(itemId) || !player.isAutoPot(itemId))
			return;
		
		stopGroup(player, new int[]
		{
			itemId
		});
		player.sendMessage("Auto potion OFF: " + itemId + ".");
	}
	
	private static int interval(int itemId)
	{
		switch (itemId)
		{
			case 1540:
				return Config.HP_POT_CD;
			case 728:
				return Config.MP_POT_CD;
			case 5592:
				return Config.CP_POT_CD;
			default:
				return 1000;
		}
	}
	
	public static void start(Player player)
	{
		if (isOn(player))
		{
			player.sendMessage("Auto potions already ON.");
			return;
		}
		
		int started = 0;
		started += startGroup(player, HP_IDS, Config.HP_POT_CD);
		started += startGroup(player, MP_IDS, Config.MP_POT_CD);
		started += startGroup(player, CP_IDS, Config.CP_POT_CD);
		
		if (started == 0)
		{
			player.sendMessage("No potions in inventory (HP 1540, MP 728, CP 5592).");
			return;
		}
		
		player.sendMessage("Auto potions ON.");
		player.sendPacket(new ExShowScreenMessage("Auto potions ON", 2000));
	}
	
	public static void stop(Player player)
	{
		stopGroup(player, HP_IDS);
		stopGroup(player, MP_IDS);
		stopGroup(player, CP_IDS);
		player.sendMessage("Auto potions OFF.");
		player.sendPacket(new ExShowScreenMessage("Auto potions OFF", 2000));
	}
	
	private static int startGroup(Player player, int[] ids, int interval)
	{
		int count = 0;
		for (int id : ids)
		{
			final ItemInstance item = player.getInventory().getItemByItemId(id);
			if (item == null || item.getCount() <= 0)
				continue;
			
			player.sendPacket(new ExAutoSoulShot(id, 1));
			player.setAutoPot(id, ThreadPool.scheduleAtFixedRate(new AutoPot(id, player), 500, Math.max(200, interval)), true);
			count++;
		}
		return count;
	}
	
	private static void stopGroup(Player player, int[] ids)
	{
		for (int id : ids)
		{
			if (!player.isAutoPot(id))
				continue;
			
			player.sendPacket(new ExAutoSoulShot(id, 0));
			player.setAutoPot(id, null, false);
		}
	}
	
	private static final class AutoPot implements Runnable
	{
		private final int _id;
		private final Player _player;
		
		private AutoPot(int id, Player player)
		{
			_id = id;
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player == null || !_player.isOnline())
			{
				if (_player != null && _player.isAutoPot(_id))
					_player.setAutoPot(_id, null, false);
				return;
			}
			
			// Dead: skip this tick, keep the toggle so it resumes after revive.
			if (_player.isDead() || _player.isAlikeDead())
				return;
			
			final ItemInstance item = _player.getInventory().getItemByItemId(_id);
			if (item == null || item.getCount() <= 0)
			{
				_player.sendPacket(new ExAutoSoulShot(_id, 0));
				_player.setAutoPot(_id, null, false);
				_player.sendMessage("Auto potion stopped: no more item " + _id + ".");
				return;
			}
			
			if (!needsPotion(_id, _player))
				return;
			
			new ItemSkills().useItem(_player, item, true);
			_player.broadcastPacket(new MagicSkillUse(_player, _player, skillId(_id), 1, 0, 100));
		}
		
		private static boolean needsPotion(int id, Player player)
		{
			switch (id)
			{
				case 1540:
					return player.getCurrentHp() < player.getMaxHp() * Config.HP_REQUIRED;
				case 728:
					return player.getCurrentMp() < player.getMaxMp() * Config.MP_REQUIRED;
				case 5592:
					return player.getCurrentCp() < player.getMaxCp() * Config.CP_REQUIRED;
				default:
					return false;
			}
		}
		
		private static int skillId(int itemId)
		{
			switch (itemId)
			{
				case 1540:
					return 2038;
				case 728:
					return 2279;
				case 5592:
					return 2166;
				default:
					return 0;
			}
		}
	}
}
