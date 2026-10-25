package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.BossAnnounceManager;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.manager.RaidBossManager;
import net.sf.l2j.gameserver.data.manager.RaidPointManager;
import net.sf.l2j.gameserver.events.soloboss.SoloBoss;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.ai.type.AttackableAI;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class manages all classic raid bosses.<br>
 * <br>
 * Raid Bosses (RB) are mobs which are supposed to be defeated by a party of several players. It extends most of {@link Monster} aspects.<br>
 * They automatically teleport if out of their initial spawn area, and can randomly attack a Player from their Hate List once attacked.
 */
public class RaidBoss extends Monster
{
	private ScheduledFuture<?> _maintenanceTask;
	private ScheduledFuture<?> _aiTask;
	public static final int SPECIAL_TREE_ID = 13007;
	public RaidBoss(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setRaid(true);
		
		if (Config.FLAG_RAIDS && Config.LIST_FLAG_RAID_IDS.contains(getNpcId()))
		{
			_aiTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				for (Player player : getKnownTypeInRadius(Player.class, Config.FLAG_RANGE))
				{
					if (player != null)
						player.updatePvPStatus();
				}
			}, 3000, 3000);		
		}
	}
	
	@Override
	public void onSpawn()
	{
		// No random walk allowed.
		setNoRndWalk(true);
		
		// Basic behavior.
		super.onSpawn();
		
		// "AI task" for regular bosses.
		_maintenanceTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			// Don't bother with dead bosses.
			if (!isDead())
			{
					// Gordon is excluded too.
					if (getNpcId() != 29095 && Rnd.nextBoolean())
					{
						// Spawn must exist.
						final L2Spawn spawn = getSpawn();
						if (spawn == null)
							return;
						
						// If the boss is above drift range (or 200 minimum), teleport him on his spawn.
						if (!isInsideRadius(spawn.getLocX(), spawn.getLocY(), spawn.getLocZ(), Math.max(Config.MAX_DRIFT_RANGE, 200), true, false))
							teleportTo(spawn.getLoc(), 0);
					}
				
				// Randomized attack if the boss is already attacking.
				else if (Rnd.get(5) == 0)
					((AttackableAI) getAI()).aggroReconsider();
			}
			
			// For each minion (if any), randomize the attack.
			if (hasMinions())
			{
				for (Monster minion : getMinionList().getSpawnedMinions())
				{
					// Don't bother with dead minions.
					if (minion.isDead() || !minion.isInCombat())
						return;
					
					// Randomized attack if the boss is already attacking.
					if (Rnd.get(3) == 0)
						((AttackableAI) minion.getAI()).aggroReconsider();
				}
			}
		}, 1000, 60000);
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}
		
		if (killer != null)
		{
			final Player player = killer.getActingPlayer();
			if (player != null)
			{
				if (SoloBoss.is_started())
				{
					final int npcId = getNpcId();
					if (npcId == Config.SOLO_BOSS_ID_ONE)
					{
						ThreadPool.schedule(() -> SoloBoss.SpawnRaid(Config.SOLO_BOSS_ID_TWO, Config.SOLO_BOSS_ID_TWO_LOC), Rnd.get(2000, 3000));
					}
					else if (npcId == Config.SOLO_BOSS_ID_TWO)
					{
						ThreadPool.schedule(() -> SoloBoss.SpawnRaid(Config.SOLO_BOSS_ID_THREE, Config.SOLO_BOSS_ID_THREE_LOC), Rnd.get(2000, 3000));
					}
					else if (npcId == Config.SOLO_BOSS_ID_THREE)
					{
						ThreadPool.schedule(() -> SoloBoss.SpawnRaid(Config.SOLO_BOSS_ID_FOUR, Config.SOLO_BOSS_ID_FOUR_LOC), Rnd.get(2000, 3000));
					}
					else if (npcId == Config.SOLO_BOSS_ID_FOUR)
					{
						ThreadPool.schedule(() -> SoloBoss.SpawnRaid(Config.SOLO_BOSS_ID_FIVE, Config.SOLO_BOSS_ID_FIVE_LOC), Rnd.get(2000, 3000));
					}
					else if (npcId == Config.SOLO_BOSS_ID_FIVE)
					{
						SoloBoss._aborted = true;
						ThreadPool.schedule(() -> SoloBoss.Finish_Event(), 1000);
					}
				}
				
				broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
				broadcastPacket(new PlaySound("systemmsg_e.1209"));
				
				final Party party = player.getParty();
				if (party != null)
				{
					for (Player member : party.getMembers())
					{
						if (Config.ENABLE_RAIDBOSS_NOBLES)
						{
							int bossId = getNpcId();
							
							if (Config.LIST_BOSS_NOBLES_IDS.contains(bossId) && !member.isNoble() && member.isInsideRadius(getX(), getY(), getZ(), 1000, false, false))
							{
								member.setNoble(true, true);
								NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setHtml("<html><body><title>Congratulations!!!</title><br><center><img src=l2font-e.replay_logo-e width=255 height=60><br><br><br><br><font color=\"LEVEL\">Congratulations!</font><br><br>You acquired all<br1>status from a Noblesse.<br><br><table><tr><td><img src=icon.skill1323 width=32 height=32></td><td width=5></td><td><img src=icon.skill1324 width=32 height=32></td><td width=5></td><td><img src=icon.skill1325 width=32 height=32></td><td width=5></td><td><img src=icon.skill1326 width=32 height=32></td></tr><tr></tr><tr></tr><tr><td><img src=icon.skill1327 width=32 height=32></td><td width=5></td><td><img src=icon.skill0325 width=32 height=32></td><td width=5></td><td><img src=icon.skill0326 width=32 height=32></td><td width=5></td><td><img src=icon.skill0327 width=32 height=32></td></tr></table></center></body></html>");
								member.sendPacket(html);
								member.getInventory().addItem("Nobles Circlets", 7694, 1, member, null);
								member.sendPacket(new ItemList(member, true));
								member.sendMessage("You receive the Noblesse Tiara");
							}
						}
						RaidPointManager.getInstance().addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
						if (member.isNoble())
							HeroManager.getInstance().setRBkilled(member.getObjectId(), getNpcId());
					}
				}
				else
					if (Config.ENABLE_RAIDBOSS_NOBLES)
					{
						int bossId = getNpcId();
						
						if (Config.LIST_BOSS_NOBLES_IDS.contains(bossId) && !player.isNoble())
						{
							player.setNoble(true, true);
							NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
							html.setHtml("<html><body><title>Congratulations!!!</title><br><center><img src=l2font-e.replay_logo-e width=255 height=60><br><br><br><br><font color=\"LEVEL\">Congratulations!</font><br><br>You acquired all<br1>status from a Noblesse.<br><br><table><tr><td><img src=icon.skill1323 width=32 height=32></td><td width=5></td><td><img src=icon.skill1324 width=32 height=32></td><td width=5></td><td><img src=icon.skill1325 width=32 height=32></td><td width=5></td><td><img src=icon.skill1326 width=32 height=32></td></tr><tr></tr><tr></tr><tr><td><img src=icon.skill1327 width=32 height=32></td><td width=5></td><td><img src=icon.skill0325 width=32 height=32></td><td width=5></td><td><img src=icon.skill0326 width=32 height=32></td><td width=5></td><td><img src=icon.skill0327 width=32 height=32></td></tr></table></center></body></html>");
							player.sendPacket(html);
							player.getInventory().addItem("Nobles Circlets", 7694, 1, player, null);
							player.sendPacket(new ItemList(player, true));
							player.sendMessage("You receive the Noblesse Tiara");
						}
					}
				{
					RaidPointManager.getInstance().addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
					if (player.isNoble())
						HeroManager.getInstance().setRBkilled(player.getObjectId(), getNpcId());
				}
			}
		}

		RaidBossManager.getInstance().onDeath(this);
		
		if (killer != null)
			BossAnnounceManager.getInstance().onBossKill(this, killer.getActingPlayer());
		
		if (SoloBoss.is_started() && isSoloBossEventNpc(getNpcId()))
			rewardSoloEventPlayers();
		
		return true;
	}
	
	private static boolean isSoloBossEventNpc(int npcId)
	{
		return npcId == Config.SOLO_BOSS_ID_ONE || npcId == Config.SOLO_BOSS_ID_TWO || npcId == Config.SOLO_BOSS_ID_THREE || npcId == Config.SOLO_BOSS_ID_FOUR || npcId == Config.SOLO_BOSS_ID_FIVE;
	}
	
	private static Map<Integer, List<RewardHolder>> getSoloBossRewardsMap()
	{
		final Map<Integer, List<RewardHolder>> rewardsMap = new HashMap<>();
		rewardsMap.put(Config.SOLO_BOSS_ID_ONE, Config.SOLO_BOSS_REWARDS_ONE);
		rewardsMap.put(Config.SOLO_BOSS_ID_TWO, Config.SOLO_BOSS_REWARDS_TWO);
		rewardsMap.put(Config.SOLO_BOSS_ID_THREE, Config.SOLO_BOSS_REWARDS_THREE);
		rewardsMap.put(Config.SOLO_BOSS_ID_FOUR, Config.SOLO_BOSS_REWARDS_FOUR);
		rewardsMap.put(Config.SOLO_BOSS_ID_FIVE, Config.SOLO_BOSS_REWARDS_FIVE);
		return rewardsMap;
	}
	
	private void rewardSoloEventPlayers()
	{
		final int npcId = getNpcId();
		final List<RewardHolder> rewards = getSoloBossRewardsMap().get(npcId);
		if (rewards == null || rewards.isEmpty())
			return;
		
		final List<String> rewardedHwid = new ArrayList<>();
		for (Player player : getKnownTypeInRadius(Player.class, Config.RANGE_SOLO_BOSS))
		{
			final String playerHwid = player.getHWID();
			if (playerHwid == null || rewardedHwid.contains(playerHwid))
				continue;
			
			rewardedHwid.add(playerHwid);
			applySoloBossRewards(player, rewards);
		}
	}
	
	private static void applySoloBossRewards(Player player, List<RewardHolder> rewards)
	{
		for (RewardHolder reward : rewards)
		{
			if (Rnd.get(100) > reward.getRewardChance())
				continue;
			
			int amount = (reward.getMin() == reward.getMax()) ? reward.getMin() : Rnd.get(reward.getMin(), reward.getMax());
			if (player.isVip())
				amount = (int) (amount * Config.VIP_DROP_RATES);
			
			player.addItem("Solo Reward", reward.getRewardId(), amount, player, true);
		}
		
		player.broadcastPacket(new MagicSkillUse(player, player, 2023, 1, 1, 0));
	}
	
	@Override
	public void deleteMe()
	{
		if (_maintenanceTask != null)
		{
			_maintenanceTask.cancel(false);
			_maintenanceTask = null;
		}

		if (Config.FLAG_RAIDS && Config.LIST_FLAG_RAID_IDS.contains(getNpcId()))
		{
			if (_aiTask != null)
			{
				_aiTask.cancel(true);
				_aiTask = null;
			}
		}
		
		super.deleteMe();
	}
}