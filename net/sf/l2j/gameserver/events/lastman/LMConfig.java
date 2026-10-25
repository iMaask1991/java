package net.sf.l2j.gameserver.events.lastman;

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

public class LMConfig
{
	protected static final Logger _log = Logger.getLogger(LMConfig.class.getName());
	
	private static final String LM_FILE = "./config/HadesProject/Events/LastMan.properties";
	
	public static boolean LM_EVENT_ENABLED;
	public static String[] LM_EVENT_INTERVAL;
	public static Long LM_EVENT_PARTICIPATION_TIME;
	public static int LM_EVENT_RUNNING_TIME;
	public static String LM_NPC_LOC_NAME;
	public static int LM_EVENT_PARTICIPATION_NPC_ID;
	public static short LM_EVENT_PLAYER_CREDITS;
	public static int[] LM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] LM_EVENT_PARTICIPATION_FEE = new int[2];
	public static int LM_EVENT_MIN_PLAYERS;
	public static int LM_EVENT_MAX_PLAYERS;
	public static int LM_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int LM_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static List<int[]> LM_EVENT_PLAYER_COORDINATES = new ArrayList<>();
	public static boolean LM_EVENT_SCROLL_ALLOWED;
	public static boolean LM_EVENT_POTIONS_ALLOWED;
	public static boolean LM_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> LM_DOORS_IDS_TO_OPEN = new ArrayList<>();
	public static List<Integer> LM_DOORS_IDS_TO_CLOSE = new ArrayList<>();
	public static boolean LM_REWARD_PLAYERS_TIE;
	public static int LM_EVENT_VIP_WIN_HOURS;
	public static byte LM_EVENT_MIN_LVL;
	public static byte LM_EVENT_MAX_LVL;
	public static int LM_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> LM_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> LM_EVENT_MAGE_BUFFS;
	public static boolean LM_EVENT_MULTIBOX_PROTECTION_ENABLE;
	public static int LM_EVENT_NUMBER_BOX_REGISTER;
	public static String LM_EVENT_ON_KILL;
	public static String DISABLE_ID_CLASSES_STRING;
	public static List<Integer> DISABLE_ID_CLASSES;
	public static boolean ALLOW_EVENT_COMMANDS;
	public static String LM_ID_RESTRICT;
	public static List<Integer> LM_LISTID_RESTRICT;
	public static List<Integer> LM_SKILL_LIST = new ArrayList<>();	
	public static boolean BLOCK_SKILLS_HEAL_LM;

	public static void init()
	{
	    ExProperties events = load(LM_FILE);
	    DISABLE_ID_CLASSES = new ArrayList<>();
	    BLOCK_SKILLS_HEAL_LM =  events.getProperty("BlockSkillsHealInLM", false);
	    ALLOW_EVENT_COMMANDS = events.getProperty("AllowEventCommandLM", false);
	    for (String id : events.getProperty("LMSkillIds", "0").split(","))
		{
			LM_SKILL_LIST.add(Integer.parseInt(id));
		}
	    LM_ID_RESTRICT = events.getProperty("ItemsRestrictionLM");
		LM_LISTID_RESTRICT = new ArrayList<>();
		for (String id : LM_ID_RESTRICT.split(","))
			LM_LISTID_RESTRICT.add(Integer.parseInt(id));
		Long time = 0L;
		LM_EVENT_ENABLED = events.getProperty("LMEventEnabled", false);
		LM_EVENT_INTERVAL = events.getProperty("LMEventInterval", "8:00,14:00,20:00,2:00").split(",");
		String[] timeParticipation = events.getProperty("LMEventParticipationTime", "01:00:00").split(":");
		time = 0L;
		time += Long.parseLong(timeParticipation[0]) * 3600L;
		time += Long.parseLong(timeParticipation[1]) * 60L;
		time += Long.parseLong(timeParticipation[2]);
		LM_EVENT_PARTICIPATION_TIME = time * 1000L;
		LM_EVENT_RUNNING_TIME = events.getProperty("LMEventRunningTime", 1800);
		LM_NPC_LOC_NAME = events.getProperty("LMNpcLocName", "Giran Town");
		LM_EVENT_PARTICIPATION_NPC_ID = events.getProperty("LMEventParticipationNpcId", 0);
		short credits = Short.parseShort(events.getProperty("LMEventPlayerCredits", "1"));
		LM_EVENT_PLAYER_CREDITS = (credits > 0 ? credits : 1);
		if (LM_EVENT_PARTICIPATION_NPC_ID == 0)
		{
			LM_EVENT_ENABLED = false;
			_log.warning("LMEventEngine[Config.load()]: invalid config property -> LMEventParticipationNpcId");
		}
		else
		{
			String[] propertySplit = events.getProperty("LMEventParticipationNpcCoordinates", "0,0,0").split(",");
			if (propertySplit.length < 3)
			{
				LM_EVENT_ENABLED = false;
				_log.warning("LMEventEngine[Config.load()]: invalid config property -> LMEventParticipationNpcCoordinates");
			}
			else
			{
				{
					LM_DOORS_IDS_TO_OPEN = new ArrayList<>();
					LM_DOORS_IDS_TO_CLOSE = new ArrayList<>();
					LM_EVENT_PLAYER_COORDINATES = new ArrayList<>();

					
					LM_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
					LM_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
					LM_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
					LM_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
					
					if (propertySplit.length == 4) LM_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplit[3]);
					LM_EVENT_MIN_PLAYERS = events.getProperty("LMEventMinPlayers", 1);
					LM_EVENT_MAX_PLAYERS = events.getProperty("LMEventMaxPlayers", 20);
					LM_EVENT_MIN_LVL = (byte) events.getProperty("LMEventMinPlayerLevel", 1);
					LM_EVENT_MAX_LVL = (byte) events.getProperty("LMEventMaxPlayerLevel", 80);
					LM_EVENT_RESPAWN_TELEPORT_DELAY = events.getProperty("LMEventRespawnTeleportDelay", 20);
					LM_EVENT_START_LEAVE_TELEPORT_DELAY = events.getProperty("LMEventStartLeaveTeleportDelay", 20);
					LM_EVENT_EFFECTS_REMOVAL = events.getProperty("LMEventEffectsRemoval", 0);
					LM_EVENT_MULTIBOX_PROTECTION_ENABLE = events.getProperty("LMEventMultiBoxEnable", false);
					LM_EVENT_NUMBER_BOX_REGISTER = events.getProperty("LMEventNumberBoxRegister", 1);
					
					propertySplit = events.getProperty("LMEventParticipationFee", "0,0").split(",");
					try
					{
						LM_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplit[0]);
						LM_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplit[1]);
					}
					catch (NumberFormatException nfe)
					{
						if (propertySplit.length > 0) _log.warning("LMEventEngine[Config.load()]: invalid config property -> LMEventParticipationFee");
					}
					
					LM_EVENT_VIP_WIN_HOURS = events.getProperty("LMEventRewardVipWin", 2);
					
					propertySplit = events.getProperty("LMEventPlayerCoordinates", "0,0,0").split(";");
					for (String coordPlayer : propertySplit)
					{
						String[] coordSplit = coordPlayer.split(",");
						if (coordSplit.length != 3) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMEventPlayerCoordinates \"", coordPlayer, "\""));
						else
						{
							try
							{
								LM_EVENT_PLAYER_COORDINATES.add(new int[] { Integer.parseInt(coordSplit[0]), Integer.parseInt(coordSplit[1]), Integer.parseInt(coordSplit[2]) });
							}
							catch (NumberFormatException nfe)
							{
								if (!coordPlayer.isEmpty()) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMEventPlayerCoordinates \"", coordPlayer, "\""));
							}
						}
					}
					
					LM_EVENT_SCROLL_ALLOWED = events.getProperty("LMEventScrollsAllowed", false);
					LM_EVENT_POTIONS_ALLOWED = events.getProperty("LMEventPotionsAllowed", false);
					LM_EVENT_SUMMON_BY_ITEM_ALLOWED = events.getProperty("LMEventSummonByItemAllowed", false);
					LM_REWARD_PLAYERS_TIE = events.getProperty("LMRewardPlayersTie", false);
					LM_EVENT_ON_KILL = events.getProperty("LMEventOnKill", "pmteam");
        			DISABLE_ID_CLASSES_STRING = events.getProperty("LMDisabledForClasses");
        			DISABLE_ID_CLASSES = new ArrayList<>();
        			for(String class_id : DISABLE_ID_CLASSES_STRING.split(","))
        				DISABLE_ID_CLASSES.add(Integer.parseInt(class_id));

					propertySplit = events.getProperty("LMDoorsToOpen", "").split(";");
					for (String door : propertySplit)
					{
						try
						{
							LM_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
						}
						catch (NumberFormatException nfe)
						{
							if (!door.isEmpty()) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMDoorsToOpen \"", door, "\""));
						}
					}
					
					propertySplit = events.getProperty("LMDoorsToClose", "").split(";");
					for (String door : propertySplit)
					{
						try
						{
							LM_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
						}
						catch (NumberFormatException nfe)
						{
							if (!door.isEmpty()) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMDoorsToClose \"", door, "\""));
						}
					}
					
					propertySplit = events.getProperty("LMEventFighterBuffs", "").split(";");
					if (!propertySplit[0].isEmpty())
					{
						LM_EVENT_FIGHTER_BUFFS = new HashMap<>(propertySplit.length);
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMEventFighterBuffs \"", skill, "\""));
							else
							{
								try
								{
									LM_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty()) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMEventFighterBuffs \"", skill, "\""));
								}
							}
						}
					}
					
					propertySplit = events.getProperty("LMEventMageBuffs", "").split(";");
					if (!propertySplit[0].isEmpty())
					{
						LM_EVENT_MAGE_BUFFS = new HashMap<>(propertySplit.length);
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMEventMageBuffs \"", skill, "\""));
							else
							{
								try
								{
									LM_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty()) _log.warning(StringUtil.concat("LMEventEngine[Config.load()]: invalid config property -> LMEventMageBuffs \"", skill, "\""));
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