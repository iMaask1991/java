package net.sf.l2j.gameserver.events.bossevent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.events.multievent.MultiEventConfig;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;

import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.IPManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.RewardHolder;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;

public class KTBEvent
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

	protected static final Logger _log = Logger.getLogger(KTBEvent.class.getName());
	/** html path **/
	private static final String htmlPath = "data/html/mods/events/ktb/";
	/** The state of the KTBEvent<br> */
	private static EventState _state = EventState.INACTIVE;
	/** The spawn of the participation npc<br> */
	private static L2Spawn _npcSpawn = null;
	/** the npc instance of the participation npc<br> */
	private static Npc _lastNpcSpawn = null;
	/** The spawn of the raidboss npc<br> */
	private static L2Spawn _raidSpawn = null;
	/** the npc instance of the raidboss npc<br> */
	private static Npc _lastRaidSpawn = null;
	private static Map<Integer, KTBPlayer> _ktbPlayer = new HashMap<>();

	private static void clearParticipants()
	{
		for (KTBPlayer participant : _ktbPlayer.values())
		{
			if (participant != null)
				participant.clear();
		}

		_ktbPlayer.clear();
	}

	
	public KTBEvent()
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
	 * Sets the KTBEvent state<br><br>
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
	 * Is KTBEvent inactive?<br><br>
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
	 * Is KTBEvent in inactivating?<br><br>
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
	 * Is KTBEvent in participation?<br><br>
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
	 * Is KTBEvent starting?<br><br>
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
	 * Is KTBEvent started?<br><br>
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
	 * Is KTBEvent rewadrding?<br><br>
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
				doorInstance.closeMe();
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
				doorInstance.openMe();
		}
	}

	/**
	 * UnSpawns the KTBEvent npc
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
	 * UnSpawns the KTBEvent boss
	 */
	private static void unSpawnRaid()
	{
		// Delete the npc
		_lastRaidSpawn.deleteMe();
		SpawnTable.getInstance().deleteSpawn(_lastRaidSpawn.getSpawn(), false);
		// Stop respawning of the npc
	//	_npcSpawn.stopRespawn();
		_raidSpawn.doRespawn();
		_raidSpawn = null;
		_lastRaidSpawn = null;
	}
	/**
	 * Starts the participation of the KTBEvent<br>
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

		NpcTemplate tmpl = NpcData.getInstance().getTemplate(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_ID);

		if (tmpl == null)
		{
			_log.warning("KTBEventEngine[KTBEvent.startParticipation()]: L2NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}

		try
		{
			_npcSpawn = new L2Spawn(tmpl);

		//	_npcSpawn.setLocx(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[0]);
		//	_npcSpawn.setLocy(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[1]);
		//	_npcSpawn.setLocz(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[2]);
		//	_npcSpawn.getAmount();
		//	_npcSpawn.setHeading(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setLoc(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[0], KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[1], KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[2], KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);
			_npcSpawn.setRespawnState(false);
		//	_npcSpawn.doSpawn(false);
			// later no need to delete spawn from db, we don't store it (false)
			SpawnTable.getInstance().addSpawn(_npcSpawn, false);
		//	_npcSpawn.wait();
			_lastNpcSpawn = _npcSpawn.doSpawn(false);
			_lastNpcSpawn.setCurrentHp(_lastNpcSpawn.getMaxHp());
			_lastNpcSpawn.setTitle("KTB Event");
			_lastNpcSpawn.isAggressive();
			_lastNpcSpawn.decayMe();
			_lastNpcSpawn.spawnMe(_npcSpawn.getLocX(), _npcSpawn.getLocY(), _npcSpawn.getLocZ());
			_lastNpcSpawn.broadcastPacket(new MagicSkillUse(_lastNpcSpawn, _lastNpcSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "KTBEventEngine[KTBEvent.startParticipation()]: exception: " + e.getMessage(), e);
			return false;
		}

		setState(EventState.PARTICIPATING);
		return true;
	}

	public static boolean spawnTheEventBoss()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(KTBConfig.LIST_KTB_EVENT_BOSS_ID.get(Rnd.get(KTBConfig.LIST_KTB_EVENT_BOSS_ID.size())));

		if (tmpl == null)
		{
			_log.warning("KTBEventEngine[KTBEvent.spawnTheEventBoss()]: L2NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}

		try
		{
			_raidSpawn = new L2Spawn(tmpl);
			_raidSpawn.setLoc(KTBConfig.KTB_EVENT_BOSS_COORDINATES[0], KTBConfig.KTB_EVENT_BOSS_COORDINATES[1], KTBConfig.KTB_EVENT_BOSS_COORDINATES[2], KTBConfig.KTB_EVENT_BOSS_COORDINATES[3]);
			_raidSpawn.setRespawnDelay(1);
			SpawnTable.getInstance().addSpawn(_raidSpawn, false);
			_lastRaidSpawn = _raidSpawn.doSpawn(false);
			_lastRaidSpawn.setCurrentHp(_lastRaidSpawn.getMaxHp());
			_lastRaidSpawn.setTitle("Event Raid");
			_lastRaidSpawn._isKTBEvent = true;
			_lastRaidSpawn.isAggressive();
			_lastRaidSpawn.decayMe();
			_lastRaidSpawn.spawnMe(_raidSpawn.getLocX(), _raidSpawn.getLocY(), _raidSpawn.getLocZ());
			_lastRaidSpawn.broadcastPacket(new MagicSkillUse(_lastRaidSpawn, _lastRaidSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "KTBEventEngine[KTBEvent.spawnTheEventBoss()]: exception: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	/**
	 * Starts the KTBEvent fight<br>
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
		if (_ktbPlayer.size() < KTBConfig.KTB_EVENT_MIN_PLAYERS)
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);

			// Cleanup of participants
			clearParticipants();

			// Unspawn the event NPC
			unSpawnNpc();
			return false;
		}

		spawnTheEventBoss();
		// Closes all doors specified in configs for dm
		closeDoors(KTBConfig.KTB_DOORS_IDS_TO_CLOSE);
		// Set state STARTED
		setState(EventState.STARTED);

		for (KTBPlayer player: _ktbPlayer.values())
		{
			if (player != null)
			{
				player.getPlayer().setLastCords(player.getPlayer().getX(), player.getPlayer().getY(), player.getPlayer().getZ());
				new KTBEventTeleporter(player.getPlayer(), false, false);
			}
		} 
		return true;
	}

	/**
	 * Calculates the KTBEvent reward<br>
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
		//if (_playersWon == 0)
		//	return "KTB The event is over and you have failed to kill the raid.";

		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);

		for (KTBPlayer player : _ktbPlayer.values())
		{
			if (player != null)
				rewardPlayers(player);
		}

		// Get team which has more points
		return "KTB Event finish! The raid boss has been defeated!";
	}

	private static void rewardPlayers(KTBPlayer player)
	{
		Player activeChar = player.getPlayer();

		// Check for nullpointer
		if (activeChar == null)
			return;

		for (RewardHolder reward : KTBConfig.KTB_EVENT_REWARDS)
		{
			final int amount = activeChar.isVip() ? (int) (reward.getMin() * Config.VIP_DROP_RATES) : reward.getMin();
			activeChar.addItem("Reward", reward.getRewardId(), amount, null, true);
		}
		
	//	if (Config.ACTIVE_MISSION_KTB)
	//	{							
	//		if (!activeChar.checkMissions(activeChar.getObjectId()))
	//			activeChar.updateMissions();
		
	//		if (!(activeChar.isKTBCompleted() || activeChar.getKTBCont() >= Config.MISSION_KTB_COUNT))
	//			activeChar.setKTBCont(activeChar.getKTBCont() + 1);
	//	}

		StatusUpdate statusUpdate = new StatusUpdate(activeChar);
		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

		statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
		npcHtmlMessage.setHtml(HtmCache.getInstance().getHtmForce(htmlPath + "Reward.htm"));
		activeChar.sendPacket(statusUpdate);
		activeChar.sendPacket(npcHtmlMessage);
	}

	/**
	 * Stops the KTBEvent fight<br>
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
		//Unspawn event npc's
		unSpawnNpc();
		unSpawnRaid();
		// Opens all doors specified in configs for DM
		openDoors(KTBConfig.KTB_DOORS_IDS_TO_CLOSE);
		// Closes all doors specified in Configs for DM
		closeDoors(KTBConfig.KTB_DOORS_IDS_TO_OPEN);
	
		for (KTBPlayer player : _ktbPlayer.values())
		{
			if (player != null && player.getPlayer() != null)
				new KTBEventTeleporter(player.getPlayer(), KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
		}

		// Cleanup list
		clearParticipants();
		_ktbPlayer = new HashMap<>();

		// Set state INACTIVE
		setState(EventState.INACTIVE);
	}
	
	/**
	 * Adds a player to a KTBEvent<br>
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

		//String hexCode = hexToString(generateHex(16));
		_ktbPlayer.put(activeChar.getObjectId(), new KTBPlayer(activeChar/*, hexCode*/));
		return true;
	}

	public static boolean isPlayerParticipant(Player activeChar)
	{
		if (activeChar == null)
			return false;
		try
		{
			if (_ktbPlayer.containsKey(activeChar.getObjectId()))
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
	 * Removes a KTBEvent player<br>
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
			_ktbPlayer.remove(activeChar.getObjectId());
		}
		catch (Exception e) 
		{
			return false;
		}

		return true;
	}

	public static boolean payParticipationFee(Player activeChar)
	{
		int itemId = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[0];
		int itemNum = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[1];
		if (itemId == 0 || itemNum == 0)
			return true;

		if (activeChar.getInventory().getInventoryItemCount(itemId, -1) < itemNum)
			return false;

		return activeChar.destroyItemByItemId("KTB Participation Fee", itemId, itemNum, _lastNpcSpawn, true);
	}

	public static String getParticipationFee()
	{
		int itemId = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[0];
		int itemNum = KTBConfig.KTB_EVENT_PARTICIPATION_FEE[1];

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
		CreatureSay cs = new CreatureSay(0, Say2.PARTY, "KTB Manager", message);

		for (KTBPlayer player : _ktbPlayer.values())
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

		new KTBEventTeleporter(activeChar, false, false);
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
		//	if(activeChar.isNoCarrier())
		//	{
		//		return;
		//	}
			if (removeParticipant(activeChar))
				activeChar.teleToLocation(KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)-50, KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)-50, KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES[2], 0);
		}
	}

	/**
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

		if (command.equals("ktb_event_participation"))
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
			else if (OlympiadManager.getInstance().isRegisteredInComp(activeChar) || activeChar.isInOlympiadMode() || activeChar.getOlympiadGameId() > 0)
			{
				activeChar.sendMessage("You can't register while you are in olympiad!");
				return;
			}
			else if (activeChar.isAio() && !Config.ALLOW_AIO_IN_EVENTS)
			{
				activeChar.sendMessage("AIO charactes are not allowed to participate in events.");
				return;
			}
			else if (TvTEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
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
			else if (LMEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (DMEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				activeChar.sendMessage("You already participated in another event!");
				return;
			}
			else if (KTBConfig.DISABLE_ID_CLASSES != null && KTBConfig.DISABLE_ID_CLASSES.contains(activeChar.getClassId().getId()))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Class.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
				}
			}
			else if (playerLevel < KTBConfig.KTB_EVENT_MIN_LVL || playerLevel > KTBConfig.KTB_EVENT_MAX_LVL)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(KTBConfig.KTB_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(KTBConfig.KTB_EVENT_MAX_LVL));
				}
			}
			else if (_ktbPlayer.size() == KTBConfig.KTB_EVENT_MAX_PLAYERS)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Full.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(KTBConfig.KTB_EVENT_MAX_PLAYERS));
				}
			}
			else if (KTBConfig.KTB_EVENT_MULTIBOX_PROTECTION_ENABLE && onMultiBoxRestriction(activeChar))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "MultiBox.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%maxbox%", String.valueOf(KTBConfig.KTB_EVENT_NUMBER_BOX_REGISTER));
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
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtmForce(htmlPath + "Registered.htm"));
			else if (addParticipant(activeChar))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtmForce(htmlPath + "Registered.htm"));
			else
				return;

			if (npcHtmlMessage.getHtml() != null)
				activeChar.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("ktb_event_remove_participation"))
		{
			if (isPlayerParticipant(activeChar))
			{
				removeParticipant(activeChar);

				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtmForce(htmlPath + "Unregistered.htm"));
				if (npcHtmlMessage.getHtml() != null)
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

		if (isPlayerParticipant(objectId) && !KTBConfig.KTB_EVENT_SCROLL_ALLOWED)
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

		if (isPlayerParticipant(objectId) && !KTBConfig.KTB_EVENT_POTIONS_ALLOWED)
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

		if (isPlayerParticipant(objectId) && !KTBConfig.KTB_EVENT_SUMMON_BY_ITEM_ALLOWED)
			return false;

		return true;
	}

	/**
	 * @param killedPlayerInstance as L2PcInstance
	 */
	public static void onDie(Player killedPlayerInstance)
	{
		if (killedPlayerInstance == null || !isStarted()) 
			return;

		if (!isPlayerParticipant(killedPlayerInstance.getObjectId())) 
			return;

		new KTBEventTeleporter(killedPlayerInstance, false, false);
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
			if (KTBConfig.KTB_EVENT_MAGE_BUFFS != null && !KTBConfig.KTB_EVENT_MAGE_BUFFS.isEmpty())
			{
				for (int i : KTBConfig.KTB_EVENT_MAGE_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, KTBConfig.KTB_EVENT_MAGE_BUFFS.get(i));
					if (skill != null)
						skill.getEffects(activeChar, activeChar);
				}
			}
		}
		else
		{
			if (KTBConfig.KTB_EVENT_FIGHTER_BUFFS != null && !KTBConfig.KTB_EVENT_FIGHTER_BUFFS.isEmpty())
			{
				for (int i : KTBConfig.KTB_EVENT_FIGHTER_BUFFS.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(i, KTBConfig.KTB_EVENT_FIGHTER_BUFFS.get(i));
					if (skill != null)
						skill.getEffects(activeChar, activeChar);
				}
			}
		}
	}

	public static int getPlayerCounts()
	{
		return _ktbPlayer.size();
	}
	
	public static Map<Integer, Player> allParticipants()
	{
	    Map<Integer, Player> all = new HashMap<>();
	    if (getPlayerCounts() > 0)
	    {
	        for (KTBPlayer dp : _ktbPlayer.values())
	            all.put(dp.getPlayer().getObjectId(), dp.getPlayer());
	    }
	    return all;
	}


	public static boolean onMultiBoxRestriction(Player activeChar)
	{
		return IPManager.getInstance().validBox(activeChar, KTBConfig.KTB_EVENT_NUMBER_BOX_REGISTER, allParticipants().values(), false);
	}
}