package net.sf.l2j.gameserver.customdrop;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;

/**
 * Custom drops por grupos, leidos de config/HadesProject/CustomDrops.properties.
 * <ul>
 * <li>Se SUMAN al drop del XML (el XML queda retail).</li>
 * <li>Excepcion ADENA: si el grupo tiene adena (57), la adena del XML se reemplaza por la del properties. Si el grupo no tiene adena, queda la del XML.</li>
 * <li>Cada item va en su propia categoria: se tira por separado.</li>
 * <li>Se aplica al iniciar el server, con //reload config y despues de //reload npc.</li>
 * <li>Respawn opcional por grupo (Grupo_RespawnDelay en segundos); sin esa linea queda el respawn de la base.</li>
 * <li>Al recargar, primero se restaura el drop original del XML y despues se vuelve a aplicar el properties.</li>
 * </ul>
 */
public final class CustomDropManager
{
	private static final Logger LOGGER = Logger.getLogger(CustomDropManager.class.getName());

	public static final String FILE = "./config/HadesProject/CustomDrops.properties";

	private static final int ADENA_ID = 57;
	
	/** Categorias custom arrancan en este id para no pisar las del XML. */
	private static final int FIRST_CATEGORY = 1000;

	/** Template -> lista de drops ORIGINAL del XML (para poder restaurar). */
	private static final Map<NpcTemplate, List<DropCategory>> ORIGINALS = new IdentityHashMap<>();

	/** Spawn -> {respawnDelay, respawnRandom} ORIGINAL de la base (para poder restaurar). */
	private static final Map<L2Spawn, int[]> SPAWN_ORIGINALS = new IdentityHashMap<>();
	
	/** npcId -> respawn custom en segundos (del properties). */
	private static final Map<Integer, Integer> RESPAWN_BY_NPC = new HashMap<>();
	
	private static boolean _spawnsReady = false;
	
	private CustomDropManager()
	{
	}

	/**
	 * Llamado desde GameServer DESPUES de cargar el SpawnTable: recien ahi se pueden tocar los respawns.
	 */
	public static synchronized void onSpawnsLoaded()
	{
		_spawnsReady = true;
		applyRespawns();
	}
	
	public static synchronized void load()
	{
		// 1) Restaurar lo original (drops del XML y respawns de la base) de todo lo que tocamos antes.
		for (Map.Entry<NpcTemplate, List<DropCategory>> entry : ORIGINALS.entrySet())
			entry.getKey().setDropData(entry.getValue());
		ORIGINALS.clear();
		restoreRespawns();
		RESPAWN_BY_NPC.clear();
		
		// 2) Leer el properties.
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(FILE); InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
		{
			props.load(reader);
		}
		catch (Exception e)
		{
			LOGGER.warning("CustomDrops: no se pudo leer " + FILE + " (" + e.getMessage() + "). Sin drops custom.");
			return;
		}
		
		if (!Boolean.parseBoolean(get(props, "CustomDropEnabled", "False")))
		{
			LOGGER.info("CustomDrops: desactivado.");
			return;
		}
		
		int groups = 0;
		int npcs = 0;
		
		for (String group : split(get(props, "CustomDropGroups", ""), ","))
		{
			if (!Boolean.parseBoolean(get(props, group + "_Enabled", "True")))
				continue;
			
			// IDs validos del grupo.
			final List<NpcTemplate> templates = new ArrayList<>();
			for (String idStr : split(get(props, group + "_NpcIds", ""), ","))
			{
				final int npcId;
				try
				{
					npcId = Integer.parseInt(idStr);
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("CustomDrops: grupo '" + group + "' npcId invalido: " + idStr);
					continue;
				}
				
				final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
				if (template == null)
				{
					LOGGER.warning("CustomDrops: grupo '" + group + "' npcId " + npcId + " no existe.");
					continue;
				}
				templates.add(template);
			}
			
			if (templates.isEmpty())
			{
				LOGGER.warning("CustomDrops: grupo '" + group + "' sin npcIds validos, se ignora.");
				continue;
			}
			
			// Respawn (opcional, en segundos).
			final String respawnStr = get(props, group + "_RespawnDelay", "");
			if (!respawnStr.isEmpty())
			{
				try
				{
					final int delay = Integer.parseInt(respawnStr);
					if (delay > 0)
					{
						for (NpcTemplate template : templates)
							RESPAWN_BY_NPC.put(template.getNpcId(), delay);
					}
					else
						LOGGER.warning("CustomDrops: grupo '" + group + "' RespawnDelay tiene que ser mayor a 0.");
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("CustomDrops: grupo '" + group + "' RespawnDelay invalido: " + respawnStr);
				}
			}
			
			// Drops.
			final List<DropData> drops = parseDrops(group, get(props, group + "_Drops", ""));
			if (drops.isEmpty())
			{
				LOGGER.info("CustomDrops: grupo '" + group + "' sin drops (queda el drop del XML).");
				continue;
			}
			
			boolean hasAdena = false;
			for (DropData drop : drops)
				if (drop.getItemId() == ADENA_ID)
					hasAdena = true;
			
			for (NpcTemplate template : templates)
			{
				final List<DropCategory> current = template.getDropData();
				if (!ORIGINALS.containsKey(template))
					ORIGINALS.put(template, current);
				
				final boolean boss = template.isType("RaidBoss") || template.isType("GrandBoss");
				template.setDropData(buildDropList(current, drops, boss, hasAdena));
			}
			
			groups++;
			npcs += templates.size();
			LOGGER.info("CustomDrops: grupo '" + group + "' -> " + drops.size() + " items en " + templates.size() + " mobs.");
		}
		
		LOGGER.info("CustomDrops: " + groups + " grupos aplicados, " + npcs + " mobs con drop custom.");
		
		// 3) Respawns (solo si el SpawnTable ya esta cargado; al iniciar lo hace onSpawnsLoaded()).
		applyRespawns();
	}
	
	private static void applyRespawns()
	{
		if (!_spawnsReady || RESPAWN_BY_NPC.isEmpty())
			return;
		
		int count = 0;
		for (L2Spawn spawn : SpawnTable.getInstance().getSpawns())
		{
			final Integer delay = RESPAWN_BY_NPC.get(spawn.getNpcId());
			if (delay == null)
				continue;
			
			if (!SPAWN_ORIGINALS.containsKey(spawn))
				SPAWN_ORIGINALS.put(spawn, new int[]
				{
					spawn.getRespawnDelay(),
					spawn.getRespawnRandom()
				});
			
			spawn.setRespawnDelay(delay);
			if (spawn.getRespawnRandom() > delay)
				spawn.setRespawnRandom(delay);
			count++;
		}
		LOGGER.info("CustomDrops: respawn custom aplicado a " + count + " spawns.");
	}
	
	private static void restoreRespawns()
	{
		for (Map.Entry<L2Spawn, int[]> entry : SPAWN_ORIGINALS.entrySet())
		{
			entry.getKey().setRespawnDelay(entry.getValue()[0]);
			entry.getKey().setRespawnRandom(entry.getValue()[1]);
		}
		SPAWN_ORIGINALS.clear();
	}
	
	/**
	 * Formato: itemId,min,max,chance% ; itemId,min,max,chance% ; ...
	 */
	private static List<DropData> parseDrops(String group, String value)
	{
		final List<DropData> result = new ArrayList<>();
		for (String entry : split(value, ";"))
		{
			final String[] parts = entry.split(",");
			if (parts.length != 4)
			{
				LOGGER.warning("CustomDrops: grupo '" + group + "' entrada invalida (usar itemId,min,max,chance): " + entry);
				continue;
			}

			try
			{
				final int itemId = Integer.parseInt(parts[0].trim());
				final int min = Integer.parseInt(parts[1].trim());
				final int max = Integer.parseInt(parts[2].trim());
				final double chancePct = Double.parseDouble(parts[3].trim().replace("%", ""));

				if (ItemTable.getInstance().getTemplate(itemId) == null)
				{
					LOGGER.warning("CustomDrops: grupo '" + group + "' item " + itemId + " no existe.");
					continue;
				}

				if (min < 1 || max < min || chancePct <= 0)
				{
					LOGGER.warning("CustomDrops: grupo '" + group + "' valores invalidos: " + entry);
					continue;
				}

				final DropData drop = new DropData();
				drop.setItemId(itemId);
				drop.setMinDrop(min);
				drop.setMaxDrop(max);
				drop.setChance((int) Math.max(1, Math.min(DropData.MAX_CHANCE, Math.round(chancePct * 10000))));
				result.add(drop);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("CustomDrops: grupo '" + group + "' numero invalido: " + entry);
			}
		}
		return result;
	}

	/**
	 * Arma la lista final: drop del XML (sin la adena si replaceAdena) + una categoria por cada item custom.
	 * Nunca modifica las categorias originales.
	 */
	static List<DropCategory> buildDropList(List<DropCategory> current, List<DropData> drops, boolean boss, boolean replaceAdena)
	{
		final List<DropCategory> list = new ArrayList<>();
		if (current != null)
		{
			for (DropCategory cat : current)
			{
				// Si el grupo define adena, la adena del XML (drop normal, no spoil) se saca.
				if (!replaceAdena || cat.isSweep() || !hasItem(cat, ADENA_ID))
				{
					list.add(cat);
					continue;
				}
				
				// Categoria nueva sin la adena (no se toca la original, asi se puede restaurar).
				final DropCategory rebuilt = new DropCategory(cat.getCategoryType());
				for (DropData drop : cat.getAllDrops())
				{
					if (drop.getItemId() != ADENA_ID)
						rebuilt.addDropData(drop, boss);
				}
				
				if (!rebuilt.getAllDrops().isEmpty())
					list.add(rebuilt);
			}
		}

		int catId = FIRST_CATEGORY;
		for (DropCategory cat : list)
			catId = Math.max(catId, cat.getCategoryType() + 1);

		for (DropData drop : drops)
		{
			final DropCategory cat = new DropCategory(catId++);
			cat.addDropData(copy(drop), boss);
			list.add(cat);
		}

		return list;
	}
	
	private static boolean hasItem(DropCategory cat, int itemId)
	{
		for (DropData drop : cat.getAllDrops())
			if (drop.getItemId() == itemId)
				return true;
		return false;
	}
	
	private static DropData copy(DropData src)
	{
		final DropData drop = new DropData();
		drop.setItemId(src.getItemId());
		drop.setMinDrop(src.getMinDrop());
		drop.setMaxDrop(src.getMaxDrop());
		drop.setChance(src.getChance());
		return drop;
	}

	private static String get(Properties props, String key, String def)
	{
		final String value = props.getProperty(key);
		return (value == null) ? def : value.trim();
	}

	private static List<String> split(String value, String sep)
	{
		final List<String> result = new ArrayList<>();
		for (String part : value.split(sep))
		{
			final String trimmed = part.trim();
			if (!trimmed.isEmpty())
				result.add(trimmed);
		}
		return result;
	}
}
