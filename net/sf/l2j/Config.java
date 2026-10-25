package net.sf.l2j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sf.l2j.commons.config.ExProperties;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.enums.GeoType;
import net.sf.l2j.gameserver.model.holder.BuffSkillHolder;
import net.sf.l2j.gameserver.model.holder.CustomEpicSpawnHolder;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.holder.RewardHolder;

/**
 * This class contains global server configuration.<br>
 * It has static final fields initialized from configuration files.
 */
public final class Config
{
	private static final CLogger LOGGER = new CLogger(Config.class.getName());

	public static final String CLANS_FILE = "./config/clans.properties";
	public static final String EVENTS_FILE = "./config/events.properties";
	public static final String GEOENGINE_FILE = "./config/geoengine.properties";
	public static final String HEXID_FILE = "./config/hexid.txt";
	public static final String LOGIN_CONFIGURATION_FILE = "./config/loginserver.properties";
	public static final String NPCS_FILE = "./config/npcs.properties";
	public static final String PLAYERS_FILE = "./config/players.properties";
	public static final String SERVER_FILE = "./config/server.properties";
	public static final String SIEGE_FILE = "./config/siege.properties";
	
	//Mods
	public static final String MODS_FILE = "./config/HadesProject/ModsHadesProject.properties";	
	public static final String URL_FILE = "./config/HadesProject/Urls.properties";
	public static final String OFFLINE_FILE = "./config/HadesProject/Offlineshop.properties";
	public static final String DRESSME_FILE = "./config/HadesProject/Dressme.properties";
	public static final String SUBS_FILE = "./config/HadesProject/Subclass.properties";
	public static final String BALANCE_FILE = "./config/HadesProject/Balance.properties";
	public static final String SKILLS_CHANCE_FILE = "./config/HadesProject/SkillsChance.properties";
	public static final String CLASS_BALANCE_FILE = "./config/HadesProject/balance/ClassBalance.properties";
	public static final String CLASS_BALANCE_CLASSES_DIR = "./config/HadesProject/balance/classes";
	public static final String ENCHANT_FILE = "./config/HadesProject/Enchants.properties";
	public static final String VIP_FILE = "./config/HadesProject/Vip.properties";
	public static final String HERO_FILE = "./config/HadesProject/Hero.properties";
	public static final String AUTOSPOIL_FILE = "./config/HadesProject/AutoSpoil.properties";
	public static final String DONATION_MANAGER = "./config/HadesProject/donationmanager.properties";
	public static final String AIO_FILE = "./config/HadesProject/Aio.properties";
	public static final String REBIRTH_FILE = "./config/HadesProject/Rebirth.properties";
	public static final String PVP_FILE = "./config/HadesProject/Pvp.properties";
	public static final String AUTO_POTS_FILE = "./config/HadesProject/AutoPotas.properties";
	public static final String RUNE_FILE = "./config/HadesProject/Runes.properties";
	public static final String AGATHION_FILE = "./config/HadesProject/Agathions.properties";
	public static final String RATES_FILE = "./config/HadesProject/Rates.properties";
	public static final String BOSSES_FILE = "./config/HadesProject/Bosses.properties";
	public static final String STARTING_FILE = "./config/HadesProject/starting.properties";
	public static final String SKINS_FILE = "./config/HadesProject/skins.properties";
	
    //Protections
	public static final String PROTECT_FILE = "./config/HadesProject/captcha.properties";
	public static final String D_GUARD_FILE = "./config/HadesProject/Protection.properties";
	public static final String FLOODS_FILE = "./config/HadesProject/Floods.properties";
	public static final String MULTISKILL_FILE = "./config/HadesProject/MultiSkill.properties";
	public static final String SKILL_ENCHANT_FILE = "./config/HadesProject/SkillEnchant.properties";
	public static final String BATTLEPASS_FILE = "./config/HadesProject/BattlePass.properties";
	
	//Events
	public static final String PARTY_FARM_FILE = "./config/HadesProject/Events/PartyFarm.properties";
	public static final String PVP_FARM_FILE = "./config/HadesProject/Events/PvpFarm.properties";
	public static final String INSTANCE_FARM_FILE = "./config/HadesProject/InstanceFarmZones.properties";
	public static final String FARM_HWID_FILE = "./config/HadesProject/FarmHwid.properties";
	public static final String AUTOFARM_FILE = "./config/HadesProject/AutoFarm.properties";
	public static final String TOURNAMENT_FILE = "./config/HadesProject/events/Tournament.properties";
	public static final String OLYMPIAD_FILE = "./config/HadesProject/Events/Olympiad.properties";
	public static final String CUSTOM_OLY_FILE = "./config/HadesProject/Events/CustomOly.properties";
	public static final String MEDAL_EVENT_FILE = "./config/HadesProject/Events/MedalEvent.properties";
	public static final String BUFFER_FILE = "./config/HadesProject/Buffer.properties";
	public static final String SOLO_BOSS_EVENT_FILE = "./config/HadesProject/Events/SoloBossEvent.properties";
	public static final String REWARD_SOLO_EVENT_FILE = "./config/HadesProject/Events/RewardSoloEvent.properties";

	public static final int[] HERO_SKILLS = null;

	// --------------------------------------------------
	// Skins settings
	// --------------------------------------------------
	public static boolean ALLOW_SKIN_SYSTEM;
	public static String DRESS_ME_COMMAND;
	public static boolean ALLOW_DRESS_ME_IN_OLY;	

	// --------------------------------------------------
	// Starting settings
	// --------------------------------------------------
	public static boolean STARTING_LOCATION;
	public static int[] NEW_CHAR_COORDINATES = new int[3];
	public static int STARTING_LEVEL;
	public static String NEW_CHAR_TITLE;
	public static boolean STARTING_BUFFS;
	public static List<int[]> STARTING_BUFFS_M = new ArrayList<>(); 
	public static List<int[]> STARTING_BUFFS_F = new ArrayList<>();
	
	// --------------------------------------------------
	// Bosses settings
	// --------------------------------------------------
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_DEFENCE_MULTIPLIER;
	public static int RAID_MINION_RESPAWN_TIMER;
	
	public static boolean RAID_DISABLE_CURSE;
	
	public static int SPAWN_INTERVAL_AQ;
	public static int RANDOM_SPAWN_TIME_AQ;
	
	public static int SPAWN_INTERVAL_ANTHARAS;
	public static int RANDOM_SPAWN_TIME_ANTHARAS;
	public static int WAIT_TIME_ANTHARAS;
	
	public static int SPAWN_INTERVAL_BAIUM;
	public static int RANDOM_SPAWN_TIME_BAIUM;
	
	public static int SPAWN_INTERVAL_CORE;
	public static int RANDOM_SPAWN_TIME_CORE;
	
	public static int SPAWN_INTERVAL_FRINTEZZA;
	public static int RANDOM_SPAWN_TIME_FRINTEZZA;
	public static int WAIT_TIME_FRINTEZZA;
	
	public static int SPAWN_INTERVAL_ORFEN;
	public static int RANDOM_SPAWN_TIME_ORFEN;
	
	public static int SPAWN_INTERVAL_SAILREN;
	public static int RANDOM_SPAWN_TIME_SAILREN;
	public static int WAIT_TIME_SAILREN;
	
	public static int SPAWN_INTERVAL_VALAKAS;
	public static int RANDOM_SPAWN_TIME_VALAKAS;
	public static int WAIT_TIME_VALAKAS;
	
	public static int SPAWN_INTERVAL_ZAKEN;
	public static int RANDOM_SPAWN_TIME_ZAKEN;
	
	public static boolean RESPAWN_CUSTOM;
	public static List<Integer> RESPAWN_EASY_RAID_IDS;
	public static int RESPAWN_EASY_MIN;
	public static int RESPAWN_EASY_RANDOM;
	public static List<Integer> RESPAWN_MID_RAID_IDS;
	public static int RESPAWN_MID_MIN;
	public static int RESPAWN_MID_RANDOM;
	public static List<Integer> RESPAWN_HARD_RAID_IDS;
	public static int RESPAWN_HARD_MIN;
	public static int RESPAWN_HARD_RANDOM;
	
	public static boolean CUSTOM_EPIC_SPAWN_ENABLED;
	public static List<CustomEpicSpawnHolder> CUSTOM_EPIC_SPAWNS;

	public static boolean FLAG_RAIDS;
	public static int FLAG_RANGE;
	public static String FLAG_RAID_IDS;
	public static List<Integer> LIST_FLAG_RAID_IDS;
	
	public static boolean ANNOUNCE_RAID_BOSS_HP;
	public static String ANNOUNCE_RAID_BOSS_HP_IDS_STR;
	public static List<Integer> ANNOUNCE_RAID_BOSS_HP_IDS;
	public static String ANNOUNCE_GRAND_BOSS_HP_IDS_STR;
	public static List<Integer> ANNOUNCE_GRAND_BOSS_HP_IDS;
	public static List<Integer> BOSS_HP_ANNOUNCE_THRESHOLDS;
	public static boolean ANNOUNCE_RAID_BOSS_KILL;
	public static boolean ANNOUNCE_GRAND_BOSS_KILL;
	public static boolean ANNOUNCE_RAID_BOSS_SPAWN_WARNING;
	public static boolean ANNOUNCE_GRAND_BOSS_SPAWN_WARNING;
	public static List<Integer> BOSS_SPAWN_WARNING_MINUTES;
	public static boolean BOSS_SPAWN_WARNING_SHOW_TIME;
	
	public static boolean ENABLE_RAIDBOSS_NOBLES;
	public static String BOSS_NOBLES_IDS;
	public static List<Integer> LIST_BOSS_NOBLES_IDS;
	
	public static int QUEST_BAIUM;
	public static int QUEST_VALAKAS;
	public static int QUEST_ANTHARAS;
	public static int QUEST_SAILREN;
	public static int QUEST_FRINTEZZA;
	
	// --------------------------------------------------
	// Floods settings
	// --------------------------------------------------
	public static int ROLL_DICE_TIME;
	public static int HERO_VOICE_TIME;
	public static int SUBCLASS_TIME;
	public static int DROP_ITEM_TIME;
	public static int SERVER_BYPASS_TIME;
	public static int MULTISELL_TIME;
	public static int MANUFACTURE_TIME;
	public static int MANOR_TIME;
	public static int SENDMAIL_TIME;
	public static int CHARACTER_SELECT_TIME;
	public static int GLOBAL_CHAT_TIME;
	public static int TRADE_CHAT_TIME;
	public static boolean GLOBAL_SHOUT_TRADE_CHAT;
	public static int SOCIAL_TIME;
    public static int MOVE_TIME;
    public static int ATTACK_TIME;
    public static int SKILL_TIME;
    
	// --------------------------------------------------
	// Rates settings
	// --------------------------------------------------
	/** Si False, se ignoran DynamicExpRates.xml y manda RateXp/RateSp. */
	public static boolean ENABLE_DYNAMIC_RATES;
	public static double RATE_XP;
	public static double RATE_SP;
	public static double RATE_PARTY_XP;
	public static double RATE_PARTY_SP;
	public static double RATE_DROP_ADENA;
	public static int DROP_MAX_LEVEL_DIFFERENCE;
	public static double RATE_DROP_ITEMS;
	public static double RATE_DROP_ITEMS_BY_RAID;
	public static double RATE_DROP_SPOIL;
	public static int RATE_DROP_MANOR;
	
	public static double RATE_QUEST_DROP;
	public static double RATE_QUEST_REWARD;
	public static double RATE_QUEST_REWARD_XP;
	public static double RATE_QUEST_REWARD_SP;
	public static double RATE_QUEST_REWARD_ADENA;
	
	public static double RATE_KARMA_EXP_LOST;
	public static double RATE_SIEGE_GUARDS_PRICE;
	
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	
	public static double PET_XP_RATE;
	public static int PET_FOOD_RATE;
	public static double SINEATER_XP_RATE;
	
	public static double RATE_DROP_COMMON_HERBS;
	public static double RATE_DROP_HP_HERBS;
	public static double RATE_DROP_MP_HERBS;
	public static double RATE_DROP_SPECIAL_HERBS;
	
	// --------------------------------------------------
	// Olympiad settings
	// --------------------------------------------------
	public static int ALT_OLY_START_TIME;
	public static int ALT_OLY_MIN;
	public static long ALT_OLY_CPERIOD;
	public static long ALT_OLY_BATTLE;
	public static long ALT_OLY_WPERIOD;
	public static long ALT_OLY_VPERIOD;
	public static int ALT_OLY_WAIT_TIME;
	public static int ALT_OLY_WAIT_BATTLE;
	public static int ALT_OLY_WAIT_END;
	public static int ALT_OLY_START_POINTS;
	public static int ALT_OLY_WEEKLY_POINTS;
	public static int ALT_OLY_MIN_MATCHES;
	public static int ALT_OLY_CLASSED;
	public static int ALT_OLY_NONCLASSED;
	public static IntIntHolder[] ALT_OLY_CLASSED_REWARD;
	public static IntIntHolder[] ALT_OLY_NONCLASSED_REWARD;
	public static int ALT_OLY_GP_PER_POINT;
	public static int ALT_OLY_HERO_POINTS;
	public static int ALT_OLY_RANK1_POINTS;
	public static int ALT_OLY_RANK2_POINTS;
	public static int ALT_OLY_RANK3_POINTS;
	public static int ALT_OLY_RANK4_POINTS;
	public static int ALT_OLY_RANK5_POINTS;
	public static int ALT_OLY_MAX_POINTS;
	public static int ALT_OLY_DIVIDER_CLASSED;
	public static int ALT_OLY_DIVIDER_NON_CLASSED;
	public static boolean ALT_OLY_ANNOUNCE_GAMES;
	
	public static boolean ENABLE_OLYMPIAD_ANNOUNCE;
	public static String OLYMPIAD_ANNOUNCE;
	
	public static boolean OLYMPIAD_REMOVE_SKILL_REUSE;
	
	public static int OLYMPIAD_COUNTDOWN;
	public static boolean OLYMPIAD_COUNTDOWN_HEAL;
	
	public static Map<Integer, Integer> OLYMPIAD_FIGHTER_BUFFS;
	public static Map<Integer, Integer> OLYMPIAD_MAGE_BUFFS;
	
	// --------------------------------------------------
	// Agathions settings
	// --------------------------------------------------
	public static int SKILL_ID_AGATHION;
	public static int SKILL_LEVEL_AGATHION;
	public static int ANIMATION_SPAWN_SKILL_ID_AGATHION;
	public static int ANIMATION_SUMMON_SKILL_ID_AGATHION;
	public static boolean AGATHION_ONLY_FOLLOW;
	public static boolean AGATHION_ALLOW_SOCIAL;
	public static boolean AGATHION_HEAL_OWNER;
	public static double AGATHION_PERCENT_TO_HEAL;

	//----------------------------------------
	//   PartyFarm.properties
	//----------------------------------------
	public static String NAME_EVENT;
	public static List<RewardHolder> PARTY_FARM_REWARDS = new ArrayList<>();
	public static int RADIUS_TO_PARTYFARM;
	public static boolean PARTY_FARM_EVENT_ENABLED;
	public static boolean ENABLE_SPAWN_PARTYFARM_MONSTERS;
	public static boolean ENABLE_DUALBOX_PARTYFARM;
	public static int EVENT_BEST_FARM_TIME;
	public static String[] EVENT_BEST_FARM_INTERVAL_BY_TIME_OF_DAY;
	public static int PARTY_FARM_MONSTER_DALAY;
	public static boolean PARTY_MESSAGE_ENABLED;
	public static String PARTY_FARM_MESSAGE_TEXT;
	public static int PARTY_FARM_MESSAGE_TIME;
	public static int monsterId;
	public static List<Integer> PART_ZONE_MONSTERS_EVENT_ID = new ArrayList<>();
	public static List<int[]> CUSTOM_SPAWN_PARTYFARM = new ArrayList<>();
	public static int AMOUNT_MONSTROS_SPAWN_PT;
	public static int RADIUS_SPAWN_LOC_PTFARM;
	
	/** PvP Farm (clon del Party Farm: flag por rango de mob + reward individual) */
	public static String PVPFARM_NAME;
	public static boolean PVPFARM_EVENT_ENABLED;
	public static boolean PVPFARM_SPAWN_MONSTERS;
	public static int PVPFARM_EVENT_TIME;
	public static String[] PVPFARM_START_TIME;
	public static boolean PVPFARM_FLAG_MONSTERS;
	public static int PVPFARM_FLAG_RANGE;
	public static int PVPFARM_FLAG_INTERVAL;
	public static boolean PVPFARM_FLAG_ON_KILL;
	public static int PVPFARM_FLAG_ON_KILL_TIME;
	public static List<Integer> PVPFARM_MONSTER_IDS = new ArrayList<>();
	public static List<Integer> PVPFARM_FLAG_MONSTER_IDS = new ArrayList<>();
	public static int PVPFARM_MONSTER_DELAY;
	public static List<int[]> PVPFARM_SPAWN_POINTS = new ArrayList<>();
	public static int PVPFARM_SPAWN_AMOUNT;
	public static int PVPFARM_SPAWN_RADIUS;
	public static boolean PVPFARM_MESSAGE_ENABLED;
	public static String PVPFARM_MESSAGE_TEXT;
	public static int PVPFARM_MESSAGE_TIME;
	public static boolean PVPFARM_CUSTOM_REWARD;
	public static List<RewardHolder> PVPFARM_REWARDS = new ArrayList<>();
	public static boolean PVPFARM_ANTI_DUALBOX;
	
	/** Instance Farm Zones (TIME publica / KILL privada) */
	public static boolean INSTANCE_FARM_ENABLED;
	public static boolean FARM_HWID_ENABLED;
	public static boolean FARM_HWID_IGNORE_GM;
	public static int FARM_HWID_UNBIND_COOLDOWN_HOURS;
	public static int INSTANCE_FARM_HUB_NPC_ID;
	public static boolean INSTANCE_FARM_SPAWN_HUB;
	public static int INSTANCE_FARM_HUB_X;
	public static int INSTANCE_FARM_HUB_Y;
	public static int INSTANCE_FARM_HUB_Z;
	public static int INSTANCE_FARM_HUB_HEADING;
	/** 0,0,0 = pueblo mas cercano. */
	public static int INSTANCE_FARM_EXIT_X;
	public static int INSTANCE_FARM_EXIT_Y;
	public static int INSTANCE_FARM_EXIT_Z;
	public static String INSTANCE_FARM_ZONES_FILE;
	/** TIME: item que se paga para entrar y minutos que da. */
	public static int INSTANCE_FARM_TIME_PAY_ITEM_ID;
	public static int INSTANCE_FARM_TIME_PAY_ITEM_COUNT;
	public static int INSTANCE_FARM_TIME_PAY_MINUTES;
	public static int INSTANCE_FARM_TIME_MIN_LEVEL;
	public static boolean INSTANCE_FARM_TIME_SHOW_TIMER;
	/** KILL: packs comprables. Cada int[] = {kills, itemId, count}. */
	public static List<int[]> INSTANCE_FARM_KILL_PACKS = new ArrayList<>();
	public static int INSTANCE_FARM_KILL_SPAWN_DELAY_SECONDS;
	public static int INSTANCE_FARM_KILL_KICK_DELAY_SECONDS;
	public static boolean INSTANCE_FARM_KILL_SHOW_COUNTER;
	/** Recompensa directa al inventario por cada mob del Kill Farm: {itemId, min, max, chance%}. */
	public static List<double[]> INSTANCE_FARM_KILL_REWARDS = new ArrayList<>();
	public static boolean INSTANCE_FARM_KILL_NORMAL_DROP;
	public static boolean INSTANCE_FARM_KILL_NO_CORPSE;
	public static List<double[]> INSTANCE_FARM_TIME_REWARDS = new ArrayList<>();
	public static boolean INSTANCE_FARM_TIME_NORMAL_DROP;
	public static boolean INSTANCE_FARM_TIME_NO_CORPSE;
	public static boolean INSTANCE_FARM_KILL_AOE_AGGRO;
	public static boolean INSTANCE_FARM_KILL_FACE_PLAYER;
	public static int INSTANCE_FARM_KILL_DEATH_KICK_SECONDS;
	/** NPC aparte solo para el Kill Farm (Instance Solo), con su html instancefarm/kill.htm. */
	public static int INSTANCE_FARM_KILL_NPC_ID;
	/** Personajes de la misma PC (HWID) que pueden estar adentro a la vez, en Time y en Kill. 0 = sin limite. */
	public static int INSTANCE_FARM_MAX_PER_HWID;
	/** Contadores en pantalla (Kills / Tiempo): letra chica y posicion (nombre de ExShowScreenMessage.SMPOS). */
	public static boolean INSTANCE_FARM_COUNTER_SMALL;
	public static String INSTANCE_FARM_COUNTER_POSITION;
	public static int INSTANCE_FARM_COUNTER_UPDATE_MS;
	public static int INSTANCE_FARM_TIME_TIMER_SECONDS;
	
	/** AutoFarm debug (ModsHadesProject.properties) */
	public static boolean DEBUG_FARM;
	public static boolean AUTOFARM_ENABLED;
	
	/** AutoFarm por tiempo (AutoFarm.properties) */
	public static boolean AUTOFARM_TIME_ENABLED;
	public static int AUTOFARM_FREE_MINUTES;
	public static int AUTOFARM_VIP_MINUTES;
	public static int AUTOFARM_TIME_ITEM_ID;
	public static int AUTOFARM_TIME_ITEM_COUNT;
	public static int AUTOFARM_TIME_ITEM_MINUTES;
	public static int AUTOFARM_RESET_HOUR;
	
	/** AutoFarm visual: titulo + aura mientras el bot esta prendido */
	public static boolean AUTOFARM_SHOW_TITLE;
	public static String AUTOFARM_TITLE_TEXT;
	public static int AUTOFARM_AURA;
	public static int ANNOUNCE_ID_EVENT;
	
 	//------------------------------------------------
	//  Runes - System
	//------------------------------------------------
	public static int RUNE_1_ID;
	public static double RUNE_1_EXP;
	public static double RUNE_1_SP;
	public static double RUNE_1_ADENA;
	public static double RUNE_1_SPOIL;
	public static double RUNE_1_DROP;
	public static double RUNE_1_RAID_DROP;
	public static double RUNE_1_ENCHANT;
	public static double RUNE_1_AUGMENT_SKILL;
	
	public static int RUNE_2_ID;
	public static double RUNE_2_EXP;
	public static double RUNE_2_SP;
	public static double RUNE_2_ADENA;
	public static double RUNE_2_SPOIL;
	public static double RUNE_2_DROP;
	public static double RUNE_2_RAID_DROP;
	public static double RUNE_2_ENCHANT;
	public static double RUNE_2_AUGMENT_SKILL;
	
	public static int RUNE_3_ID;
	public static double RUNE_3_EXP;
	public static double RUNE_3_SP;
	public static double RUNE_3_ADENA;
	public static double RUNE_3_SPOIL;
	public static double RUNE_3_DROP;
	public static double RUNE_3_RAID_DROP;
	public static double RUNE_3_ENCHANT;
	public static double RUNE_3_AUGMENT_SKILL;
	
 	//------------------------------------------------
	//  PVP - System
	//------------------------------------------------
	public static boolean ENABLE_PVP_REWARD;
	public static boolean ENABLE_PVP_REWARD_IP;
	public static int PVP_REWARD_ID;
	public static int PVP_REWARD_COUNT;
	public static boolean ENABLE_PK_REWARD;
	public static boolean ENABLE_PK_REWARD_IP;
	public static int PK_REWARD_ID;
	public static int PK_REWARD_COUNT;
	public static boolean ENABLE_WAR_REWARD;
	public static int WAR_REWARD_ID;
	public static int WAR_REWARD_COUNT;
	/** True = pierden buffs al morir (fuera de eventos que lo manejen aparte). */
	public static boolean LEAVE_BUFFS_ON_DIE;
	/** True = los participantes de TvT/CTF/DM/LM conservan sus buffs al morir. */
	public static boolean EVENTS_KEEP_BUFFS_ON_DIE;
	
	//------------------------------------------------
	//  Auto Potions (like Soulshots)
	//------------------------------------------------
	public static int CP_POT_CD;
	public static int HP_POT_CD;
	public static int MP_POT_CD;
	public static double CP_REQUIRED;
	public static double HP_REQUIRED;
	public static double MP_REQUIRED;
	
	//------------------------------------------------
	//  Rebirth - System
	//------------------------------------------------
	public static boolean ENABLE_REBIRTH_SYSTEM;
	public static int REBIRTH_REWARD_ID;
	public static int REBIRTH_REWARD_COUNT;
	public static int REBIRTH_PRICE_ID;
	public static int REBIRTH_PRICE_COUNT;
	public static int REBIRTH_MIN_SKILL_ID;
	public static int REBIRTH_MAX_SKILL_ID;
	public static int REBIRTH_DELEVEL;
	public static int REBIRTH_LEVEL_MIN;
	public static int REBIRTH_COUNT_ID;
	public static int REBIRTH_COUNT;
	
	//------------------------------------------------
	//  Aio - System
	//------------------------------------------------
	public static boolean ENABLE_AIO_SYSTEM;
	public static Map<Integer, Integer> AIO_SKILLS;
	/** True = al activar AIO antepone AioNamePrefix al nick. */
	public static boolean CHANGE_AIO_NAME;
	/** Prefijo de nick AIO. Ej: "[ AIO ]" */
	public static String AIO_NAME_PREFIX;
	public static boolean ALLOW_AIO_NCOLOR;
	public static int AIO_NCOLOR;
	public static boolean ALLOW_AIO_TCOLOR;
	public static int AIO_TCOLOR;
	public static boolean ALLOW_AIO_TELEPORT;
	public static boolean ENABLE_AIO_FREE_TELEPORT;
	public static boolean ALLOW_AIO_EQUIP_ITEM;
	public static boolean ALLOW_AIO_IN_EVENTS;
	public static List<int[]> AIO_ITEMS = new ArrayList<>();
	/** itemId -> days. Aio_Coin1/2/3 + Aio_1/2/3 en Aio.properties. */
	public static Map<Integer, Integer> AIO_COIN_DAYS = new HashMap<>();
	public static boolean ENABLE_AIO_COMMAND;
	public static String AIO_HTML_FILE;
	public static String AIO_DATE_FORMAT;
	
    //---------------------------------------------------
	// VOTE SYSTEM
	//---------------------------------------------------
	public static boolean ENABLE_VOTE_SYSTEM;
	public static boolean ENABLE_INDIVIDUAL_VOTE;
	public static boolean ENABLE_GLOBAL_VOTE;
	public static int NEXT_TIME_TO_AUTO_UPDATE_TOTAL_VOTE;
	public static int NEXT_TIME_TO_AUTO_UPDATE_INDIVIDUAL_VOTES;
	public static int NEXT_TIME_TO_AUTO_CLEAN_INECESARY_VOTES;
	public static int NEXT_TIME_TO_CHECK_AUTO_GLOBAL_VOTES_REWARD;
	public static int INTERVAL_TO_NEXT_VOTE;
	public static int GLOBAL_VOTES_AMOUNT_TO_NEXT_REWARD;
	public static boolean ENABLE_VOTING_COMMAND; 
	public static String VOTING_COMMAND;
	public static String VOTE_LINK_TGS;
	public static String TGS_API_KEY;
	public static String VOTE_LINK_TOP_CO;
	public static String TOP_CO_SRV_ID;
	public static String VOTE_LINK_ITOPZ;
	public static String ITOPZ_API_KEY;
	public static String ITOPZ_SRV_ID;
	public static String VOTE_LINK_VTS;
	public static String VTS_API_KEY;
	public static String VTS_SID;
	public static String VOTE_LINK_HZ;
	public static String HZ_API_KEY;
	public static String VOTE_NETWORK_LINK;
	public static String VOTE_NETWORK_USER_NAME;
	public static String VOTE_NETWORK_API_KEY;
	public static String VOTE_LINK_TSS;
	public static String TSS_API_TOKEN;
    public static String TS_SRV_ID;
    public static String TS_DOMAIN_NAME;
	public static String BRASIL_VOTE_LINK;
	public static String BRASIL_USER_NAME;
	public static String VOTE_LINK_MMOTOP;
	public static String MMOTOP_API_KEY;
	public static String VOTE_LINK_TZ;
	public static String TZ_API_KEY;
	public static String VOTE_LINK_SERVERS;
	public static String SERVERS_HASH_CODE;
	public static String SERVERS_SRV_ID;
    public static String TEST_IP;

	// --------------------------------------------------
	// Donation Manager
	// --------------------------------------------------
	public static int ITEMSKIN;
	public static int ITEMSEXO;
	public static int CANTIDADSKIN;
	public static int CANTIDADSEXO;
	public static int ITEMHERO;
	public static int CANTIDADHERO;
	public static int ITEMNOBLES;
	public static int CANTIDADNOBLES;
	public static int ITEMAIO;
	public static int CANTIDADAIO;
	public static int ITEMVIP;
	public static int CANTIDADVIP;
	public static int ITEMVIP1;
	public static int CANTIDADVIP1;	
	public static int ITEMVIP2;
	public static int CANTIDADVIP2;	
	
	public static int VIP_DIAS;
	public static int VIP_DIAS1;
	public static int VIP_DIAS2;
	
	public static int ITEMCLANREP;
	public static int CANTIDADCLANREP;	
	public static int ITEMCLANLVL;
	public static int CANTIDADCLANLVL;	
	public static int ITEMARGU;
	public static int CANTIDADARGU;	
	/** Donate Shop: Socket Perfecto y Elemento Perfecto (donationmanager.properties) */
	public static int DONATE_ALCHEMY_ITEM;
	public static int DONATE_ALCHEMY_COUNT;
	public static int DONATE_ELEMENT_ITEM;
	public static int DONATE_ELEMENT_COUNT;
	/** Donate Shop: Skill Enchant al maximo (donationmanager.properties) */
	public static int DONATE_SKILL_ENCHANT_ITEM;
	/** NPC Custom Level (Subclass.properties) */
	public static int CUSTOM_LEVEL_TARGET;
	public static int CUSTOM_LEVEL_ADENA;
	public static int CUSTOM_LEVEL_COIN_ID;
	public static int CUSTOM_LEVEL_COIN_COUNT;
	public static int DONATE_SKILL_ENCHANT_COUNT;
	public static int ITEMCLANSKILL;
	public static int CANTIDADCLANSKILL;
	public static int ITEMNAME;
	public static int CANTIDADNAME;
	public static int ITEMNAMECOLOR;
	public static int CANTIDADNAMECOLOR;
	public static int ITEMTITLECOLOR;
	public static int CANTIDADTITLECOLOR;
	public static int CANTIDAD_VIP_1D;
	public static int CANTIDAD_VIP_30D;
	public static int CANTIDAD_VIP_90D;
	public static int CANTIDAD_HERO_1D;
	public static int CANTIDAD_HERO_30D;
	public static int CANTIDAD_HERO_90D;
	public static int CANTIDAD_AIO_1D;
	public static int CANTIDAD_AIO_30D;
	public static int CANTIDAD_AIO_90D;
	public static int DIAS_VIP_1D;
	public static int DIAS_VIP_30D;
	public static int DIAS_VIP_90D;
	public static int DIAS_HERO_1D;
	public static int DIAS_HERO_30D;
	public static int DIAS_HERO_90D;
	public static int DIAS_AIO_1D;
	public static int DIAS_AIO_30D;
	public static int DIAS_AIO_90D;
	
	/** COMBO: VIP + Hero + Nobless en una sola compra (DonationManager.properties) */
	public static boolean COMBO_ENABLED;
	public static int CANTIDAD_COMBO_30D;
	public static int DIAS_COMBO_30D;
	public static boolean COMBO_GIVE_VIP;
	public static boolean COMBO_GIVE_HERO;
	public static boolean COMBO_GIVE_NOBLESS;
	public static boolean STARTER_VIP_ENABLED;
	public static int STARTER_VIP_HOURS;
	public static boolean GB_STATUS_ENABLED;
	public static int ITEM_HERO_GB;
	public static int CANTIDAD_HERO_GB;
	public static int DIAS_HERO_GB;
	public static int ITEM_AIO_GB;
	public static int CANTIDAD_AIO_GB;
	public static int DIAS_AIO_GB;
	
	// --------------------------------------------------
	// Captcha settings
	// --------------------------------------------------
	/** Bot Prevention System */
	public static boolean BPS_BOTS_PREVENTION;
	public static boolean BPS_ALLOW_INCORRECT_CHOICE;
	public static int BPS_KILLS_COUNTER;
	public static int BPS_KILLS_COUNTER_RANDOMIZATION;
	public static int BPS_VALIDATION_TIME;
	public static int BPS_PUNISHMENT;
	public static int BPS_PUNISHMENT_TIME;
	public static boolean BPS_CHECK_BISHOPS;
	public static boolean BPS_CHECK_ELDERS;
	public static boolean BPS_CHECK_PROPHETS;
	public static boolean BPS_CHECK_BLADEDANCERS;
	public static boolean BPS_CHECK_SWORDSINGERS;
	public static boolean BPS_CHECK_WARCRYERS;
	public static boolean BPS_CHECK_OVERLORDS;
	public static boolean BPS_ENABLE_REWARD;
	public static int BPS_REWARD_ID;
	public static int BPS_REWARD_AMOUNT;
	
	/** multibox */
	public static boolean MULTIBOX_PROTECTION_ENABLED;
	public static int MULTIBOX_PROTECTION_CLIENTS_PER_PC;
	public static int MULTIBOX_PROTECTION_PUNISH;
	public static int TIME_MULTIBOX;	
	
	// --------------------------------------------------
	// Vip settings
	// --------------------------------------------------
	
	public static double VIP_RATE_XP;
	public static double VIP_RATE_SP;
	public static double VIP_ADENA_RATES;
	public static double VIP_SPOIL_RATES;
	public static double VIP_DROP_RATES;
	public static double VIP_RATE_DROP_ITEMS_BY_RAID;
	/** Enchant success chance multiplier. 1.0 = nothing, 1.5 = x1.5. */
	public static double VIP_ENCHANT_RATES;
	/** Augment skill chance multiplier (not glow). 1.0 = nothing, 1.5 = x1.5. */
	public static double VIP_AUGMENT_SKILL_RATES;
	public static boolean ENABLE_VIP_CHAT_PREFIX;
	public static String VIP_CHAT_PREFIX;
	public static boolean VIP_CHAT_GLOBAL;
	public static boolean ENABLE_VIP_COMMAND;
	public static String VIP_HTML_FILE;
	public static String VIP_DATE_FORMAT;
	/** Color de title mientras VIP este activo (como AIO). */
	public static boolean ALLOW_VIP_TCOLOR;
	public static int VIP_TCOLOR;
	/** itemId -> days. Vip_Coin1/2/3 + Vip_1/2/3 en Vip.properties. */
	public static Map<Integer, Integer> VIP_COIN_DAYS = new HashMap<>();
	
	// --------------------------------------------------
	// Auto Spoil (inventory item, same model as premium runes)
	// --------------------------------------------------
	public static boolean ENABLE_AUTOSPOIL_SYSTEM;
	/** Item ID that enables Auto Spoil while in inventory. */
	public static int AUTOSPOIL_ITEM_ID;
	
	// --------------------------------------------------
	// Hero settings (custom / coins)
	// --------------------------------------------------
	public static boolean ENABLE_HERO_COMMAND;
	public static String HERO_HTML_FILE;
	public static String HERO_DATE_FORMAT;
	/** itemId -> days. Hero_Coin1/2/3 + Hero_1/2/3 en Hero.properties. */
	public static Map<Integer, Integer> HERO_COIN_DAYS = new HashMap<>();
	
	// --------------------------------------------------
	// Solo boss / reward solo events
	// --------------------------------------------------
	public static String[] SOLO_BOSS_EVENT_INTERVAL_BY_TIME_OF_DAY;
	public static boolean SCREN_MSG_BOSS_SOLO;
	public static boolean SOLO_BOSS_EVENT;
	public static int SOLO_BOSS_ID_ONE;
	public static int SOLO_BOSS_ID_TWO;
	public static int SOLO_BOSS_ID_THREE;
	public static int SOLO_BOSS_ID_FOUR;
	public static int SOLO_BOSS_ID_FIVE;
	public static int RANGE_SOLO_BOSS;
	public static List<RewardHolder> SOLO_BOSS_REWARDS_ONE = new ArrayList<>();
	public static List<RewardHolder> SOLO_BOSS_REWARDS_TWO = new ArrayList<>();
	public static List<RewardHolder> SOLO_BOSS_REWARDS_THREE = new ArrayList<>();
	public static List<RewardHolder> SOLO_BOSS_REWARDS_FOUR = new ArrayList<>();
	public static List<RewardHolder> SOLO_BOSS_REWARDS_FIVE = new ArrayList<>();
	public static int[] SOLO_BOSS_ID_ONE_LOC = new int[3];
	public static int[] SOLO_BOSS_ID_TWO_LOC = new int[3];
	public static int[] SOLO_BOSS_ID_THREE_LOC = new int[3];
	public static int[] SOLO_BOSS_ID_FOUR_LOC = new int[3];
	public static int[] SOLO_BOSS_ID_FIVE_LOC = new int[3];
	public static int EVENT_SOLOBOSS_TIME;
	public static int EVENT_SOLO_FARM_TIME;
	public static String[] EVENT_SOLO_FARM_INTERVAL_BY_TIME_OF_DAY;
	public static boolean SOLO_FARM_BY_TIME_OF_DAY;
	public static boolean SOLO_MESSAGE_ENABLED;
	public static String SOLO_FARM_MESSAGE_TEXT;
	public static List<Integer> LIST_NPC_CUSTOM_MONSTER = new ArrayList<>();
	/** Happy Hour: drop rate multiplier for List_Monster while event is active (1.0 = no boost). */
	public static double SOLO_EVENT_DROP_RATE_MULTIPLIER;
	
	//----------------------------------------
	//   Tournament
	//----------------------------------------
	public static boolean TOUR_GRADE_A_1X1;
	public static int ALT_TOUR_ENCHANT_LIMIT;	
	public static boolean TOURNAMENT_EVENT_START;
	public static boolean TOURNAMENT_EVENT_TIME;
	public static boolean TOURNAMENT_EVENT_SUMMON;
	public static int TOURNAMENT_TIME;
	public static String[] TOURNAMENT_EVENT_INTERVAL_BY_TIME_OF_DAY;	
	public static int TOUR_AURA_TEAM1;
	public static int TOUR_AURA_TEAM2;
	public static String TITLE_COLOR_TEAM1;
	public static String TITLE_COLOR_TEAM2;	
	public static String MSG_TEAM1;
	public static String MSG_TEAM2;	
	public static boolean Allow_Same_HWID_On_Tournament;
	public static int ARENA_NPC;
	public static int NPC_locx;
	public static int NPC_locy;
	public static int NPC_locz;
	public static int NPC_Heading;
	public static int NPC_locx2;
	public static int NPC_locy2;
	public static int NPC_locz2;
	public static int NPC_Heading2;
	public static int Tournament_locx;
	public static int Tournament_locy;
	public static int Tournament_locz;
	public static boolean ALLOW_1X1_REGISTER;
	public static boolean ARENA_MESSAGE_ENABLED;
	public static String ARENA_MESSAGE_TEXT;
	public static int ARENA_MESSAGE_TIME;
	public static int ARENA_EVENT_COUNT_1X1;
	public static int[][] ARENA_EVENT_LOCS_1X1;
	public static int ARENA_PVP_AMOUNT;
	public static int ARENA_REWARD_ID_1X1;
	public static int ARENA_WIN_REWARD_COUNT_1X1;
	public static int ARENA_VIP_WIN_REWARD_COUNT_1X1;
	public static int ARENA_LOST_REWARD_COUNT_1X1;
	public static int ARENA_CHECK_INTERVAL_1X1;
	public static int ARENA_CALL_INTERVAL_1X1;
	public static int ARENA_WAIT_INTERVAL_1X1;
	public static String TOURNAMENT_ID_RESTRICT;
	public static List<Integer> TOURNAMENT_LISTID_RESTRICT;
	public static boolean ARENA_SKILL_PROTECT;
	public static List<Integer> ARENA_SKILL_LIST = new ArrayList<>();
	public static List<Integer> ARENA_DISABLE_SKILL_LIST = new ArrayList<>();
	public static List<Integer> ARENA_STOP_SKILL_LIST = new ArrayList<>();
	public static List<Integer> ARENA_DISABLE_SKILL_LIST_PERM = new ArrayList<>();
	public static boolean ARENA_PROTECT;
	public static String NAME_TOUR;
	
	//----------------------------------------
	//   Custom Olympiad
	//----------------------------------------
	public static boolean CUSTOM_OLY_ENABLED;
	public static String CUSTOM_OLY_NAME;
	public static int CUSTOM_OLY_PERIOD_DAYS;
	public static int CUSTOM_OLY_VALIDATION_DAYS;
	public static String CUSTOM_OLY_HERO_GIVE_HOUR;
	public static int CUSTOM_OLY_HERO_DAYS;
	public static boolean CUSTOM_OLY_AUTO_START;
	public static int CUSTOM_OLY_HERO_POINTS;
	public static int CUSTOM_OLY_HERO_SLOTS;
	public static int CUSTOM_OLY_RANK_SLOTS;
	public static int CUSTOM_OLY_RANK_MIN_POINTS;
	public static int CUSTOM_OLY_STEAL_POINTS;
	public static boolean CUSTOM_OLY_STEAL_PER_PLAYER;
	public static int CUSTOM_OLY_NPC_ID;
	public static String[] CUSTOM_OLY_SPAWN_TIMES;
	public static int CUSTOM_OLY_DURATION_MINUTES;
	public static int CUSTOM_OLY_NPC_X;
	public static int CUSTOM_OLY_NPC_Y;
	public static int CUSTOM_OLY_NPC_Z;
	public static int CUSTOM_OLY_NPC_HEADING;
	public static int[][] CUSTOM_OLY_ARENAS;
	public static int[] CUSTOM_OLY_TEAM1_SPAWN;
	public static int[] CUSTOM_OLY_TEAM2_SPAWN;
	public static int CUSTOM_OLY_SPAWN_SPREAD;
	public static int CUSTOM_OLY_WAIT_SECONDS;
	public static int CUSTOM_OLY_CHECK_MS;
	public static int CUSTOM_OLY_CALL_SECONDS;
	public static int CUSTOM_OLY_FIGHT_TIMEOUT;
	public static int CUSTOM_OLY_ENCHANT_CAP;
	public static boolean CUSTOM_OLY_ALLOW_POTIONS;
	public static boolean CUSTOM_OLY_ALLOW_SAME_HWID;
	public static boolean CUSTOM_OLY_USE_INSTANCE;
	public static boolean CUSTOM_OLY_ALLOW_1X1;
	public static boolean CUSTOM_OLY_ALLOW_2X2;
	public static boolean CUSTOM_OLY_ALLOW_3X3;
	public static List<Integer> CUSTOM_OLY_ALLOW_ITEMS = new ArrayList<>();
	public static List<Integer> CUSTOM_OLY_DENY_ITEMS = new ArrayList<>();
	public static Map<Integer, Integer> CUSTOM_OLY_FIGHTER_BUFFS = new HashMap<>();
	public static Map<Integer, Integer> CUSTOM_OLY_MAGE_BUFFS = new HashMap<>();
	
	//----------------------------------------
	//   Medal Collecting Event
	//----------------------------------------
	public static boolean MEDAL_EVENT_ENABLED;
	public static int MEDAL_EVENT_MEDAL_ID;
	public static int MEDAL_EVENT_GLITTERING_ID;
	public static double MEDAL_EVENT_MEDAL_CHANCE;
	public static double MEDAL_EVENT_GLITTERING_CHANCE;
	public static int MEDAL_EVENT_MIN_NPC_LEVEL;
	public static boolean MEDAL_EVENT_ALLOW_RAID;
	public static boolean MEDAL_EVENT_ALLOW_MINIONS;
	public static boolean MEDAL_EVENT_DROP_CUSTOM_MOBS;
	public static int[] MEDAL_EVENT_RANK_COST;
	public static int MEDAL_EVENT_TOSS_CHANCE;
	public static List<int[]> MEDAL_EVENT_PRIZES = new ArrayList<>();
	
	//----------------------------------------
	//   Buffer profiles (aCis 409)
	//----------------------------------------
	public static int BUFFER_MAX_SCHEMES;
	public static int BUFFER_MAX_SKILLS;
	public static String BUFFER_BUFFS;
	public static Map<Integer, BuffSkillHolder> BUFFER_BUFFLIST;
	public static List<Integer> FIGHTER_SKILL_LIST;
	public static List<Integer> MAGE_SKILL_LIST;
	public static List<Integer> SUMMONER_SKILL_LIST;
	public static boolean RESTRICT_USE_BUFFER_ON_PVPFLAG;
	public static boolean RESTRICT_USE_BUFFER_IN_COMBAT;
	
	// --------------------------------------------------
	// Mods settings
	// --------------------------------------------------

	/** Infinity SS and Arrows */
	public static boolean INFINITY_SS;
	public static boolean INFINITY_ARROWS;
	
	/** Unstuck Time */
	public static int UNSTUCK_TIME;

    public static boolean ENABLE_TIME_ZONE_SYSTEM;
    /** True = auto rotate by intervalMinutes. False = only //next_zone */
    public static boolean TIME_FARM_AUTO_ROTATE;
    
    
	/** Buffer */
	public static String FIGHTER_SET;
	public static int[] FIGHTER_SET_LIST;
	public static String MAGE_SET;
	public static int[] MAGE_SET_LIST;
	
	public static boolean ANNOUNCE_MULTISELL;
	public static List<Integer> ANNOUNCE_MULTISELL_LIST_IDS = new ArrayList<>();
	
	/** NPC visual aura - list 1 (functional NPCs by id) */
	public static boolean ENABLE_NPC_VISUAL_AURA;
	public static int NPC_VISUAL_AURA_COLOR;
	public static List<Integer> NPC_VISUAL_AURA_IDS = new ArrayList<>();
	
	/** Mob visual aura - list 2 blue / list 3 red (by mob id) */
	public static boolean ENABLE_MOB_VISUAL_AURA;
	public static List<Integer> MOB_VISUAL_AURA_BLUE_IDS = new ArrayList<>();
	public static List<Integer> MOB_VISUAL_AURA_RED_IDS = new ArrayList<>();
	
	/** Baking System */
	public static boolean ALLOW_BAKING_SYSTEM;
	public static int BANKING_SYSTEM_GOLDBARS;
	public static int BANKING_SYSTEM_GOLDBAR_ID;
	public static int BANKING_SYSTEM_ADENA;
	/** Auto Adena-Coin (menu) interval in seconds. */
	public static int AUTO_GOLDBAR_INTERVAL;
	
	public static boolean MENU_PANEL;
	
	/** Sell Buffs System */
	public static boolean SELLBUFF_ENABLED;
	public static int SELLBUFF_MP_MULTIPLER;
	public static int SELLBUFF_PAYMENT_ID;
	public static int SELLBUFF_MIN_PRICE;
	public static int SELLBUFF_MAX_PRICE;
	public static int SELLBUFF_MAX_BUFFS;
	
	public static boolean ENABLE_ONLINE;
	public static int PLAYERS_ONLINE_TRICK;

	public static int PVP_COLOR_ANNOUNCE;
	/** PvP title color by kills (used by Player.colorsCheck). */
	public static boolean ALLOW_PVP_TITLE_COLOR_SYSTEM = false;
	public static Map<Integer, Integer> PVP_COLORS = new HashMap<>();
	/** Tournament 5x5 resurrect gate (0 = blocked). */
	public static int bs_COUNT_5X5 = 0;
	
	public static boolean RESTART_BY_TIME_OF_DAY;
	public static int RESTART_SECONDS;
	public static String[] RESTART_INTERVAL_BY_TIME_OF_DAY;
	public static int TPAIN_RANGE;
	/** Transfer Pain en los dos sentidos: si le pegan al servitor, parte del dano lo recibe el dueno. */
	public static boolean TPAIN_REVERSE;
	/** Maximo % del dano del servitor que puede recibir el dueno. */
	public static int TPAIN_REVERSE_MAX_PERCENT;
	
	// --------------------------------------------------
	// Url settings
	// --------------------------------------------------

	public static boolean OPEN_URL_ENABLE;
	public static String OPEN_URL_SITE;
	public static String OPEN_URL_COLOR;
	public static String OPEN_URL_FACE;
	public static String OPEN_URL_WEB;
	public static String OPEN_URL_DISCORD;
	public static String OPEN_URL_GRUPO;
	public static String OPEN_URL_STREAMER1;
	public static String OPEN_URL_STREAMER2;
	public static String OPEN_URL_STREAMER3;
	public static String OPEN_URL_STREAMER4;
	public static String OPEN_URL_STREAMER5;
	
	// --------------------------------------------------
	// Offline
	// --------------------------------------------------
		
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
	public static boolean OFFLINE_MODE_NO_DAMAGE;
	public static boolean RESTORE_OFFLINERS;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	public static boolean OFFLINE_SET_NAME_COLOR;
	public static int OFFLINE_NAME_COLOR;
	
 	// --------------------------------------------------
	// dressme settings
	// --------------------------------------------------
	
	public static boolean ALLOW_DRESS_ME_SYSTEM;
	public static Map<String, Integer> DRESS_ME_CHESTS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_LEGS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_BOOTS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_GLOVES = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_WEAPONS = new HashMap<>();
	public static Map<String, Integer> DRESS_ME_DHAIR = new HashMap<>();
	public static String SKIN_NAME1;
	public static String SKIN_NAME2;
	public static String SKIN_NAME3;
	public static String SKIN_NAME4;
	public static String SKIN_NAME5;
	public static String SKIN_NAME6;
	public static String SKIN_NAME7;
	public static String SKIN_NAME8;
	public static String SKIN_NAME9;
	public static String SKIN_NAME10;
	public static String SKIN_NAME11;
	public static String SKIN_NAME12;
	public static String SKIN_NAME13;
	public static String SKIN_NAME14;
	public static String SKIN_NAME15;
	public static String SKIN_NAME16;
	public static String SKIN_NAME17;
	public static String SKIN_NAME18;
	public static String SKIN_NAME19;
	public static String SKIN_NAME20;
	public static String SKIN_NAME21;
	public static String SKIN_NAME22;
	public static String SKIN_NAME23;
	public static String SKIN_NAME24;
	public static String SKIN_NAME25;
	public static String SKIN_NAME26;
	public static String SKIN_NAME27;
	public static String SKIN_NAME28;
	public static String SKIN_NAME29;
	public static String SKIN_NAME30;
	public static int SKIN_ITEM_1;
	public static int SKIN_ITEM_2;
	public static int SKIN_ITEM_3;
	public static int SKIN_ITEM_4;
	public static int SKIN_ITEM_5;
	public static int SKIN_ITEM_6;
	public static int SKIN_ITEM_7;
	public static int SKIN_ITEM_8;
	public static int SKIN_ITEM_9;
	public static int SKIN_ITEM_10;
	public static int SKIN_ITEM_11;
	public static int SKIN_ITEM_12;
	public static int SKIN_ITEM_13;
	public static int SKIN_ITEM_14;
	public static int SKIN_ITEM_15;
	public static int SKIN_ITEM_16;
	public static int SKIN_ITEM_17;
	public static int SKIN_ITEM_18;
	public static int SKIN_ITEM_19;
	public static int SKIN_ITEM_20;
	public static int SKIN_ITEM_21;
	public static int SKIN_ITEM_22;
	public static int SKIN_ITEM_23;
	public static int SKIN_ITEM_24;
	public static int SKIN_ITEM_25;
	public static int SKIN_ITEM_26;
	public static int SKIN_ITEM_27;
	public static int SKIN_ITEM_28;
	public static int SKIN_ITEM_29;
	public static int SKIN_ITEM_30;
	
 	// --------------------------------------------------
	// Sub settings
	// --------------------------------------------------
	
	public static boolean SUB_ACU;
	public static boolean SUB_OL_WAR;
	public static boolean SUB_ELFS;
	
	// --------------------------------------------------
	// MultiSkill settings
	// --------------------------------------------------
	public static boolean MULTISKILL_ENABLED;
	public static double MULTISKILL_SP_MULTIPLIER;
	
	// --------------------------------------------------
	// Skill Enchant (custom NPC: retail + safe)
	// --------------------------------------------------
	public static boolean SKILL_ENCHANT_CUSTOM_ENABLED;
	public static int SKILL_ENCHANT_NPC_ID;
	/** Max enchant step (+1..+30). Retail routes are 101-130 / 141-170. */
	public static int SKILL_ENCHANT_MAX;
	/** -1 = use NormalChance76-80. 0-100 = same % for every skill on NPC 60003. */
	public static int SKILL_ENCHANT_NORMAL_CHANCE;
	/** Per player level 76-80 when NormalChance = -1. -1 = XML. */
	public static int[] SKILL_ENCHANT_NORMAL_BY_LEVEL = new int[5];
	public static int SKILL_ENCHANT_SAFE_CHANCE;
	public static int SKILL_ENCHANT_SAFE_ITEM_ID;
	public static int SKILL_ENCHANT_SAFE_ITEM_COUNT;
	public static boolean SKILL_ENCHANT_FAIL_RESET;
	
	// --------------------------------------------------
	// Battle Pass (free + VIP tracks)
	// --------------------------------------------------
	public static boolean BATTLEPASS_ENABLED;
	public static int BATTLEPASS_MAX_LEVEL;
	public static boolean BATTLEPASS_VIP_ENABLED;
	public static int BATTLEPASS_VIP_MAX_LEVEL;
	
	public static int SUB_COUNT;
	/** Nivel al crear una subclass. Retail 40. */
	public static int BASE_SUBCLASS_LEVEL;
	/** Primer nivel inalcanzable de subclass. 81 = max jugable 80 @ 100%. */
	public static int MAX_SUBCLASS_LEVEL;
	/** Primer nivel inalcanzable de MAIN. 81 = max jugable 80 @ 100%. 100 = 99 @ 100%. */
	public static int MAX_PLAYER_LEVEL;
	
	public static boolean ALT_GAME_SUBCLASS_EVERYWHERE;
	
	/** Anuncios al agregar sub en Village Master. */
	public static boolean VILLAGE_SUB_ANNOUNCE;
	/** Costo al cancelar/cambiar sub en Village Master (0 = gratis). */
	public static int VILLAGE_CANCEL_SUB_ITEM_ID;
	public static int VILLAGE_CANCEL_SUB_COUNT;
	
	/** Textos anuncio 1ª/2ª/3ª sub (Village Master). */
	public static String CUSTOM_SUB_ANNOUNCE_TEXT;
	public static String CUSTOM_SUB_ANNOUNCE_TEXT_1;
	public static String CUSTOM_SUB_ANNOUNCE_TEXT_2;
	public static String CUSTOM_SUB_ANNOUNCE_TEXT_3;
	
	public static boolean CHECK_SKILL_LIST;
	
	// --------------------------------------------------
	// Balance settings
	// --------------------------------------------------
	public static boolean ENABLE_MODIFY_DEBUFF_DURATION;
	public static float DEBUFF_TIME;
	public static List<Integer> DEBUFF_TIME_LIST = new ArrayList<>();
	
	public static int MAX_CANCEL_COUNT;
	
	public static boolean RETURN_CANCEL_BUFF;
	public static int RETURN_CANCEL_TIME;
	
	public static float MCRIT_POWER;
	/** Physical crit damage base multiplier (retail 2.0). Used as calcStat(CRITICAL_DAMAGE) init, same role as MCritPower. */
	public static float PCRIT_POWER;
	public static boolean RANDOM_DAMAGE;
	public static boolean ENABLE_CUSTOM_CRIT_POWER;
	public static float MAGIC_CRITICAL_POWER;
	public static float FIGHTER_CRITICAL_POWER;
	public static float GHOSTSENTINEL_CRITICAL_POWER;
	public static boolean ENABLE_CUSTOM_CRIT;
	public static int MAX_PCRIT_RATE;
	public static int PCRIT_RATE_ArcherHuman;
	public static int PCRIT_RATE_ArcherElfo;
	public static int PCRIT_RATE_ArcherDarkElfo;
	public static int MAX_MCRIT_RATE;
	public static int MCRIT_RATE_Archmage;
	public static int MCRIT_RATE_Soultaker;
	public static int MCRIT_RATE_Mystic_Muse;
	public static int MCRIT_RATE_Storm_Screamer;
	public static int MCRIT_RATE_Dominator;
	public static int MAX_MATK_SPEED;
	public static int MAX_PATK_SPEED;
	/** Tope de velocidad de corrida para players (0 = sin tope). */
	public static int MAX_RUN_SPEED;
	public static int MAX_PATK_SPEED_GHOST;
	public static int MAX_PATK_SPEED_MOONL;
	/** Minimum ms between autoattacks (0 = disabled). Russian-style MaxAtkSpdDelay. */
	public static int MIN_ATKSPD_DELAY;
	/** Tope de Cast Spd SOLO para el tiempo de casteo de skills magicos (no se ve en Alt+T). 0 = sin tope. */
	public static int SKILL_MCAST_SPD_CAP;
	/** Tope de Atk Spd SOLO para el tiempo de casteo de skills fisicos (no se ve en Alt+T). 0 = sin tope. */
	public static int SKILL_PCAST_SPD_CAP;
	public static float ALT_ATTACK_DELAY;
	public static float ALT_CAST_DELAY;
	/** Physics Config */
	public static int BLOW_ATTACK_FRONT;
	public static int BLOW_ATTACK_SIDE;
	public static int BLOW_ATTACK_BEHIND;
	public static int BACKSTAB_ATTACK_FRONT;
	public static int BACKSTAB_ATTACK_SIDE;
	public static int BACKSTAB_ATTACK_BEHIND;
	/** Flat damage multiplier for dagger (BLOW) skills, on top of the normal P.Atk + power calc. Tunable without recompiling. */
	public static float DAGGER_BLOW_MULTIPLIER;

	// --------------------------------------------------
	// Balance PvE (jugadores contra NPC) y defensa elemental de NPC
	// --------------------------------------------------
	/** Multiplicador del dano FISICO de jugadores contra NPC (1.0 = sin cambio). */
	public static float PVE_PHYSICAL_DAMAGE_MULTIPLIER;
	/** Multiplicador del dano MAGICO de jugadores contra NPC (1.0 = sin cambio). */
	public static float PVE_MAGIC_DAMAGE_MULTIPLIER;
	/** Suma fija (puntos) a la defensa elemental de todos los NPC. 0 = como H5. */
	public static int NPC_ELEMENT_DEFENSE_BONUS;
	/** Multiplicador sobre la defensa elemental de cada NPC (1.0 = como H5). */
	public static float NPC_ELEMENT_DEFENSE_RATE;
	/** Si la defensa elemental de los NPC se clampea a un piso (NpcElementDefenseMinEnabled). */
	public static boolean NPC_ELEMENT_DEFENSE_MIN_ENABLED;
	/** Piso de defensa elemental de un NPC (solo si el anterior esta en true). */
	public static int NPC_ELEMENT_DEFENSE_MIN;
	/** Si esta en true, //dmg muestra el desglose del ultimo golpe contra NPC (solo GM). */
	public static boolean PVE_DAMAGE_DEBUG;
	/** Cap por golpe contra NPC: maximo % de la vida del target (0 = sin cap). */
	public static double PVE_MAX_HIT_PERCENT_OF_HP;

	public static boolean ENABLE_ALTERNATIVE_SKILL_DURATION;
	public static HashMap<Integer, Integer> SKILL_DURATION_LIST;
	
	// --------------------------------------------------
	// Class vs class damage balance
	// --------------------------------------------------
	public static boolean ENABLE_CLASS_DAMAGE_BALANCE;
	
	// --------------------------------------------------
	// Skills chance multiplier settings
	// --------------------------------------------------
	public static boolean ENABLE_CUSTOM_CHANCE_SKILL;
	public static float SURRENDER_TO_FIRE;
	public static float VORTEX_TO_FIRE;
	public static float SURRENDER_TO_WIND;
	public static float WIND_VORTEX;
	public static float CURSE_GLOOM;
	public static float DARK_VORTEX;
	public static float SURRENDER_TO_WATER;
	public static float ICE_VORTEX;
	public static float LIGHT_VORTEX;
	public static float SILENCE;
	public static float SLEEP;
	public static float CURSE_FEAR;
	public static float ANCHOR;
	public static float CURSE_OF_DOOM;
	public static float CURSE_OF_ABYSS;
	public static float CANCELLATION;
	public static float MASS_SLOW;
	public static float MASS_FEAR;
	public static float MASS_GLOOM;
	public static float SLEEPING_CLOUD;
	public static float HEROIC_GRANDEUR;
	public static float HEROIC_DREAD;
	public static float STUNNING_SHOT;
	public static float HEX;
	public static float SHOCK_STOMP;
	public static float THUNDER_STORM;
	public static float SHIELD_STUN;
	public static float SHIELD_SLAM;
	public static float SHACKLE;
	public static float MASS_SHACKLING;
	public static float ARREST;
	public static float BLUFF;
	public static float SWITCH;
	public static float STUNNING_FIST;
	public static float FEAR_OVER;
	public static float SEAL_OF_SILENCE;
	public static float SEAL_OF_SUSPENSION;
	public static float STUN_ATTACK;
	public static float ARMOR_CRUSH;
	public static float SLOW;
	public static float SEAL_OF_DESPAIR;
	public static float DREAMING_SPIRIT;
	public static float SEAL_OF_BINDING;
	public static float MASS_WARRIOR_BANE;
	public static float MASS_MAGE_BANE;
	public static float SHIELD_BASH;
	public static float SHOCK_BLAST;
	
	// --------------------------------------------------
	// Enchants settings
	// --------------------------------------------------
	
	/** Enchant item custom */
	public static HashMap<Integer, Integer> NORMAL_WEAPON_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> BLESS_WEAPON_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> CRYSTAL_WEAPON_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> DONATOR_WEAPON_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> NORMAL_ARMOR_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> BLESS_ARMOR_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> CRYSTAL_ARMOR_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> DONATOR_ARMOR_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> NORMAL_JEWELRY_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> BLESS_JEWELRY_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> CRYSTAL_JEWELRY_ENCHANT_LEVEL = new HashMap<>();
	public static HashMap<Integer, Integer> DONATOR_JEWELRY_ENCHANT_LEVEL = new HashMap<>();
	public static boolean ENCHANT_HERO_WEAPON;
	public static boolean SCROLL_STACKABLE;
	public static int GOLD_WEAPON;
	public static int GOLD_ARMOR;
	public static int ENCHANT_SAFE_MAX;
	public static int ENCHANT_SAFE_MAX_FULL;
	public static int ENCHANT_WEAPON_MAX;
	public static int ENCHANT_ARMOR_MAX;
	public static int ENCHANT_JEWELRY_MAX;
	public static int BLESSED_ENCHANT_WEAPON_MAX;
	public static int BLESSED_ENCHANT_ARMOR_MAX;
	public static int BLESSED_ENCHANT_JEWELRY_MAX;
	public static int BREAK_ENCHANT;
	public static int CRYSTAL_ENCHANT_MIN;
	public static int CRYSTAL_ENCHANT_WEAPON_MAX;
	public static int CRYSTAL_ENCHANT_ARMOR_MAX;
	public static int CRYSTAL_ENCHANT_JEWELRY_MAX;
	public static int DONATOR_ENCHANT_MIN;
	public static int DONATOR_ENCHANT_WEAPON_MAX;
	public static int DONATOR_ENCHANT_ARMOR_MAX;
	public static int DONATOR_ENCHANT_JEWELRY_MAX;
	public static boolean DONATOR_DECREASE_ENCHANT;
	public static int DONATOR_BREAK_ENCHANT;
	public static boolean DONATOR_LOST_ENCHANT;
	public static boolean ENABLE_ENCHANT_ANNOUNCE;
	public static String ENCHANT_ANNOUNCE_LEVEL;
	public static ArrayList<Integer> LIST_ENCHANT_ANNOUNCE_LEVEL = new ArrayList<>();
	
	// --------------------------------------------------
	// Clans settings
	// --------------------------------------------------
	
	/** Clans */
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static int ALT_CLAN_WAR_PENALTY_WHEN_ENDED;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	
	/** Manor */
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_MAINTENANCE_MIN;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	
	/** Clan Hall function */
	public static long CH_TELE_FEE_RATIO;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_ITEM_FEE_RATIO;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static long CH_MPREG_FEE_RATIO;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static long CH_HPREG_FEE_RATIO;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static long CH_EXPREG_FEE_RATIO;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static long CH_SUPPORT_FEE_RATIO;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static long CH_FRONT_FEE_RATIO;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	
	// --------------------------------------------------
	// Events settings
	// --------------------------------------------------
	
	/** SevenSigns Festival */
	public static boolean ALT_GAME_CASTLE_DAWN;
	public static boolean ALT_GAME_CASTLE_DUSK;
	public static int ALT_FESTIVAL_MIN_PLAYER;
	public static int ALT_MAXIMUM_PLAYER_CONTRIB;
	public static long ALT_FESTIVAL_MANAGER_START;
	public static long ALT_FESTIVAL_LENGTH;
	public static long ALT_FESTIVAL_CYCLE_LENGTH;
	public static long ALT_FESTIVAL_FIRST_SPAWN;
	public static long ALT_FESTIVAL_FIRST_SWARM;
	public static long ALT_FESTIVAL_SECOND_SPAWN;
	public static long ALT_FESTIVAL_SECOND_SWARM;
	public static long ALT_FESTIVAL_CHEST_SPAWN;
	
	/** Four Sepulchers */
	public static int FS_TIME_ENTRY;
	public static int FS_TIME_END;
	public static int FS_PARTY_MEMBER_COUNT;
	/** If true: solo entry allowed, skip quest state + entrance pass (for testing). */
	public static boolean FS_FREE_ENTRY;
	
	/** dimensional rift */
	public static int RIFT_MIN_PARTY_SIZE;
	public static int RIFT_SPAWN_DELAY;
	public static int RIFT_MAX_JUMPS;
	public static int RIFT_AUTO_JUMPS_TIME_MIN;
	public static int RIFT_AUTO_JUMPS_TIME_MAX;
	public static int RIFT_ENTER_COST_RECRUIT;
	public static int RIFT_ENTER_COST_SOLDIER;
	public static int RIFT_ENTER_COST_OFFICER;
	public static int RIFT_ENTER_COST_CAPTAIN;
	public static int RIFT_ENTER_COST_COMMANDER;
	public static int RIFT_ENTER_COST_HERO;
	public static double RIFT_BOSS_ROOM_TIME_MUTIPLY;
	
	/** Wedding system */
	public static boolean ALLOW_WEDDING;
	public static int WEDDING_PRICE;
	public static boolean WEDDING_SAMESEX;
	public static boolean WEDDING_FORMALWEAR;
	
	/** Lottery */
	public static int ALT_LOTTERY_PRIZE;
	public static int ALT_LOTTERY_TICKET_PRICE;
	public static double ALT_LOTTERY_5_NUMBER_RATE;
	public static double ALT_LOTTERY_4_NUMBER_RATE;
	public static double ALT_LOTTERY_3_NUMBER_RATE;
	public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	
	/** Fishing tournament */
	public static boolean ALT_FISH_CHAMPIONSHIP_ENABLED;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_ITEM;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_1;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_2;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_3;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_4;
	public static int ALT_FISH_CHAMPIONSHIP_REWARD_5;
	
	// --------------------------------------------------
	// GeoEngine
	// --------------------------------------------------
	
	/** Geodata */
	public static String GEODATA_PATH;
	public static GeoType GEODATA_TYPE;
	
	/** Path checking */
	public static int PART_OF_CHARACTER_HEIGHT;
	public static int MAX_OBSTACLE_HEIGHT;
	
	/** Path finding */
	public static String PATHFIND_BUFFERS;
	public static int BASE_WEIGHT;
	public static int DIAGONAL_WEIGHT;
	public static int HEURISTIC_WEIGHT;
	public static int OBSTACLE_MULTIPLIER;
	public static int MAX_ITERATIONS;
	public static boolean DEBUG_PATH;
	public static boolean DEBUG_GEO_NODE;
	
	// --------------------------------------------------
	// HexID
	// --------------------------------------------------
	
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	
	// --------------------------------------------------
	// Loginserver
	// --------------------------------------------------
	
	public static String LOGIN_BIND_ADDRESS;
	public static int PORT_LOGIN;
	
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static boolean ACCEPT_NEW_GAMESERVER;
	
	public static boolean SHOW_LICENCE;
	
	public static boolean AUTO_CREATE_ACCOUNTS;
	
	public static boolean LOG_LOGIN_CONTROLLER;
	
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	
	// --------------------------------------------------
	// NPCs / Monsters
	// --------------------------------------------------
	
	/** Champion Mod */
	public static int CHAMPION_FREQUENCY;
	public static int CHAMP_MIN_LVL;
	public static int CHAMP_MAX_LVL;
	public static int CHAMPION_HP;
	public static int CHAMPION_REWARDS;
	public static int CHAMPION_ADENAS_REWARDS;
	public static double CHAMPION_HP_REGEN;
	public static double CHAMPION_ATK;
	public static double CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD;
	public static int CHAMPION_REWARD_ID;
	public static int CHAMPION_REWARD_QTY;
	public static boolean CHAMPION_AURA;
	public static int CHAMPION_AURA_COLOR;
	
	/** Misc */
	public static boolean ALLOW_CLASS_MASTERS;
	public static ClassMasterSettings CLASS_MASTER_SETTINGS;
	public static boolean ALLOW_ENTIRE_TREE;
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean SHOW_NPC_LVL;
	public static boolean SHOW_NPC_CREST;
	public static boolean SHOW_SUMMON_CREST;
	
	/** Wyvern Manager */
	public static boolean WYVERN_ALLOW_UPGRADER;
	public static int WYVERN_REQUIRED_LEVEL;
	public static int WYVERN_REQUIRED_CRYSTALS;

	/** AI */
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static int MAX_DRIFT_RANGE;
	/** Ms before a mob gives up combat if not refreshed (default 90000 = 1m30). */
	public static int MAX_ATTACK_TIMEOUT;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	
	/** On-kill nearby respawn (e.g. Magma Drake / Ashuras) */
	public static boolean KILL_SPAWN_ON_DEATH_ENABLED;
	public static int[] KILL_SPAWN_ON_DEATH_LIST = {};
	public static int[] KILL_SPAWN_NPC = {};
	public static int KILL_SPAWN_CANTIDAD;
	public static int KILL_SPAWN_CHANCE;
	public static int KILL_SPAWN_DELAY;
	public static int KILL_SPAWN_DESPAWN;
		
	// --------------------------------------------------
	// Players
	// --------------------------------------------------
	
	/** Misc */
	public static boolean EFFECT_CANCELING;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static double RESPAWN_RESTORE_HP;
	public static int MAX_PVTSTORE_SLOTS_DWARF;
	public static int MAX_PVTSTORE_SLOTS_OTHER;
	public static boolean DEEPBLUE_DROP_RULES;
	public static boolean ALT_GAME_DELEVEL;
	public static int DEATH_PENALTY_CHANCE;
	
	/** Inventory & WH */
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_QUEST_ITEMS;
	public static int INVENTORY_MAXIMUM_PET;
	public static int MAX_ITEM_IN_PACKET;
	public static double ALT_WEIGHT_LIMIT;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int FREIGHT_SLOTS;
	public static boolean ALT_GAME_FREIGHTS;
	public static int ALT_GAME_FREIGHT_PRICE;

	/** Augmentations */
	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;
	/** If true, augmented items can be P2P traded and used in private store sell/buy (still not droppable/sellable to NPC). */
	public static boolean ALLOW_TRADE_AUGMENTED_ITEMS;
	/** Adena cost when applying fast augment (plus 25 Gemstone C). 0 = no adena. */
	public static int FAST_AUGMENT_ADENA;
	/** Adena cost when removing augment via life stone. 0 = free. */
	public static int FAST_REMOVE_ADENA;
	
	/** Karma & PvP */
	public static boolean KARMA_PLAYER_CAN_BE_KILLED_IN_PZ;
	public static boolean KARMA_PLAYER_CAN_SHOP;
	public static boolean KARMA_PLAYER_CAN_USE_GK;
	public static boolean KARMA_PLAYER_CAN_TELEPORT;
	public static boolean KARMA_PLAYER_CAN_TRADE;
	public static boolean KARMA_PLAYER_CAN_USE_WH;
	
	public static boolean KARMA_DROP_GM;
	public static boolean KARMA_AWARD_PK_KILL;
	public static int KARMA_PK_LIMIT;
	
	public static String KARMA_NONDROPPABLE_PET_ITEMS;
	public static String KARMA_NONDROPPABLE_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_PET_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_ITEMS;
	
	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;
	
	/** Party */
	public static String PARTY_XP_CUTOFF_METHOD;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_RANGE;
	
	/** GMs & Admin Stuff */
	public static int DEFAULT_ACCESS_LEVEL;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_AUTO_LIST;
	
	/** petitions */
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	
	/** Crafting **/
	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_BLACKSMITH_USE_RECIPES;
	
	/** Skills & Classes **/
	public static boolean AUTO_LEARN_SKILLS;
	public static boolean MAGIC_FAILURES;
	public static int PERFECT_SHIELD_BLOCK_RATE;
	/** If false, classic chance-based shield block is disabled. */
	public static boolean SHIELD_BLOCK_ENABLED;
	/** If true, equipped shields always reduce physical damage. */
	public static boolean SHIELD_DAMAGE_REDUCE_ENABLED;
	/** Base physical damage reduce with a shield equipped (0.20 = 20%). */
	public static double SHIELD_BASE_DAMAGE_REDUCE;
	/** Cap for total shield damage reduce (base + skills). */
	public static double SHIELD_DAMAGE_REDUCE_MAX;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean SP_BOOK_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean SUBCLASS_WITHOUT_QUESTS;
	
	/** Buffs */
	public static boolean STORE_SKILL_COOLTIME;
	public static int MAX_BUFFS_AMOUNT;
	/** Estilo H5: slots aparte para songs/dances y para buffs que salen por chance (triggered). */
	public static int MAX_DANCE_AMOUNT;
	public static int MAX_TRIGGERED_BUFF_AMOUNT;
	/** Modo C4: los debuffs ocupan slots de buffs. */
	public static boolean DEBUFFS_TAKE_BUFF_SLOTS;
	
	// --------------------------------------------------
	// Sieges
	// --------------------------------------------------
	
	public static int SIEGE_LENGTH;
	public static int MINIMUM_CLAN_LEVEL;
	public static int MAX_ATTACKERS_NUMBER;
	public static int MAX_DEFENDERS_NUMBER;
	public static int ATTACKERS_RESPAWN_DELAY;
	public static IntIntHolder[] REWARD_WINNER_SIEGE_CLAN;
	public static IntIntHolder[] LEADER_REWARD_WINNER_SIEGE_CLAN;	
	
	// --------------------------------------------------
	// Server
	// --------------------------------------------------
	
	public static String GAMESERVER_HOSTNAME;
	public static int PORT_GAME;
	public static String HOSTNAME;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	
	/** Access to database */
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	
	/** serverList & Test */
	public static boolean SERVER_LIST_BRACKET;
	public static boolean SERVER_LIST_CLOCK;
	public static int SERVER_LIST_AGE;
	public static boolean SERVER_LIST_TESTSERVER;
	public static boolean SERVER_LIST_PVPSERVER;
	public static boolean SERVER_GMONLY;
	
	/** clients related */
	public static int DELETE_DAYS;
	public static int MAXIMUM_ONLINE_USERS;
	
	/** Auto-loot */
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_HERBS;
	public static boolean AUTO_LOOT_RAID;
	
	/** Items Management */
	public static boolean ALLOW_DISCARDITEM;
	public static boolean MULTIPLE_ITEM_DROP;
	public static int HERB_AUTO_DESTROY_TIME;
	public static int ITEM_AUTO_DESTROY_TIME;
	public static int EQUIPABLE_ITEM_AUTO_DESTROY_TIME;
	public static Map<Integer, Integer> SPECIAL_ITEM_DESTROY_TIME;
	public static int PLAYER_DROPPED_ITEM_MULTIPLIER;
	
	/** Allow types */
	public static boolean ALLOW_FREIGHT;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static boolean ALLOW_LOTTERY;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_BOAT;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean ALLOW_MANOR;
	public static boolean ENABLE_FALLING_DAMAGE;
	
	/** Debug & Dev */
	public static boolean ALT_DEV_NO_SPAWNS;
	public static boolean DEVELOPER;
	public static boolean PACKET_HANDLER_DEBUG;
	public static int DEBUG_MOVEMENT;
	
	/** Deadlock Detector */
	public static boolean DEADLOCK_DETECTOR;
	public static int DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	
	/** Logs */
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	public static boolean GMAUDIT;
	
	/** Community Board */
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String BBS_DEFAULT;
		
	/** ThreadPool */
	public static int SCHEDULED_THREAD_POOL_COUNT;
	public static int THREADS_PER_SCHEDULED_THREAD_POOL;
	public static int INSTANT_THREAD_POOL_COUNT;
	public static int THREADS_PER_INSTANT_THREAD_POOL;
	
	/** Misc */
	public static boolean L2WALKER_PROTECTION;
	public static boolean SERVER_NEWS;
	public static int ZONE_TOWN;
	public static boolean DISABLE_TUTORIAL;

	public static int MIN_PROTOCOL_REVISION;
	public static int MAX_PROTOCOL_REVISION;	
	
	// --------------------------------------------------
	// Those "hidden" settings haven't configs to avoid admins to fuck their server
	// You still can experiment changing values here. But don't say I didn't warn you.
	// --------------------------------------------------
	
	/** Reserve Host on LoginServerThread */
	public static boolean RESERVE_HOST_ON_LOGIN = false; // default false
	
	/** MMO settings */
	public static int MMO_SELECTOR_SLEEP_TIME = 20; // default 20
	public static int MMO_MAX_SEND_PER_PASS = 80; // default 80
	public static int MMO_MAX_READ_PER_PASS = 80; // default 80
	public static int MMO_HELPER_BUFFER_COUNT = 20; // default 20
	
	/** Client Packets Queue settings */
	public static int CLIENT_PACKET_QUEUE_SIZE = MMO_MAX_READ_PER_PASS + 2; // default MMO_MAX_READ_PER_PASS + 2
	public static int CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = MMO_MAX_READ_PER_PASS + 1; // default MMO_MAX_READ_PER_PASS + 1
	public static int CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = 160; // default 160
	public static int CLIENT_PACKET_QUEUE_MEASURE_INTERVAL = 5; // default 5
	public static int CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND = 80; // default 80
	public static int CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN = 2; // default 2
	public static int CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN = 1; // default 1
	public static int CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN = 1; // default 1
	public static int CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN = 5; // default 5
	
	// --------------------------------------------------
	
	/**
	 * Initialize {@link ExProperties} from specified configuration file.
	 * @param filename : File name to be loaded.
	 * @return ExProperties : Initialized {@link ExProperties}.
	 */
	public static final ExProperties initProperties(String filename)
	{
		final ExProperties result = new ExProperties();
		
		try
		{
			result.load(new File(filename));
		}
		catch (Exception e)
		{
			LOGGER.error("An error occured loading '{}' config.", e, filename);
		}
		
		return result;
	}
	
	/**
	 * Loads starting settings.
	 */
	private static final void loadStarting()
	{
		final ExProperties starting = initProperties(STARTING_FILE);
		STARTING_LOCATION = starting.getProperty("StartingLocation", false);
		String[] newCharcoords = starting.getProperty("NewCharCoordinates", "0,0,0").split(",");
		if (newCharcoords.length < 3)
			STARTING_LOCATION = false;
		else
		{
			NEW_CHAR_COORDINATES[0] = Integer.parseInt(newCharcoords[0]);
			NEW_CHAR_COORDINATES[1] = Integer.parseInt(newCharcoords[1]);
			NEW_CHAR_COORDINATES[2] = Integer.parseInt(newCharcoords[2]);
		}
		STARTING_LEVEL = starting.getProperty("StartingLevel", 0);
		NEW_CHAR_TITLE = starting.getProperty("NewCharTitle", "");

		STARTING_BUFFS = starting.getProperty("StartingBuffs", true);

		String[] propertySplit = starting.getProperty("StartingBuffsMage", "1204,2").split(";");
		STARTING_BUFFS_M.clear();
		for (String buff : propertySplit)
		{
		    String[] buffSplit = buff.split(",");
			if (buffSplit.length != 2)
			LOGGER.warn("StartingBuffsMage[Config.load()]: invalid config property -> StartingBuffsMage \"" + buff + "\"");
			else
			{
				try
				{
				STARTING_BUFFS_M.add(new int[]{Integer.parseInt(buffSplit[0]), Integer.parseInt(buffSplit[1])});
				}
				catch (NumberFormatException nfe)
				{
				if (STARTING_BUFFS_M.equals(""))
				System.out.println("EROOOOOOOOOOOR WITH STARTING BUFS");
				}
			}
		}
		propertySplit = starting.getProperty("StartingBuffsFighter", "1204,2").split(";");
		STARTING_BUFFS_F.clear();
		for (String buff : propertySplit)
		{
			 String[] buffSplit = buff.split(",");
			 if (buffSplit.length != 2)
			 LOGGER.warn("StartingBuffsFighter[Config.load()]: invalid config property -> StartingBuffsFighter \"" + buff + "\"");
			 else
			 {
				 try
				 {
				 STARTING_BUFFS_F.add(new int[]{Integer.parseInt(buffSplit[0]), Integer.parseInt(buffSplit[1])});
				 }
					catch (NumberFormatException nfe)
					{
					if (STARTING_BUFFS_F.equals(""))
					System.out.println("EROOOOOOOOOOOR WITH STARTING BUFS");
					}
			 }
		}  
	}
	
	private static final void loadBosses()
	{
		final ExProperties bosses = initProperties(BOSSES_FILE);
		RAID_HP_REGEN_MULTIPLIER = bosses.getProperty("RaidHpRegenMultiplier", 1.);
		RAID_MP_REGEN_MULTIPLIER = bosses.getProperty("RaidMpRegenMultiplier", 1.);
		RAID_DEFENCE_MULTIPLIER = bosses.getProperty("RaidDefenceMultiplier", 1.);
		RAID_MINION_RESPAWN_TIMER = bosses.getProperty("RaidMinionRespawnTime", 300000);
		
		RAID_DISABLE_CURSE = bosses.getProperty("DisableRaidCurse", false);
		
		SPAWN_INTERVAL_AQ = bosses.getProperty("AntQueenSpawnInterval", 36);
		RANDOM_SPAWN_TIME_AQ = bosses.getProperty("AntQueenRandomSpawn", 17);
		
		SPAWN_INTERVAL_ANTHARAS = bosses.getProperty("AntharasSpawnInterval", 264);
		RANDOM_SPAWN_TIME_ANTHARAS = bosses.getProperty("AntharasRandomSpawn", 72);
		WAIT_TIME_ANTHARAS = bosses.getProperty("AntharasWaitTime", 30) * 60000;
		
		SPAWN_INTERVAL_BAIUM = bosses.getProperty("BaiumSpawnInterval", 168);
		RANDOM_SPAWN_TIME_BAIUM = bosses.getProperty("BaiumRandomSpawn", 48);
		
		SPAWN_INTERVAL_CORE = bosses.getProperty("CoreSpawnInterval", 60);
		RANDOM_SPAWN_TIME_CORE = bosses.getProperty("CoreRandomSpawn", 23);
		
		SPAWN_INTERVAL_FRINTEZZA = bosses.getProperty("FrintezzaSpawnInterval", 48);
		RANDOM_SPAWN_TIME_FRINTEZZA = bosses.getProperty("FrintezzaRandomSpawn", 8);
		WAIT_TIME_FRINTEZZA = bosses.getProperty("FrintezzaWaitTime", 1) * 60000;
		
		SPAWN_INTERVAL_ORFEN = bosses.getProperty("OrfenSpawnInterval", 48);
		RANDOM_SPAWN_TIME_ORFEN = bosses.getProperty("OrfenRandomSpawn", 20);
		
		SPAWN_INTERVAL_SAILREN = bosses.getProperty("SailrenSpawnInterval", 36);
		RANDOM_SPAWN_TIME_SAILREN = bosses.getProperty("SailrenRandomSpawn", 24);
		WAIT_TIME_SAILREN = bosses.getProperty("SailrenWaitTime", 5) * 60000;
		
		SPAWN_INTERVAL_VALAKAS = bosses.getProperty("ValakasSpawnInterval", 264);
		RANDOM_SPAWN_TIME_VALAKAS = bosses.getProperty("ValakasRandomSpawn", 72);
		WAIT_TIME_VALAKAS = bosses.getProperty("ValakasWaitTime", 30) * 60000;
		
		SPAWN_INTERVAL_ZAKEN = bosses.getProperty("ZakenSpawnInterval", 60);
		RANDOM_SPAWN_TIME_ZAKEN = bosses.getProperty("ZakenRandomSpawn", 20);
		
		// Mega-style raid respawn override by difficulty (hours)
		RESPAWN_CUSTOM = Boolean.parseBoolean(bosses.getProperty("RespawnCustom", "false"));
		RESPAWN_EASY_RAID_IDS = parseNpcIdList(bosses.getProperty("Respawn_EasyRaid_IDs", ""));
		RESPAWN_EASY_MIN = Integer.parseInt(bosses.getProperty("Respawn_Easy_raidboss", "4").trim());
		RESPAWN_EASY_RANDOM = Integer.parseInt(bosses.getProperty("Respawn_Easy_random_raidboss", "0").trim());
		RESPAWN_MID_RAID_IDS = parseNpcIdList(bosses.getProperty("Respawn_MidRaid_IDs", ""));
		RESPAWN_MID_MIN = Integer.parseInt(bosses.getProperty("Respawn_Mid_raidboss", "4").trim());
		RESPAWN_MID_RANDOM = Integer.parseInt(bosses.getProperty("Respawn_Mid_random_raidboss", "0").trim());
		RESPAWN_HARD_RAID_IDS = parseNpcIdList(bosses.getProperty("Respawn_HardRaid_IDs", ""));
		RESPAWN_HARD_MIN = Integer.parseInt(bosses.getProperty("Respawn_Hard_raidboss", "4").trim());
		RESPAWN_HARD_RANDOM = Integer.parseInt(bosses.getProperty("Respawn_Hard_random_raidboss", "0").trim());
		
		FLAG_RAIDS = bosses.getProperty("Flag_Raids", false);
		FLAG_RANGE = bosses.getProperty("Flag_Range", 3000);
		FLAG_RAID_IDS = bosses.getProperty("Flag_RaidIds", "");
		LIST_FLAG_RAID_IDS = parseNpcIdList(FLAG_RAID_IDS);
		
		ANNOUNCE_RAID_BOSS_HP = Boolean.parseBoolean(bosses.getProperty("AnnounceRaidBossHp", "false"));
		ANNOUNCE_RAID_BOSS_HP_IDS_STR = bosses.getProperty("AnnounceRaidBossHpIds", "");
		ANNOUNCE_RAID_BOSS_HP_IDS = parseNpcIdList(ANNOUNCE_RAID_BOSS_HP_IDS_STR);
		ANNOUNCE_GRAND_BOSS_HP_IDS_STR = bosses.getProperty("AnnounceGrandBossHpIds", "");
		ANNOUNCE_GRAND_BOSS_HP_IDS = parseNpcIdList(ANNOUNCE_GRAND_BOSS_HP_IDS_STR);
		BOSS_HP_ANNOUNCE_THRESHOLDS = parseIntegerList(bosses.getProperty("BossHpAnnounceThresholds", "80,60,40,20,10"));
		ANNOUNCE_RAID_BOSS_KILL = Boolean.parseBoolean(bosses.getProperty("AnnounceRaidBossKill", "false"));
		ANNOUNCE_GRAND_BOSS_KILL = Boolean.parseBoolean(bosses.getProperty("AnnounceGrandBossKill", "false"));
		ANNOUNCE_RAID_BOSS_SPAWN_WARNING = Boolean.parseBoolean(bosses.getProperty("AnnounceRaidBossSpawnWarning", "false"));
		ANNOUNCE_GRAND_BOSS_SPAWN_WARNING = Boolean.parseBoolean(bosses.getProperty("AnnounceGrandBossSpawnWarning", "false"));
		BOSS_SPAWN_WARNING_MINUTES = parseIntegerList(bosses.getProperty("BossSpawnWarningMinutes", "10,5"));
		BOSS_SPAWN_WARNING_SHOW_TIME = Boolean.parseBoolean(bosses.getProperty("BossSpawnWarningShowTime", "true"));
		
		ENABLE_RAIDBOSS_NOBLES = Boolean.parseBoolean(bosses.getProperty("RaidBossNobles", "false"));
		BOSS_NOBLES_IDS = bosses.getProperty("RaidBossNoblesIDs", "0,0");
		LIST_BOSS_NOBLES_IDS = new ArrayList<>();
		for (String val : BOSS_NOBLES_IDS.split(","))
		{
			int npcId = Integer.parseInt(val);
			LIST_BOSS_NOBLES_IDS.add(npcId);
		}
		
		CUSTOM_EPIC_SPAWN_ENABLED = bosses.getProperty("CustomEpicSpawnEnabled", false);
		CUSTOM_EPIC_SPAWNS = new ArrayList<>();
		for (int i = 1; i <= 50; i++)
		{
			if (!bosses.getProperty("CustomEpic_" + i + "_Enabled", false))
				continue;
			
			final int npcId = Integer.parseInt(bosses.getProperty("CustomEpic_" + i + "_Id", "0").trim());
			if (npcId <= 0)
			{
				LOGGER.warn("CustomEpic_" + i + ": invalid Id.");
				continue;
			}
			
			final String[] loc = bosses.getProperty("CustomEpic_" + i + "_Loc", "0,0,0,0").trim().split(",");
			if (loc.length < 3)
			{
				LOGGER.warn("CustomEpic_" + i + ": invalid Loc.");
				continue;
			}
			
			final int x = Integer.parseInt(loc[0].trim());
			final int y = Integer.parseInt(loc[1].trim());
			final int z = Integer.parseInt(loc[2].trim());
			final int heading = (loc.length >= 4) ? Integer.parseInt(loc[3].trim()) : 0;
			final boolean announce = bosses.getProperty("CustomEpic_" + i + "_Announce", true);
			final List<int[]> schedules = CustomEpicSpawnHolder.parseDaysAndHours(bosses.getProperty("CustomEpic_" + i + "_DaysAndHours", "").trim());
			if (schedules.isEmpty())
			{
				LOGGER.warn("CustomEpic_" + i + ": invalid DaysAndHours.");
				continue;
			}
			
			CUSTOM_EPIC_SPAWNS.add(new CustomEpicSpawnHolder(npcId, x, y, z, heading, announce, schedules));
		}
		
		QUEST_BAIUM = Integer.parseInt(bosses.getProperty("QuestBaium", "4295"));	
		QUEST_VALAKAS = Integer.parseInt(bosses.getProperty("QuestValakas", "7267"));	
		QUEST_ANTHARAS = Integer.parseInt(bosses.getProperty("QuestAntharas", "3865"));	
		QUEST_SAILREN = Integer.parseInt(bosses.getProperty("QuestSailren", "8784"));
		QUEST_FRINTEZZA = Integer.parseInt(bosses.getProperty("QuestFrintezza", "8073"));
	}
	private static final void loadFloods()
	{
		final ExProperties floods = initProperties(FLOODS_FILE);
		ROLL_DICE_TIME = floods.getProperty("RollDiceTime", 4200);
		HERO_VOICE_TIME = floods.getProperty("HeroVoiceTime", 10000);
		SUBCLASS_TIME = floods.getProperty("SubclassTime", 2000);
		DROP_ITEM_TIME = floods.getProperty("DropItemTime", 1000);
		SERVER_BYPASS_TIME = floods.getProperty("ServerBypassTime", 500);
		MULTISELL_TIME = floods.getProperty("MultisellTime", 100);
		MANUFACTURE_TIME = floods.getProperty("ManufactureTime", 300);
		MANOR_TIME = floods.getProperty("ManorTime", 3000);
		SENDMAIL_TIME = floods.getProperty("SendMailTime", 10000);
		CHARACTER_SELECT_TIME = floods.getProperty("CharacterSelectTime", 3000);
		GLOBAL_CHAT_TIME = floods.getProperty("GlobalChatTime", 0);
		TRADE_CHAT_TIME = floods.getProperty("TradeChatTime", 0);
		GLOBAL_SHOUT_TRADE_CHAT = floods.getProperty("GlobalShoutTradeChat", true);
		SOCIAL_TIME = floods.getProperty("SocialTime", 2000);
		MOVE_TIME = floods.getProperty("MoveTime", 30);
		ATTACK_TIME = floods.getProperty("AttackTime", 30);
		SKILL_TIME = floods.getProperty("SkillTime", 30);
	}
	private static final void loadRates()
	{
		final ExProperties rates = initProperties(RATES_FILE);

		ENABLE_DYNAMIC_RATES = rates.getProperty("EnableDynamicRates", false);
		RATE_XP = rates.getProperty("RateXp", 1.);
		RATE_SP = rates.getProperty("RateSp", 1.);
		RATE_PARTY_XP = rates.getProperty("RatePartyXp", 1.);
		RATE_PARTY_SP = rates.getProperty("RatePartySp", 1.);
		RATE_DROP_ADENA = rates.getProperty("RateDropAdena", 1.);
		DROP_MAX_LEVEL_DIFFERENCE = Math.max(0, rates.getProperty("DropMaxLevelDifference", 0));
		RATE_DROP_ITEMS = rates.getProperty("RateDropItems", 1.);
		RATE_DROP_ITEMS_BY_RAID = rates.getProperty("RateRaidDropItems", 1.);
		RATE_DROP_SPOIL = rates.getProperty("RateDropSpoil", 1.);
		RATE_DROP_MANOR = rates.getProperty("RateDropManor", 1);
		RATE_QUEST_DROP = rates.getProperty("RateQuestDrop", 1.);
		RATE_QUEST_REWARD = rates.getProperty("RateQuestReward", 1.);
		RATE_QUEST_REWARD_XP = rates.getProperty("RateQuestRewardXP", 1.);
		RATE_QUEST_REWARD_SP = rates.getProperty("RateQuestRewardSP", 1.);
		RATE_QUEST_REWARD_ADENA = rates.getProperty("RateQuestRewardAdena", 1.);
		RATE_KARMA_EXP_LOST = rates.getProperty("RateKarmaExpLost", 1.);
		RATE_SIEGE_GUARDS_PRICE = rates.getProperty("RateSiegeGuardsPrice", 1.);
		RATE_DROP_COMMON_HERBS = rates.getProperty("RateCommonHerbs", 1.);
		RATE_DROP_HP_HERBS = rates.getProperty("RateHpHerbs", 1.);
		RATE_DROP_MP_HERBS = rates.getProperty("RateMpHerbs", 1.);
		RATE_DROP_SPECIAL_HERBS = rates.getProperty("RateSpecialHerbs", 1.);
		PLAYER_DROP_LIMIT = rates.getProperty("PlayerDropLimit", 3);
		PLAYER_RATE_DROP = rates.getProperty("PlayerRateDrop", 5);
		PLAYER_RATE_DROP_ITEM = rates.getProperty("PlayerRateDropItem", 70);
		PLAYER_RATE_DROP_EQUIP = rates.getProperty("PlayerRateDropEquip", 25);
		PLAYER_RATE_DROP_EQUIP_WEAPON = rates.getProperty("PlayerRateDropEquipWeapon", 5);
		PET_XP_RATE = rates.getProperty("PetXpRate", 1.);
		PET_FOOD_RATE = rates.getProperty("PetFoodRate", 1);
		SINEATER_XP_RATE = rates.getProperty("SinEaterXpRate", 1.);
		KARMA_DROP_LIMIT = rates.getProperty("KarmaDropLimit", 10);
		KARMA_RATE_DROP = rates.getProperty("KarmaRateDrop", 70);
		KARMA_RATE_DROP_ITEM = rates.getProperty("KarmaRateDropItem", 50);
		KARMA_RATE_DROP_EQUIP = rates.getProperty("KarmaRateDropEquip", 40);
		KARMA_RATE_DROP_EQUIP_WEAPON = rates.getProperty("KarmaRateDropEquipWeapon", 10);
	}
	private static final void loadOlympiads()
	{
		final ExProperties olys = initProperties(OLYMPIAD_FILE);

		ALT_OLY_START_TIME = olys.getProperty("AltOlyStartTime", 18);
		ALT_OLY_MIN = olys.getProperty("AltOlyMin", 0);
		ALT_OLY_CPERIOD = olys.getProperty("AltOlyCPeriod", 21600000);
		ALT_OLY_BATTLE = olys.getProperty("AltOlyBattle", 180000);
		ALT_OLY_WPERIOD = olys.getProperty("AltOlyWPeriod", 604800000);
		ALT_OLY_VPERIOD = olys.getProperty("AltOlyVPeriod", 86400000);
		ALT_OLY_WAIT_TIME = olys.getProperty("AltOlyWaitTime", 30);
		ALT_OLY_WAIT_BATTLE = olys.getProperty("AltOlyWaitBattle", 60);
		ALT_OLY_WAIT_END = olys.getProperty("AltOlyWaitEnd", 40);
		ALT_OLY_START_POINTS = olys.getProperty("AltOlyStartPoints", 18);
		ALT_OLY_WEEKLY_POINTS = olys.getProperty("AltOlyWeeklyPoints", 3);
		ALT_OLY_MIN_MATCHES = olys.getProperty("AltOlyMinMatchesToBeClassed", 5);
		ALT_OLY_CLASSED = olys.getProperty("AltOlyClassedParticipants", 5);
		ALT_OLY_NONCLASSED = olys.getProperty("AltOlyNonClassedParticipants", 9);
		ALT_OLY_CLASSED_REWARD = olys.parseIntIntList("AltOlyClassedReward", "6651-50");
		ALT_OLY_NONCLASSED_REWARD = olys.parseIntIntList("AltOlyNonClassedReward", "6651-30");
		ALT_OLY_GP_PER_POINT = olys.getProperty("AltOlyGPPerPoint", 1000);
		ALT_OLY_HERO_POINTS = olys.getProperty("AltOlyHeroPoints", 300);
		ALT_OLY_RANK1_POINTS = olys.getProperty("AltOlyRank1Points", 100);
		ALT_OLY_RANK2_POINTS = olys.getProperty("AltOlyRank2Points", 75);
		ALT_OLY_RANK3_POINTS = olys.getProperty("AltOlyRank3Points", 55);
		ALT_OLY_RANK4_POINTS = olys.getProperty("AltOlyRank4Points", 40);
		ALT_OLY_RANK5_POINTS = olys.getProperty("AltOlyRank5Points", 30);
		ALT_OLY_MAX_POINTS = olys.getProperty("AltOlyMaxPoints", 10);
		ALT_OLY_DIVIDER_CLASSED = olys.getProperty("AltOlyDividerClassed", 3);
		ALT_OLY_DIVIDER_NON_CLASSED = olys.getProperty("AltOlyDividerNonClassed", 5);
		ALT_OLY_ANNOUNCE_GAMES = olys.getProperty("AltOlyAnnounceGames", true);

		ENABLE_OLYMPIAD_ANNOUNCE = olys.getProperty("EnableOlympiadAnnounce", true);
		OLYMPIAD_ANNOUNCE = olys.getProperty("OlympiadAnnounce", "took points from");
		
		OLYMPIAD_REMOVE_SKILL_REUSE = olys.getProperty("OlympiadRemoveSkillReuse", true);
		
		OLYMPIAD_COUNTDOWN = olys.getProperty("OlympiadCountdown", 20);
		OLYMPIAD_COUNTDOWN_HEAL = olys.getProperty("OlympiadCountdownHeal", true);
		
		String[] propertySplit = olys.getProperty("OlympiadFighterBuffs", "").split(";");
		if (!propertySplit[0].isEmpty())
		{
			OLYMPIAD_FIGHTER_BUFFS = new HashMap<>(propertySplit.length);
			for (String skill : propertySplit)
			{
				String[] skillSplit = skill.split(",");
				if (skillSplit.length != 2)
				{
					LOGGER.warn(StringUtil.concat("Olympiad: invalid config property -> OlympiadFighterBuffs \"", skill, "\""));
				}
				else
				{
					try
					{
						OLYMPIAD_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOGGER.warn(StringUtil.concat("Olympiad: invalid config property -> OlympiadFighterBuffs \"", skill, "\""));
						}
					}
				}
			}
		}

		propertySplit = olys.getProperty("OlympiadMageBuffs", "").split(";");
		if (!propertySplit[0].isEmpty())
		{
			OLYMPIAD_MAGE_BUFFS = new HashMap<>(propertySplit.length);
			for (String skill : propertySplit)
			{
				String[] skillSplit = skill.split(",");
				if (skillSplit.length != 2)
				{
					LOGGER.warn(StringUtil.concat("Olympiad: invalid config property -> OlympiadMageBuffs \"", skill, "\""));
				}
				else
				{
					try
					{
						OLYMPIAD_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOGGER.warn(StringUtil.concat("Olympiad: invalid config property -> OlympiadMageBuffs \"", skill, "\""));
						}
					}
				}
			}
		}		
	}
	private static final void loadAgathions()
	{
		final ExProperties aga = initProperties(AGATHION_FILE);

	    SKILL_ID_AGATHION = aga.getProperty("SkillIdCastAgathion", 1011);
	    ANIMATION_SPAWN_SKILL_ID_AGATHION = aga.getProperty("AnimationSpawnSkillID", 1011);
	    ANIMATION_SUMMON_SKILL_ID_AGATHION = aga.getProperty("AnimationSummonSkillID", 1011);
	    SKILL_LEVEL_AGATHION = aga.getProperty("SkillLevelCastAgathion", 18);
	    AGATHION_ONLY_FOLLOW = aga.getProperty("AgathionOnlyFollow", false);
	    AGATHION_ALLOW_SOCIAL = aga.getProperty("AgathionAllowSocial", true);
	    AGATHION_HEAL_OWNER = aga.getProperty("AgathionHealOwner", false);
	    AGATHION_PERCENT_TO_HEAL = aga.getProperty("AgathionPercentToHeal", 0.25);
	}
	private static final void load_PartyFarm()
	{
		final ExProperties partyFarm = initProperties(Config.PARTY_FARM_FILE);
		
		NAME_EVENT = partyFarm.getProperty("NamePartyFarm", "Party Farm").replace(",", "").trim();
		PARTY_FARM_EVENT_ENABLED = Boolean.parseBoolean(partyFarm.getProperty("PartyFarmEventEnabled", "false"));
		ENABLE_SPAWN_PARTYFARM_MONSTERS = Boolean.parseBoolean(partyFarm.getProperty("EnableSpawnPartyFarmMonsters", "true"));
		ENABLE_DUALBOX_PARTYFARM = Boolean.parseBoolean(partyFarm.getProperty("RenewalDualBoxPTFarm", "false"));
		EVENT_BEST_FARM_TIME = Integer.parseInt(partyFarm.getProperty("EventBestFarmTime", "40"));
		EVENT_BEST_FARM_INTERVAL_BY_TIME_OF_DAY = partyFarm.getProperty("BestFarmStartTime", "20:00").split(",");
		
		PARTY_MESSAGE_ENABLED = Boolean.parseBoolean(partyFarm.getProperty("ScreenPartyMessageEnable", "true"));
		PARTY_FARM_MESSAGE_TEXT = partyFarm.getProperty("ScreenPartyFarmMessageText", "Party Farm is active");
		PARTY_FARM_MESSAGE_TIME = Integer.parseInt(partyFarm.getProperty("ScreenPartyFarmMessageTime", "6")) * 1000;
		
		PARTY_FARM_MONSTER_DALAY = Integer.parseInt(partyFarm.getProperty("MonsterDelay", "10"));
		monsterId = Integer.parseInt(partyFarm.getProperty("MonsterId", "1"));
		
		PART_ZONE_MONSTERS_EVENT_ID = new ArrayList<>();
		final String eventMonsters = partyFarm.getProperty("PartyEventMonster", String.valueOf(monsterId)).trim();
		if (!eventMonsters.isEmpty())
		{
			for (String id : eventMonsters.split(","))
			{
				id = id.trim();
				if (!id.isEmpty())
					PART_ZONE_MONSTERS_EVENT_ID.add(Integer.parseInt(id));
			}
		}
		if (PART_ZONE_MONSTERS_EVENT_ID.isEmpty())
			PART_ZONE_MONSTERS_EVENT_ID.add(monsterId);
		
		PARTY_FARM_REWARDS = parseReward(partyFarm, "PartyFarmReward");
		RADIUS_TO_PARTYFARM = Integer.parseInt(partyFarm.getProperty("RadiusToPartyFarm", "1500"));
		
		CUSTOM_SPAWN_PARTYFARM = new ArrayList<>();
		final String customSpawnLocation = partyFarm.getProperty("SpawnPointMonster", "").trim();
		if (!customSpawnLocation.isEmpty())
		{
			for (String location : customSpawnLocation.split(";"))
			{
				location = location.trim();
				if (location.isEmpty())
					continue;
				
				final String[] parts = location.split(",");
				if (parts.length < 3)
					continue;
				
				CUSTOM_SPAWN_PARTYFARM.add(new int[]
				{
					Integer.parseInt(parts[0].trim()),
					Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim())
				});
			}
		}
		
		AMOUNT_MONSTROS_SPAWN_PT = Integer.parseInt(partyFarm.getProperty("QuantiyMonstersSpawns", "40"));
		RADIUS_SPAWN_LOC_PTFARM = Integer.parseInt(partyFarm.getProperty("RadiusSpawnMonsters", "800"));
	}
	
	/**
	 * PvP Farm: clon del Party Farm con flag PvP por rango alrededor de los mobs (patron Flag_Raids) y reward individual.
	 */
	private static final void load_PvpFarm()
	{
		final ExProperties pvpFarm = initProperties(Config.PVP_FARM_FILE);
		
		PVPFARM_NAME = pvpFarm.getProperty("NamePvpFarm", "PvP Farm").replace(",", "").trim();
		PVPFARM_EVENT_ENABLED = Boolean.parseBoolean(pvpFarm.getProperty("PvpFarmEventEnabled", "false"));
		PVPFARM_SPAWN_MONSTERS = Boolean.parseBoolean(pvpFarm.getProperty("EnableSpawnPvpFarmMonsters", "true"));
		
		PVPFARM_EVENT_TIME = Integer.parseInt(pvpFarm.getProperty("EventPvpFarmTime", "60"));
		PVPFARM_START_TIME = pvpFarm.getProperty("PvpFarmStartTime", "14:00").split(",");
		
		// Flag PvP por rango: mismo patron que Flag_Raids / Flag_Range / Flag_RaidIds de Bosses.properties.
		PVPFARM_FLAG_MONSTERS = Boolean.parseBoolean(pvpFarm.getProperty("Flag_Monsters", "true"));
		PVPFARM_FLAG_RANGE = Integer.parseInt(pvpFarm.getProperty("Flag_Range", "3000"));
		PVPFARM_FLAG_INTERVAL = Integer.parseInt(pvpFarm.getProperty("Flag_Interval", "3000"));
		PVPFARM_FLAG_ON_KILL = Boolean.parseBoolean(pvpFarm.getProperty("FlagOnKill", "true"));
		PVPFARM_FLAG_ON_KILL_TIME = Integer.parseInt(pvpFarm.getProperty("FlagOnKillTime", "60")) * 1000;
		
		PVPFARM_MESSAGE_ENABLED = Boolean.parseBoolean(pvpFarm.getProperty("ScreenPvpMessageEnable", "true"));
		PVPFARM_MESSAGE_TEXT = pvpFarm.getProperty("ScreenPvpFarmMessageText", "PvP Farm is active - Teleport Now!");
		PVPFARM_MESSAGE_TIME = Integer.parseInt(pvpFarm.getProperty("ScreenPvpFarmMessageTime", "6")) * 1000;
		
		PVPFARM_MONSTER_DELAY = Integer.parseInt(pvpFarm.getProperty("MonsterDelay", "60"));
		PVPFARM_MONSTER_IDS = parseNpcIdList(pvpFarm.getProperty("MonsterIds", ""));
		
		PVPFARM_FLAG_MONSTER_IDS = parseNpcIdList(pvpFarm.getProperty("Flag_MonstersIds", ""));
		if (PVPFARM_FLAG_MONSTER_IDS.isEmpty())
			PVPFARM_FLAG_MONSTER_IDS.addAll(PVPFARM_MONSTER_IDS);
		
		PVPFARM_CUSTOM_REWARD = Boolean.parseBoolean(pvpFarm.getProperty("PvpFarmCustomReward", "false"));
		PVPFARM_REWARDS = parseReward(pvpFarm, "PvpFarmReward");
		PVPFARM_ANTI_DUALBOX = Boolean.parseBoolean(pvpFarm.getProperty("PvpFarmAntiDualbox", "true"));
		
		PVPFARM_SPAWN_POINTS = new ArrayList<>();
		final String customSpawnLocation = pvpFarm.getProperty("SpawnPointMonster", "").trim();
		if (!customSpawnLocation.isEmpty())
		{
			for (String location : customSpawnLocation.split(";"))
			{
				location = location.trim();
				if (location.isEmpty())
					continue;
				
				final String[] parts = location.split(",");
				if (parts.length < 3)
					continue;
				
				PVPFARM_SPAWN_POINTS.add(new int[]
				{
					Integer.parseInt(parts[0].trim()),
					Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim())
				});
			}
		}
		
		PVPFARM_SPAWN_AMOUNT = Integer.parseInt(pvpFarm.getProperty("QuantiyMonstersSpawns", "20"));
		PVPFARM_SPAWN_RADIUS = Integer.parseInt(pvpFarm.getProperty("RadiusSpawnMonsters", "800"));
	}
	
	private static final void loadInstanceFarm()
	{
		final ExProperties p = initProperties(INSTANCE_FARM_FILE);
		
		INSTANCE_FARM_ENABLED = Boolean.parseBoolean(p.getProperty("InstanceFarmEnabled", "False"));
		INSTANCE_FARM_HUB_NPC_ID = Integer.parseInt(p.getProperty("InstanceFarmHubNpcId", "60020"));
		INSTANCE_FARM_SPAWN_HUB = Boolean.parseBoolean(p.getProperty("InstanceFarmSpawnHub", "False"));
		INSTANCE_FARM_HUB_X = Integer.parseInt(p.getProperty("InstanceFarmHubX", "0"));
		INSTANCE_FARM_HUB_Y = Integer.parseInt(p.getProperty("InstanceFarmHubY", "0"));
		INSTANCE_FARM_HUB_Z = Integer.parseInt(p.getProperty("InstanceFarmHubZ", "0"));
		INSTANCE_FARM_HUB_HEADING = Integer.parseInt(p.getProperty("InstanceFarmHubHeading", "0"));
		INSTANCE_FARM_EXIT_X = Integer.parseInt(p.getProperty("InstanceFarmExitX", "0"));
		INSTANCE_FARM_EXIT_Y = Integer.parseInt(p.getProperty("InstanceFarmExitY", "0"));
		INSTANCE_FARM_EXIT_Z = Integer.parseInt(p.getProperty("InstanceFarmExitZ", "0"));
		INSTANCE_FARM_ZONES_FILE = p.getProperty("InstanceFarmZonesFile", "./data/xml/custom/InstanceFarmZones.xml");
		
		INSTANCE_FARM_TIME_PAY_ITEM_ID = Integer.parseInt(p.getProperty("TimePayItemId", "10001").trim());
		INSTANCE_FARM_TIME_PAY_ITEM_COUNT = Math.max(1, Integer.parseInt(p.getProperty("TimePayItemCount", "1").trim()));
		INSTANCE_FARM_TIME_PAY_MINUTES = Math.max(1, Integer.parseInt(p.getProperty("TimePayMinutes", "60").trim()));
		INSTANCE_FARM_TIME_MIN_LEVEL = Math.max(1, Integer.parseInt(p.getProperty("TimeMinLevel", "1").trim()));
		INSTANCE_FARM_TIME_SHOW_TIMER = Boolean.parseBoolean(p.getProperty("TimeShowTimer", "True").trim());
		
		INSTANCE_FARM_KILL_PACKS.clear();
		for (String pack : p.getProperty("KillPacks", "").split(";"))
		{
			final String[] parts = pack.trim().split(",");
			if (parts.length != 3)
				continue;
			
			try
			{
				final int kills = Integer.parseInt(parts[0].trim());
				final int itemId = Integer.parseInt(parts[1].trim());
				final int count = Integer.parseInt(parts[2].trim());
				if (kills > 0 && itemId > 0 && count > 0)
					INSTANCE_FARM_KILL_PACKS.add(new int[]
					{
						kills,
						itemId,
						count
					});
			}
			catch (NumberFormatException e)
			{
				// pack mal escrito, se ignora
			}
		}
		INSTANCE_FARM_KILL_SPAWN_DELAY_SECONDS = Math.max(0, Integer.parseInt(p.getProperty("KillSpawnDelaySeconds", "3").trim()));
		INSTANCE_FARM_KILL_KICK_DELAY_SECONDS = Math.max(0, Integer.parseInt(p.getProperty("KillKickDelaySeconds", "5").trim()));
		INSTANCE_FARM_KILL_SHOW_COUNTER = Boolean.parseBoolean(p.getProperty("KillShowCounter", "True").trim());
		
		parseFarmRewards(p.getProperty("TimeRewards", ""), INSTANCE_FARM_TIME_REWARDS);
		INSTANCE_FARM_TIME_NORMAL_DROP = Boolean.parseBoolean(p.getProperty("TimeNormalDrop", "True").trim());
		INSTANCE_FARM_TIME_NO_CORPSE = Boolean.parseBoolean(p.getProperty("TimeNoCorpse", "False").trim());
		
		INSTANCE_FARM_KILL_REWARDS.clear();
		for (String reward : p.getProperty("KillRewards", "").split(";"))
		{
			final String[] parts = reward.trim().split(",");
			if (parts.length != 4)
				continue;
			
			try
			{
				final double itemId = Integer.parseInt(parts[0].trim());
				final double min = Integer.parseInt(parts[1].trim());
				final double max = Integer.parseInt(parts[2].trim());
				final double chance = Double.parseDouble(parts[3].trim());
				if (itemId > 0 && min > 0 && max >= min && chance > 0)
					INSTANCE_FARM_KILL_REWARDS.add(new double[]
					{
						itemId,
						min,
						max,
						chance
					});
			}
			catch (NumberFormatException e)
			{
				// recompensa mal escrita, se ignora
			}
		}
		INSTANCE_FARM_KILL_NORMAL_DROP = Boolean.parseBoolean(p.getProperty("KillNormalDrop", "False").trim());
		INSTANCE_FARM_KILL_NO_CORPSE = Boolean.parseBoolean(p.getProperty("KillNoCorpse", "True").trim());
		INSTANCE_FARM_KILL_AOE_AGGRO = Boolean.parseBoolean(p.getProperty("KillAoeAggro", "True").trim());
		INSTANCE_FARM_KILL_FACE_PLAYER = Boolean.parseBoolean(p.getProperty("KillFacePlayer", "True").trim());
		INSTANCE_FARM_KILL_DEATH_KICK_SECONDS = Math.max(0, Integer.parseInt(p.getProperty("KillDeathKickSeconds", "3").trim()));
		INSTANCE_FARM_KILL_NPC_ID = Integer.parseInt(p.getProperty("KillNpcId", "60023").trim());
		INSTANCE_FARM_MAX_PER_HWID = Math.max(0, Integer.parseInt(p.getProperty("MaxPerHwid", "1").trim()));
		INSTANCE_FARM_COUNTER_SMALL = Boolean.parseBoolean(p.getProperty("CounterSmallFont", "True").trim());
		INSTANCE_FARM_COUNTER_POSITION = p.getProperty("CounterPosition", "BOTTOM_RIGHT").trim().toUpperCase();
		INSTANCE_FARM_COUNTER_UPDATE_MS = Math.max(200, Integer.parseInt(p.getProperty("CounterUpdateMs", "1000").trim()));
		INSTANCE_FARM_TIME_TIMER_SECONDS = Math.max(0, Integer.parseInt(p.getProperty("TimeTimerSeconds", "1").trim()));
	}
	
	private static final void loadFarmHwid()
	{
		final ExProperties p = initProperties(FARM_HWID_FILE);
		FARM_HWID_ENABLED = Boolean.parseBoolean(p.getProperty("FarmHwidEnabled", "True"));
		FARM_HWID_IGNORE_GM = Boolean.parseBoolean(p.getProperty("FarmHwidIgnoreGM", "True"));
		FARM_HWID_UNBIND_COOLDOWN_HOURS = Math.max(0, Integer.parseInt(p.getProperty("FarmHwidUnbindCooldownHours", "168")));
	}
	
	private static final void loadAutoFarm()
	{
		final ExProperties p = initProperties(AUTOFARM_FILE);
		AUTOFARM_ENABLED = p.getProperty("EnableAutoFarm", true);
		
		AUTOFARM_TIME_ENABLED = p.getProperty("EnableAutoFarmTime", false);
		AUTOFARM_FREE_MINUTES = p.getProperty("AutoFarmFreeMinutes", 120);
		AUTOFARM_VIP_MINUTES = p.getProperty("AutoFarmVipMinutes", 180);
		AUTOFARM_TIME_ITEM_ID = p.getProperty("AutoFarmTimeItemId", 10005);
		AUTOFARM_TIME_ITEM_COUNT = p.getProperty("AutoFarmTimeItemCount", 1);
		AUTOFARM_TIME_ITEM_MINUTES = p.getProperty("AutoFarmTimeItemMinutes", 30);
		AUTOFARM_RESET_HOUR = p.getProperty("AutoFarmResetHour", 0);
		
		AUTOFARM_SHOW_TITLE = p.getProperty("AutoFarmShowTitle", true);
		AUTOFARM_TITLE_TEXT = p.getProperty("AutoFarmTitleText", "Auto Farm");
		AUTOFARM_AURA = p.getProperty("AutoFarmAura", 1);
	}
	
	public static boolean isDropAllowedByLevel(int playerLevel, int npcLevel)
	{
		return DROP_MAX_LEVEL_DIFFERENCE <= 0 || playerLevel - npcLevel <= DROP_MAX_LEVEL_DIFFERENCE;
	}
	
	private static Map<Integer, Integer> parseItemAmountMap(String raw)
	{
		final Map<Integer, Integer> map = new HashMap<>();
		if (raw == null || raw.trim().isEmpty())
			return map;
		
		for (String part : raw.split(";"))
		{
			part = part.trim();
			if (part.isEmpty())
				continue;
			
			final String[] split = part.split(",");
			if (split.length < 2)
				continue;
			
			try
			{
				map.put(Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim()));
			}
			catch (NumberFormatException e)
			{
				// skip bad entry
			}
		}
		return map;
	}
	
	/**
	 * Parses random rewards: itemId,min,max[,chance]
	 */
	public static List<RewardHolder> parseReward(ExProperties propertie, String configName)
	{
		final List<RewardHolder> auxReturn = new ArrayList<>();
		final String aux = propertie.getProperty(configName, "").trim();
		if (aux.isEmpty())
			return auxReturn;
		
		for (String randomReward : aux.split(";"))
		{
			randomReward = randomReward.trim();
			if (randomReward.isEmpty())
				continue;
			
			final String[] infos = randomReward.split(",");
			try
			{
				if (infos.length >= 4)
					auxReturn.add(new RewardHolder(Integer.parseInt(infos[0].trim()), Integer.parseInt(infos[1].trim()), Integer.parseInt(infos[2].trim()), Integer.parseInt(infos[3].trim())));
				else if (infos.length == 3)
					auxReturn.add(new RewardHolder(Integer.parseInt(infos[0].trim()), Integer.parseInt(infos[1].trim()), Integer.parseInt(infos[2].trim())));
				else
					LOGGER.warn(StringUtil.concat("parseReward: invalid entry -> \"", randomReward, "\" in ", configName, " (expected itemId,min,max[,chance])"));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn(StringUtil.concat("parseReward: invalid entry -> \"", randomReward, "\" in ", configName));
			}
		}
		return auxReturn;
	}
	
	/**
	 * Parses static event rewards: itemId,cantidad;itemId2,cantidad2
	 * If extra values are present, only itemId and cantidad are used.
	 */
	public static List<RewardHolder> parseStaticReward(ExProperties propertie, String configName)
	{
		final List<RewardHolder> auxReturn = new ArrayList<>();
		final String aux = propertie.getProperty(configName, "").trim();
		if (aux.isEmpty())
			return auxReturn;
		
		for (String entry : aux.split(";"))
		{
			entry = entry.trim();
			if (entry.isEmpty())
				continue;
			
			final String[] infos = entry.split(",");
			if (infos.length < 2)
			{
				LOGGER.warn(StringUtil.concat("parseStaticReward: invalid entry -> \"", entry, "\" in ", configName, " (expected itemId,cantidad)"));
				continue;
			}
			
			try
			{
				final int itemId = Integer.parseInt(infos[0].trim());
				final int amount = Integer.parseInt(infos[1].trim());
				if (infos.length > 2)
					LOGGER.warn(StringUtil.concat("parseStaticReward: extra values ignored -> \"", entry, "\" in ", configName, " (using itemId,cantidad only)"));
				
				auxReturn.add(new RewardHolder(itemId, amount, amount));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn(StringUtil.concat("parseStaticReward: invalid entry -> \"", entry, "\" in ", configName));
			}
		}
		return auxReturn;
	}
	
		private static final void loadRuneSystem(){
		// runes settings
		ExProperties runes = initProperties(RUNE_FILE);
		RUNE_1_ID = Integer.valueOf(runes.getProperty("Rune_1_ID", "0"));
		RUNE_1_EXP = runes.getProperty("Rune_1_EXP", 1.);
		RUNE_1_SP = runes.getProperty("Rune_1_SP", 1.);
		RUNE_1_ADENA = runes.getProperty("Rune_1_ADENA", 1.);
		RUNE_1_SPOIL = runes.getProperty("Rune_1_SPOIL", 1.);
		RUNE_1_DROP = runes.getProperty("Rune_1_DROP", 1.);
		RUNE_1_RAID_DROP = runes.getProperty("Rune_1_RAID_DROP", 1.);
		RUNE_1_ENCHANT = runes.getProperty("Rune_1_ENCHANT", 1.);
		RUNE_1_AUGMENT_SKILL = runes.getProperty("Rune_1_AUGMENT_SKILL", 1.);
		
		RUNE_2_ID = Integer.valueOf(runes.getProperty("Rune_2_ID", "0"));
		RUNE_2_EXP = runes.getProperty("Rune_2_EXP", 1.);
		RUNE_2_SP = runes.getProperty("Rune_2_SP", 1.);
		RUNE_2_ADENA = runes.getProperty("Rune_2_ADENA", 1.);
		RUNE_2_SPOIL = runes.getProperty("Rune_2_SPOIL", 1.);
		RUNE_2_DROP = runes.getProperty("Rune_2_DROP", 1.);
		RUNE_2_RAID_DROP = runes.getProperty("Rune_2_RAID_DROP", 1.);
		RUNE_2_ENCHANT = runes.getProperty("Rune_2_ENCHANT", 1.);
		RUNE_2_AUGMENT_SKILL = runes.getProperty("Rune_2_AUGMENT_SKILL", 1.);
		
		RUNE_3_ID = Integer.valueOf(runes.getProperty("Rune_3_ID", "0"));
		RUNE_3_EXP = runes.getProperty("Rune_3_EXP", 1.);
		RUNE_3_SP = runes.getProperty("Rune_3_SP", 1.);
		RUNE_3_ADENA = runes.getProperty("Rune_3_ADENA", 1.);
		RUNE_3_SPOIL = runes.getProperty("Rune_3_SPOIL", 1.);
		RUNE_3_DROP = runes.getProperty("Rune_3_DROP", 1.);
		RUNE_3_RAID_DROP = runes.getProperty("Rune_3_RAID_DROP", 1.);
		RUNE_3_ENCHANT = runes.getProperty("Rune_3_ENCHANT", 1.);
		RUNE_3_AUGMENT_SKILL = runes.getProperty("Rune_3_AUGMENT_SKILL", 1.);
	}
	
		private static final void loadPvpSystem(){
		// Pvp settings
		ExProperties pvp = initProperties(PVP_FILE);
		ENABLE_PVP_REWARD = pvp.getProperty("EnablePvPSystem", false);
		ENABLE_PVP_REWARD_IP = pvp.getProperty("EnablePvPSystemIP", false);
		PVP_REWARD_ID = Integer.valueOf(pvp.getProperty("PvPRewardID", "0"));
		PVP_REWARD_COUNT = Integer.valueOf(pvp.getProperty("PvPRewardCount", "0"));
		ENABLE_PK_REWARD = pvp.getProperty("EnablePKSystem", false);
		ENABLE_PK_REWARD_IP = pvp.getProperty("EnablePKSystemIP", false);
		PK_REWARD_ID = Integer.valueOf(pvp.getProperty("PKRewardID", "0"));
		PK_REWARD_COUNT = Integer.valueOf(pvp.getProperty("PKRewardCount", "0"));
		ENABLE_WAR_REWARD = pvp.getProperty("EnableWarSystem", false);
		WAR_REWARD_ID = Integer.valueOf(pvp.getProperty("WarRewardID", "0"));
		WAR_REWARD_COUNT = Integer.valueOf(pvp.getProperty("WarRewardCount", "0"));
		LEAVE_BUFFS_ON_DIE = Boolean.parseBoolean(pvp.getProperty("LeaveBuffsOnDie", "True"));
		EVENTS_KEEP_BUFFS_ON_DIE = Boolean.parseBoolean(pvp.getProperty("KeepBuffsOnDieInEvents", "True"));
	}
	
	private static final void loadAPots()
	{
		final ExProperties apots = initProperties(AUTO_POTS_FILE);
		CP_POT_CD = Integer.parseInt(apots.getProperty("CP_POT_CD", "1000"));
		HP_POT_CD = Integer.parseInt(apots.getProperty("HP_POT_CD", "1000"));
		MP_POT_CD = Integer.parseInt(apots.getProperty("MP_POT_CD", "1000"));
		CP_REQUIRED = apots.getProperty("CP_REQUIRED", 0.85);
		HP_REQUIRED = apots.getProperty("HP_REQUIRED", 0.85);
		MP_REQUIRED = apots.getProperty("MP_REQUIRED", 0.85);
	}
			
	private static final void loadRebirthystem(){
		// Rebirth settings
		ExProperties rebirth = initProperties(REBIRTH_FILE);
		ENABLE_REBIRTH_SYSTEM = rebirth.getProperty("EnableRebirthSystem", false);
		REBIRTH_REWARD_ID = Integer.valueOf(rebirth.getProperty("RebirthRewardID", "0"));
		REBIRTH_REWARD_COUNT = Integer.valueOf(rebirth.getProperty("RebirthRewardCount", "0"));
		REBIRTH_PRICE_ID = Integer.valueOf(rebirth.getProperty("RebirthPriceID", "0"));
		REBIRTH_PRICE_COUNT = Integer.valueOf(rebirth.getProperty("RebirthPriceCount", "0"));
		REBIRTH_MIN_SKILL_ID = Integer.valueOf(rebirth.getProperty("MinSkillID", "0"));
		REBIRTH_MAX_SKILL_ID = Integer.valueOf(rebirth.getProperty("MaxSkillID", "0"));
		REBIRTH_DELEVEL = Integer.valueOf(rebirth.getProperty("Delevel", "40"));
		REBIRTH_LEVEL_MIN = Integer.valueOf(rebirth.getProperty("MinLevel", "80"));
		REBIRTH_COUNT_ID = Integer.valueOf(rebirth.getProperty("RebirthCountID", "0"));
		REBIRTH_COUNT = Integer.valueOf(rebirth.getProperty("RebirthCount", "0"));
   }

	private static final void loadAioSystem(){
		// AIO settings
		ExProperties aio = initProperties(AIO_FILE);
		ENABLE_AIO_SYSTEM = aio.getProperty("EnableAioSystem", false);
		CHANGE_AIO_NAME = aio.getProperty("ChangeAioName", true);
		// Valor exacto del properties (sin inventar espacios). Default Mega: [AIO]
		AIO_NAME_PREFIX = aio.getProperty("AioNamePrefix", "[AIO]");
		if (AIO_NAME_PREFIX != null)
			AIO_NAME_PREFIX = AIO_NAME_PREFIX.trim();
		ALLOW_AIO_NCOLOR = aio.getProperty("AllowAioNameColor", false);
        AIO_NCOLOR = Integer.decode("0x" + aio.getProperty("AioNameColor", "88AA88"));
		ALLOW_AIO_TCOLOR = aio.getProperty("AllowAioTitleColor", false);
		AIO_TCOLOR = Integer.decode("0x" + aio.getProperty("AioTitleColor", "88AA88"));
		ALLOW_AIO_TELEPORT = aio.getProperty("AllowAIOTeleport", false);
		ENABLE_AIO_FREE_TELEPORT = aio.getProperty("EnableFreeTeleport", false);
		ENABLE_AIO_COMMAND = aio.getProperty("EnableAioCommand", true);
		AIO_HTML_FILE = aio.getProperty("AioHtmlFile", "data/html/mods/aio/status.htm");
		AIO_DATE_FORMAT = aio.getProperty("AioDateFormat", "dd/MM/yyyy HH:mm");
		                  
		ALLOW_AIO_EQUIP_ITEM = Boolean.parseBoolean(aio.getProperty("AllowItems", "False"));
		ALLOW_AIO_IN_EVENTS = aio.getProperty("AllowAioInEvents", false);
		if (ALLOW_AIO_EQUIP_ITEM) {
			String[] propertySplit4 = aio.getProperty("AioItems", "57,0").split(";");
			AIO_ITEMS.clear();
			for (String reward : propertySplit4) {
				String[] rewardSplit = reward.split(",");
				if (rewardSplit.length != 2) {
					LOGGER.info("AioItems[Config.load()]: invalid config property -> AioItems \"" + reward + "\"");
				} else {
					try {
						AIO_ITEMS.add(new int[] { Integer.parseInt(rewardSplit[0]), 
							Integer.parseInt(rewardSplit[1]) });
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
						if (!reward.isEmpty())
							LOGGER.info("AioItems[Config.load()]: invalid config property -> AioItems \"" + reward + "\""); 
					} 
				} 
			} 
		} 		                  
		if (ENABLE_AIO_SYSTEM) // create map if system is enabled
		{
		String[] AioSkillsSplit = aio.getProperty("AioSkills", "").split(";");
		AIO_SKILLS = new HashMap<>(AioSkillsSplit.length);
		for (String skill : AioSkillsSplit)
		{
               String[] skillSplit = skill.split(",");
               if (skillSplit.length != 2)
            	   LOGGER.info("[Aio System]: invalid config property in AIO.properties -> AioSkills \"" + skill + "\"");
	              else
               {
                   try
                   {
                       AIO_SKILLS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
                   }
                   catch (NumberFormatException nfe)
                   {
                       if (!skill.equals(""))
                    	   LOGGER.info("[Aio System]: invalid config property in AIO.properties -> AioSkills \"" + skillSplit[0] + "\"" + skillSplit[1]);
                   }
               }
           }
       }
		
		AIO_COIN_DAYS.clear();
		registerAioCoin(aio.getProperty("Aio_Coin1", 0), aio.getProperty("Aio_1", 0), "Aio_Coin1/Aio_1");
		registerAioCoin(aio.getProperty("Aio_Coin2", 0), aio.getProperty("Aio_2", 0), "Aio_Coin2/Aio_2");
		registerAioCoin(aio.getProperty("Aio_Coin3", 0), aio.getProperty("Aio_3", 0), "Aio_Coin3/Aio_3");
   }
	
	private static void registerAioCoin(int itemId, int days, String label)
	{
		if (itemId <= 0 || days <= 0)
		{
			if (itemId > 0 || days > 0)
				LOGGER.warn("[Aio] " + label + " incompleto (itemId=" + itemId + ", days=" + days + ").");
			return;
		}
		AIO_COIN_DAYS.put(itemId, days);
	}
	
	/**
	 * Loads DonationManager settings.<br>
	 * Such as DonationManager, inventory/warehouse, enchant, augmentation, DonationManager, party, DonationManager, petition, skill learn.
	 */
	private static final void loadDonationManager()
	{
		final ExProperties donationmanager = initProperties(DONATION_MANAGER);
		ITEMSKIN = Integer.valueOf(donationmanager.getProperty("ItemSkin", "0"));
		ITEMSEXO = Integer.valueOf(donationmanager.getProperty("ItemSexo", "0"));
		ITEMHERO = Integer.valueOf(donationmanager.getProperty("ItemHero", "0"));
		ITEMNOBLES = Integer.valueOf(donationmanager.getProperty("ItemNobles", "0"));
		ITEMAIO = Integer.valueOf(donationmanager.getProperty("ItemAio", "0"));
		ITEMVIP = Integer.valueOf(donationmanager.getProperty("ItemVip", "0"));
		ITEMVIP1 = Integer.valueOf(donationmanager.getProperty("ItemVip1", "0"));
		ITEMVIP2 = Integer.valueOf(donationmanager.getProperty("ItemVip2", "0"));
		ITEMCLANREP = Integer.valueOf(donationmanager.getProperty("ItemClanRep", "0"));
		ITEMCLANLVL = Integer.valueOf(donationmanager.getProperty("ItemClanLvL", "0"));
		ITEMARGU = Integer.valueOf(donationmanager.getProperty("ItemArgu", "0"));
		ITEMCLANSKILL = Integer.valueOf(donationmanager.getProperty("ItemClanSkill", "0"));
		ITEMNAME = Integer.valueOf(donationmanager.getProperty("ItemName", "0"));
		ITEMNAMECOLOR = Integer.valueOf(donationmanager.getProperty("ItemNameColor", "0"));
		ITEMTITLECOLOR = Integer.valueOf(donationmanager.getProperty("ItemTitleColor", "0"));
		CANTIDADSKIN = Integer.valueOf(donationmanager.getProperty("CantidadSkin", "0"));
		CANTIDADSEXO = Integer.valueOf(donationmanager.getProperty("CantidadSexo", "0"));
		CANTIDADHERO = Integer.valueOf(donationmanager.getProperty("CantidadHero", "0"));
		CANTIDADNOBLES = Integer.valueOf(donationmanager.getProperty("CantidadNobles", "0"));
		CANTIDADAIO = Integer.valueOf(donationmanager.getProperty("CantidadAio", "0"));
		CANTIDADVIP = Integer.valueOf(donationmanager.getProperty("CantidadVip", "0"));
		CANTIDADVIP1 = Integer.valueOf(donationmanager.getProperty("CantidadVip1", "0"));
		CANTIDADVIP2 = Integer.valueOf(donationmanager.getProperty("CantidadVip2", "0"));
		VIP_DIAS = Integer.valueOf(donationmanager.getProperty("VipDias", "0"));
		VIP_DIAS1 = Integer.valueOf(donationmanager.getProperty("VipDias1", "0"));
		VIP_DIAS2 = Integer.valueOf(donationmanager.getProperty("VipDias2", "0"));
		CANTIDADCLANREP = Integer.valueOf(donationmanager.getProperty("CantidadClanRep", "0"));		
		CANTIDADCLANLVL = Integer.valueOf(donationmanager.getProperty("CantidadClanLvL", "0"));
		CANTIDADARGU = Integer.valueOf(donationmanager.getProperty("CantidadArgu", "0"));
		DONATE_ALCHEMY_ITEM = donationmanager.getProperty("ItemSocketPerfecto", 10001);
		DONATE_ALCHEMY_COUNT = donationmanager.getProperty("CantidadSocketPerfecto", 5);
		DONATE_ELEMENT_ITEM = donationmanager.getProperty("ItemElementoPerfecto", 10001);
		DONATE_ELEMENT_COUNT = donationmanager.getProperty("CantidadElementoPerfecto", 5);
		DONATE_SKILL_ENCHANT_ITEM = donationmanager.getProperty("ItemSkillEnchant", 10001);
		DONATE_SKILL_ENCHANT_COUNT = donationmanager.getProperty("CantidadSkillEnchant", 10);
		CANTIDADCLANSKILL = Integer.valueOf(donationmanager.getProperty("CantidadClanSkill", "0"));
		CANTIDADNAME = Integer.valueOf(donationmanager.getProperty("CantidadName", "0"));
		CANTIDADNAMECOLOR = Integer.valueOf(donationmanager.getProperty("CantidadNameColor", "0"));
		CANTIDADTITLECOLOR = Integer.valueOf(donationmanager.getProperty("CantidadTitleColor", "0"));
		CANTIDAD_VIP_1D = Integer.valueOf(donationmanager.getProperty("CantidadVip1d", "5"));
		CANTIDAD_VIP_30D = Integer.valueOf(donationmanager.getProperty("CantidadVip30d", "30"));
		CANTIDAD_VIP_90D = Integer.valueOf(donationmanager.getProperty("CantidadVip90d", "70"));
		CANTIDAD_HERO_1D = Integer.valueOf(donationmanager.getProperty("CantidadHero1d", "8"));
		CANTIDAD_HERO_30D = Integer.valueOf(donationmanager.getProperty("CantidadHero30d", "50"));
		CANTIDAD_HERO_90D = Integer.valueOf(donationmanager.getProperty("CantidadHero90d", "120"));
		CANTIDAD_AIO_1D = Integer.valueOf(donationmanager.getProperty("CantidadAio1d", "10"));
		CANTIDAD_AIO_30D = Integer.valueOf(donationmanager.getProperty("CantidadAio30d", "60"));
		CANTIDAD_AIO_90D = Integer.valueOf(donationmanager.getProperty("CantidadAio90d", "150"));
		DIAS_VIP_1D = Integer.valueOf(donationmanager.getProperty("DiasVip1d", "1"));
		DIAS_VIP_30D = Integer.valueOf(donationmanager.getProperty("DiasVip30d", "30"));
		DIAS_VIP_90D = Integer.valueOf(donationmanager.getProperty("DiasVip90d", "90"));
		DIAS_HERO_1D = Integer.valueOf(donationmanager.getProperty("DiasHero1d", "1"));
		DIAS_HERO_30D = Integer.valueOf(donationmanager.getProperty("DiasHero30d", "30"));
		DIAS_HERO_90D = Integer.valueOf(donationmanager.getProperty("DiasHero90d", "90"));
		DIAS_AIO_1D = Integer.valueOf(donationmanager.getProperty("DiasAio1d", "1"));
		DIAS_AIO_30D = Integer.valueOf(donationmanager.getProperty("DiasAio30d", "30"));
		DIAS_AIO_90D = Integer.valueOf(donationmanager.getProperty("DiasAio90d", "90"));
		COMBO_ENABLED = Boolean.parseBoolean(donationmanager.getProperty("ComboEnabled", "True"));
		CANTIDAD_COMBO_30D = Integer.valueOf(donationmanager.getProperty("CantidadCombo30d", "300"));
		DIAS_COMBO_30D = Integer.valueOf(donationmanager.getProperty("DiasCombo30d", "30"));
		COMBO_GIVE_VIP = Boolean.parseBoolean(donationmanager.getProperty("ComboGiveVip", "True"));
		COMBO_GIVE_HERO = Boolean.parseBoolean(donationmanager.getProperty("ComboGiveHero", "True"));
		COMBO_GIVE_NOBLESS = Boolean.parseBoolean(donationmanager.getProperty("ComboGiveNobless", "True"));
		STARTER_VIP_ENABLED = Boolean.parseBoolean(donationmanager.getProperty("StarterVipEnabled", "True"));
		STARTER_VIP_HOURS = Integer.valueOf(donationmanager.getProperty("StarterVipHours", "12"));
		GB_STATUS_ENABLED = Boolean.parseBoolean(donationmanager.getProperty("GbStatusEnabled", "True"));
		ITEM_HERO_GB = Integer.valueOf(donationmanager.getProperty("ItemHeroGb", "0"));
		CANTIDAD_HERO_GB = Integer.valueOf(donationmanager.getProperty("CantidadHeroGb", "50"));
		DIAS_HERO_GB = Integer.valueOf(donationmanager.getProperty("DiasHeroGb", "30"));
		ITEM_AIO_GB = Integer.valueOf(donationmanager.getProperty("ItemAioGb", "0"));
		CANTIDAD_AIO_GB = Integer.valueOf(donationmanager.getProperty("CantidadAioGb", "50"));
		DIAS_AIO_GB = Integer.valueOf(donationmanager.getProperty("DiasAioGb", "30"));
	}
		
 	/**
	 * Loads protect settings.<br>
	 * Bot Prevention.
	 */
	private static final void loadProtect()
	{
		final ExProperties protect = initProperties(PROTECT_FILE);
		BPS_BOTS_PREVENTION = protect.getProperty("EnableBotsPrevention", false);
		BPS_ALLOW_INCORRECT_CHOICE = protect.getProperty("AllowIncorrectChoice", false);
		BPS_KILLS_COUNTER = protect.getProperty("KillsCounter", 60);
		BPS_KILLS_COUNTER_RANDOMIZATION = protect.getProperty("KillsCounterRandomization", 50);
		BPS_VALIDATION_TIME = protect.getProperty("ValidationTime", 60);
		BPS_PUNISHMENT = protect.getProperty("Punishment", 0);
		BPS_PUNISHMENT_TIME = protect.getProperty("PunishmentTime", 60);

		BPS_CHECK_BISHOPS = protect.getProperty("Bishops", false);
		BPS_CHECK_ELDERS = protect.getProperty("Elders", false);
		BPS_CHECK_PROPHETS = protect.getProperty("Prophets", false);
		BPS_CHECK_BLADEDANCERS = protect.getProperty("Bladedancers", false);
		BPS_CHECK_SWORDSINGERS = protect.getProperty("Swordsingers", false);
		BPS_CHECK_WARCRYERS = protect.getProperty("Warcryers", false);
		BPS_CHECK_OVERLORDS = protect.getProperty("Overlords", false);
		BPS_ENABLE_REWARD = protect.getProperty("Rewards", false);
		BPS_REWARD_ID = protect.getProperty("Item", 0);
		BPS_REWARD_AMOUNT = protect.getProperty("Amount", 0);
		
		MULTIBOX_PROTECTION_ENABLED = protect.getProperty("MultiboxProtectionEnabled", false);
		MULTIBOX_PROTECTION_CLIENTS_PER_PC = protect.getProperty("ClientsPerPc", 2);
		MULTIBOX_PROTECTION_PUNISH = protect.getProperty("MultiboxPunish", 2);
		TIME_MULTIBOX = protect.getProperty("Disconect_Multibox", 2);
	}

	/**
	 * Loads Vip settings.
	 */
	private static final void loadVip()
	{
		ExProperties vip = initProperties(VIP_FILE);
		VIP_RATE_XP = vip.getProperty("VipXpRates", 1.5);
		VIP_RATE_SP = vip.getProperty("VipSpRates", 1.5);
		VIP_ADENA_RATES = vip.getProperty("VipAdenaDrop", 1.5);
		VIP_SPOIL_RATES = vip.getProperty("VipSpoilRates", 1.5);
		VIP_DROP_RATES = vip.getProperty("VipDrop", 1.5);
		VIP_RATE_DROP_ITEMS_BY_RAID = vip.getProperty("VipRaidDrop", 1.);
		VIP_ENCHANT_RATES = vip.getProperty("VipEnchantRates", 1.);
		VIP_AUGMENT_SKILL_RATES = vip.getProperty("VipAugmentSkillRates", 1.);
		ENABLE_VIP_CHAT_PREFIX = vip.getProperty("EnableVipChatPrefix", true);
		VIP_CHAT_PREFIX = vip.getProperty("VipChatPrefix", "[VIP] ");
		VIP_CHAT_GLOBAL = vip.getProperty("VipChatGlobal", true);
		ENABLE_VIP_COMMAND = vip.getProperty("EnableVipCommand", true);
		VIP_HTML_FILE = vip.getProperty("VipHtmlFile", "data/html/mods/vip/status.htm");
		VIP_DATE_FORMAT = vip.getProperty("VipDateFormat", "dd/MM/yyyy HH:mm");
		
		ALLOW_VIP_TCOLOR = vip.getProperty("AllowVipTitleColor", false);
		VIP_TCOLOR = Integer.decode("0x" + vip.getProperty("VipTitleColor", "FFD700"));
		
		VIP_COIN_DAYS.clear();
		registerVipCoin(vip.getProperty("Vip_Coin1", 0), vip.getProperty("Vip_1", 0), "Vip_Coin1/Vip_1");
		registerVipCoin(vip.getProperty("Vip_Coin2", 0), vip.getProperty("Vip_2", 0), "Vip_Coin2/Vip_2");
		registerVipCoin(vip.getProperty("Vip_Coin3", 0), vip.getProperty("Vip_3", 0), "Vip_Coin3/Vip_3");
	}
	
	private static void registerVipCoin(int itemId, int days, String label)
	{
		if (itemId <= 0 || days <= 0)
		{
			if (itemId > 0 || days > 0)
				LOGGER.warn("[Vip] " + label + " incompleto (itemId=" + itemId + ", days=" + days + ").");
			return;
		}
		VIP_COIN_DAYS.put(itemId, days);
	}
	
	private static final void loadAutoSpoil()
	{
		ExProperties autoSpoil = initProperties(AUTOSPOIL_FILE);
		ENABLE_AUTOSPOIL_SYSTEM = autoSpoil.getProperty("EnableAutoSpoilSystem", true);
		AUTOSPOIL_ITEM_ID = autoSpoil.getProperty("AutoSpoil_ItemId", 10152);
	}
	
	/**
	 * Loads Hero (custom/coins) settings.
	 */
	private static final void loadHero()
	{
		ExProperties hero = initProperties(HERO_FILE);
		ENABLE_HERO_COMMAND = hero.getProperty("EnableHeroCommand", true);
		HERO_HTML_FILE = hero.getProperty("HeroHtmlFile", "data/html/mods/hero/status.htm");
		HERO_DATE_FORMAT = hero.getProperty("HeroDateFormat", "dd/MM/yyyy HH:mm");
		
		HERO_COIN_DAYS.clear();
		registerHeroCoin(hero.getProperty("Hero_Coin1", 0), hero.getProperty("Hero_1", 0), "Hero_Coin1/Hero_1");
		registerHeroCoin(hero.getProperty("Hero_Coin2", 0), hero.getProperty("Hero_2", 0), "Hero_Coin2/Hero_2");
		registerHeroCoin(hero.getProperty("Hero_Coin3", 0), hero.getProperty("Hero_3", 0), "Hero_Coin3/Hero_3");
	}
	
	private static void registerHeroCoin(int itemId, int days, String label)
	{
		if (itemId <= 0 || days <= 0)
		{
			if (itemId > 0 || days > 0)
				LOGGER.warn("[Hero] " + label + " incompleto (itemId=" + itemId + ", days=" + days + ").");
			return;
		}
		HERO_COIN_DAYS.put(itemId, days);
	}
	
	/**
 * Loads tournament settings.
 */
private static final void loadTournament()
{
	final ExProperties tournament = initProperties(Config.TOURNAMENT_FILE);
	TOURNAMENT_EVENT_START = tournament.getProperty("TournamentStartOn", false);
	TOURNAMENT_EVENT_TIME = tournament.getProperty("TournamentAutoEvent", false);
	TOURNAMENT_EVENT_SUMMON = tournament.getProperty("TournamentSummon", false);
	
	TOUR_GRADE_A_1X1 = tournament.getProperty("Tour_gradeS_1x1", false);
	ALT_TOUR_ENCHANT_LIMIT = Integer.parseInt(tournament.getProperty("Tour_MaxEnchant", "-1"));
	
	TOURNAMENT_EVENT_INTERVAL_BY_TIME_OF_DAY = tournament.getProperty("TournamentStartTime", "20:00").split(",");
	TOURNAMENT_TIME = Integer.parseInt(tournament.getProperty("TournamentEventTime", "1"));
	
	TOUR_AURA_TEAM1 = Integer.parseInt(tournament.getProperty("Team_1_aura", "1"));
	TOUR_AURA_TEAM2 = Integer.parseInt(tournament.getProperty("Team_2_aura", "2"));
	
	TITLE_COLOR_TEAM1 = tournament.getProperty("TitleColorTeam_1", "FFFFFF");
	TITLE_COLOR_TEAM2 = tournament.getProperty("TitleColorTeam_2", "FFFFFF");
	
	MSG_TEAM1 = tournament.getProperty("TitleTeam_1", "Team [1]");
	MSG_TEAM2 = tournament.getProperty("TitleTeam_2", "Team [2]");
	
	Allow_Same_HWID_On_Tournament = Boolean.parseBoolean(tournament.getProperty("Allow_Same_HWID_On_Tournament", "false"));
	
	ARENA_NPC = Integer.parseInt(tournament.getProperty("NPCRegister", "1"));
	
	NPC_locx = Integer.parseInt(tournament.getProperty("Locx", "1"));
	NPC_locy = Integer.parseInt(tournament.getProperty("Locy", "1"));
	NPC_locz = Integer.parseInt(tournament.getProperty("Locz", "1"));
	NPC_Heading = Integer.parseInt(tournament.getProperty("Heading", "1"));
	
	NPC_locx2 = Integer.parseInt(tournament.getProperty("Locx2", "1"));
	NPC_locy2 = Integer.parseInt(tournament.getProperty("Locy2", "1"));
	NPC_locz2 = Integer.parseInt(tournament.getProperty("Locz2", "1"));
	NPC_Heading2 = Integer.parseInt(tournament.getProperty("Heading2", "1"));
	
	Tournament_locx = Integer.parseInt(tournament.getProperty("TournamentLocx", "1"));
	Tournament_locy = Integer.parseInt(tournament.getProperty("TournamentLocy", "1"));
	Tournament_locz = Integer.parseInt(tournament.getProperty("TournamentLocz", "1"));
	
	ALLOW_1X1_REGISTER = Boolean.parseBoolean(tournament.getProperty("Allow1x1Register", "true"));
	
	ARENA_MESSAGE_ENABLED = Boolean.parseBoolean(tournament.getProperty("ScreenArenaMessageEnable", "false"));
	ARENA_MESSAGE_TEXT = tournament.getProperty("ScreenArenaMessageText", "Welcome to L2J server!");
	ARENA_MESSAGE_TIME = Integer.parseInt(tournament.getProperty("ScreenArenaMessageTime", "10")) * 1000;
	
	final String[] arenaLocs1x1 = tournament.getProperty("ArenasLoc1x1", "").split(";");
	ARENA_EVENT_COUNT_1X1 = arenaLocs1x1.length;
	ARENA_EVENT_LOCS_1X1 = new int[ARENA_EVENT_COUNT_1X1][3];
	for (int i = 0; i < ARENA_EVENT_COUNT_1X1; i++)
	{
		final String[] locSplit = arenaLocs1x1[i].split(",");
		for (int j = 0; j < 3; j++)
			ARENA_EVENT_LOCS_1X1[i][j] = Integer.parseInt(locSplit[j].trim());
	}
	
	ARENA_PVP_AMOUNT = tournament.getProperty("ArenaPvpJoin", 10);
	
	ARENA_REWARD_ID_1X1 = tournament.getProperty("ArenaRewardId1x1", 57);
	ARENA_WIN_REWARD_COUNT_1X1 = tournament.getProperty("ArenaWinRewardCount1x1", 1);
	ARENA_VIP_WIN_REWARD_COUNT_1X1 = tournament.getProperty("ArenaVipWinRewardCount1x1", 1);
	ARENA_LOST_REWARD_COUNT_1X1 = tournament.getProperty("ArenaLostRewardCount1x1", 1);
	
	ARENA_CHECK_INTERVAL_1X1 = tournament.getProperty("ArenaBattleCheckInterval1x1", 15) * 1000;
	ARENA_CALL_INTERVAL_1X1 = tournament.getProperty("ArenaBattleCallInterval1x1", 60);
	ARENA_WAIT_INTERVAL_1X1 = tournament.getProperty("ArenaBattleWaitInterval1x1", 20);
	
	TOURNAMENT_ID_RESTRICT = tournament.getProperty("ItemsRestriction");
	
	TOURNAMENT_LISTID_RESTRICT = new ArrayList<>();
	for (String id : TOURNAMENT_ID_RESTRICT.split(","))
		TOURNAMENT_LISTID_RESTRICT.add(Integer.parseInt(id));
	
	ARENA_SKILL_PROTECT = Boolean.parseBoolean(tournament.getProperty("ArenaSkillProtect", "false"));
	
	for (String id : tournament.getProperty("ArenaDisableSkillList", "0").split(","))
		ARENA_SKILL_LIST.add(Integer.parseInt(id));
	
	for (String id : tournament.getProperty("DisableSkillList", "0").split(","))
		ARENA_DISABLE_SKILL_LIST_PERM.add(Integer.parseInt(id));
	
	for (String id : tournament.getProperty("ArenaDisableSkillList_noStart", "0").split(","))
		ARENA_DISABLE_SKILL_LIST.add(Integer.parseInt(id));
	
	for (String id : tournament.getProperty("ArenaStopSkillList", "0").split(","))
		ARENA_STOP_SKILL_LIST.add(Integer.parseInt(id));
	
	ARENA_PROTECT = Boolean.parseBoolean(tournament.getProperty("ArenaProtect", "true"));
	NAME_TOUR = tournament.getProperty("NameTournament", "Tournament:");
}

	private static final void loadCustomOly()
	{
		final ExProperties p = initProperties(CUSTOM_OLY_FILE);
		CUSTOM_OLY_ENABLED = p.getProperty("Enabled", false);
		CUSTOM_OLY_NAME = p.getProperty("EventName", "Grand Duel");
		CUSTOM_OLY_PERIOD_DAYS = p.getProperty("PeriodDays", 7);
		CUSTOM_OLY_VALIDATION_DAYS = p.getProperty("ValidationDays", 1);
		CUSTOM_OLY_HERO_GIVE_HOUR = p.getProperty("HeroGiveHour", "18:00");
		CUSTOM_OLY_HERO_DAYS = p.getProperty("HeroDays", 7);
		CUSTOM_OLY_AUTO_START = p.getProperty("AutoStartSeason", true);
		CUSTOM_OLY_HERO_POINTS = p.getProperty("HeroPoints", 300);
		CUSTOM_OLY_HERO_SLOTS = p.getProperty("HeroSlotsPerClass", 5);
		CUSTOM_OLY_RANK_SLOTS = p.getProperty("RankSlots", 5);
		CUSTOM_OLY_RANK_MIN_POINTS = p.getProperty("RankMinPoints", 50);
		CUSTOM_OLY_STEAL_POINTS = p.getProperty("StealPoints", 5);
		CUSTOM_OLY_STEAL_PER_PLAYER = p.getProperty("StealPerPlayer", true);
		CUSTOM_OLY_NPC_ID = p.getProperty("NpcId", 60041);
		CUSTOM_OLY_SPAWN_TIMES = p.getProperty("NpcSpawnTimes", "18:00,21:00").split(",");
		CUSTOM_OLY_DURATION_MINUTES = p.getProperty("NpcDurationMinutes", 60);
		CUSTOM_OLY_NPC_X = p.getProperty("NpcX", 82856);
		CUSTOM_OLY_NPC_Y = p.getProperty("NpcY", 148616);
		CUSTOM_OLY_NPC_Z = p.getProperty("NpcZ", -3464);
		CUSTOM_OLY_NPC_HEADING = p.getProperty("NpcHeading", 0);
		
		String[] arenaLocs = p.getProperty("ArenasLoc", "").split(";");
		CUSTOM_OLY_ARENAS = new int[Math.max(1, arenaLocs.length)][3];
		if (p.getProperty("ArenasLoc", "").isEmpty())
		{
			CUSTOM_OLY_ARENAS[0][0] = -114536;
			CUSTOM_OLY_ARENAS[0][1] = -213194;
			CUSTOM_OLY_ARENAS[0][2] = -3327;
		}
		else
		{
			CUSTOM_OLY_ARENAS = new int[arenaLocs.length][3];
			for (int i = 0; i < arenaLocs.length; i++)
			{
				String[] loc = arenaLocs[i].split(",");
				CUSTOM_OLY_ARENAS[i][0] = Integer.parseInt(loc[0].trim());
				CUSTOM_OLY_ARENAS[i][1] = Integer.parseInt(loc[1].trim());
				CUSTOM_OLY_ARENAS[i][2] = Integer.parseInt(loc[2].trim());
			}
		}
		
		CUSTOM_OLY_TEAM1_SPAWN = parsePoint(p.getProperty("Team1Spawn", ""));
		CUSTOM_OLY_TEAM2_SPAWN = parsePoint(p.getProperty("Team2Spawn", ""));
		CUSTOM_OLY_SPAWN_SPREAD = p.getProperty("TeamSpawnSpread", 40);
		
		CUSTOM_OLY_WAIT_SECONDS = p.getProperty("WaitSeconds", 20);
		CUSTOM_OLY_CHECK_MS = p.getProperty("CheckIntervalMs", 1000);
		CUSTOM_OLY_CALL_SECONDS = p.getProperty("CallIntervalSeconds", 3);
		CUSTOM_OLY_FIGHT_TIMEOUT = p.getProperty("FightTimeoutSeconds", 360);
		CUSTOM_OLY_ENCHANT_CAP = p.getProperty("EnchantCap", 16);
		CUSTOM_OLY_ALLOW_POTIONS = p.getProperty("AllowPotions", true);
		CUSTOM_OLY_ALLOW_SAME_HWID = p.getProperty("AllowSameHwid", false);
		CUSTOM_OLY_USE_INSTANCE = p.getProperty("UsePrivateInstance", true);
		CUSTOM_OLY_ALLOW_1X1 = p.getProperty("Allow1x1", true);
		CUSTOM_OLY_ALLOW_2X2 = p.getProperty("Allow2x2", true);
		CUSTOM_OLY_ALLOW_3X3 = p.getProperty("Allow3x3", true);
		
		CUSTOM_OLY_ALLOW_ITEMS.clear();
		CUSTOM_OLY_DENY_ITEMS.clear();
		parseIdList(p.getProperty("AllowItemIds", ""), CUSTOM_OLY_ALLOW_ITEMS);
		parseIdList(p.getProperty("DenyItemIds", ""), CUSTOM_OLY_DENY_ITEMS);
		
		CUSTOM_OLY_FIGHTER_BUFFS = parseBuffMap(p.getProperty("FighterBuffs", "1204,2;1086,1"));
		CUSTOM_OLY_MAGE_BUFFS = parseBuffMap(p.getProperty("MageBuffs", "1204,2"));
	}
	
	private static final void loadMedalEvent()
	{
		final ExProperties p = initProperties(MEDAL_EVENT_FILE);
		MEDAL_EVENT_ENABLED = p.getProperty("Enabled", true);
		MEDAL_EVENT_MEDAL_ID = p.getProperty("MedalId", 6392);
		MEDAL_EVENT_GLITTERING_ID = p.getProperty("GlitteringId", 6393);
		MEDAL_EVENT_MEDAL_CHANCE = p.getProperty("MedalChance", 15.0);
		MEDAL_EVENT_GLITTERING_CHANCE = p.getProperty("GlitteringChance", 0.8);
		MEDAL_EVENT_MIN_NPC_LEVEL = p.getProperty("MinNpcLevel", 1);
		MEDAL_EVENT_ALLOW_RAID = p.getProperty("AllowRaid", false);
		MEDAL_EVENT_ALLOW_MINIONS = p.getProperty("AllowMinions", false);
		MEDAL_EVENT_DROP_CUSTOM_MOBS = p.getProperty("DropOnCustomMobs", false);
		MEDAL_EVENT_TOSS_CHANCE = p.getProperty("TossWinChance", 50);
		
		String[] costs = p.getProperty("RankCosts", "5,10,20,40").split(",");
		MEDAL_EVENT_RANK_COST = new int[] { 5, 10, 20, 40 };
		for (int i = 0; i < Math.min(4, costs.length); i++)
			MEDAL_EVENT_RANK_COST[i] = Integer.parseInt(costs[i].trim());
		
		MEDAL_EVENT_PRIZES.clear();
		String prizes = p.getProperty("Prizes", "");
		if (prizes != null && !prizes.trim().isEmpty())
		{
			for (String row : prizes.split(";"))
			{
				String[] parts = row.trim().split(",");
				if (parts.length < 4)
					continue;
				MEDAL_EVENT_PRIZES.add(new int[] {
					Integer.parseInt(parts[0].trim()),
					Integer.parseInt(parts[1].trim()),
					Integer.parseInt(parts[2].trim()),
					Integer.parseInt(parts[3].trim())
				});
			}
		}
	}
	
	/**
	 * Loads custom profile buffer settings (aCis 409).
	 */
	private static final void loadBuffer()
	{
		final ExProperties buffer = initProperties(BUFFER_FILE);
		
		BUFFER_MAX_SCHEMES = buffer.getProperty("BufferMaxProfilesPerChar", 3);
		
		// Slots de buffs (estilo H5). Antes estaban en players.properties.
		MAX_BUFFS_AMOUNT = buffer.getProperty("MaxBuffsAmount", 36);
		MAX_DANCE_AMOUNT = buffer.getProperty("MaxDanceAmount", 12);
		MAX_TRIGGERED_BUFF_AMOUNT = buffer.getProperty("MaxTriggeredBuffAmount", 12);
		DEBUFFS_TAKE_BUFF_SLOTS = buffer.getProperty("DebuffsTakeBuffSlots", false);
		STORE_SKILL_COOLTIME = buffer.getProperty("StoreSkillCooltime", true);
		
		// 0 = automatico: buffs + 4 de Divine Inspiration + danzas.
		BUFFER_MAX_SKILLS = buffer.getProperty("BufferMaxSkillsPerProfile", 0);
		if (BUFFER_MAX_SKILLS <= 0)
			BUFFER_MAX_SKILLS = MAX_BUFFS_AMOUNT + 4 + MAX_DANCE_AMOUNT;
		BUFFER_BUFFS = buffer.getProperty("BufferBuffs", "");
		
		BUFFER_BUFFLIST = new HashMap<>();
		if (BUFFER_BUFFS != null && !BUFFER_BUFFS.isEmpty())
		{
			for (String skillInfo : BUFFER_BUFFS.split(";"))
			{
				if (skillInfo.isBlank())
					continue;
				
				final String[] infos = skillInfo.split(",");
				if (infos.length < 3)
					continue;
				
				BUFFER_BUFFLIST.put(Integer.valueOf(infos[0]), new BuffSkillHolder(Integer.valueOf(infos[0]), Integer.valueOf(infos[1]), infos[2], skillInfo));
			}
		}
		
		RESTRICT_USE_BUFFER_ON_PVPFLAG = buffer.getProperty("RestrictUseBufferOnPvPFlag", true);
		RESTRICT_USE_BUFFER_IN_COMBAT = buffer.getProperty("RestrictUseBufferInCombat", true);
		
		FIGHTER_SKILL_LIST = new ArrayList<>();
		for (String skillId : buffer.getProperty("FighterSkillList", "").split(";"))
		{
			if (skillId.isBlank())
				continue;
			FIGHTER_SKILL_LIST.add(Integer.parseInt(skillId.trim()));
		}
		
		MAGE_SKILL_LIST = new ArrayList<>();
		for (String skillId : buffer.getProperty("MageSkillList", "").split(";"))
		{
			if (skillId.isBlank())
				continue;
			MAGE_SKILL_LIST.add(Integer.parseInt(skillId.trim()));
		}
		
		SUMMONER_SKILL_LIST = new ArrayList<>();
		for (String skillId : buffer.getProperty("SummonerSkillList", "").split(";"))
		{
			if (skillId.isBlank())
				continue;
			SUMMONER_SKILL_LIST.add(Integer.parseInt(skillId.trim()));
		}
		
		// Duracion de buffs (antes en Balance.properties).
		ENABLE_ALTERNATIVE_SKILL_DURATION = Boolean.parseBoolean(buffer.getProperty("EnableAlternativeSkillDuration", "false"));
		if(ENABLE_ALTERNATIVE_SKILL_DURATION)
		{
			SKILL_DURATION_LIST = new HashMap<>();
			
			String[] propertySplit;
			propertySplit = buffer.getProperty("SkillDurationList", "").split(";");
			
			for(String skill : propertySplit)
			{
				if (skill.isBlank())
					continue;
				
				String[] skillSplit = skill.split(",");
				if(skillSplit.length != 2)
				{
					System.out.println("[SkillDurationList]: invalid config property -> SkillDurationList \"" + skill + "\"");
				}
				else
				{
					try
					{
						SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0].trim()), Integer.parseInt(skillSplit[1].trim()));
					}
					catch(NumberFormatException nfe)
					{
						nfe.printStackTrace();
						
						if(!skill.equals(""))
						{
							System.out.println("[SkillDurationList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
						}
					}
				}
			}				
		}
	}
	
	private static void parseIdList(String raw, List<Integer> out)
	{
		if (raw == null || raw.trim().isEmpty())
			return;
		for (String id : raw.split(","))
		{
			String t = id.trim();
			if (!t.isEmpty())
				out.add(Integer.parseInt(t));
		}
	}
	
	private static Map<Integer, Integer> parseBuffMap(String raw)
	{
		Map<Integer, Integer> map = new HashMap<>();
		if (raw == null || raw.trim().isEmpty())
			return map;
		for (String token : raw.split(";"))
		{
			String[] sp = token.split(",");
			if (sp.length == 2)
			{
				try
				{
					map.put(Integer.parseInt(sp[0].trim()), Integer.parseInt(sp[1].trim()));
				}
				catch (Exception e)
				{
				}
			}
		}
		return map;
	}

	private static int[] parsePoint(String raw)
	{
		if (raw == null || raw.trim().isEmpty())
			return null;
		String[] sp = raw.trim().split(",");
		if (sp.length < 3)
			return null;
		try
		{
			return new int[] { Integer.parseInt(sp[0].trim()), Integer.parseInt(sp[1].trim()), Integer.parseInt(sp[2].trim()) };
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * Loads Mods settings.
	 */
	private static final void loadMods()
	{
	   ExProperties mods = initProperties(MODS_FILE);
		INFINITY_SS = mods.getProperty("InfinitySS", false);
		INFINITY_ARROWS = mods.getProperty("InfinityArrows", false);
		
		UNSTUCK_TIME = mods.getProperty("UnstuckTime", 30);
		ENABLE_TIME_ZONE_SYSTEM = Boolean.parseBoolean(mods.getProperty("EnableFarmTimeEvent", "False"));
		TIME_FARM_AUTO_ROTATE = Boolean.parseBoolean(mods.getProperty("EnableFarmTimeAutoRotate", "False"));
		
		ANNOUNCE_ID_EVENT = Integer.parseInt(mods.getProperty("AnnounceIdEvents", "18"));
		DEBUG_FARM = Boolean.parseBoolean(mods.getProperty("Debug_farm", "false"));
		
		FIGHTER_SET = mods.getProperty("FighterSet", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
		MAGE_SET = mods.getProperty("MageSet", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
		
		String[] FighterList = FIGHTER_SET.split(",");
		FIGHTER_SET_LIST = new int[FighterList.length];
		for (int i = 0; i < FighterList.length; i++)
			FIGHTER_SET_LIST[i] = Integer.parseInt(FighterList[i]);
		
		String[] MageList = MAGE_SET.split(",");
		MAGE_SET_LIST = new int[MageList.length];
		for (int i = 0; i < MageList.length; i++)
			MAGE_SET_LIST[i] = Integer.parseInt(MageList[i]);
		
		ALLOW_BAKING_SYSTEM = Boolean.parseBoolean(mods.getProperty("EnableCommandBaking", "False"));
		BANKING_SYSTEM_GOLDBARS = Integer.parseInt(mods.getProperty("BankingGoldbarCount", "1"));
		BANKING_SYSTEM_GOLDBAR_ID = Integer.parseInt(mods.getProperty("BankingGoldbarItemId", "10004"));
		BANKING_SYSTEM_ADENA = Integer.parseInt(mods.getProperty("BankingAdenaCount", "500000000"));
		AUTO_GOLDBAR_INTERVAL = Math.max(1, Integer.parseInt(mods.getProperty("AutoGoldBarInterval", "2")));
		
		MENU_PANEL = Boolean.parseBoolean(mods.getProperty("MenuPanelCmd", "true"));
		
		SELLBUFF_ENABLED = Boolean.parseBoolean(mods.getProperty("SellBuffEnable", "False"));
		SELLBUFF_MP_MULTIPLER = mods.getProperty("MpCostMultipler", 1);
		SELLBUFF_PAYMENT_ID = mods.getProperty("PaymentID", 57);
		SELLBUFF_MIN_PRICE = mods.getProperty("MinimumPrice", 100000);
		SELLBUFF_MAX_PRICE = mods.getProperty("MaximumPrice", 100000000);
		SELLBUFF_MAX_BUFFS = mods.getProperty("MaxBuffs", 20);
		
		ENABLE_ONLINE = Boolean.parseBoolean(mods.getProperty("OnlineCmd", "true"));
		PLAYERS_ONLINE_TRICK = Integer.parseInt(mods.getProperty("OnlinePlayerAdd", "0"));
		
		PVP_COLOR_ANNOUNCE = Integer.parseInt(mods.getProperty("ColorAnnounce", "16"));
		
		RESTART_BY_TIME_OF_DAY = Boolean.parseBoolean(mods.getProperty("EnableRestartSystem", "false"));
		RESTART_SECONDS = Integer.parseInt(mods.getProperty("RestartSeconds", "360"));
		RESTART_INTERVAL_BY_TIME_OF_DAY = mods.getProperty("RestartByTimeOfDay", "20:00").split(",");
		ANNOUNCE_MULTISELL = Boolean.parseBoolean(mods.getProperty("AnnounceMultisell", "false"));
		ANNOUNCE_MULTISELL_LIST_IDS = parseIntegerList(mods.getProperty("AnnounceMultisellIds", ""));
		
		ENABLE_NPC_VISUAL_AURA = Boolean.parseBoolean(mods.getProperty("EnableNpcVisualAura", "false"));
		NPC_VISUAL_AURA_COLOR = Math.min(2, Math.max(0, mods.getProperty("NpcVisualAuraColor", 1)));
		NPC_VISUAL_AURA_IDS = parseIntegerList(mods.getProperty("NpcVisualAuraIds", ""));
		
		ENABLE_MOB_VISUAL_AURA = Boolean.parseBoolean(mods.getProperty("EnableMobVisualAura", "false"));
		MOB_VISUAL_AURA_BLUE_IDS = parseIntegerList(mods.getProperty("MobVisualAuraBlueIds", ""));
		MOB_VISUAL_AURA_RED_IDS = parseIntegerList(mods.getProperty("MobVisualAuraRedIds", ""));

	}
	
	/**
	 * Loads Url settings.
	 */
	private static final void loadUrl()
	{
	   ExProperties url = initProperties(URL_FILE);
       OPEN_URL_ENABLE = url.getProperty("OpenUrlEnable", true);
       OPEN_URL_SITE = url.getProperty("OpenUrlSite", "");
       OPEN_URL_COLOR = url.getProperty("OpenUrlColor", "");
       OPEN_URL_FACE = url.getProperty("OpenUrlFace", "");
       OPEN_URL_WEB = url.getProperty("OpenUrlWeb", "");
       OPEN_URL_DISCORD = url.getProperty("OpenUrlDiscord", "");
       OPEN_URL_GRUPO = url.getProperty("OpenUrlGrupo", "");
       OPEN_URL_STREAMER1 = url.getProperty("OpenUrlStreamer1", "");
       OPEN_URL_STREAMER2 = url.getProperty("OpenUrlStreamer2", "");
       OPEN_URL_STREAMER3 = url.getProperty("OpenUrlStreamer3", "");
       OPEN_URL_STREAMER4 = url.getProperty("OpenUrlStreamer4", "");
       OPEN_URL_STREAMER5 = url.getProperty("OpenUrlStreamer5", "");
	}
	
    /**
     * Loads offline shop settings
     */
    private static final void loadOfflineShop()
    {
        final ExProperties offline = initProperties(OFFLINE_FILE);
        OFFLINE_TRADE_ENABLE = offline.getProperty("OfflineTradeEnable", false);
        OFFLINE_CRAFT_ENABLE = offline.getProperty("OfflineCraftEnable", false);
        OFFLINE_MODE_IN_PEACE_ZONE = offline.getProperty("OfflineModeInPeaceZone", false);
        OFFLINE_MODE_NO_DAMAGE = offline.getProperty("OfflineModeNoDamage", false);
        OFFLINE_SET_NAME_COLOR = offline.getProperty("OfflineSetNameColor", false);
        OFFLINE_NAME_COLOR = Integer.decode("0x" + offline.getProperty("OfflineNameColor", "808080"));
        RESTORE_OFFLINERS = offline.getProperty("RestoreOffliners", false);
        OFFLINE_MAX_DAYS = offline.getProperty("OfflineMaxDays", 10);
        OFFLINE_DISCONNECT_FINISHED = offline.getProperty("OfflineDisconnectFinished", true);
    }
	
	/**
     * Loads subclass.
     */
	private static final void loadDressme()
	{
	ExProperties dressme = initProperties(DRESSME_FILE);
	ALLOW_DRESS_ME_SYSTEM = Boolean.parseBoolean(dressme.getProperty("AllowDressMeSystem", "false"));
	SKIN_NAME1 = String.valueOf(dressme.getProperty("SkinName1", "SkinName"));
	SKIN_NAME2 = String.valueOf(dressme.getProperty("SkinName2", "SkinName"));
	SKIN_NAME3 = String.valueOf(dressme.getProperty("SkinName3", "SkinName"));
	SKIN_NAME4 = String.valueOf(dressme.getProperty("SkinName4", "SkinName"));
	SKIN_NAME5 = String.valueOf(dressme.getProperty("SkinName5", "SkinName"));
	SKIN_NAME6 = String.valueOf(dressme.getProperty("SkinName6", "SkinName"));
	SKIN_NAME7 = String.valueOf(dressme.getProperty("SkinName7", "SkinName"));
	SKIN_NAME8 = String.valueOf(dressme.getProperty("SkinName8", "SkinName"));
	SKIN_NAME9 = String.valueOf(dressme.getProperty("SkinName9", "SkinName"));
	SKIN_NAME10 = String.valueOf(dressme.getProperty("SkinName10", "SkinName"));
	SKIN_NAME11 = String.valueOf(dressme.getProperty("SkinName11", "SkinName"));
	SKIN_NAME12 = String.valueOf(dressme.getProperty("SkinName12", "SkinName"));
	SKIN_NAME13 = String.valueOf(dressme.getProperty("SkinName13", "SkinName"));
	SKIN_NAME14 = String.valueOf(dressme.getProperty("SkinName14", "SkinName"));
	SKIN_NAME15 = String.valueOf(dressme.getProperty("SkinName15", "SkinName"));
	SKIN_NAME16 = String.valueOf(dressme.getProperty("SkinName16", "SkinName"));
	SKIN_NAME17 = String.valueOf(dressme.getProperty("SkinName17", "SkinName"));
	SKIN_NAME18 = String.valueOf(dressme.getProperty("SkinName18", "SkinName"));
	SKIN_NAME19 = String.valueOf(dressme.getProperty("SkinName19", "SkinName"));
	SKIN_NAME20 = String.valueOf(dressme.getProperty("SkinName20", "SkinName"));
	SKIN_NAME21 = String.valueOf(dressme.getProperty("SkinName21", "SkinName"));
	SKIN_NAME22 = String.valueOf(dressme.getProperty("SkinName22", "SkinName"));
	SKIN_NAME23 = String.valueOf(dressme.getProperty("SkinName23", "SkinName"));
	SKIN_NAME24 = String.valueOf(dressme.getProperty("SkinName24", "SkinName"));
	SKIN_NAME25 = String.valueOf(dressme.getProperty("SkinName25", "SkinName"));
	SKIN_NAME26 = String.valueOf(dressme.getProperty("SkinName26", "SkinName"));
	SKIN_NAME27 = String.valueOf(dressme.getProperty("SkinName27", "SkinName"));
	SKIN_NAME28 = String.valueOf(dressme.getProperty("SkinName28", "SkinName"));
	SKIN_NAME29 = String.valueOf(dressme.getProperty("SkinName29", "SkinName"));
	SKIN_NAME30 = String.valueOf(dressme.getProperty("SkinName30", "SkinName"));
	
	SKIN_ITEM_1 = Integer.parseInt(dressme.getProperty("SkinItem1", "0"));
	SKIN_ITEM_2 = Integer.parseInt(dressme.getProperty("SkinItem2", "0"));
	SKIN_ITEM_3 = Integer.parseInt(dressme.getProperty("SkinItem3", "0"));
	SKIN_ITEM_4 = Integer.parseInt(dressme.getProperty("SkinItem4", "0"));
	SKIN_ITEM_5 = Integer.parseInt(dressme.getProperty("SkinItem5", "0"));
	SKIN_ITEM_6 = Integer.parseInt(dressme.getProperty("SkinItem6", "0"));
	SKIN_ITEM_7 = Integer.parseInt(dressme.getProperty("SkinItem7", "0"));
	SKIN_ITEM_8 = Integer.parseInt(dressme.getProperty("SkinItem8", "0"));
	SKIN_ITEM_9 = Integer.parseInt(dressme.getProperty("SkinItem9", "0"));
	SKIN_ITEM_10 = Integer.parseInt(dressme.getProperty("SkinItem10", "0"));
	SKIN_ITEM_11 = Integer.parseInt(dressme.getProperty("SkinItem11", "0"));
	SKIN_ITEM_12 = Integer.parseInt(dressme.getProperty("SkinItem12", "0"));
	SKIN_ITEM_13 = Integer.parseInt(dressme.getProperty("SkinItem13", "0"));
	SKIN_ITEM_14 = Integer.parseInt(dressme.getProperty("SkinItem14", "0"));
	SKIN_ITEM_15 = Integer.parseInt(dressme.getProperty("SkinItem15", "0"));
	String temp = dressme.getProperty("DressMeChests", "");
	String[] temp2 = temp.split(";");
	for (String s : temp2)
	{
		String[] t = s.split(",");
		DRESS_ME_CHESTS.put(t[0], Integer.parseInt(t[1]));
	}
		temp = dressme.getProperty("DressMeHair", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_DHAIR.put(t[0], Integer.parseInt(t[1]));
		}
		temp = dressme.getProperty("DressMeLegs", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_LEGS.put(t[0], Integer.parseInt(t[1]));
		}
		temp = dressme.getProperty("DressMeBoots", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_BOOTS.put(t[0], Integer.parseInt(t[1]));
		}
		temp = dressme.getProperty("DressMeGloves", "");
		temp2 = temp.split(";");
		for (String s : temp2)
		{
			String[] t = s.split(",");
			DRESS_ME_GLOVES.put(t[0], Integer.parseInt(t[1]));
		}
		temp = dressme.getProperty("DressMeWeapons", "");
				temp2 = temp.split(";");
			for (String s : temp2)
			{
				String[] t = s.split(",");
				DRESS_ME_WEAPONS.put(t[0], Integer.parseInt(t[1]));
			}	
	}
	
	/**
	 * Loads MultiSkill settings.
	 */
	private static final void loadMultiSkill()
	{
		final ExProperties ms = initProperties(MULTISKILL_FILE);
		MULTISKILL_ENABLED = ms.getProperty("Enabled", true);
		MULTISKILL_SP_MULTIPLIER = Double.parseDouble(ms.getProperty("SpMultiplier", "1.0"));
		if (MULTISKILL_SP_MULTIPLIER < 0)
			MULTISKILL_SP_MULTIPLIER = 0;
	}
	
	/**
	 * Loads custom Skill Enchant NPC settings (retail + safe on one NPC).
	 */
	private static final void loadSkillEnchant()
	{
		final ExProperties se = initProperties(SKILL_ENCHANT_FILE);
		SKILL_ENCHANT_CUSTOM_ENABLED = se.getProperty("Enabled", true);
		SKILL_ENCHANT_NPC_ID = se.getProperty("NpcId", 60003);
		SKILL_ENCHANT_MAX = se.getProperty("MaxEnchant", 30);
		if (SKILL_ENCHANT_MAX < 1)
			SKILL_ENCHANT_MAX = 1;
		else if (SKILL_ENCHANT_MAX > 30)
			SKILL_ENCHANT_MAX = 30;
		
		SKILL_ENCHANT_NORMAL_CHANCE = se.getProperty("NormalChance", -1);
		if (SKILL_ENCHANT_NORMAL_CHANCE < -1)
			SKILL_ENCHANT_NORMAL_CHANCE = -1;
		else if (SKILL_ENCHANT_NORMAL_CHANCE > 100)
			SKILL_ENCHANT_NORMAL_CHANCE = 100;
		
		final int[] defLv =
		{
			76,
			77,
			78,
			79,
			80
		};
		for (int i = 0; i < 5; i++)
		{
			int v = se.getProperty("NormalChance" + (76 + i), defLv[i]);
			if (v < -1)
				v = -1;
			else if (v > 100)
				v = 100;
			SKILL_ENCHANT_NORMAL_BY_LEVEL[i] = v;
		}
		
		SKILL_ENCHANT_SAFE_CHANCE = se.getProperty("SafeChance", 100);
		if (SKILL_ENCHANT_SAFE_CHANCE < 0)
			SKILL_ENCHANT_SAFE_CHANCE = 0;
		else if (SKILL_ENCHANT_SAFE_CHANCE > 100)
			SKILL_ENCHANT_SAFE_CHANCE = 100;
		
		SKILL_ENCHANT_SAFE_ITEM_ID = se.getProperty("SafeItemId", 57);
		SKILL_ENCHANT_SAFE_ITEM_COUNT = se.getProperty("SafeItemCount", 1000000);
		if (SKILL_ENCHANT_SAFE_ITEM_COUNT < 0)
			SKILL_ENCHANT_SAFE_ITEM_COUNT = 0;
		
		SKILL_ENCHANT_FAIL_RESET = se.getProperty("FailResetToBase", true);
	}
	
	private static final void loadBattlePass()
	{
		final ExProperties bp = initProperties(BATTLEPASS_FILE);
		BATTLEPASS_ENABLED = bp.getProperty("Enabled", true);
		BATTLEPASS_MAX_LEVEL = clampBattlePassLevel(bp.getProperty("MaxLevel", 105));
		
		BATTLEPASS_VIP_ENABLED = bp.getProperty("VipEnabled", true);
		BATTLEPASS_VIP_MAX_LEVEL = clampBattlePassLevel(bp.getProperty("VipMaxLevel", 105));
	}
	
	private static int clampBattlePassLevel(int level)
	{
		if (level < 1)
			return 1;
		return level > 128 ? 128 : level;
	}
	
	/**
     * Loads subclass.
     */
	private static final void loadSubclass()
	{
		ExProperties sub = initProperties(SUBS_FILE);
		SUB_ACU = sub.getProperty("Sub_Acu", true);
		SUB_OL_WAR = sub.getProperty("Sub_Ol_War", true);
		SUB_ELFS = sub.getProperty("Sub_Elfs", true);
		SUB_COUNT = sub.getProperty("Sub_Count", 3);
		BASE_SUBCLASS_LEVEL = sub.getProperty("BaseSubclassLevel", 40);
		MAX_SUBCLASS_LEVEL = sub.getProperty("MaxSubclassLevel", 81);
		MAX_PLAYER_LEVEL = sub.getProperty("MaxPlayerLevel", 100);
		// Clamp to Experience table bounds (index = level)
		final int maxExpIndex = net.sf.l2j.gameserver.model.actor.player.Experience.LEVEL.length - 1;
		if (MAX_SUBCLASS_LEVEL < 2)
			MAX_SUBCLASS_LEVEL = 2;
		if (MAX_SUBCLASS_LEVEL > maxExpIndex)
			MAX_SUBCLASS_LEVEL = maxExpIndex;
		if (MAX_PLAYER_LEVEL < 2)
			MAX_PLAYER_LEVEL = 2;
		if (MAX_PLAYER_LEVEL > maxExpIndex)
			MAX_PLAYER_LEVEL = maxExpIndex;
		// Max*Level = primer nivel INALCANZABLE. Si queda <= Base, el tope jugable
		// cae por debajo del base (ej. Base=80 + Max=80 → jugable 79 @ 100%).
		if (MAX_SUBCLASS_LEVEL <= BASE_SUBCLASS_LEVEL)
		{
			LOGGER.warn("MaxSubclassLevel ({}) must be > BaseSubclassLevel ({}). Using {}.", MAX_SUBCLASS_LEVEL, BASE_SUBCLASS_LEVEL, BASE_SUBCLASS_LEVEL + 1);
			MAX_SUBCLASS_LEVEL = BASE_SUBCLASS_LEVEL + 1;
		}
		ALT_GAME_SUBCLASS_EVERYWHERE = sub.getProperty("AltSubclassEverywhere", false);
		VILLAGE_SUB_ANNOUNCE = sub.getProperty("VillageSubAnnounce", true);
		VILLAGE_CANCEL_SUB_ITEM_ID = sub.getProperty("VillageCancelSubItemId", 0);
		VILLAGE_CANCEL_SUB_COUNT = sub.getProperty("VillageCancelSubCount", 1);
		
		CUSTOM_SUB_ANNOUNCE_TEXT = sub.getProperty("CustomSubAnnounceText", "Felicitaciones %playername% por hacer SubClass");
		CUSTOM_SUB_ANNOUNCE_TEXT_1 = sub.getProperty("CustomSubAnnounceText1", "Felicitaciones %playername% por hacer su Primera SubClass");
		CUSTOM_SUB_ANNOUNCE_TEXT_2 = sub.getProperty("CustomSubAnnounceText2", "Felicitaciones %playername% por hacer su Segunda SubClass");
		CUSTOM_SUB_ANNOUNCE_TEXT_3 = sub.getProperty("CustomSubAnnounceText3", "Felicitaciones %playername% por hacer su Tercera SubClass");
			
		CHECK_SKILL_LIST = sub.getProperty("CheckSkill", true);
		
		// NPC Custom Level: 0 = usa el tope jugable del server (MaxPlayerLevel - 1).
		CUSTOM_LEVEL_TARGET = sub.getProperty("CustomLevelTarget", 78);
		if (CUSTOM_LEVEL_TARGET <= 0 || CUSTOM_LEVEL_TARGET > MAX_PLAYER_LEVEL - 1)
			CUSTOM_LEVEL_TARGET = MAX_PLAYER_LEVEL - 1;
		if (CUSTOM_LEVEL_TARGET < 1)
			CUSTOM_LEVEL_TARGET = 1;
		CUSTOM_LEVEL_ADENA = sub.getProperty("CustomLevelAdena", 2000000000);
		CUSTOM_LEVEL_COIN_ID = sub.getProperty("CustomLevelCoinId", 10001);
		CUSTOM_LEVEL_COIN_COUNT = sub.getProperty("CustomLevelCoinCount", 10000);
	}
	
	/**
	 * Loads balance settings.
	 */
	private static final void loadBalance()
	{
		final ExProperties balance = initProperties(Config.BALANCE_FILE);
		TPAIN_RANGE = Integer.parseInt(balance.getProperty("TPainRange", "900"));
		TPAIN_REVERSE = Boolean.parseBoolean(balance.getProperty("TPainReverse", "true"));
		TPAIN_REVERSE_MAX_PERCENT = Math.max(0, Math.min(100, Integer.parseInt(balance.getProperty("TPainReverseMaxPercent", "50"))));
		ENABLE_MODIFY_DEBUFF_DURATION = Boolean.parseBoolean(balance.getProperty("EnableModifyDebuffDuration", "false"));
		DEBUFF_TIME = Float.parseFloat(balance.getProperty("DebuffTime", "0.2"));
		for (String id : balance.getProperty("DebuffTimeList", "0").split(","))
		{
			DEBUFF_TIME_LIST.add(Integer.parseInt(id));
		}

		MAX_CANCEL_COUNT = Integer.parseInt(balance.getProperty("MaxCancelCount", "6"));
		
		RETURN_CANCEL_BUFF = Boolean.parseBoolean(balance.getProperty("ReturnCancelBuff", "false"));
		RETURN_CANCEL_TIME = Integer.parseInt(balance.getProperty("ReturnCancelTime", "15"));
		
		MCRIT_POWER = Float.parseFloat(balance.getProperty("MCritPower", "3.0"));
		PCRIT_POWER = Float.parseFloat(balance.getProperty("PCritPower", "2.0"));
		RANDOM_DAMAGE = Boolean.parseBoolean(balance.getProperty("RandomDamage", "true"));
		ENABLE_CUSTOM_CRIT_POWER = Boolean.parseBoolean(balance.getProperty("EnableCustom_CriticalPower", "true"));
		MAGIC_CRITICAL_POWER = Float.parseFloat(balance.getProperty("MagicCriticalPower", "3.0"));
		FIGHTER_CRITICAL_POWER = Float.parseFloat(balance.getProperty("FighterCriticalPower", "1.0"));
		GHOSTSENTINEL_CRITICAL_POWER = Float.parseFloat(balance.getProperty("GhostSentinelCriticalPower", "1.0"));
		ENABLE_CUSTOM_CRIT = Boolean.parseBoolean(balance.getProperty("EnableCustom_CriticalChance", "true"));
		MAX_PCRIT_RATE = Integer.parseInt(balance.getProperty("MaxPCritRate", "500"));
		PCRIT_RATE_ArcherHuman = Integer.parseInt(balance.getProperty("PCritRate_ArcherHuman", "300"));
		PCRIT_RATE_ArcherElfo = Integer.parseInt(balance.getProperty("PCritRate_ArcherElfo", "300"));
		PCRIT_RATE_ArcherDarkElfo = Integer.parseInt(balance.getProperty("PCritRate_ArcherDarkElfo", "300"));
		MAX_MCRIT_RATE = Integer.parseInt(balance.getProperty("MaxMCritRate", "300"));
		MCRIT_RATE_Archmage = Integer.parseInt(balance.getProperty("MCritRate_Archmage", "300"));
		MCRIT_RATE_Soultaker = Integer.parseInt(balance.getProperty("MCritRate_Soultaker", "300"));
		MCRIT_RATE_Mystic_Muse = Integer.parseInt(balance.getProperty("MCritRate_Mystic_Muse", "300"));
		MCRIT_RATE_Storm_Screamer = Integer.parseInt(balance.getProperty("MCritRate_Storm_Screamer", "300"));
		MCRIT_RATE_Dominator = Integer.parseInt(balance.getProperty("MCritRate_Dominator", "300"));
		MAX_MATK_SPEED = Integer.parseInt(balance.getProperty("MaxMAtkSpeed", "1999"));
		MAX_PATK_SPEED = Integer.parseInt(balance.getProperty("MaxPAtkSpeed", "1500"));
		MAX_RUN_SPEED = Integer.parseInt(balance.getProperty("MaxRunSpeed", "0").trim());
		MAX_PATK_SPEED_GHOST = Integer.parseInt(balance.getProperty("MaxPAtkSpeedGhost", "1500"));
		MAX_PATK_SPEED_MOONL = Integer.parseInt(balance.getProperty("MaxPAtkSpeedMoonl", "1500"));
		MIN_ATKSPD_DELAY = Integer.parseInt(balance.getProperty("MaxAtkSpdDelay", "150"));
		SKILL_MCAST_SPD_CAP = Integer.parseInt(balance.getProperty("SkillMCastSpdCap", "0").trim());
		SKILL_PCAST_SPD_CAP = Integer.parseInt(balance.getProperty("SkillPCastSpdCap", "0").trim());
		ALT_ATTACK_DELAY = Float.parseFloat(balance.getProperty("AltAttackDelay", "1.00"));
		ALT_CAST_DELAY = Float.parseFloat(balance.getProperty("AltCastDelay", "1.00"));
		BLOW_ATTACK_FRONT = Integer.parseInt(balance.getProperty("BlowAttackFront", "50"));
		BLOW_ATTACK_SIDE = Integer.parseInt(balance.getProperty("BlowAttackSide", "60"));
		BLOW_ATTACK_BEHIND = Integer.parseInt(balance.getProperty("BlowAttackBehind", "70"));

		BACKSTAB_ATTACK_FRONT = Integer.parseInt(balance.getProperty("BackstabAttackFront", "0"));
		BACKSTAB_ATTACK_SIDE = Integer.parseInt(balance.getProperty("BackstabAttackSide", "0"));
		BACKSTAB_ATTACK_BEHIND = Integer.parseInt(balance.getProperty("BackstabAttackBehind", "70"));
		DAGGER_BLOW_MULTIPLIER = Float.parseFloat(balance.getProperty("DaggerBlowMultiplier", "1.0"));
		
		PVE_PHYSICAL_DAMAGE_MULTIPLIER = Float.parseFloat(balance.getProperty("PvePhysicalDamageMultiplier", "1.0").trim());
		PVE_MAGIC_DAMAGE_MULTIPLIER = Float.parseFloat(balance.getProperty("PveMagicDamageMultiplier", "1.0").trim());
		NPC_ELEMENT_DEFENSE_BONUS = Integer.parseInt(balance.getProperty("NpcElementDefenseBonus", "0").trim());
		NPC_ELEMENT_DEFENSE_RATE = Float.parseFloat(balance.getProperty("NpcElementDefenseRate", "1.0").trim());
		NPC_ELEMENT_DEFENSE_MIN_ENABLED = Boolean.parseBoolean(balance.getProperty("NpcElementDefenseMinEnabled", "false").trim());
		NPC_ELEMENT_DEFENSE_MIN = Integer.parseInt(balance.getProperty("NpcElementDefenseMin", "20").trim());
		PVE_DAMAGE_DEBUG = Boolean.parseBoolean(balance.getProperty("PveDamageDebug", "false").trim());
		PVE_MAX_HIT_PERCENT_OF_HP = Double.parseDouble(balance.getProperty("PveMaxHitPercentOfHp", "0").trim());
	}
	
	/**
	 * Loads skill chance multiplier settings.
	 */
	/**
	 * Loads class vs class PvP damage balance.
	 */
	private static final void loadClassBalance()
	{
		final ExProperties classBalance = initProperties(Config.CLASS_BALANCE_FILE);
		ENABLE_CLASS_DAMAGE_BALANCE = Boolean.parseBoolean(classBalance.getProperty("EnableClassDamageBalance", "true"));
		net.sf.l2j.gameserver.instancemanager.ClassDamageManager.load();
	}
	
	private static final void loadSkillsChance()
	{
		final ExProperties skillsChance = initProperties(Config.SKILLS_CHANCE_FILE);
		
		ENABLE_CUSTOM_CHANCE_SKILL = Boolean.parseBoolean(skillsChance.getProperty("Enable_chance_skills", "true"));
		SURRENDER_TO_FIRE = Float.parseFloat(skillsChance.getProperty("Surrender_to_fire", "1.0"));
		VORTEX_TO_FIRE = Float.parseFloat(skillsChance.getProperty("Vortex_to_fire", "1.0"));
		SURRENDER_TO_WIND = Float.parseFloat(skillsChance.getProperty("Surrender_to_wind", "1.0"));
		WIND_VORTEX = Float.parseFloat(skillsChance.getProperty("Vortex_to_wind", "1.0"));
		CURSE_GLOOM = Float.parseFloat(skillsChance.getProperty("Curse_of_Gloom", "1.0"));
		DARK_VORTEX = Float.parseFloat(skillsChance.getProperty("Dark_vortex", "1.0"));
		SURRENDER_TO_WATER = Float.parseFloat(skillsChance.getProperty("Surrender_to_water", "1.0"));
		ICE_VORTEX = Float.parseFloat(skillsChance.getProperty("Ice_vortex", "1.0"));
		LIGHT_VORTEX = Float.parseFloat(skillsChance.getProperty("Light_vortex", "1.0"));
		SILENCE = Float.parseFloat(skillsChance.getProperty("Silence", "1.0"));
		SLEEP = Float.parseFloat(skillsChance.getProperty("Sleep", "1.0"));
		CURSE_FEAR = Float.parseFloat(skillsChance.getProperty("Curse_fear", "1.0"));
		ANCHOR = Float.parseFloat(skillsChance.getProperty("Anchor", "1.0"));
		CURSE_OF_DOOM = Float.parseFloat(skillsChance.getProperty("Curse_of_Doom", "1.0"));
		CURSE_OF_ABYSS = Float.parseFloat(skillsChance.getProperty("Curse_of_Abyss", "1.0"));
		CANCELLATION = Float.parseFloat(skillsChance.getProperty("Cancellation", "1.0"));
		MASS_SLOW = Float.parseFloat(skillsChance.getProperty("Mass_slow", "1.0"));
		MASS_FEAR = Float.parseFloat(skillsChance.getProperty("Mass_fear", "1.0"));
		MASS_GLOOM = Float.parseFloat(skillsChance.getProperty("Mass_gloom", "1.0"));
		SLEEPING_CLOUD = Float.parseFloat(skillsChance.getProperty("Sleeping_cloud", "1.0"));
		HEROIC_GRANDEUR = Float.parseFloat(skillsChance.getProperty("Heroic_grandeur", "1.0"));
		HEROIC_DREAD = Float.parseFloat(skillsChance.getProperty("Heroic_dread", "1.0"));
		STUNNING_SHOT = Float.parseFloat(skillsChance.getProperty("Stunning_shot", "1.0"));
		HEX = Float.parseFloat(skillsChance.getProperty("Hex", "1.0"));
		SHOCK_STOMP = Float.parseFloat(skillsChance.getProperty("Shock_stomp", "1.0"));
		THUNDER_STORM = Float.parseFloat(skillsChance.getProperty("Thunder_storm", "1.0"));
		SHIELD_STUN = Float.parseFloat(skillsChance.getProperty("Shield_stun", "1.0"));
		SHIELD_SLAM = Float.parseFloat(skillsChance.getProperty("Shield_slam", "1.0"));
		SHACKLE = Float.parseFloat(skillsChance.getProperty("Shackle", "1.0"));
		MASS_SHACKLING = Float.parseFloat(skillsChance.getProperty("Mass_shackling", "1.0"));
		ARREST = Float.parseFloat(skillsChance.getProperty("Arrest", "1.0"));
		BLUFF = Float.parseFloat(skillsChance.getProperty("Bluff", "1.0"));
		SWITCH = Float.parseFloat(skillsChance.getProperty("Switch", "1.0"));
		STUNNING_FIST = Float.parseFloat(skillsChance.getProperty("Stunning_fist", "1.0"));
		FEAR_OVER = Float.parseFloat(skillsChance.getProperty("Fear_over", "1.0"));
		SEAL_OF_SILENCE = Float.parseFloat(skillsChance.getProperty("Seal_of_Silence", "1.0"));
		SEAL_OF_SUSPENSION = Float.parseFloat(skillsChance.getProperty("Seal_of_Suspension", "1.0"));
		STUN_ATTACK = Float.parseFloat(skillsChance.getProperty("Stun_attack", "1.0"));
		ARMOR_CRUSH = Float.parseFloat(skillsChance.getProperty("Armor_crush", "1.0"));
		SLOW = Float.parseFloat(skillsChance.getProperty("Slow", "1.0"));
		SEAL_OF_DESPAIR = Float.parseFloat(skillsChance.getProperty("Seal_of_Despair", "1.0"));
		DREAMING_SPIRIT = Float.parseFloat(skillsChance.getProperty("Dreaming_spirit", "1.0"));
		SEAL_OF_BINDING = Float.parseFloat(skillsChance.getProperty("Seal_of_Binding", "1.0"));
		MASS_WARRIOR_BANE = Float.parseFloat(skillsChance.getProperty("Mass_Warrior_Bane", "1.0"));
		MASS_MAGE_BANE = Float.parseFloat(skillsChance.getProperty("Mass_Mage_Bane", "1.0"));
		SHIELD_BASH = Float.parseFloat(skillsChance.getProperty("Shield_Bash", "1.0"));
		SHOCK_BLAST = Float.parseFloat(skillsChance.getProperty("Shock_Blast", "1.0"));
	}
	
	/**
	 * Loads enchant settings.
	 */
	private static final void loadEnchant()
	{
		final ExProperties enchant = initProperties(Config.ENCHANT_FILE);
		
		String[] propertySplit = enchant.getProperty("NormalWeaponEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					NORMAL_WEAPON_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("BlessWeaponEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					BLESS_WEAPON_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("CrystalWeaponEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					CRYSTAL_WEAPON_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("DonatorWeaponEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				System.out.println("invalid config property");
			}
			else
			{
				try
				{
					DONATOR_WEAPON_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						System.out.println("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("NormalArmorEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					NORMAL_ARMOR_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("BlessArmorEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					BLESS_ARMOR_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("CrystalArmorEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					CRYSTAL_ARMOR_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("DonatorArmorEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				System.out.println("invalid config property");
			}
			else
			{
				try
				{
					DONATOR_ARMOR_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						System.out.println("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("NormalJewelryEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					NORMAL_JEWELRY_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("BlessJewelryEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					BLESS_JEWELRY_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("CrystalJewelryEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				LOGGER.info("invalid config property");
			}
			else
			{
				try
				{
					CRYSTAL_JEWELRY_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						LOGGER.info("invalid config property");
					}
				}
			}
		}
		propertySplit = enchant.getProperty("DonatorJewelryEnchantLevel", "").split(";");
		for (String readData : propertySplit)
		{
			String[] writeData = readData.split(",");
			if (writeData.length != 2)
			{
				System.out.println("invalid config property");
			}
			else
			{
				try
				{
					DONATOR_JEWELRY_ENCHANT_LEVEL.put(Integer.valueOf(Integer.parseInt(writeData[0])), Integer.valueOf(Integer.parseInt(writeData[1])));
				}
				catch (NumberFormatException nfe)
				{
					if (DEVELOPER)
					{
						nfe.printStackTrace();
					}
					if (!readData.equals(""))
					{
						System.out.println("invalid config property");
					}
				}
			}
		}
		ENCHANT_HERO_WEAPON = Boolean.parseBoolean(enchant.getProperty("EnableEnchantHeroWeapons", "False"));
		
		GOLD_WEAPON = Integer.parseInt(enchant.getProperty("IdEnchantDonatorWeapon", "10010"));
		
		GOLD_ARMOR = Integer.parseInt(enchant.getProperty("IdEnchantDonatorArmor", "10011"));
		
		ENCHANT_SAFE_MAX = Integer.parseInt(enchant.getProperty("EnchantSafeMax", "3"));
		
		ENCHANT_SAFE_MAX_FULL = Integer.parseInt(enchant.getProperty("EnchantSafeMaxFull", "4"));
		
		SCROLL_STACKABLE = Boolean.parseBoolean(enchant.getProperty("ScrollStackable", "False"));
		
		ENCHANT_WEAPON_MAX = Integer.parseInt(enchant.getProperty("EnchantWeaponMax", "25"));
		ENCHANT_ARMOR_MAX = Integer.parseInt(enchant.getProperty("EnchantArmorMax", "25"));
		ENCHANT_JEWELRY_MAX = Integer.parseInt(enchant.getProperty("EnchantJewelryMax", "25"));
		
		BLESSED_ENCHANT_WEAPON_MAX = Integer.parseInt(enchant.getProperty("BlessedEnchantWeaponMax", "25"));
		BLESSED_ENCHANT_ARMOR_MAX = Integer.parseInt(enchant.getProperty("BlessedEnchantArmorMax", "25"));
		BLESSED_ENCHANT_JEWELRY_MAX = Integer.parseInt(enchant.getProperty("BlessedEnchantJewelryMax", "25"));
		
		BREAK_ENCHANT = Integer.valueOf(enchant.getProperty("BreakEnchant", "0")).intValue();
		
		CRYSTAL_ENCHANT_MIN = Integer.parseInt(enchant.getProperty("CrystalEnchantMin", "20"));
		CRYSTAL_ENCHANT_WEAPON_MAX = Integer.parseInt(enchant.getProperty("CrystalEnchantWeaponMax", "25"));
		CRYSTAL_ENCHANT_ARMOR_MAX = Integer.parseInt(enchant.getProperty("CrystalEnchantArmorMax", "25"));
		CRYSTAL_ENCHANT_JEWELRY_MAX = Integer.parseInt(enchant.getProperty("CrystalEnchantJewelryMax", "25"));
		
		DONATOR_ENCHANT_MIN = Integer.parseInt(enchant.getProperty("DonatorEnchantMin", "20"));
		DONATOR_ENCHANT_WEAPON_MAX = Integer.parseInt(enchant.getProperty("DonatorEnchantWeaponMax", "25"));
		DONATOR_ENCHANT_ARMOR_MAX = Integer.parseInt(enchant.getProperty("DonatorEnchantArmorMax", "25"));
		DONATOR_ENCHANT_JEWELRY_MAX = Integer.parseInt(enchant.getProperty("DonatorEnchantJewelryMax", "25"));
		DONATOR_DECREASE_ENCHANT = Boolean.valueOf(enchant.getProperty("DonatorDecreaseEnchant", "false")).booleanValue();
		DONATOR_BREAK_ENCHANT = Integer.parseInt(enchant.getProperty("DonatorBreakEnchant", "25"));
		DONATOR_LOST_ENCHANT = Boolean.valueOf(enchant.getProperty("DonatorLostEnchant", "false"));
		
		ENABLE_ENCHANT_ANNOUNCE = Boolean.parseBoolean(enchant.getProperty("EnableEnchantAnnounce", "False"));
		ENCHANT_ANNOUNCE_LEVEL = enchant.getProperty("EnchantAnnounceLevels", "6,10,16,20");
		LIST_ENCHANT_ANNOUNCE_LEVEL = new ArrayList<>();
		for (String id : ENCHANT_ANNOUNCE_LEVEL.split(","))
		{
			LIST_ENCHANT_ANNOUNCE_LEVEL.add(Integer.valueOf(Integer.parseInt(id)));
		}
		
	}

    /**
     * Loads skin settings
     */
    private static final void loadSkins()
    {
        final ExProperties skins = initProperties(SKINS_FILE);
        ALLOW_SKIN_SYSTEM = skins.getProperty("AllowSkinSystem", false);
        DRESS_ME_COMMAND = skins.getProperty("DressMeCommand", "dressme");
        ALLOW_DRESS_ME_IN_OLY = skins.getProperty("AllowDressMeInOly", false);

    }
    
	/**
	 * Loads clan and clan hall settings.
	 */
	private static final void loadClans()
	{
		final ExProperties clans = initProperties(CLANS_FILE);
		ALT_CLAN_JOIN_DAYS = clans.getProperty("DaysBeforeJoinAClan", 5);
		ALT_CLAN_CREATE_DAYS = clans.getProperty("DaysBeforeCreateAClan", 10);
		ALT_MAX_NUM_OF_CLANS_IN_ALLY = clans.getProperty("AltMaxNumOfClansInAlly", 3);
		ALT_CLAN_MEMBERS_FOR_WAR = clans.getProperty("AltClanMembersForWar", 15);
		ALT_CLAN_WAR_PENALTY_WHEN_ENDED = clans.getProperty("AltClanWarPenaltyWhenEnded", 5);
		ALT_CLAN_DISSOLVE_DAYS = clans.getProperty("DaysToPassToDissolveAClan", 7);
		ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = clans.getProperty("DaysBeforeJoinAllyWhenLeaved", 1);
		ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = clans.getProperty("DaysBeforeJoinAllyWhenDismissed", 1);
		ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = clans.getProperty("DaysBeforeAcceptNewClanWhenDismissed", 1);
		ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = clans.getProperty("DaysBeforeCreateNewAllyWhenDissolved", 10);
		ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = clans.getProperty("AltMembersCanWithdrawFromClanWH", false);
		
		ALT_MANOR_REFRESH_TIME = clans.getProperty("AltManorRefreshTime", 20);
		ALT_MANOR_REFRESH_MIN = clans.getProperty("AltManorRefreshMin", 0);
		ALT_MANOR_APPROVE_TIME = clans.getProperty("AltManorApproveTime", 6);
		ALT_MANOR_APPROVE_MIN = clans.getProperty("AltManorApproveMin", 0);
		ALT_MANOR_MAINTENANCE_MIN = clans.getProperty("AltManorMaintenanceMin", 6);
		ALT_MANOR_SAVE_PERIOD_RATE = clans.getProperty("AltManorSavePeriodRate", 2) * 3600000;
		
		CH_TELE_FEE_RATIO = clans.getProperty("ClanHallTeleportFunctionFeeRatio", 86400000);
		CH_TELE1_FEE = clans.getProperty("ClanHallTeleportFunctionFeeLvl1", 7000);
		CH_TELE2_FEE = clans.getProperty("ClanHallTeleportFunctionFeeLvl2", 14000);
		CH_SUPPORT_FEE_RATIO = clans.getProperty("ClanHallSupportFunctionFeeRatio", 86400000);
		CH_SUPPORT1_FEE = clans.getProperty("ClanHallSupportFeeLvl1", 17500);
		CH_SUPPORT2_FEE = clans.getProperty("ClanHallSupportFeeLvl2", 35000);
		CH_SUPPORT3_FEE = clans.getProperty("ClanHallSupportFeeLvl3", 49000);
		CH_SUPPORT4_FEE = clans.getProperty("ClanHallSupportFeeLvl4", 77000);
		CH_SUPPORT5_FEE = clans.getProperty("ClanHallSupportFeeLvl5", 147000);
		CH_SUPPORT6_FEE = clans.getProperty("ClanHallSupportFeeLvl6", 252000);
		CH_SUPPORT7_FEE = clans.getProperty("ClanHallSupportFeeLvl7", 259000);
		CH_SUPPORT8_FEE = clans.getProperty("ClanHallSupportFeeLvl8", 364000);
		CH_MPREG_FEE_RATIO = clans.getProperty("ClanHallMpRegenerationFunctionFeeRatio", 86400000);
		CH_MPREG1_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl1", 14000);
		CH_MPREG2_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl2", 26250);
		CH_MPREG3_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl3", 45500);
		CH_MPREG4_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl4", 96250);
		CH_MPREG5_FEE = clans.getProperty("ClanHallMpRegenerationFeeLvl5", 140000);
		CH_HPREG_FEE_RATIO = clans.getProperty("ClanHallHpRegenerationFunctionFeeRatio", 86400000);
		CH_HPREG1_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl1", 4900);
		CH_HPREG2_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl2", 5600);
		CH_HPREG3_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl3", 7000);
		CH_HPREG4_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl4", 8166);
		CH_HPREG5_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl5", 10500);
		CH_HPREG6_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl6", 12250);
		CH_HPREG7_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl7", 14000);
		CH_HPREG8_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl8", 15750);
		CH_HPREG9_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl9", 17500);
		CH_HPREG10_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl10", 22750);
		CH_HPREG11_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl11", 26250);
		CH_HPREG12_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl12", 29750);
		CH_HPREG13_FEE = clans.getProperty("ClanHallHpRegenerationFeeLvl13", 36166);
		CH_EXPREG_FEE_RATIO = clans.getProperty("ClanHallExpRegenerationFunctionFeeRatio", 86400000);
		CH_EXPREG1_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl1", 21000);
		CH_EXPREG2_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl2", 42000);
		CH_EXPREG3_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl3", 63000);
		CH_EXPREG4_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl4", 105000);
		CH_EXPREG5_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl5", 147000);
		CH_EXPREG6_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl6", 163331);
		CH_EXPREG7_FEE = clans.getProperty("ClanHallExpRegenerationFeeLvl7", 210000);
		CH_ITEM_FEE_RATIO = clans.getProperty("ClanHallItemCreationFunctionFeeRatio", 86400000);
		CH_ITEM1_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl1", 210000);
		CH_ITEM2_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl2", 490000);
		CH_ITEM3_FEE = clans.getProperty("ClanHallItemCreationFunctionFeeLvl3", 980000);
		CH_CURTAIN_FEE_RATIO = clans.getProperty("ClanHallCurtainFunctionFeeRatio", 86400000);
		CH_CURTAIN1_FEE = clans.getProperty("ClanHallCurtainFunctionFeeLvl1", 2002);
		CH_CURTAIN2_FEE = clans.getProperty("ClanHallCurtainFunctionFeeLvl2", 2625);
		CH_FRONT_FEE_RATIO = clans.getProperty("ClanHallFrontPlatformFunctionFeeRatio", 86400000);
		CH_FRONT1_FEE = clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", 3031);
		CH_FRONT2_FEE = clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", 9331);
	}
	
	/**
	 * Loads event settings.<br>
	 * Such as olympiad, seven signs festival, four sepulchures, dimensional rift, weddings, lottery, fishing championship.
	 */
	private static final void loadEvents()
	{
		final ExProperties events = initProperties(EVENTS_FILE);		
		ALT_GAME_CASTLE_DAWN = events.getProperty("AltCastleForDawn", true);
		ALT_GAME_CASTLE_DUSK = events.getProperty("AltCastleForDusk", true);
		ALT_FESTIVAL_MIN_PLAYER = MathUtil.limit(events.getProperty("AltFestivalMinPlayer", 5), 2, 9);
		ALT_MAXIMUM_PLAYER_CONTRIB = events.getProperty("AltMaxPlayerContrib", 1000000);
		ALT_FESTIVAL_MANAGER_START = events.getProperty("AltFestivalManagerStart", 120000);
		ALT_FESTIVAL_LENGTH = events.getProperty("AltFestivalLength", 1080000);
		ALT_FESTIVAL_CYCLE_LENGTH = events.getProperty("AltFestivalCycleLength", 2280000);
		ALT_FESTIVAL_FIRST_SPAWN = events.getProperty("AltFestivalFirstSpawn", 120000);
		ALT_FESTIVAL_FIRST_SWARM = events.getProperty("AltFestivalFirstSwarm", 300000);
		ALT_FESTIVAL_SECOND_SPAWN = events.getProperty("AltFestivalSecondSpawn", 540000);
		ALT_FESTIVAL_SECOND_SWARM = events.getProperty("AltFestivalSecondSwarm", 720000);
		ALT_FESTIVAL_CHEST_SPAWN = events.getProperty("AltFestivalChestSpawn", 900000);
		
		FS_TIME_ENTRY = events.getProperty("EntryTime", 55);
		FS_TIME_END = events.getProperty("EndTime", 50);
		FS_PARTY_MEMBER_COUNT = MathUtil.limit(events.getProperty("NeededPartyMembers", 4), 1, 9);
		FS_FREE_ENTRY = events.getProperty("FreeEntry", false);
		
		RIFT_MIN_PARTY_SIZE = events.getProperty("RiftMinPartySize", 2);
		RIFT_MAX_JUMPS = events.getProperty("MaxRiftJumps", 4);
		RIFT_SPAWN_DELAY = events.getProperty("RiftSpawnDelay", 10000);
		RIFT_AUTO_JUMPS_TIME_MIN = events.getProperty("AutoJumpsDelayMin", 480);
		RIFT_AUTO_JUMPS_TIME_MAX = events.getProperty("AutoJumpsDelayMax", 600);
		RIFT_ENTER_COST_RECRUIT = events.getProperty("RecruitCost", 18);
		RIFT_ENTER_COST_SOLDIER = events.getProperty("SoldierCost", 21);
		RIFT_ENTER_COST_OFFICER = events.getProperty("OfficerCost", 24);
		RIFT_ENTER_COST_CAPTAIN = events.getProperty("CaptainCost", 27);
		RIFT_ENTER_COST_COMMANDER = events.getProperty("CommanderCost", 30);
		RIFT_ENTER_COST_HERO = events.getProperty("HeroCost", 33);
		RIFT_BOSS_ROOM_TIME_MUTIPLY = events.getProperty("BossRoomTimeMultiply", 1.);
		
		ALLOW_WEDDING = events.getProperty("AllowWedding", false);
		WEDDING_PRICE = events.getProperty("WeddingPrice", 1000000);
		WEDDING_SAMESEX = events.getProperty("WeddingAllowSameSex", false);
		WEDDING_FORMALWEAR = events.getProperty("WeddingFormalWear", true);
		
		ALT_LOTTERY_PRIZE = events.getProperty("AltLotteryPrize", 50000);
		ALT_LOTTERY_TICKET_PRICE = events.getProperty("AltLotteryTicketPrice", 2000);
		ALT_LOTTERY_5_NUMBER_RATE = events.getProperty("AltLottery5NumberRate", 0.6);
		ALT_LOTTERY_4_NUMBER_RATE = events.getProperty("AltLottery4NumberRate", 0.2);
		ALT_LOTTERY_3_NUMBER_RATE = events.getProperty("AltLottery3NumberRate", 0.2);
		ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = events.getProperty("AltLottery2and1NumberPrize", 200);
		
		ALT_FISH_CHAMPIONSHIP_ENABLED = events.getProperty("AltFishChampionshipEnabled", true);
		ALT_FISH_CHAMPIONSHIP_REWARD_ITEM = events.getProperty("AltFishChampionshipRewardItemId", 57);
		ALT_FISH_CHAMPIONSHIP_REWARD_1 = events.getProperty("AltFishChampionshipReward1", 800000);
		ALT_FISH_CHAMPIONSHIP_REWARD_2 = events.getProperty("AltFishChampionshipReward2", 500000);
		ALT_FISH_CHAMPIONSHIP_REWARD_3 = events.getProperty("AltFishChampionshipReward3", 300000);
		ALT_FISH_CHAMPIONSHIP_REWARD_4 = events.getProperty("AltFishChampionshipReward4", 200000);
		ALT_FISH_CHAMPIONSHIP_REWARD_5 = events.getProperty("AltFishChampionshipReward5", 100000);
	}
	
	private static void parseSoloBossLoc(String locValue, int[] target)
	{
		final String[] parts = locValue.split(",");
		if (parts.length >= 3)
		{
			target[0] = Integer.parseInt(parts[0].trim());
			target[1] = Integer.parseInt(parts[1].trim());
			target[2] = Integer.parseInt(parts[2].trim());
		}
	}
	
	private static final void loadSoloBossEvent()
	{
		final ExProperties bossSolo = initProperties(SOLO_BOSS_EVENT_FILE);
		SOLO_BOSS_EVENT = bossSolo.getProperty("SoloBossEvent", false);
		SCREN_MSG_BOSS_SOLO = bossSolo.getProperty("AutoInviteSoloBoss", false);
		SOLO_BOSS_EVENT_INTERVAL_BY_TIME_OF_DAY = bossSolo.getProperty("SoloBossStartTime", "20:00").split(",");
		EVENT_SOLOBOSS_TIME = bossSolo.getProperty("TimeDurationEvent", 1);
		RANGE_SOLO_BOSS = bossSolo.getProperty("RangeRewardPlayers", 5000);
		
		SOLO_BOSS_ID_ONE = bossSolo.getProperty("SoloBossOne", 1);
		parseSoloBossLoc(bossSolo.getProperty("SoloBossOne_Loc", "0,0,0"), SOLO_BOSS_ID_ONE_LOC);
		SOLO_BOSS_ID_TWO = bossSolo.getProperty("SoloBossTWO", 1);
		parseSoloBossLoc(bossSolo.getProperty("SoloBossTWO_Loc", "0,0,0"), SOLO_BOSS_ID_TWO_LOC);
		SOLO_BOSS_ID_THREE = bossSolo.getProperty("SoloBossTHREE", 1);
		parseSoloBossLoc(bossSolo.getProperty("SoloBossTHREE_Loc", "0,0,0"), SOLO_BOSS_ID_THREE_LOC);
		SOLO_BOSS_ID_FOUR = bossSolo.getProperty("SoloBossFOUR", 1);
		parseSoloBossLoc(bossSolo.getProperty("SoloBossFOUR_Loc", "0,0,0"), SOLO_BOSS_ID_FOUR_LOC);
		SOLO_BOSS_ID_FIVE = bossSolo.getProperty("SoloBossFIVE", 1);
		parseSoloBossLoc(bossSolo.getProperty("SoloBossFIVE_Loc", "0,0,0"), SOLO_BOSS_ID_FIVE_LOC);
		
		SOLO_BOSS_REWARDS_ONE = parseStaticReward(bossSolo, "ListaDeRecompensaBoss_1");
		SOLO_BOSS_REWARDS_TWO = parseStaticReward(bossSolo, "ListaDeRecompensaBoss_2");
		SOLO_BOSS_REWARDS_THREE = parseStaticReward(bossSolo, "ListaDeRecompensaBoss_3");
		SOLO_BOSS_REWARDS_FOUR = parseStaticReward(bossSolo, "ListaDeRecompensaBoss_4");
		SOLO_BOSS_REWARDS_FIVE = parseStaticReward(bossSolo, "ListaDeRecompensaBoss_5");
	}
	
	private static final void loadRewardSoloEvent()
	{
		final ExProperties soloEvent = initProperties(REWARD_SOLO_EVENT_FILE);
		SOLO_FARM_BY_TIME_OF_DAY = Boolean.parseBoolean(soloEvent.getProperty("RewardSoloEventEnabled", "false"));
		EVENT_SOLO_FARM_TIME = Integer.parseInt(soloEvent.getProperty("RewardSoloEventTime", "1"));
		EVENT_SOLO_FARM_INTERVAL_BY_TIME_OF_DAY = soloEvent.getProperty("RewardSoloStartTime", "20:00").split(",");
		SOLO_MESSAGE_ENABLED = Boolean.parseBoolean(soloEvent.getProperty("SoloEventMessageEnabled", "false"));
		SOLO_FARM_MESSAGE_TEXT = soloEvent.getProperty("ScreenRewardSoloMessageText", "Happy Hour is active!");
		
		LIST_NPC_CUSTOM_MONSTER.clear();
		for (String listId : soloEvent.getProperty("List_Monster", "0").split(","))
		{
			final String trimmed = listId.trim();
			if (!trimmed.isEmpty())
				LIST_NPC_CUSTOM_MONSTER.add(Integer.parseInt(trimmed));
		}
		
		SOLO_EVENT_DROP_RATE_MULTIPLIER = Double.parseDouble(soloEvent.getProperty("DropRateMultiplier", "1.0"));
	}
	
	/**
	 * Loads geoengine settings.
	 */
	private static final void loadGeoengine()
	{
		final ExProperties geoengine = initProperties(GEOENGINE_FILE);
		GEODATA_PATH = geoengine.getProperty("GeoDataPath", "./data/geodata/");
		
		PART_OF_CHARACTER_HEIGHT = geoengine.getProperty("PartOfCharacterHeight", 75);
		MAX_OBSTACLE_HEIGHT = geoengine.getProperty("MaxObstacleHeight", 32);
		
		PATHFIND_BUFFERS = geoengine.getProperty("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
		BASE_WEIGHT = geoengine.getProperty("BaseWeight", 10);
		DIAGONAL_WEIGHT = geoengine.getProperty("DiagonalWeight", 14);
		OBSTACLE_MULTIPLIER = geoengine.getProperty("ObstacleMultiplier", 10);
		HEURISTIC_WEIGHT = geoengine.getProperty("HeuristicWeight", 20);
		MAX_ITERATIONS = geoengine.getProperty("MaxIterations", 3500);
		DEBUG_PATH = geoengine.getProperty("DebugPath", false);
		DEBUG_GEO_NODE = geoengine.getProperty("DebugGeoNode", false);
	}
	
	/**
	 * Loads hex ID settings.
	 */
	private static final void loadHexID()
	{
		final ExProperties hexid = initProperties(HEXID_FILE);
		SERVER_ID = Integer.parseInt(hexid.getProperty("ServerID"));
		HEX_ID = new BigInteger(hexid.getProperty("HexID"), 16).toByteArray();
	}
	
	/**
	 * Saves hex ID file.
	 * @param serverId : The ID of server.
	 * @param hexId : The hex ID of server.
	 */
	public static final void saveHexid(int serverId, String hexId)
	{
		saveHexid(serverId, hexId, HEXID_FILE);
	}
	
	/**
	 * Saves hexID file.
	 * @param serverId : The ID of server.
	 * @param hexId : The hexID of server.
	 * @param filename : The file name.
	 */
	public static final void saveHexid(int serverId, String hexId, String filename)
	{
		try
		{
			final File file = new File(filename);
			file.createNewFile();
			
			final Properties hexSetting = new Properties();
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			
			try (OutputStream out = new FileOutputStream(file))
			{
				hexSetting.store(out, "the hexID to auth into login");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to save hex ID to '{}' file.", e, filename);
		}
	}
	
	/**
	 * Loads NPC settings.<br>
	 * Such as champion monsters, NPC buffer, class master, wyvern, raid bosses and grand bosses, AI.
	 */
	private static final void loadNpcs()
	{
		final ExProperties npcs = initProperties(NPCS_FILE);
		CHAMPION_FREQUENCY = npcs.getProperty("ChampionFrequency", 0);
		CHAMP_MIN_LVL = npcs.getProperty("ChampionMinLevel", 20);
		CHAMP_MAX_LVL = npcs.getProperty("ChampionMaxLevel", 70);
		CHAMPION_HP = npcs.getProperty("ChampionHp", 8);
		CHAMPION_HP_REGEN = npcs.getProperty("ChampionHpRegen", 1.);
		CHAMPION_REWARDS = npcs.getProperty("ChampionRewards", 8);
		CHAMPION_ADENAS_REWARDS = npcs.getProperty("ChampionAdenasRewards", 1);
		CHAMPION_ATK = npcs.getProperty("ChampionAtk", 1.);
		CHAMPION_SPD_ATK = npcs.getProperty("ChampionSpdAtk", 1.);
		CHAMPION_REWARD = npcs.getProperty("ChampionRewardItem", 0);
		CHAMPION_REWARD_ID = npcs.getProperty("ChampionRewardItemID", 6393);
		CHAMPION_REWARD_QTY = npcs.getProperty("ChampionRewardItemQty", 1);
		CHAMPION_AURA = npcs.getProperty("ChampionAura", true);
		CHAMPION_AURA_COLOR = Math.min(2, Math.max(0, npcs.getProperty("ChampionAuraColor", 1)));

		ALLOW_CLASS_MASTERS = npcs.getProperty("AllowClassMasters", false);
		ALLOW_ENTIRE_TREE = npcs.getProperty("AllowEntireTree", false);
		if (ALLOW_CLASS_MASTERS)
			CLASS_MASTER_SETTINGS = new ClassMasterSettings(npcs.getProperty("ConfigClassMaster"));
		
		ANNOUNCE_MAMMON_SPAWN = npcs.getProperty("AnnounceMammonSpawn", true);
		ALT_MOB_AGRO_IN_PEACEZONE = npcs.getProperty("AltMobAgroInPeaceZone", true);
		SHOW_NPC_LVL = npcs.getProperty("ShowNpcLevel", false);
		SHOW_NPC_CREST = npcs.getProperty("ShowNpcCrest", false);
		SHOW_SUMMON_CREST = npcs.getProperty("ShowSummonCrest", false);
		
		WYVERN_ALLOW_UPGRADER = npcs.getProperty("AllowWyvernUpgrader", true);
		WYVERN_REQUIRED_LEVEL = npcs.getProperty("RequiredStriderLevel", 55);
		WYVERN_REQUIRED_CRYSTALS = npcs.getProperty("RequiredCrystalsNumber", 10);

		GUARD_ATTACK_AGGRO_MOB = npcs.getProperty("GuardAttackAggroMob", false);
		MAX_DRIFT_RANGE = npcs.getProperty("MaxDriftRange", 300);
		MAX_ATTACK_TIMEOUT = npcs.getProperty("MaxAttackTimeout", 90) * 1000;
		MIN_NPC_ANIMATION = npcs.getProperty("MinNPCAnimation", 20);
		MAX_NPC_ANIMATION = npcs.getProperty("MaxNPCAnimation", 40);
		MIN_MONSTER_ANIMATION = npcs.getProperty("MinMonsterAnimation", 10);
		MAX_MONSTER_ANIMATION = npcs.getProperty("MaxMonsterAnimation", 40);
		
		KILL_SPAWN_ON_DEATH_ENABLED = npcs.getProperty("KillSpawnOnDeathEnabled", false);
		KILL_SPAWN_ON_DEATH_LIST = npcs.getProperty("KillSpawnOnDeathList", new int[0]);
		KILL_SPAWN_NPC = npcs.getProperty("KillSpawnNpc", new int[0]);
		KILL_SPAWN_CANTIDAD = npcs.getProperty("KillSpawnCantidad", 1);
		KILL_SPAWN_CHANCE = npcs.getProperty("KillSpawnChance", 100);
		KILL_SPAWN_DELAY = npcs.getProperty("KillSpawnDelay", 2000);
		KILL_SPAWN_DESPAWN = npcs.getProperty("KillSpawnDespawn", 60000);
	}
	
	/**
	 * Loads player settings.<br>
	 * Such as stats, inventory/warehouse, enchant, augmentation, karma, party, admin, petition, skill learn.
	 */
	private static final void loadPlayers()
	{
		final ExProperties players = initProperties(PLAYERS_FILE);
		EFFECT_CANCELING = players.getProperty("CancelLesserEffect", true);
		HP_REGEN_MULTIPLIER = players.getProperty("HpRegenMultiplier", 1.);
		MP_REGEN_MULTIPLIER = players.getProperty("MpRegenMultiplier", 1.);
		CP_REGEN_MULTIPLIER = players.getProperty("CpRegenMultiplier", 1.);
		PLAYER_SPAWN_PROTECTION = players.getProperty("PlayerSpawnProtection", 0);
		PLAYER_FAKEDEATH_UP_PROTECTION = players.getProperty("PlayerFakeDeathUpProtection", 0);
		RESPAWN_RESTORE_HP = players.getProperty("RespawnRestoreHP", 0.7);
		MAX_PVTSTORE_SLOTS_DWARF = players.getProperty("MaxPvtStoreSlotsDwarf", 5);
		MAX_PVTSTORE_SLOTS_OTHER = players.getProperty("MaxPvtStoreSlotsOther", 4);
		DEEPBLUE_DROP_RULES = players.getProperty("UseDeepBlueDropRules", true);
		ALT_GAME_DELEVEL = players.getProperty("Delevel", true);
		DEATH_PENALTY_CHANCE = players.getProperty("DeathPenaltyChance", 20);
		
		INVENTORY_MAXIMUM_NO_DWARF = players.getProperty("MaximumSlotsForNoDwarf", 80);
		INVENTORY_MAXIMUM_DWARF = players.getProperty("MaximumSlotsForDwarf", 100);
		INVENTORY_MAXIMUM_QUEST_ITEMS = players.getProperty("MaximumSlotsForQuestItems", 100);
		INVENTORY_MAXIMUM_PET = players.getProperty("MaximumSlotsForPet", 12);
		MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, INVENTORY_MAXIMUM_DWARF);
		ALT_WEIGHT_LIMIT = players.getProperty("AltWeightLimit", 1);
		WAREHOUSE_SLOTS_NO_DWARF = players.getProperty("MaximumWarehouseSlotsForNoDwarf", 100);
		WAREHOUSE_SLOTS_DWARF = players.getProperty("MaximumWarehouseSlotsForDwarf", 120);
		WAREHOUSE_SLOTS_CLAN = players.getProperty("MaximumWarehouseSlotsForClan", 150);
		FREIGHT_SLOTS = players.getProperty("MaximumFreightSlots", 20);
		ALT_GAME_FREIGHTS = players.getProperty("AltGameFreights", false);
		ALT_GAME_FREIGHT_PRICE = players.getProperty("AltGameFreightPrice", 1000);

		AUGMENTATION_NG_SKILL_CHANCE = players.getProperty("AugmentationNGSkillChance", 15);
		AUGMENTATION_NG_GLOW_CHANCE = players.getProperty("AugmentationNGGlowChance", 0);
		AUGMENTATION_MID_SKILL_CHANCE = players.getProperty("AugmentationMidSkillChance", 30);
		AUGMENTATION_MID_GLOW_CHANCE = players.getProperty("AugmentationMidGlowChance", 40);
		AUGMENTATION_HIGH_SKILL_CHANCE = players.getProperty("AugmentationHighSkillChance", 45);
		AUGMENTATION_HIGH_GLOW_CHANCE = players.getProperty("AugmentationHighGlowChance", 70);
		AUGMENTATION_TOP_SKILL_CHANCE = players.getProperty("AugmentationTopSkillChance", 60);
		AUGMENTATION_TOP_GLOW_CHANCE = players.getProperty("AugmentationTopGlowChance", 100);
		AUGMENTATION_BASESTAT_CHANCE = players.getProperty("AugmentationBaseStatChance", 1);
		ALLOW_TRADE_AUGMENTED_ITEMS = players.getProperty("AllowTradeAugmentedItems", false);
		FAST_AUGMENT_ADENA = Math.max(0, players.getProperty("FastAugmentAdena", 250000));
		FAST_REMOVE_ADENA = Math.max(0, players.getProperty("FastRemoveAdena", 250000));
		
		KARMA_PLAYER_CAN_BE_KILLED_IN_PZ = players.getProperty("KarmaPlayerCanBeKilledInPeaceZone", false);
		KARMA_PLAYER_CAN_SHOP = players.getProperty("KarmaPlayerCanShop", false);
		KARMA_PLAYER_CAN_USE_GK = players.getProperty("KarmaPlayerCanUseGK", false);
		KARMA_PLAYER_CAN_TELEPORT = players.getProperty("KarmaPlayerCanTeleport", true);
		KARMA_PLAYER_CAN_TRADE = players.getProperty("KarmaPlayerCanTrade", true);
		KARMA_PLAYER_CAN_USE_WH = players.getProperty("KarmaPlayerCanUseWareHouse", true);
		KARMA_DROP_GM = players.getProperty("CanGMDropEquipment", false);
		KARMA_AWARD_PK_KILL = players.getProperty("AwardPKKillPVPPoint", true);
		KARMA_PK_LIMIT = players.getProperty("MinimumPKRequiredToDrop", 5);
		KARMA_NONDROPPABLE_PET_ITEMS = players.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
		KARMA_NONDROPPABLE_ITEMS = players.getProperty("ListOfNonDroppableItemsForPK", "1147,425,1146,461,10,2368,7,6,2370,2369");
		
		String[] array = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
		KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[array.length];
		
		for (int i = 0; i < array.length; i++)
			KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(array[i]);
		
		array = KARMA_NONDROPPABLE_ITEMS.split(",");
		KARMA_LIST_NONDROPPABLE_ITEMS = new int[array.length];
		
		for (int i = 0; i < array.length; i++)
			KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(array[i]);
		
		// sorting so binarySearch can be used later
		Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
		Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);
		
		PVP_NORMAL_TIME = players.getProperty("PvPVsNormalTime", 15000);
		PVP_PVP_TIME = players.getProperty("PvPVsPvPTime", 30000);
		
		PARTY_XP_CUTOFF_METHOD = players.getProperty("PartyXpCutoffMethod", "level");
		PARTY_XP_CUTOFF_PERCENT = players.getProperty("PartyXpCutoffPercent", 3.);
		PARTY_XP_CUTOFF_LEVEL = players.getProperty("PartyXpCutoffLevel", 20);
		PARTY_RANGE = players.getProperty("PartyRange", 1500);
		
		DEFAULT_ACCESS_LEVEL = players.getProperty("DefaultAccessLevel", 0);
		GM_HERO_AURA = players.getProperty("GMHeroAura", false);
		GM_STARTUP_INVULNERABLE = players.getProperty("GMStartupInvulnerable", true);
		GM_STARTUP_INVISIBLE = players.getProperty("GMStartupInvisible", true);
		GM_STARTUP_SILENCE = players.getProperty("GMStartupSilence", true);
		GM_STARTUP_AUTO_LIST = players.getProperty("GMStartupAutoList", true);
		
		PETITIONING_ALLOWED = players.getProperty("PetitioningAllowed", true);
		MAX_PETITIONS_PER_PLAYER = players.getProperty("MaxPetitionsPerPlayer", 5);
		MAX_PETITIONS_PENDING = players.getProperty("MaxPetitionsPending", 25);
		
		IS_CRAFTING_ENABLED = players.getProperty("CraftingEnabled", true);
		DWARF_RECIPE_LIMIT = players.getProperty("DwarfRecipeLimit", 50);
		COMMON_RECIPE_LIMIT = players.getProperty("CommonRecipeLimit", 50);
		ALT_BLACKSMITH_USE_RECIPES = players.getProperty("AltBlacksmithUseRecipes", true);
		
		AUTO_LEARN_SKILLS = players.getProperty("AutoLearnSkills", false);
		MAGIC_FAILURES = players.getProperty("MagicFailures", true);
		PERFECT_SHIELD_BLOCK_RATE = players.getProperty("PerfectShieldBlockRate", 5);
		SHIELD_BLOCK_ENABLED = players.getProperty("ShieldBlockEnabled", false);
		SHIELD_DAMAGE_REDUCE_ENABLED = players.getProperty("ShieldDamageReduceEnabled", true);
		SHIELD_BASE_DAMAGE_REDUCE = players.getProperty("ShieldBaseDamageReduce", 0.20);
		SHIELD_DAMAGE_REDUCE_MAX = players.getProperty("ShieldDamageReduceMax", 0.70);
		LIFE_CRYSTAL_NEEDED = players.getProperty("LifeCrystalNeeded", true);
		SP_BOOK_NEEDED = players.getProperty("SpBookNeeded", true);
		ES_SP_BOOK_NEEDED = players.getProperty("EnchantSkillSpBookNeeded", true);
		DIVINE_SP_BOOK_NEEDED = players.getProperty("DivineInspirationSpBookNeeded", true);
		SUBCLASS_WITHOUT_QUESTS = players.getProperty("SubClassWithoutQuests", false);
		
	}
	
	/**
	 * Loads siege settings.
	 */
	private static final void loadSieges()
	{
		final ExProperties sieges = initProperties(Config.SIEGE_FILE);
		
		SIEGE_LENGTH = sieges.getProperty("SiegeLength", 120);
		MINIMUM_CLAN_LEVEL = sieges.getProperty("SiegeClanMinLevel", 4);
		MAX_ATTACKERS_NUMBER = sieges.getProperty("AttackerMaxClans", 10);
		MAX_DEFENDERS_NUMBER = sieges.getProperty("DefenderMaxClans", 10);
		ATTACKERS_RESPAWN_DELAY = sieges.getProperty("AttackerRespawn", 10000);
		REWARD_WINNER_SIEGE_CLAN = sieges.parseIntIntList("MembersRewardsID", "57-100");
		LEADER_REWARD_WINNER_SIEGE_CLAN = sieges.parseIntIntList("LeaderRewardsID", "57-400");
	}
	
	/**
	 * Loads gameserver settings.<br>
	 * IP addresses, database, rates, feature enabled/disabled, misc.
	 */
	private static final void loadServer()
	{
		final ExProperties server = initProperties(SERVER_FILE);
		
		GAMESERVER_HOSTNAME = server.getProperty("GameserverHostname");
		PORT_GAME = server.getProperty("GameserverPort", 7777);
		
		HOSTNAME = server.getProperty("Hostname", "*");
		
		GAME_SERVER_LOGIN_PORT = server.getProperty("LoginPort", 9014);
		GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHost", "127.0.0.1");
		
		REQUEST_ID = server.getProperty("RequestServerID", 0);
		ACCEPT_ALTERNATE_ID = server.getProperty("AcceptAlternateID", true);
		
		DATABASE_URL = server.getProperty("URL", "jdbc:mysql://localhost/acis?serverTimezone=UTC");
		DATABASE_LOGIN = server.getProperty("Login", "root");
		DATABASE_PASSWORD = server.getProperty("Password", "");
		DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 10);
		
		SERVER_LIST_BRACKET = server.getProperty("ServerListBrackets", false);
		SERVER_LIST_CLOCK = server.getProperty("ServerListClock", false);
		SERVER_GMONLY = server.getProperty("ServerGMOnly", false);
		SERVER_LIST_AGE = server.getProperty("ServerListAgeLimit", 0);
		SERVER_LIST_TESTSERVER = server.getProperty("TestServer", false);
		SERVER_LIST_PVPSERVER = server.getProperty("PvpServer", true);
		
		DELETE_DAYS = server.getProperty("DeleteCharAfterDays", 7);
		MAXIMUM_ONLINE_USERS = server.getProperty("MaximumOnlineUsers", 100);
		
		AUTO_LOOT = server.getProperty("AutoLoot", false);
		AUTO_LOOT_HERBS = server.getProperty("AutoLootHerbs", false);
		AUTO_LOOT_RAID = server.getProperty("AutoLootRaid", false);
		
		ALLOW_DISCARDITEM = server.getProperty("AllowDiscardItem", true);
		MULTIPLE_ITEM_DROP = server.getProperty("MultipleItemDrop", true);
		HERB_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyHerbTime", 15) * 1000;
		ITEM_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyItemTime", 600) * 1000;
		EQUIPABLE_ITEM_AUTO_DESTROY_TIME = server.getProperty("AutoDestroyEquipableItemTime", 0) * 1000;
		SPECIAL_ITEM_DESTROY_TIME = new HashMap<>();
		String[] data = server.getProperty("AutoDestroySpecialItemTime", (String[]) null, ",");
		if (data != null)
		{
			for (String itemData : data)
			{
				String[] item = itemData.split("-");
				SPECIAL_ITEM_DESTROY_TIME.put(Integer.parseInt(item[0]), Integer.parseInt(item[1]) * 1000);
			}
		}
		PLAYER_DROPPED_ITEM_MULTIPLIER = server.getProperty("PlayerDroppedItemMultiplier", 1);
				
		ALLOW_FREIGHT = server.getProperty("AllowFreight", true);
		ALLOW_WAREHOUSE = server.getProperty("AllowWarehouse", true);
		ALLOW_WEAR = server.getProperty("AllowWear", true);
		WEAR_DELAY = server.getProperty("WearDelay", 5);
		WEAR_PRICE = server.getProperty("WearPrice", 10);
		ALLOW_LOTTERY = server.getProperty("AllowLottery", true);
		ALLOW_WATER = server.getProperty("AllowWater", true);
		ALLOW_MANOR = server.getProperty("AllowManor", true);
		ALLOW_BOAT = server.getProperty("AllowBoat", true);
		ALLOW_CURSED_WEAPONS = server.getProperty("AllowCursedWeapons", true);
		
		ENABLE_FALLING_DAMAGE = server.getProperty("EnableFallingDamage", true);
		
		ALT_DEV_NO_SPAWNS = server.getProperty("NoSpawns", false);
		DEVELOPER = server.getProperty("Developer", false);
		PACKET_HANDLER_DEBUG = server.getProperty("PacketHandlerDebug", false);
		DEBUG_MOVEMENT = server.getProperty("DebugMovement", 0) * 1000;
		
		DEADLOCK_DETECTOR = server.getProperty("DeadLockDetector", false);
		DEADLOCK_CHECK_INTERVAL = server.getProperty("DeadLockCheckInterval", 20);
		RESTART_ON_DEADLOCK = server.getProperty("RestartOnDeadlock", false);
		
		LOG_CHAT = server.getProperty("LogChat", false);
		LOG_ITEMS = server.getProperty("LogItems", false);
		GMAUDIT = server.getProperty("GMAudit", false);
		
		ENABLE_COMMUNITY_BOARD = server.getProperty("EnableCommunityBoard", false);
		BBS_DEFAULT = server.getProperty("BBSDefault", "_bbshome");
		
		SCHEDULED_THREAD_POOL_COUNT = server.getProperty("ScheduledThreadPoolCount", -1);
		THREADS_PER_SCHEDULED_THREAD_POOL = server.getProperty("ThreadsPerScheduledThreadPool", 4);
		INSTANT_THREAD_POOL_COUNT = server.getProperty("InstantThreadPoolCount", -1);
		THREADS_PER_INSTANT_THREAD_POOL = server.getProperty("ThreadsPerInstantThreadPool", 2);
		
		L2WALKER_PROTECTION = server.getProperty("L2WalkerProtection", false);
		ZONE_TOWN = server.getProperty("ZoneTown", 0);
		SERVER_NEWS = server.getProperty("ShowServerNews", false);
		DISABLE_TUTORIAL = server.getProperty("DisableTutorial", false);
		MIN_PROTOCOL_REVISION = server.getProperty("MinProtocolRevision", 730);
		MAX_PROTOCOL_REVISION = server.getProperty("MaxProtocolRevision", 746);
		if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
		   throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server.properties.");		
	}
	
	/**
	 * Loads loginserver settings.<br>
	 * IP addresses, database, account, misc.
	 */
	private static final void loadLogin()
	{
		final ExProperties server = initProperties(LOGIN_CONFIGURATION_FILE);
		HOSTNAME = server.getProperty("Hostname", "localhost");
		
		LOGIN_BIND_ADDRESS = server.getProperty("LoginserverHostname", "*");
		PORT_LOGIN = server.getProperty("LoginserverPort", 2106);
		
		GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHostname", "*");
		GAME_SERVER_LOGIN_PORT = server.getProperty("LoginPort", 9014);
		
		LOGIN_TRY_BEFORE_BAN = server.getProperty("LoginTryBeforeBan", 3);
		LOGIN_BLOCK_AFTER_BAN = server.getProperty("LoginBlockAfterBan", 600);
		ACCEPT_NEW_GAMESERVER = server.getProperty("AcceptNewGameServer", false);
		
		SHOW_LICENCE = server.getProperty("ShowLicence", true);
		
		DATABASE_URL = server.getProperty("URL", "jdbc:mysql://localhost/acis?serverTimezone=UTC");
		DATABASE_LOGIN = server.getProperty("Login", "root");
		DATABASE_PASSWORD = server.getProperty("Password", "");
		DATABASE_MAX_CONNECTIONS = server.getProperty("MaximumDbConnections", 10);
		
		AUTO_CREATE_ACCOUNTS = server.getProperty("AutoCreateAccounts", true);
		
		LOG_LOGIN_CONTROLLER = server.getProperty("LogLoginController", false);
		
		FLOOD_PROTECTION = server.getProperty("EnableFloodProtection", true);
		FAST_CONNECTION_LIMIT = server.getProperty("FastConnectionLimit", 15);
		NORMAL_CONNECTION_TIME = server.getProperty("NormalConnectionTime", 700);
		FAST_CONNECTION_TIME = server.getProperty("FastConnectionTime", 350);
		MAX_CONNECTION_PER_IP = server.getProperty("MaxConnectionPerIP", 50);
	}
	
	public static final void loadGameServer()
	{
		loadGameServer(true);
	}
	
	/**
	 * Hot reload: skip hexid. Changing it live does not re-auth to login and a bad file used to kill the admin thread.
	 */
	public static final void reloadGameServer()
	{
		loadGameServer(false);
	}
	
	private static final void loadGameServer(boolean loadHexId)
	{
		LOGGER.info("Loading gameserver configuration files.");
		
		// Starting settings
		loadStarting();
		
		// Bosses settings
		loadBosses();
		
		// Floods settings
		loadFloods();
		
		// Rates settings
		loadRates();
		
		// Olympiad settings
		loadOlympiads();
		
		// Agathions settings
		loadAgathions();
		
		// Party Zone settings
		load_PartyFarm();
		
		// PvP Farm settings (clon del Party Farm: flag por rango de mob + reward individual)
		load_PvpFarm();
		
		// Instance Farm Zones (Time / Kill)
		loadInstanceFarm();
		
		// Anti farm multi-cuenta por HWID
		loadFarmHwid();
		loadAutoFarm();
		
		// Rune settings
		loadRuneSystem();
		
		// PvP settings
		loadPvpSystem();
		
		// Auto pots settings
		loadAPots();
		
		// Rebirth settings
		loadRebirthystem();

		// Aio settings
		loadAioSystem();

		// Dona settings
		loadDonationManager();
		
		// captcha settings
		loadProtect();
		
		// vip settings
		loadVip();
		
		loadAutoSpoil();
		
		// hero coins / .hero
		loadHero();
		
		// tournament settings
		loadTournament();
		loadCustomOly();
		loadMedalEvent();
		loadBuffer();
		
		// mods settings
		loadMods();
		
		// url settings
		loadUrl();
		
		// offline settings
		loadOfflineShop();
		
		// dressme settings
		loadDressme();
		
		// sub settings
		loadSubclass();
		
		// multiskill settings
		loadMultiSkill();
		
		// skill enchant NPC settings
		loadSkillEnchant();
		
		// battle pass (free + VIP)
		loadBattlePass();
		
		// balance settings
		loadBalance();
		
		// skills chance multiplier settings
		loadSkillsChance();
		
		// class vs class damage balance
		loadClassBalance();
		
		// enchant settings
		loadEnchant();

		// skins settings
		loadSkins();
		
		// clans settings
		loadClans();
		
		// events settings
		loadEvents();
		loadSoloBossEvent();
		loadRewardSoloEvent();
		
		// geoengine settings
		loadGeoengine();
		
		if (loadHexId)
			loadHexID();
		
		// NPCs/monsters settings
		loadNpcs();
		
		// players settings
		loadPlayers();
		
		// siege settings
		loadSieges();
		
		// server settings
		loadServer();
		
		// custom community board
		net.sf.l2j.gameserver.communitybbs.CommunityBoardConfig.load();
	}
	
	public static final void loadLoginServer()
	{
		LOGGER.info("Loading loginserver configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadAccountManager()
	{
		LOGGER.info("Loading account manager configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadGameServerRegistration()
	{
		LOGGER.info("Loading gameserver registration configuration files.");
		
		// login settings
		loadLogin();
	}
	
	public static final void loadGeodataConverter()
	{
		LOGGER.info("Loading geodata converter configuration files.");
		
		// geoengine settings
		loadGeoengine();
	}
	
	public static final class ClassMasterSettings
	{
		private final Map<Integer, Boolean> _allowedClassChange;
		private final Map<Integer, List<IntIntHolder>> _claimItems;
		private final Map<Integer, List<IntIntHolder>> _rewardItems;
		
		public ClassMasterSettings(String configLine)
		{
			_allowedClassChange = new HashMap<>(3);
			_claimItems = new HashMap<>(3);
			_rewardItems = new HashMap<>(3);
			
			if (configLine != null)
				parseConfigLine(configLine.trim());
		}
		
		private void parseConfigLine(String configLine)
		{
			StringTokenizer st = new StringTokenizer(configLine, ";");
			while (st.hasMoreTokens())
			{
				// Get allowed class change.
				int job = Integer.parseInt(st.nextToken());
				
				_allowedClassChange.put(job, true);
				
				List<IntIntHolder> items = new ArrayList<>();
				
				// Parse items needed for class change.
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						items.add(new IntIntHolder(Integer.parseInt(st3.nextToken()), Integer.parseInt(st3.nextToken())));
					}
				}
				
				// Feed the map, and clean the list.
				_claimItems.put(job, items);
				items = new ArrayList<>();
				
				// Parse gifts after class change.
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						items.add(new IntIntHolder(Integer.parseInt(st3.nextToken()), Integer.parseInt(st3.nextToken())));
					}
				}
				
				_rewardItems.put(job, items);
			}
		}
		
		public boolean isAllowed(int job)
		{
			if (_allowedClassChange == null)
				return false;
			
			if (_allowedClassChange.containsKey(job))
				return _allowedClassChange.get(job);
			
			return false;
		}
		
		public List<IntIntHolder> getRewardItems(int job)
		{
			return _rewardItems.get(job);
		}
		
		public List<IntIntHolder> getRequiredItems(int job)
		{
			return _claimItems.get(job);
		}
	}
	
	public static List<Integer> parseNpcIdList(String value)
	{
		final List<Integer> list = new ArrayList<>();
		if (value == null || value.isEmpty())
			return list;
		
		for (String val : value.split(","))
		{
			val = val.trim();
			if (val.isEmpty())
				continue;
			
			final int id = Integer.parseInt(val);
			if (id > 0)
				list.add(id);
		}
		return list;
	}
	
	public static List<Integer> parseIntegerList(String value)
	{
		final List<Integer> list = new ArrayList<>();
		if (value == null || value.isEmpty())
			return list;
		
		for (String val : value.split(","))
		{
			val = val.trim();
			if (val.isEmpty())
				continue;
			
			list.add(Integer.parseInt(val));
		}
		return list;
	}
	
	public static List<String> parseStringList(String value)
	{
		final List<String> list = new ArrayList<>();
		if (value == null || value.isEmpty())
			return list;
		
		for (String val : value.split(","))
		{
			val = val.trim();
			if (!val.isEmpty())
				list.add(val);
		}
		return list;
	}

	/**
	 * Recompensas del Instance Farm: itemId,min,max,chance;itemId,min,max,chance;... (chance en %, acepta decimales).
	 */
	private static void parseFarmRewards(String value, List<double[]> out)
	{
		out.clear();
		for (String reward : value.split(";"))
		{
			final String[] parts = reward.trim().split(",");
			if (parts.length != 4)
				continue;
			
			try
			{
				final double itemId = Integer.parseInt(parts[0].trim());
				final double min = Integer.parseInt(parts[1].trim());
				final double max = Integer.parseInt(parts[2].trim());
				final double chance = Double.parseDouble(parts[3].trim());
				if (itemId > 0 && min > 0 && max >= min && chance > 0)
					out.add(new double[]
					{
						itemId,
						min,
						max,
						chance
					});
			}
			catch (NumberFormatException e)
			{
				// recompensa mal escrita, se ignora
			}
		}
	}
}