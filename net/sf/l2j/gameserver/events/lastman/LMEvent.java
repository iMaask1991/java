package net.sf.l2j.gameserver.events.lastman;

import java.math.BigInteger;
import java.text.DecimalFormat;
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
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;

import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
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
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class LMEvent
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

	protected static final Logger _log = Logger.getLogger(LMEvent.class.getName());
	/** html path **/
	private static final String htmlPath = "data/html/mods/events/lm/";
	/** The state of the LMEvent<br> */
	private static EventState _state = EventState.INACTIVE;
	/** The spawn of the participation npc<br> */
	private static L2Spawn _npcSpawn = null;
	/** the npc instance of the participation npc<br> */
	private static Npc _lastNpcSpawn = null;

	private static Map<Integer, LMPlayer> _lmPlayer = new HashMap<>();

	private static void clearParticipants()
	{
		for (LMPlayer participant : _lmPlayer.values())
		{
			if (participant != null)
				participant.clear();
		}

		_lmPlayer.clear();
	}

	private static DecimalFormat f = new DecimalFormat(",##0,000");

	public LMEvent() 
	{
		//?
	}

	/**
	 * LM initializing<br>
	 */
	public static void init() 
	{
		//?
	}

	/**
	 * Sets the LMEvent state<br><br>
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
	 * Is LMEvent inactive?<br><br>
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
	 * Is LMEvent in inactivating?<br><br>
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
	 * Is LMEvent in participation?<br><br>
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
	 * Is LMEvent starting?<br><br>
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
	 * Is LMEvent started?<br><br>
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
	 * Is LMEvent rewadrding?<br><br>
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
	 * Close doors specified in LMConfigs
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
	 * Open doors specified in LMConfigs
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
	 * UnSpawns the LMEvent npc
	 */
	private static void unSpawnNpc()
	{
		if (_lastNpcSpawn == null)
			return;

		// Delete the npc
		_lastNpcSpawn.deleteMe();
		SpawnTable.getInstance().deleteSpawn(_lastNpcSpawn.getSpawn(), false);
		// Stop respawning of the npc
	//	_npcSpawn.stopRespawn();
		_npcSpawn.doRespawn();
		_npcSpawn = null;
		_lastNpcSpawn = null;
	}

	/**
	 * Starts the participation of the LMEvent<br>
	 * 1. Get L2NpcTemplate by LMConfig.LM_EVENT_PARTICIPATION_NPC_ID<br>
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

		NpcTemplate tmpl = NpcData.getInstance().getTemplate(LMConfig.LM_EVENT_PARTICIPATION_NPC_ID);

		if (tmpl == null)
		{
			_log.warning("LMEventEngine[LMEvent.startParticipation()]: L2NpcTemplate is a NullPointer -> Invalid npc id in LMConfigs?");
			return false;
		}

		try
		{
			_npcSpawn = new L2Spawn(tmpl);

		//	_npcSpawn.setLocx(LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[0]);
		//	_npcSpawn.setLocy(LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[1]);
		//	_npcSpawn.setLocz(LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[2]);
		//	_npcSpawn.getAmount();
		//	_npcSpawn.setHeading(LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setLoc(LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[0], LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[1], LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[2], LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);
			_npcSpawn.setRespawnState(false);
			// later no need to delete spawn from db, we don't store it (false)
			SpawnTable.getInstance().addSpawn(_npcSpawn, false);
	//		_npcSpawn.init();
	//		_lastNpcSpawn = _npcSpawn.getLastSpawn();
			_lastNpcSpawn = _npcSpawn.doSpawn(false);
			_lastNpcSpawn.setCurrentHp(_lastNpcSpawn.getMaxHp());
			_lastNpcSpawn.setTitle("LM Event");
			_lastNpcSpawn.isAggressive();
			_lastNpcSpawn.decayMe();
			_lastNpcSpawn.spawnMe(_npcSpawn.getLocX(), _npcSpawn.getLocY(), _npcSpawn.getLocZ());
			
			_lastNpcSpawn.broadcastPacket(new MagicSkillUse(_lastNpcSpawn, _lastNpcSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "LMEventEngine[LMEvent.startParticipation()]: exception: " + e.getMessage(), e);
			return false;
		}

		setState(EventState.PARTICIPATING);
		return true;
	}

	/**
	 * Starts the LMEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in LMConfigs<br>
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
		if (getPlayerCounts() < LMConfig.LM_EVENT_MIN_PLAYERS)
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of participants
			clearParticipants();

			// Unspawn the event NPC
			unSpawnNpc();
			return false;
		}

		// Closes all doors specified in LMConfigs for lm
		closeDoors(LMConfig.LM_DOORS_IDS_TO_CLOSE);
		// Set state STARTED
		setState(EventState.STARTED);
		net.sf.l2j.gameserver.events.instance.InstanceHolder instance = net.sf.l2j.gameserver.events.instance.InstanceManager.getInstance().createInstance();
		for (LMPlayer player: _lmPlayer.values())
		{
			if (player != null)
			{
				player.getPlayer().setInstance(instance, true);
				// Teleporter implements Runnable and starts itself
				new LMEventTeleporter(player.getPlayer(), false, false);
				if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("title") || LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
				{
					player.getPlayer()._originalTitle = player.getPlayer().getTitle();
					player.getPlayer().setTitle("Kills: " + player.getPoints());
					player.getPlayer().broadcastTitleInfo();
				}
			}
		} 

		return true;
	}

	public static TreeSet<LMPlayer> orderPosition(Collection<LMPlayer> listPlayer)
	{
	    TreeSet<LMPlayer> players = new TreeSet<>(new Comparator<LMPlayer>()
	    {
	        @Override
	        public int compare(LMPlayer p1, LMPlayer p2)
	        {
	            Integer c1 = Integer.valueOf(p2.getCredits() - p1.getCredits());
	            Integer c2 = Integer.valueOf(p2.getPoints() - p1.getPoints());
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
	 * Calculates the LMEvent reward<br>
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
		TreeSet<LMPlayer> players = orderPosition(_lmPlayer.values());
		String msg = "";
		
		if (!LMConfig.LM_REWARD_PLAYERS_TIE && getPlayerCounts() > 1)
			return "Last Man ended, thanks to everyone who participated!\nEvent dont have winners!";

		for (int i = 0; i < players.size(); i++)
		{
			if (players.isEmpty())
				break;

			LMPlayer player = players.first();

			if (player.getCredits() == 0 || player.getPoints() == 0)
				break;

			rewardPlayer(player);
			players.remove(player);
			msg += " Player: " + player.getPlayer().getName();
			msg += " Killed: " + player.getPoints();
			msg += " Died: " + String.valueOf(LMConfig.LM_EVENT_PLAYER_CREDITS - player.getCredits());
			msg += "\n";
			if (!LMConfig.LM_REWARD_PLAYERS_TIE)
				break;
		}

		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		return "Last Man ended, thanks to everyone who participated!\nWinner(s):\n" + msg;
	}

//	private static void rewardPlayer(LMPlayer p)
//	{
//		L2PcInstance activeChar = p.getPlayer();
//
//		// Check for nullpointer
//		if (activeChar == null)
//			return;
//
//		SystemMessage systemMessage = null;
//		String htmltext = "";
//		for (int[] reward : LMConfig.LM_EVENT_REWARDS)
//		{
//			PcInventory inv = activeChar.getInventory();
//
//			// Check for stackable item, non stackabe items need to be added one by one
//			if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
//			{
//				inv.addItem("Last Man", reward[0], reward[1], activeChar, activeChar);
//
//				if (reward[1] > 1)
//				{
//					systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
//					systemMessage.addItemName(reward[0]);
//					systemMessage.addItemNumber(reward[1]);
//				}
//				else
//				{
//					systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
//					systemMessage.addItemName(reward[0]);
//				}
//
//				activeChar.sendPacket(systemMessage);
//			}
//			else
//			{
//				for (int i = 0; i < reward[1]; ++i)
//				{
//					inv.addItem("Last Man", reward[0], 1, activeChar, activeChar);
//					systemMessage = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
//					systemMessage.addItemName(reward[0]);
//					activeChar.sendPacket(systemMessage);
//				}
//			}
//			htmltext += " - " + (reward[1] > 999 ? f.format(reward[1]) : reward[1]) + " " + ItemTable.getInstance().getTemplate(reward[0]).getName() + "<br1>";
//		}
//
//		StatusUpdate statusUpdate = new StatusUpdate(activeChar);
//		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
//
//		statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
//		npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Reward.htm"));
//		activeChar.sendPacket(statusUpdate);
//		npcHtmlMessage.replace("%palyer%", activeChar.getName());
//		npcHtmlMessage.replace("%killed%", String.valueOf(p.getPoints()));
//		npcHtmlMessage.replace("%died%", String.valueOf(LMConfig.LM_EVENT_PLAYER_CREDITS - p.getCredits()));
//		npcHtmlMessage.replace("%reward%", htmltext);
//		activeChar.sendPacket(npcHtmlMessage);
//	}
	private static void rewardPlayer(LMPlayer p)
	{
		Player activeChar = p.getPlayer();

		if (activeChar == null)
			return;

		final int hours = LMConfig.LM_EVENT_VIP_WIN_HOURS;
		if (hours <= 0)
			return;

		if (!VipCoin.addHours(activeChar, hours))
			return;

		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
		npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Reward.htm"));
		npcHtmlMessage.replace("%palyer%", activeChar.getName());
		npcHtmlMessage.replace("%killed%", String.valueOf(p.getPoints()));
		npcHtmlMessage.replace("%died%", String.valueOf(LMConfig.LM_EVENT_PLAYER_CREDITS - p.getCredits()));
		npcHtmlMessage.replace("%reward%", hours + " hora(s) de VIP");
		npcHtmlMessage.replace("%vip_hours%", String.valueOf(hours));
		activeChar.sendPacket(npcHtmlMessage);
	}

	/**
	 * Stops the LMEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove LM npc from world<br>
	 * 3. Open doors specified in LMConfigs<br>
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
		// Opens all doors specified in LMConfigs for LM
		openDoors(LMConfig.LM_DOORS_IDS_TO_CLOSE);
		// Closes all doors specified in LMConfigs for LM
		closeDoors(LMConfig.LM_DOORS_IDS_TO_OPEN);

		for (LMPlayer player : _lmPlayer.values())
		{
			if (player == null)
				continue;
			
			final Player pc = player.getPlayer();
			if (pc == null)
				continue;
			
			net.sf.l2j.gameserver.events.instance.InstanceHolder defaultInstance = net.sf.l2j.gameserver.events.instance.InstanceManager.getInstance().getInstance(0);
			pc.setInstance(defaultInstance, true);
			new LMEventTeleporter(pc, LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
			if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("title") || LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				pc.setTitle(pc._originalTitle);
				pc.broadcastTitleInfo();
				pc.clearPoints();
			}
		}

		// Cleanup list
		clearParticipants();
		_lmPlayer = new HashMap<>();
		// Set state INACTIVE
		setState(EventState.INACTIVE);
	}

	/**
	 * Adds a player to a LMEvent<br>
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
		_lmPlayer.put(activeChar.getObjectId(), new LMPlayer(activeChar, hexCode));
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

		if (_lmPlayer.containsKey(activeChar.getObjectId())) 
			return true;

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
	 * Removes a LMEvent player<br>
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

		_lmPlayer.remove(activeChar.getObjectId());

		return true;
	}
	public static boolean payParticipationFee(Player activeChar)
	{
		int itemId = LMConfig.LM_EVENT_PARTICIPATION_FEE[0];
		int itemNum = LMConfig.LM_EVENT_PARTICIPATION_FEE[1];
		if (itemId == 0 || itemNum == 0)
			return true;

		if (activeChar.getInventory().getInventoryItemCount(itemId, -1) < itemNum)
			return false;

		return activeChar.destroyItemByItemId("LM Participation Fee", itemId, itemNum, _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = LMConfig.LM_EVENT_PARTICIPATION_FEE[0];
		int itemNum = LMConfig.LM_EVENT_PARTICIPATION_FEE[1];

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
		CreatureSay cs = new CreatureSay(0, Say2.PARTY, "LM Manager", message);

		for (LMPlayer player : _lmPlayer.values())
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

		new LMEventTeleporter(activeChar, false, false);
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
				activeChar.teleToLocation(LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)-50, LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)-50, LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);

			if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("title") || LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				activeChar.setTitle(activeChar._originalTitle);
				activeChar.broadcastTitleInfo();
			}
		}
	}

	/**
	 * Called on every bypass by npc of type L2LMEventNpc<br>
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

		if (command.equals("lm_event_participation"))
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
			else if (LMConfig.DISABLE_ID_CLASSES != null && LMConfig.DISABLE_ID_CLASSES.contains(activeChar.getClassId().getId()))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Class.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
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
			else if (playerLevel < LMConfig.LM_EVENT_MIN_LVL || playerLevel > LMConfig.LM_EVENT_MAX_LVL)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(LMConfig.LM_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(LMConfig.LM_EVENT_MAX_LVL));
				}
			}
			else if (LMConfig.LM_EVENT_MULTIBOX_PROTECTION_ENABLE && onMultiBoxRestriction(activeChar))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "MultiBox.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%maxbox%", String.valueOf(LMConfig.LM_EVENT_NUMBER_BOX_REGISTER));
				}
			}
			else if (getPlayerCounts() == LMConfig.LM_EVENT_MAX_PLAYERS)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Full.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(LMConfig.LM_EVENT_MAX_PLAYERS));
				}
			}
			else if (KTBEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
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
			
			else if (DMEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (OlympiadManager.getInstance().isRegisteredInComp(activeChar) || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() > 0)
			{
				activeChar.sendMessage("You can't register while you are in olympiad!");
				return;
			}
			else if (isPlayerParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else if (addParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else
				return;

			activeChar.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("lm_event_remove_participation"))
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

		if (isPlayerParticipant(objectId) && !LMConfig.LM_EVENT_SCROLL_ALLOWED) 
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

		if (isPlayerParticipant(objectId) && !LMConfig.LM_EVENT_POTIONS_ALLOWED) 
			return false;

		return true;
	}

	/**
	 * Called on every escape use<br><br>
	 *
	 * @param objectId as Integer<br>
	 * @return boolean: true if player is not in LM Event, otherwise false<br>
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

		if (isPlayerParticipant(objectId) && !LMConfig.LM_EVENT_SUMMON_BY_ITEM_ALLOWED) 
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

		short killedCredits = _lmPlayer.get(killedPlayerInstance.getObjectId()).getCredits();
		if (killedCredits <= 1)
		{
			removeParticipant(killedPlayerInstance);
			net.sf.l2j.gameserver.events.instance.InstanceHolder defaultInstance = net.sf.l2j.gameserver.events.instance.InstanceManager.getInstance().getInstance(0);
			killedPlayerInstance.setInstance(defaultInstance, true);
			
		//	if (killedPlayerInstance instanceof FakePlayer)
		//		killedPlayerInstance.teleToLocation(60608, -94016, -1344, 0);
		//	else
			{
				new LMEventTeleporter(killedPlayerInstance, LMConfig.LM_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);	
				if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("title") || LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
				{
					killedPlayerInstance.setTitle(killedPlayerInstance._originalTitle);
					killedPlayerInstance.broadcastTitleInfo();
				}
			}
		}
		else
		{
			_lmPlayer.get(killedPlayerInstance.getObjectId()).decreaseCredits();
			new LMEventTeleporter(killedPlayerInstance, false, false);
		}

		if (killerCharacter == null) return;

		Player killerPlayerInstance = null;

		if (killerCharacter instanceof Pet || killerCharacter instanceof Summon)
		{
			killerPlayerInstance = ((Summon) killerCharacter).getOwner();
			if (killerPlayerInstance == null) return;
		}
		else if (killerCharacter instanceof Player)
			killerPlayerInstance = (Player) killerCharacter;
		else
			return;

		if (isPlayerParticipant(killerPlayerInstance))
		{			
			_lmPlayer.get(killerPlayerInstance.getObjectId()).increasePoints();
			net.sf.l2j.gameserver.events.eventgear.EventGearManager.onEventKill(killerPlayerInstance);
			String msg = "";

			CreatureSay cs = new CreatureSay(killerPlayerInstance.getObjectId(), Say2.TELL, "Last Man", "You killed " + _lmPlayer.get(killerPlayerInstance.getObjectId()).getPoints() + " player(s)!");
			killerPlayerInstance.sendPacket(cs);
			if (killedCredits <= 1)
			{
				msg = "You do not have credits, leaving the event!";	
			}	
			else
				msg = "Now you have " + String.valueOf(killedCredits - 1) + " credit(s)!";
			cs = new CreatureSay(killedPlayerInstance.getObjectId(), Say2.TELL, "Last Man", msg);
			killedPlayerInstance.sendPacket(cs);
			
			if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("pm"))
			{
				sysMsgToAllParticipants(net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killerPlayerInstance) + " Hunted Player " + net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killedPlayerInstance) + "!");
			}
			else if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("title"))
			{
				killerPlayerInstance.increasePointScore();
				killerPlayerInstance.setTitle("Kills: " + killerPlayerInstance.getPointScore());
				killerPlayerInstance.broadcastTitleInfo();
			}
			else if (LMConfig.LM_EVENT_ON_KILL.equalsIgnoreCase("pmtitle"))
			{
				sysMsgToAllParticipants(net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killerPlayerInstance) + " Hunted Player " + net.sf.l2j.gameserver.events.eventgear.EventGearManager.getDisplayName(killedPlayerInstance) + "!");
				killerPlayerInstance.increasePointScore();
				killerPlayerInstance.setTitle("Kills: " + killerPlayerInstance.getPointScore());
				killerPlayerInstance.broadcastTitleInfo();
			}
		}

		if (getPlayerCounts() == 1)
			LMManager.getInstance().skipDelay();
	}

	/**
	 * Called on Appearing packet received (player finished teleporting)<br><br>
	 * @param activeChar 
	 * 
	 */
	public static void onTeleported(Player activeChar)
	{
		if (!isStarted() || activeChar == null || !isPlayerParticipant(activeChar.getObjectId())) return;

		if (activeChar.isMageClass())
		{
			if (LMConfig.LM_EVENT_MAGE_BUFFS != null && !LMConfig.LM_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : LMConfig.LM_EVENT_MAGE_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, LMConfig.LM_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
						skill.getEffects(activeChar, activeChar);
				}
			}
		}
		else
		{
			if (LMConfig.LM_EVENT_FIGHTER_BUFFS != null && !LMConfig.LM_EVENT_FIGHTER_BUFFS.isEmpty())
			{
				for (int i : LMConfig.LM_EVENT_FIGHTER_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, LMConfig.LM_EVENT_FIGHTER_BUFFS.get(i));
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
	public static final boolean checkForLMSkill(Player source, Player target, L2Skill skill)
	{
		if (!isStarted()) 
			return true;

		// LM is started
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
		return _lmPlayer.size();
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
	        for (LMPlayer lp : _lmPlayer.values())
	            all.put(lp.getPlayer().getObjectId(), lp.getPlayer());
	    }
	    return all;
	}


	public static boolean onMultiBoxRestriction(Player activeChar)
	{
		return IPManager.getInstance().validBox(activeChar, LMConfig.LM_EVENT_NUMBER_BOX_REGISTER, allParticipants().values(), false);
	}
}
