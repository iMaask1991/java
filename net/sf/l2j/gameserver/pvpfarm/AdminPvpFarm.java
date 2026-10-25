package net.sf.l2j.gameserver.pvpfarm;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Comando admin del PvP Farm: //pvpfarm (start / stop).
 */
public class AdminPvpFarm implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pvpfarm",
		"admin_loc_pvpfarm",
		"admin_loc_pvpfarm_del",
		"admin_loc_pvpfarm_list",
		"admin_loc_pvpfarm_clear"
	};
	
	protected static final Logger _log = Logger.getLogger(AdminPvpFarm.class.getName());
	public static boolean _pvpfarm_manual = false;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equalsIgnoreCase("admin_loc_pvpfarm"))
			return addLoc(activeChar);
		
		if (command.equalsIgnoreCase("admin_loc_pvpfarm_del"))
			return deleteLoc(activeChar);
		
		if (command.equalsIgnoreCase("admin_loc_pvpfarm_list"))
			return listLocs(activeChar);
		
		if (command.equalsIgnoreCase("admin_loc_pvpfarm_clear"))
			return clearLocs(activeChar);
		
		if (!command.equals("admin_pvpfarm"))
			return false;
		
		if (PvpFarm._started)
		{
			_log.info(Config.PVPFARM_NAME + ": Event finished manually.");
			PvpFarm._aborted = true;
			finishEventPvpFarm();
			_pvpfarm_manual = true;
			activeChar.sendMessage("SYS: Finalizaste " + Config.PVPFARM_NAME);
		}
		else
		{
			_log.info(Config.PVPFARM_NAME + ": Event started manually.");
			initEventPvpFarm();
			_pvpfarm_manual = true;
			activeChar.sendMessage("SYS: Activaste " + Config.PVPFARM_NAME);
		}
		return true;
	}
	
	private static void initEventPvpFarm()
	{
		ThreadPool.schedule(() -> PvpFarm.startEvent(), 1L);
	}
	
	private static void finishEventPvpFarm()
	{
		ThreadPool.schedule(() -> PvpFarm.finishEvent(), 1L);
	}
	
	/**
	 * //loc_pvpfarm -> agrega la posicion actual al SpawnPointMonster del PvpFarm.properties (y a memoria).
	 */
	private static boolean addLoc(Player activeChar)
	{
		final int x = activeChar.getX();
		final int y = activeChar.getY();
		final int z = activeChar.getZ();
		
		Config.PVPFARM_SPAWN_POINTS.add(new int[]
		{
			x,
			y,
			z
		});
		
		if (!saveSpawnPoints())
		{
			activeChar.sendMessage("PvP Farm: no se pudo guardar en " + Config.PVP_FARM_FILE);
			return true;
		}
		
		activeChar.sendMessage("PvP Farm: coordenada agregada -> " + x + "," + y + "," + z + " (total: " + Config.PVPFARM_SPAWN_POINTS.size() + ")");
		return true;
	}
	
	/**
	 * //loc_pvpfarm_del -> borra la coordenada mas cercana a donde estas parado.
	 */
	private static boolean deleteLoc(Player activeChar)
	{
		final List<int[]> points = Config.PVPFARM_SPAWN_POINTS;
		if (points.isEmpty())
		{
			activeChar.sendMessage("PvP Farm: no hay coordenadas cargadas.");
			return true;
		}
		
		final int x = activeChar.getX();
		final int y = activeChar.getY();
		
		int bestIndex = -1;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < points.size(); i++)
		{
			final double distance = Math.hypot(points.get(i)[0] - x, points.get(i)[1] - y);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				bestIndex = i;
			}
		}
		
		if (bestIndex < 0 || bestDistance > 2000)
		{
			activeChar.sendMessage("PvP Farm: ninguna coordenada cerca (la mas cercana esta a " + (int) bestDistance + "). Mira //loc_pvpfarm_list");
			return true;
		}
		
		final int[] removed = points.remove(bestIndex);
		saveSpawnPoints();
		activeChar.sendMessage("PvP Farm: coordenada eliminada -> " + removed[0] + "," + removed[1] + "," + removed[2] + " (quedan " + points.size() + ")");
		return true;
	}
	
	/**
	 * //loc_pvpfarm_list -> muestra las coordenadas cargadas.
	 */
	private static boolean listLocs(Player activeChar)
	{
		final List<int[]> points = Config.PVPFARM_SPAWN_POINTS;
		if (points.isEmpty())
		{
			activeChar.sendMessage("PvP Farm: no hay coordenadas cargadas (agrega con //loc_pvpfarm).");
			return true;
		}
		
		activeChar.sendMessage("PvP Farm: " + points.size() + " coordenada(s) | radio spawn " + Config.PVPFARM_SPAWN_RADIUS + " | mobs " + Config.PVPFARM_SPAWN_AMOUNT);
		for (int i = 0; i < points.size(); i++)
		{
			final int[] loc = points.get(i);
			activeChar.sendMessage("  [" + (i + 1) + "] " + loc[0] + "," + loc[1] + "," + loc[2]);
		}
		return true;
	}
	
	/**
	 * //loc_pvpfarm_clear -> borra todas las coordenadas.
	 */
	private static boolean clearLocs(Player activeChar)
	{
		Config.PVPFARM_SPAWN_POINTS.clear();
		saveSpawnPoints();
		activeChar.sendMessage("PvP Farm: coordenadas borradas en " + Config.PVP_FARM_FILE);
		return true;
	}
	
	/**
	 * Reescribe la linea "SpawnPointMonster = ..." del PvpFarm.properties con las coordenadas que estan en memoria.
	 */
	private static boolean saveSpawnPoints()
	{
		final Path path = Paths.get(Config.PVP_FARM_FILE);
		
		try
		{
			final StringBuilder sb = new StringBuilder();
			for (int[] loc : Config.PVPFARM_SPAWN_POINTS)
			{
				if (sb.length() > 0)
					sb.append(';');
				
				sb.append(loc[0]).append(',').append(loc[1]).append(',').append(loc[2]);
			}
			
			final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			
			boolean found = false;
			for (int i = 0; i < lines.size(); i++)
			{
				final String trimmed = lines.get(i).trim();
				if (!trimmed.startsWith("#") && trimmed.startsWith("SpawnPointMonster"))
				{
					lines.set(i, "SpawnPointMonster = " + sb);
					found = true;
					break;
				}
			}
			
			if (!found)
				lines.add("SpawnPointMonster = " + sb);
			
			Files.write(path, lines, StandardCharsets.UTF_8);
			return true;
		}
		catch (Exception e)
		{
			_log.warning("PvP Farm: error guardando SpawnPointMonster en " + Config.PVP_FARM_FILE + ": " + e.getMessage());
			return false;
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
