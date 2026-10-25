package net.sf.l2j.gameserver.pvpfarm;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.farmhwid.FarmHwidService;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

/**
 * Reward del PvP Farm: <b>individual</b> (solo el que mata), a diferencia de {@link net.sf.l2j.gameserver.partyfarm.PartyFarmReward} que reparte a toda la party dentro del radio.
 */
public class PvpFarmReward
{
	private PvpFarmReward()
	{
	}
	
	public static void giveReward(Creature killer, Npc monster)
	{
		if (killer == null || !(killer instanceof Playable))
			return;
		
		if (!PvpFarm.is_started())
			return;
		
		if (!Config.PVPFARM_MONSTER_IDS.contains(monster.getNpcId()))
			return;
		
		final Player player = killer.getActingPlayer();
		if (player == null)
			return;
		
		if (Config.PVPFARM_FLAG_ON_KILL)
			PvpFlagTaskManager.getInstance().add(player, Config.PVPFARM_FLAG_ON_KILL_TIME);
		
		// False = el drop lo maneja el XML del NPC (drop individual retail).
		if (!Config.PVPFARM_CUSTOM_REWARD)
			return;
		
		if (Config.PVPFARM_ANTI_DUALBOX && !FarmHwidService.canReceiveDrop(player))
			return;
		
		randomReward(player);
	}
	
	private static void randomReward(Player player)
	{
		for (RewardHolder reward : Config.PVPFARM_REWARDS)
		{
			if (Rnd.get(100) > reward.getRewardChance())
				continue;
			
			int amount = Rnd.get(reward.getMin(), reward.getMax());
			if (player.isVip())
				amount = (int) (amount * Config.VIP_DROP_RATES);
			
			player.addItem("PvP Farm Reward", reward.getRewardId(), amount, player, true);
		}
	}
}
