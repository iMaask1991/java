package net.sf.l2j.gameserver.instancemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;

import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Balance por clase (HadesProject).<br>
 * Lee ./config/HadesProject/balance/ClassBalance.properties (seccion BALANCE POR CLASE) : bonus FIJOS por ID de 3ra clase.<br>
 * <br>
 * Los bonus NO se guardan en el personaje: se calculan en cada momento segun la clase ACTIVA
 * (ver FuncBalanceClass). Por eso al cambiar de subclass se aplican solo los de la clase activa,
 * sin sumarse ni quedar pegados.
 */
public class BalanceClassManager
{
	private static final CLogger LOGGER = new CLogger(BalanceClassManager.class.getName());

	public static final String FILE = Config.CLASS_BALANCE_FILE;

	/** Stats soportados, en el orden del array de valores. */
	public static final Stats[] STATS =
	{
		Stats.MAX_HP,
		Stats.MAX_CP,
		Stats.MAX_MP,
		Stats.POWER_ATTACK_SPEED,
		Stats.MAGIC_ATTACK_SPEED,
		Stats.RUN_SPEED
	};

	/** Nombre de cada stat en el properties (mismo orden que STATS). */
	private static final String[] KEYS =
	{
		"MaxHp",
		"MaxCp",
		"MaxMp",
		"AtkSpd",
		"CastSpd",
		"RunSpd"
	};

	private volatile boolean _enabled;
	private volatile Map<Integer, int[]> _bonus = new HashMap<>();

	protected BalanceClassManager()
	{
		load();
	}

	public void load()
	{
		final Map<Integer, int[]> bonus = new HashMap<>();
		boolean enabled = false;

		final File file = new File(FILE);
		if (!file.exists())
		{
			LOGGER.warn("BalanceClass: no se encontro {}, sistema desactivado.", FILE);
			_bonus = bonus;
			_enabled = false;
			return;
		}

		final Properties props = new Properties();
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
		{
			props.load(reader);
		}
		catch (Exception e)
		{
			LOGGER.error("BalanceClass: error leyendo {}.", e, FILE);
			_bonus = bonus;
			_enabled = false;
			return;
		}

		enabled = Boolean.parseBoolean(props.getProperty("EnableBalanceClass", "True").trim());

		for (String key : props.stringPropertyNames())
		{
			final int dot = key.indexOf('.');
			if (dot <= 0)
				continue;

			final int classId;
			try
			{
				classId = Integer.parseInt(key.substring(0, dot).trim());
			}
			catch (NumberFormatException e)
			{
				continue;
			}

			final String statName = key.substring(dot + 1).trim();
			final int index = indexOf(statName);
			if (index < 0)
			{
				LOGGER.warn("BalanceClass: stat desconocido '{}' en la clave {}, se ignora.", statName, key);
				continue;
			}

			if (classId < 0 || classId >= ClassId.VALUES.length || ClassId.VALUES[classId].level() != 3)
			{
				LOGGER.warn("BalanceClass: la clave {} no es una 3ra clase, se ignora.", key);
				continue;
			}

			final String raw = props.getProperty(key, "0").trim();
			if (raw.contains("%"))
			{
				LOGGER.warn("BalanceClass: {} = {} -> solo se permiten valores fijos (sin %), se ignora.", key, raw);
				continue;
			}

			final int value;
			try
			{
				value = Integer.parseInt(raw);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn("BalanceClass: {} = {} no es un numero valido, se ignora.", key, raw);
				continue;
			}

			if (value != 0)
				bonus.computeIfAbsent(classId, k -> new int[STATS.length])[index] = value;
		}

		_bonus = bonus;
		_enabled = enabled;
		LOGGER.info("BalanceClass: {} ({} clases con bonus).", enabled ? "activado" : "desactivado", bonus.size());
	}

	/**
	 * Recarga el archivo y refresca la info de todos los jugadores online.
	 */
	public void reload()
	{
		load();

		for (Player player : World.getInstance().getPlayers())
		{
			if (player.getCurrentHp() > player.getMaxHp())
				player.setCurrentHp(player.getMaxHp());
			if (player.getCurrentMp() > player.getMaxMp())
				player.setCurrentMp(player.getMaxMp());
			if (player.getCurrentCp() > player.getMaxCp())
				player.setCurrentCp(player.getMaxCp());

			player.broadcastUserInfo();
		}
	}

	/**
	 * @param player : el jugador.
	 * @param index : indice del stat en {@link #STATS}.
	 * @return el bonus fijo de la clase ACTIVA del jugador (0 si no tiene).
	 */
	public int getBonus(Player player, int index)
	{
		if (!_enabled || player == null)
			return 0;

		final ClassId classId = player.getClassId();
		if (classId == null)
			return 0;

		final int[] values = _bonus.get(classId.getId());
		return (values == null) ? 0 : values[index];
	}

	private static int indexOf(String statName)
	{
		for (int i = 0; i < KEYS.length; i++)
		{
			if (KEYS[i].equalsIgnoreCase(statName))
				return i;
		}
		return -1;
	}

	public static BalanceClassManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final BalanceClassManager INSTANCE = new BalanceClassManager();
	}
}
