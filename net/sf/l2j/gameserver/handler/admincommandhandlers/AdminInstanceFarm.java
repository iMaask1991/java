package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmManager;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmTimeZoneDef;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmZone;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmZoneData;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.zone.type.InstanceTimeZone;

/**
 * Spawns del Instance Farm (se guardan en InstanceFarmZones.xml):
 * <ul>
 * <li>//loc_time [mobId] [zoneId] : TIME - guarda un mob en tu posicion y lo spawnea ya (publico, con respawn).</li>
 * <li>//loc_time_delete [zoneId] : TIME - borra el mob que tenes en target o el mas cercano (150 de radio).</li>
 * <li>//loc_kill [mobId] [zoneId] : guarda un spawn en tu posicion (x, y, z, heading). mobId 0 o vacio = defaultMobId de la zona.</li>
 * <li>//loc_kill_delete [zoneId] : borra el spawn del mob que tenes en target (preview), o el mas cercano a vos (150 de radio).</li>
 * <li>//loc_kill_show [zoneId] : muestra todos los spawns guardados de la zona (preview, solo en el mundo normal, sin respawn).</li>
 * <li>//loc_kill_clear : borra los preview.</li>
 * <li>//farm_reload : recarga InstanceFarmZones.xml (afecta a las proximas entradas).</li>
 * </ul>
 * Si no pones zoneId: TIME usa la zona donde estas parado (o la mas cercana), KILL la mas cercana.
 */
public class AdminInstanceFarm implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_loc_time",
		"admin_loc_time_delete",
		"admin_loc_kill",
		"admin_loc_kill_delete",
		"admin_loc_kill_show",
		"admin_loc_kill_clear",
		"admin_farm_reload"
	};
	
	private static final List<L2Spawn> PREVIEWS = new ArrayList<>();
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String cmd = st.nextToken().toLowerCase();
		
		try
		{
			switch (cmd)
			{
				case "admin_loc_time":
				{
					final int mobId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
					final InstanceFarmTimeZoneDef def = resolveTimeZone(activeChar, st);
					if (def != null)
						addTimeSpawn(activeChar, def, mobId);
					return true;
				}
				case "admin_loc_time_delete":
				{
					final InstanceFarmTimeZoneDef def = resolveTimeZone(activeChar, st);
					if (def != null)
						deleteTimeSpawn(activeChar, def);
					return true;
				}
				case "admin_loc_kill":
				{
					final int mobId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
					final InstanceFarmZone zone = resolveZone(activeChar, st);
					if (zone != null)
						addSpawn(activeChar, zone, mobId);
					return true;
				}
				case "admin_loc_kill_delete":
				{
					final InstanceFarmZone zone = resolveZone(activeChar, st);
					if (zone != null)
						deleteSpawn(activeChar, zone);
					return true;
				}
				case "admin_loc_kill_show":
				{
					final InstanceFarmZone zone = resolveZone(activeChar, st);
					if (zone != null)
					{
						clearPreviews();
						for (InstanceFarmZone.KillSpawn ks : zone.getSpawns())
							spawnPreview(ks.getMobId(), ks.getX(), ks.getY(), ks.getZ(), ks.getHeading());
						activeChar.sendMessage("Kill Farm '" + zone.getName() + "': " + zone.getSpawns().size() + " spawn(s) mostrados. //loc_kill_clear para sacarlos.");
					}
					return true;
				}
				case "admin_loc_kill_clear":
				{
					final int count = clearPreviews();
					activeChar.sendMessage("Preview borrados: " + count + ".");
					return true;
				}
				case "admin_farm_reload":
				{
					InstanceFarmZoneData.load();
					activeChar.sendMessage("InstanceFarmZones.xml recargado (" + InstanceFarmZoneData.getZones().size() + " zona(s) KILL). Aplica a las proximas entradas.");
					activeChar.sendMessage("Cambios de forma / opciones / mobs escritos a mano en <time> necesitan reiniciar el server.");
					return true;
				}
			}
		}
		catch (NumberFormatException e)
		{
			activeChar.sendMessage("Uso: //loc_time [mobId] [zoneId] | //loc_time_delete [zoneId] | //loc_kill [mobId] [zoneId] | //loc_kill_delete [zoneId] | //loc_kill_show [zoneId] | //loc_kill_clear | //farm_reload");
			return true;
		}
		
		return false;
	}
	
	private static InstanceFarmZone resolveZone(Player activeChar, StringTokenizer st)
	{
		if (st.hasMoreTokens())
		{
			final int zoneId = Integer.parseInt(st.nextToken());
			final InstanceFarmZone zone = InstanceFarmZoneData.getZone(zoneId);
			if (zone == null)
				activeChar.sendMessage("No existe la zona KILL " + zoneId + " en InstanceFarmZones.xml.");
			return zone;
		}
		
		InstanceFarmZone best = null;
		double bestDist = Double.MAX_VALUE;
		for (InstanceFarmZone zone : InstanceFarmZoneData.getZones())
		{
			final double dx = zone.getEnterLoc().getX() - activeChar.getX();
			final double dy = zone.getEnterLoc().getY() - activeChar.getY();
			final double dist = dx * dx + dy * dy;
			if (dist < bestDist)
			{
				bestDist = dist;
				best = zone;
			}
		}
		
		if (best == null)
			activeChar.sendMessage("No hay zonas KILL en InstanceFarmZones.xml.");
		return best;
	}
	
	private static InstanceFarmTimeZoneDef resolveTimeZone(Player activeChar, StringTokenizer st)
	{
		if (st.hasMoreTokens())
		{
			final int zoneId = Integer.parseInt(st.nextToken());
			final InstanceFarmTimeZoneDef def = InstanceFarmZoneData.getTimeZone(zoneId);
			if (def == null)
				activeChar.sendMessage("No existe la zona TIME " + zoneId + " en InstanceFarmZones.xml.");
			return def;
		}
		
		final InstanceTimeZone inside = InstanceTimeZone.getZoneOf(activeChar);
		if (inside != null && InstanceFarmZoneData.getTimeZone(inside.getFarmId()) != null)
			return InstanceFarmZoneData.getTimeZone(inside.getFarmId());
		
		InstanceFarmTimeZoneDef best = null;
		double bestDist = Double.MAX_VALUE;
		for (InstanceFarmTimeZoneDef def : InstanceFarmZoneData.getTimeZones())
		{
			final double dx = def.getCenter().getX() - activeChar.getX();
			final double dy = def.getCenter().getY() - activeChar.getY();
			final double dist = dx * dx + dy * dy;
			if (dist < bestDist)
			{
				bestDist = dist;
				best = def;
			}
		}
		
		if (best == null)
			activeChar.sendMessage("No hay zonas TIME en InstanceFarmZones.xml.");
		return best;
	}
	
	private static void addTimeSpawn(Player activeChar, InstanceFarmTimeZoneDef def, int mobId)
	{
		if (mobId <= 0)
			mobId = def.getDefaultMobId();
		
		if (mobId <= 0)
		{
			activeChar.sendMessage("Pone el mobId: //loc_time <mobId> (la zona no tiene defaultMobId).");
			return;
		}
		
		if (NpcData.getInstance().getTemplate(mobId) == null)
		{
			activeChar.sendMessage("No existe el npc " + mobId + ".");
			return;
		}
		
		final int x = activeChar.getX();
		final int y = activeChar.getY();
		final int z = activeChar.getZ();
		final int heading = activeChar.getHeading();
		
		final String error = InstanceFarmZoneData.addSpawn(true, def.getId(), mobId, x, y, z, heading);
		if (error != null)
		{
			activeChar.sendMessage(error);
			return;
		}
		
		// Si la zona esta activa, el mob sale ya (publico, con respawn).
		if (InstanceFarmManager.getInstance().getTimeZone(def.getId()) != null)
			InstanceFarmManager.getInstance().spawnTimeMob(def.getId(), new InstanceFarmZone.KillSpawn(mobId, x, y, z, heading), def.getRespawnSeconds());
		else
			activeChar.sendMessage("La zona TIME " + def.getId() + " esta desactivada: el mob sale cuando la actives y reinicies.");
		
		final InstanceFarmTimeZoneDef updated = InstanceFarmZoneData.getTimeZone(def.getId());
		activeChar.sendMessage("Time Farm '" + def.getName() + "': mob " + mobId + " guardado en " + x + " " + y + " " + z + ". Total: " + (updated != null ? updated.getSpawns().size() : "?") + ".");
	}
	
	private static void deleteTimeSpawn(Player activeChar, InstanceFarmTimeZoneDef def)
	{
		InstanceFarmZone.KillSpawn found = null;
		
		final WorldObject target = activeChar.getTarget();
		if (target instanceof Npc && ((Npc) target).getSpawn() != null)
		{
			final L2Spawn spawn = ((Npc) target).getSpawn();
			for (InstanceFarmZone.KillSpawn ks : def.getSpawns())
			{
				if (ks.getX() == spawn.getLocX() && ks.getY() == spawn.getLocY() && ks.getZ() == spawn.getLocZ())
				{
					found = ks;
					break;
				}
			}
		}
		
		if (found == null)
		{
			double bestDist = 150 * 150;
			for (InstanceFarmZone.KillSpawn ks : def.getSpawns())
			{
				final double dx = ks.getX() - activeChar.getX();
				final double dy = ks.getY() - activeChar.getY();
				final double dist = dx * dx + dy * dy;
				if (dist <= bestDist && Math.abs(ks.getZ() - activeChar.getZ()) < 300)
				{
					bestDist = dist;
					found = ks;
				}
			}
		}
		
		if (found == null)
		{
			activeChar.sendMessage("No hay ningun mob de la zona TIME en target ni cerca tuyo (150).");
			return;
		}
		
		final String error = InstanceFarmZoneData.removeSpawn(true, def.getId(), found.getX(), found.getY(), found.getZ());
		if (error != null)
		{
			activeChar.sendMessage(error);
			return;
		}
		
		InstanceFarmManager.getInstance().despawnTimeMob(def.getId(), found.getX(), found.getY(), found.getZ());
		
		final InstanceFarmTimeZoneDef updated = InstanceFarmZoneData.getTimeZone(def.getId());
		activeChar.sendMessage("Time Farm '" + def.getName() + "': mob borrado (" + found.getX() + " " + found.getY() + " " + found.getZ() + "). Quedan: " + (updated != null ? updated.getSpawns().size() : "?") + ".");
	}
	
	private static void addSpawn(Player activeChar, InstanceFarmZone zone, int mobId)
	{
		if (mobId <= 0)
			mobId = zone.getDefaultMobId();
		
		if (mobId <= 0)
		{
			activeChar.sendMessage("Pone el mobId: //loc_kill <mobId> (la zona no tiene defaultMobId).");
			return;
		}
		
		if (NpcData.getInstance().getTemplate(mobId) == null)
		{
			activeChar.sendMessage("No existe el npc " + mobId + ".");
			return;
		}
		
		final int x = activeChar.getX();
		final int y = activeChar.getY();
		final int z = activeChar.getZ();
		final int heading = activeChar.getHeading();
		
		final String error = InstanceFarmZoneData.addSpawn(false, zone.getId(), mobId, x, y, z, heading);
		if (error != null)
		{
			activeChar.sendMessage(error);
			return;
		}
		
		spawnPreview(mobId, x, y, z, heading);
		
		final InstanceFarmZone updated = InstanceFarmZoneData.getZone(zone.getId());
		activeChar.sendMessage("Kill Farm '" + zone.getName() + "': spawn guardado (mob " + mobId + ") en " + x + " " + y + " " + z + ". Total: " + (updated != null ? updated.getSpawns().size() : "?") + ".");
	}
	
	private static void deleteSpawn(Player activeChar, InstanceFarmZone zone)
	{
		InstanceFarmZone.KillSpawn found = null;
		
		final WorldObject target = activeChar.getTarget();
		if (target instanceof Npc && ((Npc) target).getSpawn() != null)
		{
			final L2Spawn spawn = ((Npc) target).getSpawn();
			for (InstanceFarmZone.KillSpawn ks : zone.getSpawns())
			{
				if (ks.getX() == spawn.getLocX() && ks.getY() == spawn.getLocY() && ks.getZ() == spawn.getLocZ())
				{
					found = ks;
					break;
				}
			}
		}
		
		if (found == null)
		{
			double bestDist = 150 * 150;
			for (InstanceFarmZone.KillSpawn ks : zone.getSpawns())
			{
				final double dx = ks.getX() - activeChar.getX();
				final double dy = ks.getY() - activeChar.getY();
				final double dist = dx * dx + dy * dy;
				if (dist <= bestDist && Math.abs(ks.getZ() - activeChar.getZ()) < 300)
				{
					bestDist = dist;
					found = ks;
				}
			}
		}
		
		if (found == null)
		{
			activeChar.sendMessage("No hay ningun spawn en target ni cerca tuyo (150). Usa //loc_kill_show para verlos.");
			return;
		}
		
		final String error = InstanceFarmZoneData.removeSpawn(false, zone.getId(), found.getX(), found.getY(), found.getZ());
		if (error != null)
		{
			activeChar.sendMessage(error);
			return;
		}
		
		removePreviewAt(found.getX(), found.getY(), found.getZ());
		
		final InstanceFarmZone updated = InstanceFarmZoneData.getZone(zone.getId());
		activeChar.sendMessage("Kill Farm '" + zone.getName() + "': spawn borrado (" + found.getX() + " " + found.getY() + " " + found.getZ() + "). Quedan: " + (updated != null ? updated.getSpawns().size() : "?") + ".");
	}
	
	private static void spawnPreview(int mobId, int x, int y, int z, int heading)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(mobId);
		if (template == null)
			return;
		
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(x, y, z, heading);
			spawn.setRespawnDelay(60);
			spawn.setRespawnState(false);
			SpawnTable.getInstance().addSpawn(spawn, false);
			spawn.doSpawn(false);
			synchronized (PREVIEWS)
			{
				PREVIEWS.add(spawn);
			}
		}
		catch (Exception e)
		{
			// preview opcional
		}
	}
	
	private static void removePreviewAt(int x, int y, int z)
	{
		synchronized (PREVIEWS)
		{
			final Iterator<L2Spawn> it = PREVIEWS.iterator();
			while (it.hasNext())
			{
				final L2Spawn spawn = it.next();
				if (spawn.getLocX() == x && spawn.getLocY() == y && spawn.getLocZ() == z)
				{
					deletePreview(spawn);
					it.remove();
				}
			}
		}
	}
	
	private static int clearPreviews()
	{
		synchronized (PREVIEWS)
		{
			final int count = PREVIEWS.size();
			for (L2Spawn spawn : PREVIEWS)
				deletePreview(spawn);
			PREVIEWS.clear();
			return count;
		}
	}
	
	private static void deletePreview(L2Spawn spawn)
	{
		spawn.setRespawnState(false);
		if (spawn.getNpc() != null)
			spawn.getNpc().deleteMe();
		SpawnTable.getInstance().deleteSpawn(spawn, false);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
