package net.sf.l2j.gameserver.partyfarm;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.farmhwid.FarmHwidService;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

public class PartyFarmReward
{
	private PartyFarmReward()
	{
	}
	
	public static void giveReward(Creature killer, Npc monster)
	{
		if (killer == null || !(killer instanceof Playable))
			return;
		
		if (!PartyFarm.is_started())
			return;
		
		if (!Config.PART_ZONE_MONSTERS_EVENT_ID.contains(monster.getNpcId()) && monster.getNpcId() != Config.monsterId)
			return;
		
		final Player player = killer.getActingPlayer();
		if (player == null)
			return;
		
		PvpFlagTaskManager.getInstance().add(player, 60000);
		
		if (player.isInParty())
		{
			final Map<String, Integer> playerHwids = new HashMap<>();
			
			for (Player member : player.getParty().getMembers())
			{
				if (member == null || !member.isOnline())
					continue;
				
				boolean canReward = false;
				final String pHwid = member.getHWID();
				
				if (!FarmHwidService.canReceiveDrop(member))
					canReward = false;
				else if (pHwid == null || pHwid.isEmpty() || Config.ENABLE_DUALBOX_PARTYFARM || !playerHwids.containsKey(pHwid))
				{
					if (pHwid != null && !pHwid.isEmpty())
						playerHwids.put(pHwid, 1);
					canReward = true;
				}
				else
					member.sendMessage("Ya recibiste reward desde otro personaje (HWID).");
				
				if (!canReward)
					continue;
				
				if (member.isInsideRadius(monster.getX(), monster.getY(), monster.getZ(), Config.RADIUS_TO_PARTYFARM, false, false))
					randomReward(member);
				else
					member.sendMessage("Estas demasiado lejos del party para recibir reward.");
			}
		}
		else if (FarmHwidService.canReceiveDrop(player))
			randomReward(player);
	}
	
	private static void randomReward(Player player)
	{
		for (RewardHolder reward : Config.PARTY_FARM_REWARDS)
		{
			if (Rnd.get(100) > reward.getRewardChance())
				continue;
			
			int amount = Rnd.get(reward.getMin(), reward.getMax());
			if (player.isVip())
				amount = (int) (amount * Config.VIP_DROP_RATES);
			
			player.addItem("PartyFarm Reward", reward.getRewardId(), amount, player, true);
		}
	}
	
	public static PartyFarmReward getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PartyFarmReward _instance = new PartyFarmReward();
	}
}
