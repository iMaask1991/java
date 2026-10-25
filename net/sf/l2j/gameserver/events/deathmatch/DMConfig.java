package net.sf.l2j.gameserver.events.deathmatch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.holder.RewardHolder;

public class DMConfig
{
	protected static final Logger _log = Logger.getLogger(DMConfig.class.getName());

	private static final String DM_FILE = "./config/HadesProject/Events/DeathMatch.properties";

	public static boolean DM_EVENT_ENABLED;
	public static String[] DM_EVENT_INTERVAL;
	public static Long DM_EVENT_PARTICIPATION_TIME;
	public static int DM_EVENT_RUNNING_TIME;
	public static String DM_NPC_LOC_NAME;
	public static int DM_EVENT_PARTICIPATION_NPC_ID;
	public static int[] DM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] DM_EVENT_PARTICIPATION_FEE = new int[2];
	public static int DM_EVENT_MIN_PLAYERS;
	public static int DM_EVENT_MAX_PLAYERS;
	public static int DM_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int DM_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static List<int[]> DM_EVENT_PLAYER_COORDINATES = new ArrayList<>();
	public static int DM_REWARD_FIRST_PLAYERS;
	public static boolean ALLOW_DM_COMMANDS;
	public static int DM_EVENT_VIP_1ST_HOURS;
	public static int DM_EVENT_VIP_2ND_HOURS;
	public static int DM_EVENT_VIP_3RD_HOURS;
	public static int DM_EVENT_VIP_4TH_HOURS;
	public static int DM_EVENT_VIP_5TH_HOURS;
	public static int DM_EVENT_VIP_5AFT_HOURS;
	
	public static boolean BLOCK_SKILLS_HEAL_DM;
	public static boolean DM_SHOW_TOP_RANK;
	public static int DM_TOP_RANK;
	public static boolean DM_EVENT_SCROLL_ALLOWED;
	public static boolean DM_EVENT_POTIONS_ALLOWED;
	public static boolean DM_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> DM_DOORS_IDS_TO_OPEN = new ArrayList<>();
	public static List<Integer> DM_DOORS_IDS_TO_CLOSE = new ArrayList<>();
	public static boolean DM_REWARD_PLAYERS_TIE;
//	public static boolean DM_REWARD_PLAYERS_NO_CARRIER;
	public static byte DM_EVENT_MIN_LVL;
	public static byte DM_EVENT_MAX_LVL;
	public static boolean DM_EVENT_MULTIBOX_PROTECTION_ENABLE;
	public static int DM_EVENT_NUMBER_BOX_REGISTER;
	public static Map<Integer, Integer> DM_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> DM_EVENT_MAGE_BUFFS;
	public static String DM_EVENT_ON_KILL;
	public static String DISABLE_ID_CLASSES_STRING;
	public static List<Integer> DISABLE_ID_CLASSES;
	
	public static boolean DM_PLAYER_CAN_BE_KILLED_IN_PZ;
//	public static boolean ENABLE_DM_INSTANCE;
//	public static int DM_INSTANCE_ID;
	
	public static void init()
	{
		ExProperties events = load(DM_FILE);
		DISABLE_ID_CLASSES = new ArrayList<>();
		BLOCK_SKILLS_HEAL_DM  = events.getProperty("BlockSkillsHealInDM", false);
		DM_PLAYER_CAN_BE_KILLED_IN_PZ = events.getProperty("EnableDMPeaceZoneAttack", false);
	//	ENABLE_DM_INSTANCE = events.getProperty("EnableDMInstance", false);
	//	DM_INSTANCE_ID = events.getProperty("DMInstanceId", 1);
		
		Long time = 0L;					
		DM_EVENT_ENABLED = events.getProperty("DMEventEnabled", false);
		DM_EVENT_INTERVAL = events.getProperty("DMEventInterval", "8:00,14:00,20:00,2:00").split(",");
		String[] timeParticipation = events.getProperty("DMEventParticipationTime", "01:00:00").split(":");
		time = 0L;
		time += Long.parseLong(timeParticipation[0]) * 3600L;
		time += Long.parseLong(timeParticipation[1]) * 60L;
		time += Long.parseLong(timeParticipation[2]);
		DM_EVENT_PARTICIPATION_TIME = time * 1000L;
		DM_EVENT_RUNNING_TIME = events.getProperty("DMEventRunningTime", 1800);
		DM_NPC_LOC_NAME = events.getProperty("DMNpcLocName", "Giran Town");
		DM_EVENT_PARTICIPATION_NPC_ID = events.getProperty("DMEventParticipationNpcId", 0);
		DM_SHOW_TOP_RANK = events.getProperty("DMShowTopRank", false);
		DM_TOP_RANK = events.getProperty("DMTopRank", 10);
		if (DM_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			DM_EVENT_ENABLED = false;
			_log.warning("DMEventEngine[Config.load()]: invalid config property -> DMEventParticipationNpcId");
		}
		else
		{
			String[] propertySplit = events.getProperty("DMEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (propertySplit.length < 3)
			{
				DM_EVENT_ENABLED = false;
				_log.warning("DMEventEngine[Config.load()]: invalid config property -> DMEventParticipationNpcCoordinates");
			}
			else
			{
				{
					DM_DOORS_IDS_TO_OPEN = new ArrayList<>();
					DM_DOORS_IDS_TO_CLOSE = new ArrayList<>();
					DM_EVENT_PLAYER_COORDINATES = new ArrayList<>();


					DM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
					DM_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
					DM_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
					DM_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplit[2]);

					if (propertySplit.length == 4) DM_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplit[3]);
					DM_EVENT_MIN_PLAYERS = events.getProperty("DMEventMinPlayers", 1);
					DM_EVENT_MAX_PLAYERS = events.getProperty("DMEventMaxPlayers", 20);
					DM_EVENT_MIN_LVL = (byte) events.getProperty("DMEventMinPlayerLevel", 1);
					DM_EVENT_MAX_LVL = (byte) events.getProperty("DMEventMaxPlayerLevel", 80);
					DM_EVENT_RESPAWN_TELEPORT_DELAY = events.getProperty("DMEventRespawnTeleportDelay", 20);
					DM_EVENT_START_LEAVE_TELEPORT_DELAY = events.getProperty("DMEventStartLeaveTeleportDelay", 20);
					DM_EVENT_MULTIBOX_PROTECTION_ENABLE = events.getProperty("DMEventMultiBoxEnable", false);
					DM_EVENT_NUMBER_BOX_REGISTER = events.getProperty("DMEventNumberBoxRegister", 1);

					propertySplit = events.getProperty("DMEventParticipationFee", "0,0").split(",");
					try
					{
						DM_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplit[0]);
						DM_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplit[1]);
					}
					catch (NumberFormatException nfe)
					{
						if (propertySplit.length > 0) 
							_log.warning("DMEventEngine[Config.load()]: invalid config property -> DMEventParticipationFee");
					}

					DM_REWARD_FIRST_PLAYERS = events.getProperty("DMRewardFirstPlayers", 1);
					//Novo
					ALLOW_DM_COMMANDS = events.getProperty("DMEventCommand", false);
					DM_EVENT_VIP_1ST_HOURS = events.getProperty("DMEventRewardVip1st", 2);
					DM_EVENT_VIP_2ND_HOURS = events.getProperty("DMEventRewardVip2nd", 1);
					DM_EVENT_VIP_3RD_HOURS = events.getProperty("DMEventRewardVip3rd", 1);
					DM_EVENT_VIP_4TH_HOURS = events.getProperty("DMEventRewardVip4th", 1);
					DM_EVENT_VIP_5TH_HOURS = events.getProperty("DMEventRewardVip5th", 1);
					DM_EVENT_VIP_5AFT_HOURS = events.getProperty("DMEventRewardVip5aft", 1);
					
					propertySplit = events.getProperty("DMEventPlayerCoordinates", "0,0,0").split(";");
					for (String coordPlayer : propertySplit)
					{
						String[] coordSplit = coordPlayer.split(",");
						if (coordSplit.length != 3) 
							_log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMEventPlayerCoordinates \"", coordPlayer, "\""));
						else
						{
							try
							{
								DM_EVENT_PLAYER_COORDINATES.add(new int[] { Integer.parseInt(coordSplit[0]), Integer.parseInt(coordSplit[1]), Integer.parseInt(coordSplit[2]) });
							}
							catch (NumberFormatException nfe)
							{
								if (!coordPlayer.isEmpty()) _log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMEventPlayerCoordinates \"", coordPlayer, "\""));
							}
						}
					}

					DM_EVENT_SCROLL_ALLOWED = events.getProperty("DMEventScrollsAllowed", false);
					DM_EVENT_POTIONS_ALLOWED = events.getProperty("DMEventPotionsAllowed", false);
					DM_EVENT_SUMMON_BY_ITEM_ALLOWED = events.getProperty("DMEventSummonByItemAllowed", false);
					DM_REWARD_PLAYERS_TIE = events.getProperty("DMRewardPlayersTie", false);
				//	DM_REWARD_PLAYERS_NO_CARRIER = events.getProperty("DMRewardNoCarrierPlayers", false);
					DM_EVENT_ON_KILL = events.getProperty("DMEventOnKill", "pmteam");
        			DISABLE_ID_CLASSES_STRING = events.getProperty("DMDisabledForClasses");
        			DISABLE_ID_CLASSES = new ArrayList<>();
        			for(String class_id : DISABLE_ID_CLASSES_STRING.split(","))
        				DISABLE_ID_CLASSES.add(Integer.parseInt(class_id));

					propertySplit = events.getProperty("DMDoorsToOpen", "").split(";");
					for (String door : propertySplit)
					{
						try
						{
							DM_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
						}
						catch (NumberFormatException nfe)
						{
							if (!door.isEmpty()) _log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMDoorsToOpen \"", door, "\""));
						}
					}

					propertySplit = events.getProperty("DMDoorsToClose", "").split(";");
					for (String door : propertySplit)
					{
						try
						{
							DM_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
						}
						catch (NumberFormatException nfe)
						{
							if (!door.isEmpty()) _log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMDoorsToClose \"", door, "\""));
						}
					}

					propertySplit = events.getProperty("DMEventFighterBuffs", "").split(";");
					if (!propertySplit[0].isEmpty())
					{
						DM_EVENT_FIGHTER_BUFFS = new HashMap<>(propertySplit.length);
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2)
								_log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMEventFighterBuffs \"", skill, "\""));
							else
							{
								try
								{
									DM_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty())
										_log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMEventFighterBuffs \"", skill, "\""));
								}
							}
						}
					}

					propertySplit = events.getProperty("DMEventMageBuffs", "").split(";");
					if (!propertySplit[0].isEmpty())
					{
						DM_EVENT_MAGE_BUFFS = new HashMap<>(propertySplit.length);
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2) 
								_log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMEventMageBuffs \"", skill, "\""));
							else
							{
								try
								{
									DM_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty())
										_log.warning(StringUtil.concat("DMEventEngine[Config.load()]: invalid config property -> DMEventMageBuffs \"", skill, "\""));
								}
							}
						}
					}
				}
			}
		}		
	}

	public static ExProperties load(String filename)
	{
		return load(new File(filename));
	}

	public static ExProperties load(File file)
	{
		ExProperties result = new ExProperties();

		try
		{
			result.load(file);
		}
		catch (IOException e)
		{
			_log.warning("Error loading config : " + file.getName() + "!");
		}

		return result;
	}
}