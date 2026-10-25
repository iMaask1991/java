package net.sf.l2j.gameserver.events.deathmatch;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.manager.IPManager;
import net.sf.l2j.gameserver.events.multievent.MultiEventConfig;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.handler.itemhandlers.VipCoin;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;

public class DMEvent
{
	enum EventState
	{
		INACTIVE,
		INACTIVATING,
		PARTICIPATING,
		STARTING,
		STARTED,
		REWARDING
	}

	protected static final Logger _log = Logger.getLogger(DMEvent.class.getName());
	/** html path **/
	private static final String htmlPath = "data/html/mods/events/dm/";
	/** The state of the DMEvent<br> */
	private static EventState _state = EventState.INACTIVE;
	/** The spawn of the participation npc<br> */
	private static L2Spawn _npcSpawn = null;
	/** the npc instance of the participation npc<br> */
	private static Npc _lastNpcSpawn = null;
	
	private static Map<Integer, DMPlayer> _dmPlayer = new HashMap<>();

	private static void clearParticipants()
	{
		for (DMPlayer participant : _dmPlayer.values())
		{
			if (participant != null)
				participant.clear();
		}

		_dmPlayer.clear();
	}

	public DMEvent()
	{
	}

	/**
	 * DM initializing<br>
	 */
	public static void init()
	{
		// ?
	}

	/**
	 * Sets the DMEvent state<br><br>
	 *
	 * @param state as EventState<br>
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}

	/**
	 * Is DMEvent inactive?<br><br>
	 *
	 * @return boolean: true if event is inactive(waiting for next event cycle), otherwise false<br>
	 */
	public static boolean isInactive()
	{
		boolean isInactive;

		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}

		return isInactive;
	}

	/**
	 * Is DMEvent in inactivating?<br><br>
	 *
	 * @return boolean: true if event is in inactivating progress, otherwise false<br>
	 */
	public static boolean isInactivating()
	{
		boolean isInactivating;

		synchronized (_state)
		{
			isInactivating = _state == EventState.INACTIVATING;
		}

		return isInactivating;
	}

	/**
	 * Is DMEvent in participation?<br><br>
	 *
	 * @return boolean: true if event is in participation progress, otherwise false<br>
	 */
	public static boolean isParticipating()
	{
		boolean isParticipating;

		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}

		return isParticipating;
	}

	/**
	 * Is DMEvent starting?<br><br>
	 *
	 * @return boolean: true if event is starting up(setting up fighting spot, teleport players etc.), otherwise false<br>
	 */
	public static boolean isStarting()
	{
		boolean isStarting;

		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}

		return isStarting;
	}

	/**
	 * Is DMEvent started?<br><br>
	 *
	 * @return boolean: true if event is started, otherwise false<br>
	 */
	public static boolean isStarted()
	{
		boolean isStarted;

		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}

		return isStarted;
	}

	/**
	 * Is DMEvent rewadrding?<br><br>
	 *
	 * @return boolean: true if event is currently rewarding, otherwise false<br>
	 */
	public static boolean isRewarding()
	{
		boolean isRewarding;

		synchronized (_state)
		{
			isRewarding = _state == EventState.REWARDING;
		}

		return isRewarding;
	}

	/**
	 * Close doors specified in configs
	 * @param doors 
	 */
	private static void closeDoors(List<Integer> doors)
	{
		if (doors == null)
			return;

		for (int doorId : doors)
		{
			Door doorInstance = DoorData.getInstance().getDoor(doorId);

			if (doorInstance != null)
			{
				doorInstance.closeMe();
			}
		}
	}

	/**
	 * Open doors specified in configs
	 * @param doors 
	 */
	private static void openDoors(List<Integer> doors)
	{
		if (doors == null)
			return;

		for (int doorId : doors)
		{
			Door doorInstance = DoorData.getInstance().getDoor(doorId);

			if (doorInstance != null)
			{
				doorInstance.openMe();
			}
		}
	}

	/**
	 * UnSpawns the DMEvent npc
	 */
	
	private static void unSpawnNpc()
	{
		if (_lastNpcSpawn == null)
			return;

		// Delete the npc
		_lastNpcSpawn.deleteMe();
		SpawnTable.getInstance().deleteSpawn(_lastNpcSpawn.getSpawn(), false);
		// Stop respawning of the npc
		_npcSpawn.doRespawn();
		_npcSpawn = null;
		_lastNpcSpawn = null;
	}

	/**
	 * Starts the participation of the DMEvent<br>
	 * 1. Get L2NpcTemplate by Config.DM_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br><br>
	 *
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startParticipation()
	{
		if (MultiEventConfig.ENABLED)
		{
			setState(EventState.PARTICIPATING);
			return true;
		}

		NpcTemplate tmpl = NpcData.getInstance().getTemplate(DMConfig.DM_EVENT_PARTICIPATION_NPC_ID);
		
		if (tmpl == null)
		{
			_log.warning("DMEventEngine[DMEvent.startParticipation()]: L2NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}
		
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			
			//	_npcSpawn.setLocx(DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[0]);
			//	_npcSpawn.setLocy(DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[1]);
			//	_npcSpawn.setLocz(DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[2]);
			_npcSpawn.setLoc(DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[0], DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[1], DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[2], DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			//		_npcSpawn.getAmount();
			//	_npcSpawn.setHeading(DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);
			_npcSpawn.setRespawnState(false);
			// later no need to delete spawn from db, we don't store it (false)
			SpawnTable.getInstance().addSpawn(_npcSpawn, false);
			//	_npcSpawn.init();
			_lastNpcSpawn = _npcSpawn.doSpawn(false);
			//	_lastNpcSpawn = _npcSpawn.getLastSpawn();
			_lastNpcSpawn.setCurrentHp(_lastNpcSpawn.getMaxHp());
			_lastNpcSpawn.setTitle("DM Event");
			_lastNpcSpawn.isAggressive();
			_lastNpcSpawn.decayMe();
			_lastNpcSpawn.spawnMe(_npcSpawn.getLocX(), _npcSpawn.getLocY(), _npcSpawn.getLocZ());
			_lastNpcSpawn.broadcastPacket(new MagicSkillUse(_lastNpcSpawn, _lastNpcSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "DMEventEngine[DMEvent.startParticipation()]: exception: " + e.getMessage(), e);
			return false;
		}
		
		setState(EventState.PARTICIPATING);
		return true;
	}
	/**
	 * Starts the DMEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in configs<br>
	 * 3. Abort if not enought participants(return false)<br>
	 * 4. Set state EventState.STARTED<br>
	 * 5. Teleport all participants to team spot<br><br>
	 *
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startFight()
	{
		// Set state to STARTING
		setState(EventState.STARTING);

		// Check the number of participants
		if (_dmPlayer.size() < DMConfig.DM_EVENT_MIN_PLAYERS)
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of participants
			clearParticipants();

			// Unspawn the event NPC
			unSpawnNpc();
			return false;
		}

		// Closes all doors specified in configs for dm
		closeDoors(DMConfig.DM_DOORS_IDS_TO_CLOSE);
		// Set state STARTED
		setState(EventState.STARTED);
		for (DMPlayer player: _dmPlayer.values())
		{
			if (player != null)
				new DMEventTeleporter(player.getPlayer(), false, false);
		} 

		return true;
	}

	public static TreeSet<DMPlayer> orderPosition(Collection<DMPlayer> listPlayer)
	{
	    TreeSet<DMPlayer> players = new TreeSet<>(new Comparator<DMPlayer>()
	    {
	        @Override
	        public int compare(DMPlayer p1, DMPlayer p2)
	        {
	            Integer c1 = Integer.valueOf(p2.getPoints() - p1.getPoints());
	            Integer c2 = Integer.valueOf(p1.getDeath() - p2.getDeath());
	            Integer c3 = p1.getHexCode().compareTo(p2.getHexCode());

	            if (c1 == 0)
	            {
	                if (c2 == 0) return c3;
	                return c2;
	            }       
	            return c1;
	        }
	    });
	    players.addAll(listPlayer);
	    return players;
	}


	/**
	 * Calculates the DMEvent reward<br>
	 * 1. If both teams are at a tie(points equals), send it as system message to all participants, if one of the teams have 0 participants left online abort rewarding<br>
	 * 2. Wait till teams are not at a tie anymore<br>
	 * 3. Set state EvcentState.REWARDING<br>
	 * 4. Reward team with more points<br>
	 * 5. Show win html to wining team participants<br><br>
	 *
	 * @return String: winning team name<br>
	 */
	public static String calculateRewards()
	{
		TreeSet<DMPlayer> players = orderPosition(_dmPlayer.values());
		String msg = "";
		for (int j = 0; j < DMConfig.DM_REWARD_FIRST_PLAYERS; j++)
		{
			if (players.isEmpty())
				break;

			DMPlayer player = players.first();

			if (player.getPoints() == 0)
				break;

			rewardPlayer(player, j + 1);
			players.remove(player);
			
			msg += " Player: " + player.getPlayer().getName();
			msg += " Killed: " + player.getPoints();
			msg += "\n";
			
			int playerPointPrev = player.getPoints();

			if (!DMConfig.DM_REWARD_PLAYERS_TIE)
				continue;

			while (!players.isEmpty())
			{
				player = players.first();
				if (player.getPoints() != playerPointPrev)
					break;

				rewardPlayer(player, j + 1);
				players.remove(player);
			}
		}

		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		return "DM ended, thanks to everyone who participated!\nWinner(s):\n" + msg;
	}

//	private static void rewardPlayer(DMPlayer p, int pos)
//	{
//		L2PcInstance activeChar = p.getPlayer();
//
//		// Check for nullpointer
//		if (activeChar == null)
//			return;
//
//	//	if (Config.ACTIVE_MISSION_DM)
//	//	{							
//	//		if (!activeChar.checkMissions(activeChar.getObjectId()))
//	//			activeChar.updateMissions()
//
//	//		if (!(activeChar.isDMCompleted() || activeChar.getDMCont() >= Config.MISSION_DM_COUNT))
//	//			activeChar.setDMCont(activeChar.getDMCont() + 1);
//	//	}
//		
//		switch (pos)
//		{
//		    case 1:
//	    		for (RewardHolder reward : DMConfig.DM_EVENT_REWARDS_WIN_1ST)
//	    		{
//	    			if (Rnd.get(100) <= reward.getRewardChance())
//	    			{
//						if (activeChar.isVip())
//						{
//							if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 1st Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 1st Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else
//								activeChar.addItem("DM 1st Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//						}
//	    				else
//	    					activeChar.addItem("DM 1st Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//	    			}
//	    		}
//			    break;
//		    case 2:
//	    		for (RewardHolder reward : DMConfig.DM_EVENT_REWARDS_WIN_2ND)
//	    		{
//	    			if (Rnd.get(100) <= reward.getRewardChance())
//	    			{
//	    				if (activeChar.isVip())
//						{
//							if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 2nd Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 2nd Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else
//								activeChar.addItem("DM 2nd Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//						}
//	    				else
//	    					activeChar.addItem("DM 2nd Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//	    			}
//	    		}
//			    break;
//		    case 3:
//	    		for (RewardHolder reward : DMConfig.DM_EVENT_REWARDS_WIN_3RD)
//	    		{
//	    			if (Rnd.get(100) <= reward.getRewardChance())
//	    			{
//	    				if (activeChar.isVip())
//						{
//							if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 3rd Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 3rd Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else
//								activeChar.addItem("DM 3rd Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//						}
//	    				else
//	    					activeChar.addItem("DM 3rd Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//	    			}
//	    		}
//			    break;		    
//			 case 4:
//		    	for (RewardHolder reward : DMConfig.DM_EVENT_REWARDS_WIN_4TH)
//		    	{
//		    		if (Rnd.get(100) <= reward.getRewardChance())
//		    		{
//		    			if (activeChar.isVip())
//						{
//							if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 4th Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 4th Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else
//								activeChar.addItem("DM 4th Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//						}
//	    				else
//		    				activeChar.addItem("DM 4th Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//		    		}
//		    	}
//				break;		    
//			 case 5:
//			    for (RewardHolder reward : DMConfig.DM_EVENT_REWARDS_WIN_5TH)
//			    {
//			    	if (Rnd.get(100) <= reward.getRewardChance())
//			    	{
//			    		if (activeChar.isVip())
//						{
//							if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 5th Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else if (reward.getRewardId() == 5556)
//								activeChar.addItem("DM 5th Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//							else
//								activeChar.addItem("DM 5th Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//						}
//	    				else
//			    			activeChar.addItem("DM 5th Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//			    	}
//			    }
//				break;		    
//			 case 6:
//			 case 7:
//			 case 8:
//			 case 9:
//			 case 10:
//				    for (RewardHolder reward : DMConfig.DM_EVENT_REWARDS_WIN_5AFT)
//				    {
//				    	if (Rnd.get(100) <= reward.getRewardChance())
//				    	{
//				    		if (activeChar.isVip())
//							{
//								if (reward.getRewardId() == 5556)
//									activeChar.addItem("DM 5AFT Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//								else if (reward.getRewardId() == 5556)
//									activeChar.addItem("DM 5AFT Reward", reward.getRewardId(), (int) (Rnd.get(reward.getMin(), reward.getMax()) * Config.VIP_DROP_RATES), activeChar, true);
//								else
//									activeChar.addItem("DM 5AFT Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//							}
//		    				else
//				    			activeChar.addItem("DM 5AFT Reward", reward.getRewardId(), Rnd.get(reward.getMin(), reward.getMax()), activeChar, true);
//				    	}
//				    }
//					break;
//		    default:
//			    break;
//		}
//
//		StatusUpdate statusUpdate = new StatusUpdate(activeChar);
//		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
//
//		statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
//		npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Reward.htm"));
//		activeChar.sendPacket(statusUpdate);
//		activeChar.sendPacket(npcHtmlMessage);
//	}
	private static void rewardPlayer(DMPlayer p, int pos)
	{
		Player activeChar = p.getPlayer();

		if (activeChar == null)
			return;

		int hours;
		switch (pos)
		{
			case 1:
				hours = DMConfig.DM_EVENT_VIP_1ST_HOURS;
				break;
			case 2:
				hours = DMConfig.DM_EVENT_VIP_2ND_HOURS;
				break;
			case 3:
				hours = DMConfig.DM_EVENT_VIP_3RD_HOURS;
				break;
			case 4:
				hours = DMConfig.DM_EVENT_VIP_4TH_HOURS;
				break;
			case 5:
				hours = DMConfig.DM_EVENT_VIP_5TH_HOURS;
				break;
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				hours = DMConfig.DM_EVENT_VIP_5AFT_HOURS;
				break;
			default:
				return;
		}

		if (hours <= 0)
			return;

		if (!VipCoin.addHours(activeChar, hours))
			return;

		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
		npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Reward.htm"));
		npcHtmlMessage.replace("%vip_hours%", String.valueOf(hours));
		activeChar.sendPacket(npcHtmlMessage);
	}

	/**
	 * Stops the DMEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove DM npc from world<br>
	 * 3. Open doors specified in configs<br>
	 * 4. Send Top Rank<br>
	 * 5. Teleport all participants back to participation npc location<br>
	 * 6. List players cleaning<br>
	 * 7. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);
		//Unspawn event npc
		unSpawnNpc();
		// Opens all doors specified in configs for DM
		openDoors(DMConfig.DM_DOORS_IDS_TO_CLOSE);
		// Closes all doors specified in Configs for DM
		closeDoors(DMConfig.DM_DOORS_IDS_TO_OPEN);

		String[] topPositions;
		String htmltext = "";
		if (DMConfig.DM_SHOW_TOP_RANK)
		{
			topPositions = getFirstPosition(DMConfig.DM_TOP_RANK);
			Boolean c = true;
			String c1 = "LEVEL";
			String c2 = "FFFFFF";
			if (topPositions != null)
			{
				for (int i = 0; i < topPositions.length; i++)
				{
					String color = (c ? c1 : c2);
					String[] row = topPositions[i].split("\\,");
					htmltext += "<tr>";
					htmltext += "<td width=\"35\" align=\"center\"><font color=\"" + color + "\">" + String.valueOf(i + 1) + "</font></td>";
					htmltext += "<td width=\"100\" align=\"left\"><font color=\"" + color + "\">" + row[0] + "</font></td>";
					htmltext += "<td width=\"125\" align=\"right\"><font color=\"" + color + "\">" + row[1] + "</font></td>";
					htmltext += "</tr>";
					c = !c;
				}
			}
		}

		for (DMPlayer player : _dmPlayer.values())
		{
			if (player == null)
				continue;
			
			final Player pc = player.getPlayer();
			if (pc == null)
				continue;
			
			net.sf.l2j.gameserver.events.instance.InstanceHolder defaultInstance = net.sf.l2j.gameserver.events.instance.InstanceManager.getInstance().getInstance(0);
			pc.setInstance(defaultInstance, true);
			
			if (DMConfig.DM_SHOW_TOP_RANK)
			{
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "TopRank.htm"));
				npcHtmlMessage.replace("%toprank%", htmltext);
				pc.sendPacket(npcHtmlMessage);
			}
			new DMEventTeleporter(pc, DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
			if (DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("title") || DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				ThreadPool.schedule(() ->
				{
					if (!pc.isOnline())
						return;
					pc.clearDMPoints();
				}, DMConfig.DM_EVENT_START_LEAVE_TELEPORT_DELAY * 1000);
			}
		}

		// Cleanup list
		clearParticipants();
		_dmPlayer = new HashMap<>();

		// Set state INACTIVE
		setState(EventState.INACTIVE);
	}

	/**
	 * Adds a player to a DMEvent<br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static synchronized boolean addParticipant(Player activeChar)
	{
		// Check for nullpoitner
		if (activeChar == null) 
			return false;

		if (isPlayerParticipant(activeChar)) 
			return false;

		String hexCode = hexToString(generateHex(16));
		_dmPlayer.put(activeChar.getObjectId(), new DMPlayer(activeChar, hexCode));
		return true;
	}

	public static boolean areEnemies(Player player1, Player player2)
	{
		if (!isStarted() || player1 == null || player2 == null || player1 == player2)
			return false;
		
		return isPlayerParticipant(player1) && isPlayerParticipant(player2);
	}
	
	public static boolean areParticipants(Player player1, Player player2)
	{
		if (!isStarted() || player1 == null || player2 == null)
			return false;
		
		return isPlayerParticipant(player1) && isPlayerParticipant(player2);
	}
	
	public static boolean isPlayerParticipant(Player activeChar)
	{
		if (activeChar == null)
			return false;
		try
		{
			if (_dmPlayer.containsKey(activeChar.getObjectId()))
				return true;
		}
		catch (Exception e) 
		{
			return false;
		}
		return false;
	}

	public static boolean isPlayerParticipant(int objectId)
	{
		Player activeChar = World.getInstance().getPlayer(objectId);
		if (activeChar == null)
			return false; 

		return isPlayerParticipant(activeChar);
	}

	/**
	 * Removes a DMEvent player<br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean removeParticipant(Player activeChar)
	{
		if (activeChar == null)
			return false;

		if (!isPlayerParticipant(activeChar))
			return false;

		try
		{
			_dmPlayer.remove(activeChar.getObjectId());
		}
		catch (Exception e) 
		{
			return false;
		}

		return true;
	}

	public static boolean payParticipationFee(Player activeChar)
	{
		int itemId = DMConfig.DM_EVENT_PARTICIPATION_FEE[0];
		int itemNum = DMConfig.DM_EVENT_PARTICIPATION_FEE[1];
		if (itemId == 0 || itemNum == 0)
			return true;

		if (activeChar.getInventory().getInventoryItemCount(itemId, -1) < itemNum)
			return false;

		return activeChar.destroyItemByItemId("DM Participation Fee", itemId, itemNum, _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = DMConfig.DM_EVENT_PARTICIPATION_FEE[0];
		int itemNum = DMConfig.DM_EVENT_PARTICIPATION_FEE[1];

		if (itemId == 0 || itemNum == 0)
			return "-";

		return StringUtil.concat(String.valueOf(itemNum), " ", ItemTable.getInstance().getTemplate(itemId).getName());
	}

	/**
	 * Send a SystemMessage to all participated players<br>
	 *
	 * @param message as String<br>
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		CreatureSay cs = new CreatureSay(0, Say2.PARTY, "DM Manager", message);

		for (DMPlayer player : _dmPlayer.values())
			if (player != null)
				player.getPlayer().sendPacket(cs);
	}

	/**
	 * Called when a player logs in<br><br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 */
	public static void onLogin(Player activeChar)
	{
		if (activeChar == null || (!isStarting() && !isStarted()))
			return;

		if (!isPlayerParticipant(activeChar))
			return;

	//	if (DMConfig.ENABLE_DM_INSTANCE)
	//	{
		//	activeChar.noCarrierUnparalyze();
		//	activeChar.setInstanceId(DMConfig.DM_INSTANCE_ID, true);
	//	}
		
		new DMEventTeleporter(activeChar, false, false);
	}

	/**
	 * Called when a player logs out<br><br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 */
	public static void onLogout(Player activeChar)
	{
		if (activeChar != null && (isStarting() || isStarted() || isParticipating()))
		{
		//	if (activeChar.isNoCarrier())
		//		return;
			
			if (removeParticipant(activeChar))
				activeChar.teleToLocation(DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)-50, DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)-50, DMConfig.DM_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
	
			/*
			if (DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("title") || DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				activeChar.setTitle(activeChar._originalTitle);
				activeChar.broadcastTitleInfo();
			}
			*/
		}
	}

	/**
	 * Called on every bypass by npc of type L2DMEventNpc<br>
	 * Needs synchronization cause of the max player check<br><br>
	 *
	 * @param command as String<br>
	 * @param activeChar as L2PcInstance<br>
	 */
	public static synchronized void onBypass(String command, Player activeChar)
	{
		if (activeChar == null || !isParticipating())
			return;

		final String htmContent;

		if (command.equals("dm_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = activeChar.getLevel();

			if (activeChar.isCursedWeaponEquipped())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}			
			else if (activeChar.isInArenaEvent())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Tournament.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (activeChar.isInOlympiadMode())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Olympiad.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (activeChar.getKarma() > 0)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Karma.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (DMConfig.DISABLE_ID_CLASSES != null && DMConfig.DISABLE_ID_CLASSES.contains(activeChar.getClassId().getId()))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Class.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (playerLevel < DMConfig.DM_EVENT_MIN_LVL || playerLevel > DMConfig.DM_EVENT_MAX_LVL)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(DMConfig.DM_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(DMConfig.DM_EVENT_MAX_LVL));
				}
			}
			else if (_dmPlayer.size() == DMConfig.DM_EVENT_MAX_PLAYERS)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Full.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(DMConfig.DM_EVENT_MAX_PLAYERS));
				}
			}
			else if (activeChar.isAio() && !Config.ALLOW_AIO_IN_EVENTS)
			{
				activeChar.sendMessage("AIO charactes are not allowed to participate in events.");
				return;
			}
			else if (CTFEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (activeChar.isSellBuff())
			{
				activeChar.sendMessage("Character Sell buffs No Registed in Events!");
				return;
			}
			
			else if (TvTEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (KTBEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (OlympiadManager.getInstance().isRegisteredInComp(activeChar) || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() > 0)
			{
				activeChar.sendMessage("You can't register while you are in olympiad!");
				return;
			}
			else if (LMEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (DMConfig.DM_EVENT_MULTIBOX_PROTECTION_ENABLE && onMultiBoxRestriction(activeChar))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "MultiBox.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%maxbox%", String.valueOf(DMConfig.DM_EVENT_NUMBER_BOX_REGISTER));
				}
			}
			else if (!payParticipationFee(activeChar))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "ParticipationFee.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%fee%", getParticipationFee());
				}
			}
			else if (isPlayerParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else if (addParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else
				return;

			activeChar.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("dm_event_remove_participation"))
		{
			if (isPlayerParticipant(activeChar))
			{
				removeParticipant(activeChar);

				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Unregistered.htm"));
				activeChar.sendPacket(npcHtmlMessage);
			}
		}
	}

	/**
	 * Called on every onAction in L2PcIstance<br><br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 * @param targetedPlayerObjectId 
	 * @return boolean: true if player is allowed to target, otherwise false<br>
	 */
	public static boolean onAction(Player activeChar, int targetedPlayerObjectId)
	{
		if (activeChar == null || !isStarted()) 
			return true;		

		if (activeChar.isGM())
			return true;

		if (!isPlayerParticipant(activeChar) && isPlayerParticipant(targetedPlayerObjectId)) 
			return false;		

		if (isPlayerParticipant(activeChar) && !isPlayerParticipant(targetedPlayerObjectId)) 
			return false;

		return true;
	}

	/**
	 * Called on every scroll use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to use scroll, otherwise false<br>
	 */
	public static boolean onScrollUse(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId) && !DMConfig.DM_EVENT_SCROLL_ALLOWED)
			return false;

		return true;
	}

	/**
	 * Called on every potion use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to use potions, otherwise false<br>
	 */
	public static boolean onPotionUse(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId) && !DMConfig.DM_EVENT_POTIONS_ALLOWED)
			return false;

		return true;
	}

	/**
	 * Called on every escape use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is not in DM Event, otherwise false<br>
	 */
	public static boolean onEscapeUse(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId))
			return false;

		return true;
	}

	/**
	 * Called on every summon item use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is allowed to summon by item, otherwise false<br>
	 */
	public static boolean onItemSummon(int objectId)
	{
		if (!isStarted())
			return true;

		if (isPlayerParticipant(objectId) && !DMConfig.DM_EVENT_SUMMON_BY_ITEM_ALLOWED)
			return false;

		return true;
	}

	/**
	 * Is called when a player is killed<br><br>
	 * 
	 * @param killerCharacter as L2Character<br>
	 * @param killedPlayerInstance as L2PcInstance<br>
	 */
	public static void onKill(Creature killerCharacter, Player killedPlayerInstance)
	{
		if (killedPlayerInstance == null || !isStarted()) 
			return;

		if (!isPlayerParticipant(killedPlayerInstance.getObjectId())) 
			return;

		new DMEventTeleporter(killedPlayerInstance, false, false);

		if (killerCharacter == null) 
			return;

		Player killerPlayerInstance = null;

		if (killerCharacter instanceof Pet || killerCharacter instanceof Summon)
		{
			killerPlayerInstance = ((Summon) killerCharacter).getOwner();
			if (killerPlayerInstance == null) 
				return;
		}
		else if (killerCharacter instanceof Player)
			killerPlayerInstance = (Player) killerCharacter;
		else
			return;

		if (isPlayerParticipant(killerPlayerInstance))
		{
			_dmPlayer.get(killerPlayerInstance.getObjectId()).increasePoints();
			_dmPlayer.get(killedPlayerInstance.getObjectId()).increaseDeath();
			net.sf.l2j.gameserver.events.eventgear.EventGearManager.onEventKill(killerPlayerInstance);
			
			if (DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("pm"))
			{
				sysMsgToAllParticipants(net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killerPlayerInstance) + " Hunted Player " + net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killedPlayerInstance) + "!");
			}
			else if (DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("title"))
			{
				killerPlayerInstance.increaseDMPointScore();
				killerPlayerInstance.broadcastCharInfo();
				//killerPlayerInstance.setTitle("Kills: " + killerPlayerInstance.getDMPointScore());
				//killerPlayerInstance.broadcastTitleInfo();
			}
			else if (DMConfig.DM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				sysMsgToAllParticipants(net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killerPlayerInstance) + " Hunted Player " + net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killedPlayerInstance) + "!");
				killerPlayerInstance.increaseDMPointScore();
				killerPlayerInstance.broadcastCharInfo();
				//killerPlayerInstance.setTitle("Kills: " + killerPlayerInstance.getDMPointScore());
				//killerPlayerInstance.broadcastTitleInfo();
			}
		}
	}

	/**
	 * Called on Appearing packet received (player finished teleporting)<br><br>
	 * @param activeChar 
	 */
	public static void onTeleported(Player activeChar)
	{
		if (!isStarted() || activeChar == null || !isPlayerParticipant(activeChar.getObjectId()))
			return;

		if (activeChar.isMageClass())
		{
			if (DMConfig.DM_EVENT_MAGE_BUFFS != null && !DMConfig.DM_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : DMConfig.DM_EVENT_MAGE_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, DMConfig.DM_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
						skill.getEffects(activeChar, activeChar);
				}
			}
		}
		else
		{
			if (DMConfig.DM_EVENT_FIGHTER_BUFFS != null && !DMConfig.DM_EVENT_FIGHTER_BUFFS.isEmpty())
			{
				for (int i : DMConfig.DM_EVENT_FIGHTER_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, DMConfig.DM_EVENT_FIGHTER_BUFFS.get(i));
					if (skill != null)
						skill.getEffects(activeChar, activeChar);
				}
			}
		}

		removeParty(activeChar);
	}

	/*
	 * Return true if player valid for skill
	 */
	public static final boolean checkForDMSkill(Player source, Player target, L2Skill skill)
	{
		if (!isStarted())
			return true;

		// DM is started
		final boolean isSourceParticipant = isPlayerParticipant(source);
		final boolean isTargetParticipant = isPlayerParticipant(target);

		// both players not participating
		if (!isSourceParticipant && !isTargetParticipant)
			return true;
		// one player not participating
		if (!(isSourceParticipant && isTargetParticipant))
			return false;

		return true;
	}

	public static int getPlayerCounts()
	{
		return _dmPlayer.size();
	}

	public static String[] getFirstPosition(int countPos)
	{
		TreeSet<DMPlayer> players = orderPosition(_dmPlayer.values());
		String text = "";
		for (int j = 0; j < countPos; j++)
		{
			if (players.isEmpty())
				break;

			DMPlayer player = players.first();

			if (player.getPoints() == 0)
				break;

			text += player.getPlayer().getName() + "," + String.valueOf(player.getPoints()) + ";"; 
			players.remove(player);

			int playerPointPrev = player.getPoints();

			if (!DMConfig.DM_REWARD_PLAYERS_TIE) 
				continue;

			while(!players.isEmpty())
			{
				player = players.first();
				if(player.getPoints() != playerPointPrev)
					break;
				text += player.getPlayer().getName() + "," + String.valueOf(player.getPoints()) + ";";
				players.remove(player);
			}
		}

		if (text != "") 
			return text.split("\\;");

		return null;
	}

	public static void removeParty(Player activeChar)
	{
		if (activeChar.getParty() != null)
		{
			Party party = activeChar.getParty();
			party.removePartyMember(activeChar, MessageType.LEFT);
		}
	}

	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Rnd.nextBytes(array);
		return array;
	}

	public static String hexToString(byte[] hex)
	{
		return new BigInteger(hex).toString(16);
	}

	public static Map<Integer, Player> allParticipants()
	{
	    Map<Integer, Player> all = new HashMap<>();
	    if (getPlayerCounts() > 0)
	    {
	        for (DMPlayer dp : _dmPlayer.values())
	            all.put(dp.getPlayer().getObjectId(), dp.getPlayer());
	    }
	    return all;
	}



	public static boolean onMultiBoxRestriction(Player activeChar)
	{
		return IPManager.getInstance().validBox(activeChar, DMConfig.DM_EVENT_NUMBER_BOX_REGISTER, allParticipants().values(), false);
	}
}
