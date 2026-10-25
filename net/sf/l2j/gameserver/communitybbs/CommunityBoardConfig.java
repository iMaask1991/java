package net.sf.l2j.gameserver.communitybbs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * Custom Community Board configuration (Mobius HomeBoard).
 */
public class CommunityBoardConfig
{
	private static final CLogger LOGGER = new CLogger(CommunityBoardConfig.class.getName());
	private static final String COMMUNITY_BOARD_CONFIG_FILE = "./config/Custom/CommunityBoard.ini";
	
	public static boolean CUSTOM_CB_ENABLED;
	public static int COMMUNITYBOARD_CURRENCY;
	public static boolean COMMUNITYBOARD_ENABLE_MULTISELLS;
	public static boolean COMMUNITYBOARD_ENABLE_TELEPORTS;
	public static boolean COMMUNITYBOARD_ENABLE_BUFFS;
	public static boolean COMMUNITYBOARD_ENABLE_HEAL;
	public static int COMMUNITYBOARD_TELEPORT_PRICE;
	public static int COMMUNITYBOARD_BUFF_PRICE;
	public static int COMMUNITYBOARD_HEAL_PRICE;
	public static boolean COMMUNITYBOARD_PEACE_ONLY;
	public static boolean COMMUNITYBOARD_COMBAT_DISABLED;
	public static boolean COMMUNITYBOARD_KARMA_DISABLED;
	public static boolean COMMUNITYBOARD_CAST_ANIMATIONS;
	public static boolean CUSTOM_CB_TOP_BUTTONS;
	public static Set<Integer> COMMUNITY_AVAILABLE_BUFFS;
	public static Set<Integer> COMMUNITY_RESIST_BUFFS;
	public static int RESIST_COIN_ID;
	public static int RESIST_COIN_PRICE;
	public static Map<String, Location> COMMUNITY_AVAILABLE_TELEPORTS;
	public static List<Integer> COMMUNITY_RAID_BOSS_IDS = new ArrayList<>();
	public static List<Integer> COMMUNITY_RAID_EASY_IDS = new ArrayList<>();
	public static List<Integer> COMMUNITY_RAID_MID_IDS = new ArrayList<>();
	public static List<Integer> COMMUNITY_RAID_HARD_IDS = new ArrayList<>();
	public static List<Integer> COMMUNITY_GRAND_BOSS_IDS = new ArrayList<>();
	public static int COMMUNITY_RAID_PAGE_LIMIT;
	public static int COMMUNITY_RAID_DROP_PAGE_LIMIT;
	public static String COMMUNITY_RAID_DATE_FORMAT;
	public static List<Integer> COMMUNITY_FIGHTER_BUFFS = new ArrayList<>();
	public static List<Integer> COMMUNITY_MAGE_BUFFS = new ArrayList<>();
	public static List<Integer> COMMUNITY_SUMMON_BUFFS = new ArrayList<>();
	public static List<Integer> COMMUNITY_NPC_MERCHANT = new ArrayList<>();
	public static List<Integer> COMMUNITY_NPC_BUFFER = new ArrayList<>();
	public static List<Integer> COMMUNITY_NPC_GATEKEEPER = new ArrayList<>();
	public static List<Integer> COMMUNITY_NPC_DONATE = new ArrayList<>();
	
	public static void load()
	{
		try
		{
			final ExProperties config = new ExProperties();
			config.load(COMMUNITY_BOARD_CONFIG_FILE);
			
			CUSTOM_CB_ENABLED = config.getProperty("CustomCommunityBoard", false);
			COMMUNITYBOARD_CURRENCY = config.getProperty("CommunityCurrencyId", 57);
			COMMUNITYBOARD_ENABLE_MULTISELLS = config.getProperty("CommunityEnableMultisells", true);
			COMMUNITYBOARD_ENABLE_TELEPORTS = config.getProperty("CommunityEnableTeleports", true);
			COMMUNITYBOARD_ENABLE_BUFFS = config.getProperty("CommunityEnableBuffs", true);
			COMMUNITYBOARD_ENABLE_HEAL = config.getProperty("CommunityEnableHeal", true);
			COMMUNITYBOARD_TELEPORT_PRICE = config.getProperty("CommunityTeleportPrice", 0);
			COMMUNITYBOARD_BUFF_PRICE = config.getProperty("CommunityBuffPrice", 0);
			COMMUNITYBOARD_HEAL_PRICE = config.getProperty("CommunityHealPrice", 0);
			COMMUNITYBOARD_PEACE_ONLY = config.getProperty("CommunityBoardPeaceOnly", false);
			COMMUNITYBOARD_COMBAT_DISABLED = config.getProperty("CommunityCombatDisabled", true);
			COMMUNITYBOARD_KARMA_DISABLED = config.getProperty("CommunityKarmaDisabled", true);
			COMMUNITYBOARD_CAST_ANIMATIONS = config.getProperty("CommunityCastAnimations", false);
			CUSTOM_CB_TOP_BUTTONS = config.getProperty("CustomCommunityBoardTopButtons", true);
			
			final String[] allowedBuffs = config.getProperty("CommunityAvailableBuffs", new String[0], "[\\s,;]+");
			COMMUNITY_AVAILABLE_BUFFS = new HashSet<>(allowedBuffs.length);
			for (String s : allowedBuffs)
			{
				if (!s.isEmpty())
					COMMUNITY_AVAILABLE_BUFFS.add(Integer.parseInt(s));
			}
			
			final String[] resistBuffs = config.getProperty("CommunityResistBuffs", new String[0], "[\\s,;]+");
			COMMUNITY_RESIST_BUFFS = new HashSet<>(resistBuffs.length);
			for (String s : resistBuffs)
			{
				if (!s.isEmpty())
					COMMUNITY_RESIST_BUFFS.add(Integer.parseInt(s));
			}
			
			RESIST_COIN_ID = config.getProperty("ResistCoinId", 10001);
			RESIST_COIN_PRICE = config.getProperty("ResistCoinPrice", 1);
			if (RESIST_COIN_PRICE < 0)
				RESIST_COIN_PRICE = 0;
			
			COMMUNITY_AVAILABLE_TELEPORTS = new HashMap<>();
			addTeleports(config.getProperty("CommunityTeleportList", new String[0], ";"));
			addTeleports(config.getProperty("TeleportRaidBossEasyIds", new String[0], ";"));
			addTeleports(config.getProperty("TeleportRaidBossMidIds", new String[0], ";"));
			addTeleports(config.getProperty("TeleportRaidBossHardIds", new String[0], ";"));
			
			COMMUNITY_RAID_PAGE_LIMIT = config.getProperty("CommunityRaidBossPageLimit", 10);
			COMMUNITY_RAID_DROP_PAGE_LIMIT = config.getProperty("CommunityRaidBossDropPageLimit", 6);
			COMMUNITY_RAID_DATE_FORMAT = config.getProperty("CommunityRaidBossDateFormat", "MMM dd, HH:mm");
			COMMUNITY_RAID_EASY_IDS = parseIdList(config.getProperty("CommunityRaidBossEasyIds", ""));
			COMMUNITY_RAID_MID_IDS = parseIdList(config.getProperty("CommunityRaidBossMidIds", ""));
			COMMUNITY_RAID_HARD_IDS = parseIdList(config.getProperty("CommunityRaidBossHardIds", ""));
			final List<Integer> legacyRaidIds = parseIdList(config.getProperty("CommunityRaidBossIds", ""));
			if (COMMUNITY_RAID_EASY_IDS.isEmpty() && COMMUNITY_RAID_MID_IDS.isEmpty() && COMMUNITY_RAID_HARD_IDS.isEmpty())
				COMMUNITY_RAID_HARD_IDS = legacyRaidIds;
			
			COMMUNITY_RAID_BOSS_IDS = new ArrayList<>();
			COMMUNITY_RAID_BOSS_IDS.addAll(COMMUNITY_RAID_EASY_IDS);
			COMMUNITY_RAID_BOSS_IDS.addAll(COMMUNITY_RAID_MID_IDS);
			COMMUNITY_RAID_BOSS_IDS.addAll(COMMUNITY_RAID_HARD_IDS);
			COMMUNITY_GRAND_BOSS_IDS = parseIdList(config.getProperty("CommunityGrandBossIds", ""));
			COMMUNITY_FIGHTER_BUFFS = parseIdList(config.getProperty("CommunityFighterBuffs", ""));
			COMMUNITY_MAGE_BUFFS = parseIdList(config.getProperty("CommunityMageBuffs", ""));
			COMMUNITY_SUMMON_BUFFS = parseIdList(config.getProperty("CommunitySummonBuffs", ""));
			COMMUNITY_NPC_MERCHANT = parseIdList(config.getProperty("CommunityNpcMerchant", "60000"));
			COMMUNITY_NPC_BUFFER = parseIdList(config.getProperty("CommunityNpcBuffer", "60001"));
			COMMUNITY_NPC_GATEKEEPER = parseIdList(config.getProperty("CommunityNpcGatekeeper", "60002"));
			COMMUNITY_NPC_DONATE = parseIdList(config.getProperty("CommunityNpcDonate", "60004"));
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to load Community Board config: {}", e.getMessage());
		}
	}
	
	public static boolean isTrackedBoss(int bossId)
	{
		return COMMUNITY_RAID_BOSS_IDS.contains(bossId) || COMMUNITY_GRAND_BOSS_IDS.contains(bossId);
	}
	
	public static String getNpcBoardCommand(int npcId)
	{
		if (COMMUNITY_NPC_MERCHANT.contains(npcId))
			return "_bbstop;merchant/main.html";
		if (COMMUNITY_NPC_BUFFER.contains(npcId))
			return "_bbstop;buffer/main.html";
		if (COMMUNITY_NPC_GATEKEEPER.contains(npcId))
			return "_bbstop;gatekeeper/main.html";
		if (COMMUNITY_NPC_DONATE.contains(npcId))
			return "_bbsdonate";
		return null;
	}
	
	public static Location getTeleportLocation(String name)
	{
		if (name == null || name.isEmpty())
			return null;
		
		return COMMUNITY_AVAILABLE_TELEPORTS.get(name);
	}
	
	private static void addTeleports(String[] entries)
	{
		if (entries == null)
			return;
		
		for (String s : entries)
		{
			if (s == null || s.isEmpty())
				continue;
			
			final String[] splitInfo = s.split(",");
			if (splitInfo.length < 4)
				continue;
			
			try
			{
				COMMUNITY_AVAILABLE_TELEPORTS.put(splitInfo[0].trim(), new Location(Integer.parseInt(splitInfo[1].trim()), Integer.parseInt(splitInfo[2].trim()), Integer.parseInt(splitInfo[3].trim())));
			}
			catch (NumberFormatException e)
			{
			}
		}
	}
	
	private static List<Integer> parseIdList(String raw)
	{
		final List<Integer> ids = new ArrayList<>();
		if (raw == null || raw.isEmpty())
			return ids;
		
		for (String s : raw.split("[,;\\s]+"))
		{
			if (s.isEmpty())
				continue;
			
			try
			{
				final int id = Integer.parseInt(s);
				if (id > 0)
					ids.add(id);
			}
			catch (NumberFormatException e)
			{
			}
		}
		return ids;
	}
}
