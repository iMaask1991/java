package net.sf.l2j.gameserver.instancemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * PvP class-vs-class damage multipliers loaded from balance/classes/*.ini
 */
public final class ClassDamageManager
{
	private static final CLogger LOGGER = new CLogger(ClassDamageManager.class.getName());
	
	private static final Map<String, int[]> CLASS_NAME_TO_IDS = new HashMap<>();
	private static final Map<Integer, Map<Integer, Float>> MULTIPLIERS = new HashMap<>();
	
	static
	{
		// name from .ini -> 2nd + 3rd class ids (same as L2jMega)
		put("Duelist", ClassId.GLADIATOR.getId(), ClassId.DUELIST.getId());
		put("DreadNought", ClassId.WARLORD.getId(), ClassId.DREADNOUGHT.getId());
		put("Dreadnought", ClassId.WARLORD.getId(), ClassId.DREADNOUGHT.getId());
		put("Phoenix_Knight", ClassId.PALADIN.getId(), ClassId.PHOENIX_KNIGHT.getId());
		put("Hell_Knight", ClassId.DARK_AVENGER.getId(), ClassId.HELL_KNIGHT.getId());
		put("Sagitarius", ClassId.HAWKEYE.getId(), ClassId.SAGGITARIUS.getId());
		put("Sagittarius", ClassId.HAWKEYE.getId(), ClassId.SAGGITARIUS.getId());
		put("Adventurer", ClassId.TREASURE_HUNTER.getId(), ClassId.ADVENTURER.getId());
		put("Archmage", ClassId.SORCERER.getId(), ClassId.ARCHMAGE.getId());
		put("Soultaker", ClassId.NECROMANCER.getId(), ClassId.SOULTAKER.getId());
		put("Arcana_Lord", ClassId.WARLOCK.getId(), ClassId.ARCANA_LORD.getId());
		put("Cardinal", ClassId.BISHOP.getId(), ClassId.CARDINAL.getId());
		put("Hierophant", ClassId.PROPHET.getId(), ClassId.HIEROPHANT.getId());
		put("Eva_Templar", ClassId.TEMPLE_KNIGHT.getId(), ClassId.EVAS_TEMPLAR.getId());
		put("Sword_Muse", ClassId.SWORD_SINGER.getId(), ClassId.SWORD_MUSE.getId());
		put("Wind_Rider", ClassId.PLAINS_WALKER.getId(), ClassId.WIND_RIDER.getId());
		put("Moonlight_Sentinel", ClassId.SILVER_RANGER.getId(), ClassId.MOONLIGHT_SENTINEL.getId());
		put("Mystic_Muse", ClassId.SPELLSINGER.getId(), ClassId.MYSTIC_MUSE.getId());
		put("Elemental_Master", ClassId.ELEMENTAL_SUMMONER.getId(), ClassId.ELEMENTAL_MASTER.getId());
		put("Eva_s_Saint", ClassId.ELVEN_ELDER.getId(), ClassId.EVAS_SAINT.getId());
		put("Shillien_Templar", ClassId.SHILLIEN_KNIGHT.getId(), ClassId.SHILLIEN_TEMPLAR.getId());
		put("Spectral_Dancer", ClassId.BLADEDANCER.getId(), ClassId.SPECTRAL_DANCER.getId());
		put("Ghost_Hunter", ClassId.ABYSS_WALKER.getId(), ClassId.GHOST_HUNTER.getId());
		put("Ghost_Sentinel", ClassId.PHANTOM_RANGER.getId(), ClassId.GHOST_SENTINEL.getId());
		put("Storm_Screamer", ClassId.SPELLHOWLER.getId(), ClassId.STORM_SCREAMER.getId());
		put("Spectral_Master", ClassId.PHANTOM_SUMMONER.getId(), ClassId.SPECTRAL_MASTER.getId());
		put("Shillien_Saint", ClassId.SHILLIEN_ELDER.getId(), ClassId.SHILLIEN_SAINT.getId());
		put("Titan", ClassId.DESTROYER.getId(), ClassId.TITAN.getId());
		put("Grand_Khauatari", ClassId.TYRANT.getId(), ClassId.GRAND_KHAVATARI.getId());
		put("Grand_Khavatari", ClassId.TYRANT.getId(), ClassId.GRAND_KHAVATARI.getId());
		put("Dominator", ClassId.OVERLORD.getId(), ClassId.DOMINATOR.getId());
		put("Doomcryer", ClassId.WARCRYER.getId(), ClassId.DOOMCRYER.getId());
		put("Fortune_Seeker", ClassId.BOUNTY_HUNTER.getId(), ClassId.FORTUNE_SEEKER.getId());
		put("Maestro", ClassId.WARSMITH.getId(), ClassId.MAESTRO.getId());
	}
	
	private ClassDamageManager()
	{
	}
	
	private static void put(String name, int secondClassId, int thirdClassId)
	{
		CLASS_NAME_TO_IDS.put(name, new int[]
		{
			secondClassId,
			thirdClassId
		});
	}
	
	public static synchronized void load()
	{
		MULTIPLIERS.clear();
		
		final File dir = new File(Config.CLASS_BALANCE_CLASSES_DIR);
		if (!dir.isDirectory())
		{
			LOGGER.warn("ClassDamageManager: directory not found: {}", Config.CLASS_BALANCE_CLASSES_DIR);
			return;
		}
		
		final File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".ini"));
		if (files == null || files.length == 0)
		{
			LOGGER.warn("ClassDamageManager: no .ini files in {}", Config.CLASS_BALANCE_CLASSES_DIR);
			return;
		}
		
		int entries = 0;
		int skipped = 0;
		
		for (File file : files)
		{
			final Properties props = new Properties();
			try (InputStream is = new FileInputStream(file))
			{
				props.load(is);
			}
			catch (Exception e)
			{
				LOGGER.warn("ClassDamageManager: failed to load {}", e, file.getName());
				continue;
			}
			
			for (String key : props.stringPropertyNames())
			{
				if (key == null || key.trim().isEmpty() || key.startsWith("#"))
					continue;
				
				final int vs = key.indexOf("_vs_");
				if (vs <= 0)
				{
					skipped++;
					continue;
				}
				
				final String attackerName = key.substring(0, vs);
				final String targetName = key.substring(vs + 4);
				final int[] attackerIds = CLASS_NAME_TO_IDS.get(attackerName);
				final int[] targetIds = CLASS_NAME_TO_IDS.get(targetName);
				if (attackerIds == null || targetIds == null)
				{
					LOGGER.warn("ClassDamageManager: unknown class name in {}: {}", file.getName(), key);
					skipped++;
					continue;
				}
				
				float multi;
				try
				{
					multi = Float.parseFloat(props.getProperty(key, "1.0").trim());
				}
				catch (NumberFormatException e)
				{
					LOGGER.warn("ClassDamageManager: invalid value in {}: {}", file.getName(), key);
					skipped++;
					continue;
				}
				
				for (int atkId : attackerIds)
				{
					Map<Integer, Float> byTarget = MULTIPLIERS.get(atkId);
					if (byTarget == null)
					{
						byTarget = new HashMap<>();
						MULTIPLIERS.put(atkId, byTarget);
					}
					for (int tgtId : targetIds)
						byTarget.put(tgtId, multi);
				}
				entries++;
			}
		}
		
		LOGGER.info("ClassDamageManager: loaded {} matchups from {} files ({} skipped).", entries, files.length, skipped);
	}
	
	public static float getMultiplier(Player attacker, Player target)
	{
		if (attacker == null || target == null)
			return 1.0f;
		
		final Map<Integer, Float> byTarget = MULTIPLIERS.get(attacker.getClassId().getId());
		if (byTarget == null)
			return 1.0f;
		
		final Float multi = byTarget.get(target.getClassId().getId());
		return multi != null ? multi : 1.0f;
	}
}
