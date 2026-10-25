package net.sf.l2j.gameserver.instancefarm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.events.instance.InstanceHolder;
import net.sf.l2j.gameserver.events.instance.InstanceManager;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.zone.ZoneForm;
import net.sf.l2j.gameserver.model.zone.form.ZoneCylinder;
import net.sf.l2j.gameserver.model.zone.form.ZoneNPoly;
import net.sf.l2j.gameserver.model.zone.type.InstanceTimeZone;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Instance Farm (HadesProject).<br>
 * <br>
 * <b>TIME</b> : zona PUBLICA con PvP (&lt;time&gt; en InstanceFarmZones.xml). Se paga un item en el NPC y da X minutos.
 * El tiempo solo se descuenta estando adentro y vivo, y se guarda por HWID al morir / salir / desloguear.
 * Sin tiempo guardado NO se entra: hay que pagar. Entrar por cualquier otro lado que no sea el NPC = te saca.<br>
 * <br>
 * <b>KILL</b> : instancia PRIVADA por jugador (&lt;kill&gt; en InstanceFarmZones.xml). Se compran packs de kills en el NPC,
 * se guardan por HWID y cada mob muerto descuenta 1.
 */
public class InstanceFarmManager
{
	private static final CLogger LOGGER = new CLogger(InstanceFarmManager.class.getName());
	
	/** Tiempo que tiene el jugador para llegar a la zona despues de que el NPC lo teletransporta. */
	private static final long AUTH_MS = 60_000L;
	/** Si sale de la zona y vuelve a entrar enseguida (teleport interno, etc.) no se lo saca. */
	private static final long REENTRY_GRACE_MS = 5_000L;
	
	// ---------------- KILL ----------------
	private final Map<Integer, InstanceFarmRun> _runs = new ConcurrentHashMap<>();
	private final Map<Integer, InstanceFarmRun> _runsByInstance = new ConcurrentHashMap<>();
	private final Map<String, InstanceFarmRun> _runsByKey = new ConcurrentHashMap<>();
	
	// ---------------- TIME ----------------
	private static class TimeSession
	{
		final Player player;
		final InstanceTimeZone zone;
		final InstanceFarmBank bank;
		final String hwid;
		int ticks;
		
		TimeSession(Player p, InstanceTimeZone z, InstanceFarmBank b, String h)
		{
			player = p;
			zone = z;
			bank = b;
			hwid = h;
		}
	}
	
	private final Map<Integer, TimeSession> _timeSessions = new ConcurrentHashMap<>();
	private final Map<String, TimeSession> _timeByKey = new ConcurrentHashMap<>();
	private final Map<Integer, Long> _timeAuth = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _timeTask;
	
	/** Las zonas TIME se registran en el mundo con este offset para no chocar con ids de la carpeta zones. */
	private static final int TIME_ZONE_ID_OFFSET = 960000;
	private final Map<Integer, InstanceTimeZone> _timeZoneObjects = new LinkedHashMap<>();
	private final Map<Integer, List<L2Spawn>> _timeSpawns = new ConcurrentHashMap<>();
	/** Todos los spawns de las zonas TIME (para TimeRewards / TimeNormalDrop / TimeNoCorpse). */
	private final java.util.Set<L2Spawn> _timeSpawnSet = ConcurrentHashMap.newKeySet();
	
	private L2Spawn _hubSpawn;
	
	protected InstanceFarmManager()
	{
	}
	
	public void init()
	{
		if (!Config.INSTANCE_FARM_ENABLED)
		{
			LOGGER.info("InstanceFarmManager: desactivado.");
			return;
		}
		
		InstanceFarmBank.createTable();
		InstanceFarmZoneData.load();
		buildTimeZones();
		
		if (Config.INSTANCE_FARM_SPAWN_HUB)
			spawnHub();
		
		if (_timeTask == null)
			_timeTask = ThreadPool.scheduleAtFixedRate(this::onTimeTick, 1000, 1000);
		
		LOGGER.info("InstanceFarmManager: listo ({} zona(s) TIME, {} zona(s) KILL).", getTimeZones().size(), InstanceFarmZoneData.getZones().size());
	}
	
	private void spawnHub()
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(Config.INSTANCE_FARM_HUB_NPC_ID);
		if (template == null)
		{
			LOGGER.warn("InstanceFarmManager: no existe el NPC {}.", Config.INSTANCE_FARM_HUB_NPC_ID);
			return;
		}
		
		try
		{
			_hubSpawn = new L2Spawn(template);
			_hubSpawn.setLoc(Config.INSTANCE_FARM_HUB_X, Config.INSTANCE_FARM_HUB_Y, Config.INSTANCE_FARM_HUB_Z, Config.INSTANCE_FARM_HUB_HEADING);
			_hubSpawn.setRespawnDelay(60);
			_hubSpawn.setRespawnState(true);
			SpawnTable.getInstance().addSpawn(_hubSpawn, false);
			_hubSpawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmManager: error spawneando el NPC.", e);
		}
	}
	
	// =====================================================================
	// Comun
	// =====================================================================
	
	public static List<InstanceTimeZone> getTimeZones()
	{
		final List<InstanceTimeZone> list = new ArrayList<>();
		synchronized (getInstance()._timeZoneObjects)
		{
			for (InstanceTimeZone zone : getInstance()._timeZoneObjects.values())
			{
				if (zone.isEnabled())
					list.add(zone);
			}
		}
		return list;
	}
	
	/**
	 * @param farmId : id de la zona &lt;time&gt; en el XML.
	 * @return la zona TIME activa, o null.
	 */
	public InstanceTimeZone getTimeZone(int farmId)
	{
		synchronized (_timeZoneObjects)
		{
			return _timeZoneObjects.get(farmId);
		}
	}
	
	/**
	 * Crea las zonas TIME del XML en el mundo (solo al arrancar el server) y spawnea sus mobs.
	 */
	private void buildTimeZones()
	{
		synchronized (_timeZoneObjects)
		{
			if (!_timeZoneObjects.isEmpty())
				return;
			
			for (InstanceFarmTimeZoneDef def : InstanceFarmZoneData.getTimeZones())
			{
				if (!def.isEnabled())
					continue;
				
				final InstanceTimeZone zone = new InstanceTimeZone(TIME_ZONE_ID_OFFSET + def.getId());
				zone.configure(def.getId(), def.getName(), true, def.getMinLevel(), def.isInviteParty(), def.isNoDropProtection(), def.isLoseBuff(), def.getBlockedClasses());
				for (Location loc : def.getEnters())
					zone.addLoc(loc, false);
				
				final ZoneForm form;
				if (def.getNodes().size() >= 3)
				{
					final int[] ax = new int[def.getNodes().size()];
					final int[] ay = new int[def.getNodes().size()];
					for (int i = 0; i < ax.length; i++)
					{
						ax[i] = def.getNodes().get(i)[0];
						ay[i] = def.getNodes().get(i)[1];
					}
					form = new ZoneNPoly(ax, ay, def.getMinZ(), def.getMaxZ());
				}
				else
					form = new ZoneCylinder(def.getCenter().getX(), def.getCenter().getY(), def.getMinZ(), def.getMaxZ(), def.getRadius());
				
				zone.setZone(form);
				ZoneManager.getInstance().addZone(zone.getId(), zone);
				
				final WorldRegion[][] regions = World.getInstance().getWorldRegions();
				for (int x = 0; x < regions.length; x++)
				{
					final int xLoc = World.getRegionX(x);
					final int xLoc2 = World.getRegionX(x + 1);
					for (int y = 0; y < regions[x].length; y++)
					{
						if (form.intersectsRectangle(xLoc, xLoc2, World.getRegionY(y), World.getRegionY(y + 1)))
							regions[x][y].addZone(zone);
					}
				}
				
				_timeZoneObjects.put(def.getId(), zone);
				
				for (InstanceFarmZone.KillSpawn ks : def.getSpawns())
					spawnTimeMob(def.getId(), ks, def.getRespawnSeconds());
				
				LOGGER.info("InstanceFarmManager: TIME zona {} '{}' creada ({} mob(s)).", def.getId(), def.getName(), def.getSpawns().size());
			}
		}
	}
	
	/**
	 * Spawnea un mob publico (mundo normal) de una zona TIME.
	 */
	public void spawnTimeMob(int farmId, InstanceFarmZone.KillSpawn ks, int respawnSeconds)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(ks.getMobId());
		if (template == null)
		{
			LOGGER.warn("InstanceFarmManager: no existe el npc {} (TIME zona {}).", ks.getMobId(), farmId);
			return;
		}
		
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(ks.getX(), ks.getY(), ks.getZ(), ks.getHeading());
			spawn.setRespawnDelay(respawnSeconds);
			spawn.setRespawnState(true);
			SpawnTable.getInstance().addSpawn(spawn, false);
			spawn.doSpawn(false);
			final List<L2Spawn> list = _timeSpawns.computeIfAbsent(farmId, k -> new ArrayList<>());
			synchronized (list)
			{
				list.add(spawn);
			}
			_timeSpawnSet.add(spawn);
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmManager: error spawneando {} en TIME zona {}.", e, ks.getMobId(), farmId);
		}
	}
	
	/**
	 * Despawnea el mob de una zona TIME que esta en esas coordenadas.
	 */
	public void despawnTimeMob(int farmId, int x, int y, int z)
	{
		final List<L2Spawn> list = _timeSpawns.get(farmId);
		if (list == null)
			return;
		
		synchronized (list)
		{
			final Iterator<L2Spawn> it = list.iterator();
			while (it.hasNext())
			{
				final L2Spawn spawn = it.next();
				if (spawn.getLocX() == x && spawn.getLocY() == y && spawn.getLocZ() == z)
				{
					spawn.setRespawnState(false);
					if (spawn.getNpc() != null)
						spawn.getNpc().deleteMe();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
					_timeSpawnSet.remove(spawn);
					it.remove();
				}
			}
		}
	}
	
	/**
	 * Lo manda a las coordenadas de salida del properties (0,0,0 = pueblo mas cercano).
	 */
	public static void teleportOut(Player player)
	{
		if (player == null)
			return;
		
		if (player.getInstance() != null && player.getInstance().getId() != 0)
			player.setInstance(InstanceManager.getInstance().getInstance(0), true);
		
		if (player.isDead())
			player.doRevive();
		
		if (Config.INSTANCE_FARM_EXIT_X == 0 && Config.INSTANCE_FARM_EXIT_Y == 0)
			player.teleportTo(TeleportType.TOWN);
		else
			player.teleportTo(Config.INSTANCE_FARM_EXIT_X, Config.INSTANCE_FARM_EXIT_Y, Config.INSTANCE_FARM_EXIT_Z, 0);
	}
	
	/**
	 * Chequeos generales antes de entrar a cualquier farm.
	 * @return el motivo por el que no puede, o null si puede.
	 */
	private String checkCommon(Player player)
	{
		if (!Config.INSTANCE_FARM_ENABLED)
			return "El Instance Farm esta desactivado.";
		if (player.isDead())
			return "No podes entrar muerto.";
		if (player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInObserverMode())
			return "No podes entrar ahora.";
		if (player.isInJail())
			return "No podes entrar estando en la carcel.";
		if (player.isCursedWeaponEquipped())
			return "No podes entrar con un arma maldita.";
		if (player.isInStoreMode())
			return "No podes entrar con la tienda abierta.";
		if (player.isInCombat() || player.getPvpFlag() > 0 || player.getKarma() > 0)
			return "No podes entrar en combate, flageado o con karma.";
		if (isInRun(player))
			return "Ya estas dentro del Kill Farm.";
		if (_timeSessions.containsKey(player.getObjectId()))
			return "Ya estas dentro del Time Farm.";
		if (InstanceFarmBank.resolveKey(player) == null)
			return "No se pudo identificar tu PC (HWID).";
		return null;
	}
	
	private static String itemName(int itemId)
	{
		final Item item = ItemTable.getInstance().getTemplate(itemId);
		return item != null ? item.getName() : ("Item " + itemId);
	}
	
	public static String getTimePriceText()
	{
		return Config.INSTANCE_FARM_TIME_PAY_ITEM_COUNT + " " + itemName(Config.INSTANCE_FARM_TIME_PAY_ITEM_ID);
	}
	
	public static String getPackPriceText(int[] pack)
	{
		return pack[2] + " " + itemName(pack[1]);
	}
	
	// =====================================================================
	// TIME
	// =====================================================================
	
	/**
	 * Entrar con el tiempo que ya tiene guardado (sin pagar).
	 */
	public void enterTime(Player player, int zoneId)
	{
		enterTime(player, zoneId, 0);
	}
	
	public void enterTime(Player player, int zoneId, int point)
	{
		final InstanceTimeZone zone = getTimeZone(zoneId);
		final String error = checkTimeEntry(player, zone);
		if (error != null)
		{
			player.sendMessage(error);
			return;
		}
		
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		if (bank.getTimeSeconds() <= 0)
		{
			player.sendMessage("No tenes tiempo guardado. Para entrar tenes que pagar " + getTimePriceText() + ".");
			return;
		}
		
		teleportIntoTimeZone(player, zone, point);
	}
	
	/**
	 * Pagar el item y entrar. Solo se puede pagar si NO tiene tiempo guardado.
	 */
	public void payTime(Player player, int zoneId)
	{
		payTime(player, zoneId, 0);
	}
	
	public void payTime(Player player, int zoneId, int point)
	{
		final InstanceTimeZone zone = getTimeZone(zoneId);
		final String error = checkTimeEntry(player, zone);
		if (error != null)
		{
			player.sendMessage(error);
			return;
		}
		
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		synchronized (bank)
		{
			if (bank.getTimeSeconds() > 0)
			{
				player.sendMessage("Todavia tenes " + InstanceFarmRun.formatTime(bank.getTimeSeconds()) + " guardado. Entra sin pagar.");
				return;
			}
			
			final int itemId = Config.INSTANCE_FARM_TIME_PAY_ITEM_ID;
			final int count = Config.INSTANCE_FARM_TIME_PAY_ITEM_COUNT;
			if (player.getInventory().getInventoryItemCount(itemId, -1) < count)
			{
				player.sendMessage("Necesitas " + getTimePriceText() + " para entrar.");
				return;
			}
			
			if (!player.destroyItemByItemId("InstanceFarmTime", itemId, count, player, true))
				return;
			
			bank.setTimeSeconds(Config.INSTANCE_FARM_TIME_PAY_MINUTES * 60);
			bank.save();
		}
		
		player.sendMessage("Pagaste " + getTimePriceText() + ". Tenes " + Config.INSTANCE_FARM_TIME_PAY_MINUTES + " minutos.");
		teleportIntoTimeZone(player, zone, point);
	}
	
	private String checkTimeEntry(Player player, InstanceTimeZone zone)
	{
		final String error = checkCommon(player);
		if (error != null)
			return error;
		
		if (zone == null || !zone.isEnabled())
			return "Esa zona no esta disponible.";
		if (zone.getLocs() == null || zone.getLocs().isEmpty())
			return "Esa zona no tiene punto de entrada configurado.";
		if (player.getLevel() < zone.getMinLevel())
			return "Necesitas nivel " + zone.getMinLevel() + " para entrar.";
		if (zone.isClassBlocked(player))
			return "Tu clase no puede entrar a esta zona.";
		
		if (isHwidLimitReached(countTimeByKey(InstanceFarmBank.resolveKey(player), player)))
			return "Ya hay " + Config.INSTANCE_FARM_MAX_PER_HWID + " personaje(s) de tu PC en el Time Farm (maximo por PC).";
		
		return null;
	}
	
	private void teleportIntoTimeZone(Player player, InstanceTimeZone zone, int point)
	{
		_timeAuth.put(player.getObjectId(), System.currentTimeMillis() + AUTH_MS);
		
		final List<Location> locs = zone.getLocs();
		final Location loc = (point >= 1 && point <= locs.size()) ? locs.get(point - 1) : zone.getRandomLoc();
		player.teleportTo(loc, 20);
	}
	
	/**
	 * Boton de teleport del NPC (1st, 2nd, 3rd, 4th).<br>
	 * Con tiempo guardado entra gratis a ese punto. Sin tiempo, cobra la entrada y entra.
	 * @param player : el jugador.
	 * @param zoneId : id del &lt;time&gt; en el XML.
	 * @param point : numero de &lt;enter&gt; (1 = el primero).
	 */
	public void teleportTime(Player player, int zoneId, int point)
	{
		final InstanceTimeZone zone = getTimeZone(zoneId);
		if (zone != null && zone.getLocs() != null && (point < 1 || point > zone.getLocs().size()))
		{
			player.sendMessage("Ese teleport no esta disponible.");
			return;
		}
		
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		if (bank != null && bank.getTimeSeconds() > 0)
			enterTime(player, zoneId, point);
		else
			payTime(player, zoneId, point);
	}
	
	/**
	 * Llamado por InstanceTimeZone cuando un jugador entra a la zona (por cualquier medio).
	 * @return true si puede quedarse, false si se lo saca.
	 */
	public boolean onTimeZoneEnter(Player player, InstanceTimeZone zone)
	{
		if (player.isGM())
			return true;
		
		if (_timeSessions.containsKey(player.getObjectId()))
			return true;
		
		final Long auth = _timeAuth.remove(player.getObjectId());
		if (auth == null || auth < System.currentTimeMillis())
		{
			kickFromTimeZone(player, zone, "Solo se entra a esta zona por el Instance Farm Manager.");
			return false;
		}
		
		final String key = InstanceFarmBank.resolveKey(player);
		if (key == null)
		{
			kickFromTimeZone(player, zone, "No se pudo identificar tu PC (HWID).");
			return false;
		}
		
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		if (bank.getTimeSeconds() <= 0)
		{
			kickFromTimeZone(player, zone, "No tenes tiempo. Para entrar tenes que pagar en el Instance Farm Manager.");
			return false;
		}
		
		final TimeSession session = new TimeSession(player, zone, bank, key);
		if (isHwidLimitReached(countTimeByKey(key, player)))
		{
			kickFromTimeZone(player, zone, "Solo se permiten " + Config.INSTANCE_FARM_MAX_PER_HWID + " personaje(s) por PC en el Time Farm.");
			return false;
		}
		
		_timeByKey.put(key, session);
		_timeSessions.put(player.getObjectId(), session);
		
		// Zona sin party (InviteParty = false): si entra en party, lo saca de la party.
		if (!zone.isInvitePartyAllowed() && player.isInParty())
		{
			player.getParty().removePartyMember(player, net.sf.l2j.gameserver.enums.MessageType.LEFT);
			player.sendMessage("En el Time Farm no se puede estar en party.");
		}
		
		player.sendMessage("Time Farm: te quedan " + InstanceFarmRun.formatTime(bank.getTimeSeconds()) + ". El tiempo solo corre estando adentro.");
		if (Config.INSTANCE_FARM_TIME_SHOW_TIMER)
			player.sendPacket(InstanceFarmRun.counterMessage("Tiempo: " + InstanceFarmRun.formatTime(bank.getTimeSeconds()), 3000));
		return true;
	}
	
	/**
	 * Llamado por InstanceTimeZone cuando un jugador sale de la zona (teleport, muerte + pueblo, logout, caminando).
	 */
	public void onTimeZoneExit(Player player, InstanceTimeZone zone)
	{
		final TimeSession session = _timeSessions.get(player.getObjectId());
		if (session == null || session.zone != zone)
			return;
		
		closeSession(session);
		
		if (session.bank.getTimeSeconds() > 0)
			_timeAuth.put(player.getObjectId(), System.currentTimeMillis() + REENTRY_GRACE_MS);
	}
	
	private void closeSession(TimeSession session)
	{
		_timeSessions.remove(session.player.getObjectId(), session);
		_timeByKey.remove(session.hwid, session);
		session.bank.save();
	}
	
	private void kickFromTimeZone(Player player, InstanceTimeZone zone, String reason)
	{
		player.sendMessage(reason);
		player.sendPacket(new ExShowScreenMessage(reason, 4000));
		
		ThreadPool.schedule(() ->
		{
			if (player.isOnline() && zone.isCharacterInZone(player) && !_timeSessions.containsKey(player.getObjectId()))
				teleportOut(player);
		}, 1000);
	}
	
	private void onTimeTick()
	{
		for (TimeSession s : _timeSessions.values())
		{
			try
			{
				final Player player = s.player;
				if (!player.isOnline() || !s.zone.isCharacterInZone(player))
				{
					closeSession(s);
					continue;
				}
				
				// Muerto no se le descuenta.
				if (player.isDead())
					continue;
				
				final int left = s.bank.consumeSecond();
				if (left <= 0)
				{
					closeSession(s);
					player.sendMessage("Se termino tu tiempo en el Time Farm. Para volver a entrar tenes que pagar de nuevo.");
					player.sendPacket(new ExShowScreenMessage("Se termino tu tiempo!", 4000));
					teleportOut(player);
					continue;
				}
				
				if (++s.ticks % 30 == 0)
					s.bank.save();
				
				if (left == 600 || left == 300 || left == 60)
					player.sendMessage("Time Farm: te quedan " + (left / 60) + " minuto(s).");
				
				// Temporizador en pantalla (letra chica, abajo a la derecha): cada TimeTimerSeconds se actualiza y queda fijo.
				// TimeTimerSeconds = 0: solo cada 10 minutos y a los 5 minutos (como antes).
				if (Config.INSTANCE_FARM_TIME_SHOW_TIMER)
				{
					final int every = Config.INSTANCE_FARM_TIME_TIMER_SECONDS;
					if (every > 0 ? (left % every == 0) : ((left % 600 == 0) || left == 300))
						player.sendPacket(InstanceFarmRun.counterMessage("Tiempo: " + InstanceFarmRun.formatTime(left), (every > 0 ? every * 1000 : 0) + 2500));
				}
			}
			catch (Exception e)
			{
				LOGGER.error("InstanceFarmManager: error en el tick del Time Farm.", e);
			}
		}
		
		// Limpia autorizaciones vencidas.
		final long now = System.currentTimeMillis();
		_timeAuth.values().removeIf(expire -> expire < now);
	}
	
	/**
	 * @return los segundos guardados del HWID del jugador.
	 */
	public int getTimeSeconds(Player player)
	{
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		return bank == null ? 0 : bank.getTimeSeconds();
	}
	
	public boolean isInTimeFarm(Player player)
	{
		return player != null && _timeSessions.containsKey(player.getObjectId());
	}
	
	// =====================================================================
	// KILL
	// =====================================================================
	
	public InstanceFarmRun getRun(Player player)
	{
		if (player == null)
			return null;
		return _runs.get(player.getObjectId());
	}
	
	public InstanceFarmRun getRunByInstanceId(int instanceId)
	{
		return _runsByInstance.get(instanceId);
	}
	
	public boolean isInRun(Player player)
	{
		return getRun(player) != null;
	}
	
	public void removeRun(InstanceFarmRun run)
	{
		if (run == null)
			return;
		
		_runs.remove(run.getPlayer().getObjectId(), run);
		if (run.getHwidKey() != null)
			_runsByKey.remove(run.getHwidKey(), run);
		if (run.getInstance() != null)
			_runsByInstance.remove(run.getInstance().getId(), run);
	}
	
	public void buyKills(Player player, int packIndex)
	{
		if (!Config.INSTANCE_FARM_ENABLED)
			return;
		
		if (packIndex < 0 || packIndex >= Config.INSTANCE_FARM_KILL_PACKS.size())
			return;
		
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		if (bank == null)
		{
			player.sendMessage("No se pudo identificar tu PC (HWID).");
			return;
		}
		
		final int[] pack = Config.INSTANCE_FARM_KILL_PACKS.get(packIndex);
		if (player.getInventory().getInventoryItemCount(pack[1], -1) < pack[2])
		{
			player.sendMessage("Necesitas " + getPackPriceText(pack) + " para comprar " + pack[0] + " kills.");
			return;
		}
		
		if (!player.destroyItemByItemId("InstanceFarmKills", pack[1], pack[2], player, true))
			return;
		
		bank.addKills(pack[0]);
		bank.save();
		player.sendMessage("Compraste " + pack[0] + " kills. Total: " + bank.getKillCount() + ".");
		
		final InstanceFarmRun run = _runs.get(player.getObjectId());
		if (run != null)
			run.onKillsAdded();
	}
	
	public void enterKill(Player player, int zoneId)
	{
		final String error = checkCommon(player);
		if (error != null)
		{
			player.sendMessage(error);
			return;
		}
		
		final InstanceFarmZone zone = InstanceFarmZoneData.getZone(zoneId);
		if (zone == null || !zone.isEnabled())
		{
			player.sendMessage("Esa zona no esta disponible.");
			return;
		}
		
		if (zone.getSpawns().isEmpty())
		{
			player.sendMessage("Esa zona todavia no tiene mobs configurados.");
			return;
		}
		
		if (player.getLevel() < zone.getMinLevel())
		{
			player.sendMessage("Necesitas nivel " + zone.getMinLevel() + " para entrar.");
			return;
		}
		
		if (player.isInParty())
		{
			player.sendMessage("El Kill Farm es solo: sali de la party para entrar.");
			return;
		}
		
		final InstanceFarmBank bank = InstanceFarmBank.get(player);
		if (bank.getKillCount() <= 0)
		{
			player.sendMessage("No tenes kills. Compra un pack para entrar.");
			return;
		}
		
		final String hwid = InstanceFarmBank.resolveKey(player);
		if (isHwidLimitReached(countKillByKey(hwid, player)))
		{
			player.sendMessage("Ya hay " + Config.INSTANCE_FARM_MAX_PER_HWID + " personaje(s) de tu PC en el Kill Farm (maximo por PC).");
			return;
		}
		
		final InstanceHolder instance = InstanceManager.getInstance().createInstance();
		final InstanceFarmRun run = new InstanceFarmRun(player, zone, instance, bank);
		
		_runs.put(player.getObjectId(), run);
		run.setHwidKey(hwid);
		_runsByKey.put(hwid, run);
		_runsByInstance.put(instance.getId(), run);
		
		player.setInstance(instance, true);
		player.teleportTo(zone.getEnterLoc().getX(), zone.getEnterLoc().getY(), zone.getEnterLoc().getZ(), 0);
		run.start();
		
		player.sendMessage("Entraste a " + zone.getName() + ". Kills: " + bank.getKillCount() + ". Escribi .exit para salir.");
	}
	
	/**
	 * Llamado por Monster.doDie: si el mob estaba en una instancia de Kill Farm, descuenta la kill.
	 */
	public void onMonsterKilled(Npc npc, net.sf.l2j.gameserver.model.actor.Creature killer)
	{
		// Mob de una zona TIME: recompensa directa al que lo mata (TimeRewards) y sin cadaver (TimeNoCorpse).
		if (isTimeZoneMob(npc))
		{
			final Player player = (killer != null) ? killer.getActingPlayer() : null;
			if (player != null && player.isOnline())
			{
				for (double[] reward : Config.INSTANCE_FARM_TIME_REWARDS)
				{
					if (net.sf.l2j.commons.random.Rnd.get(100000) >= reward[3] * 1000)
						continue;
					
					final int count = net.sf.l2j.commons.random.Rnd.get((int) reward[1], (int) reward[2]);
					if (count > 0)
						player.addItem("TimeFarmReward", (int) reward[0], count, player, true);
				}
			}
			
			if (Config.INSTANCE_FARM_TIME_NO_CORPSE)
				ThreadPool.schedule(() -> InstanceFarmRun.removeCorpse(npc), 300);
			return;
		}
		
		onMonsterKilled(npc);
	}
	
	/**
	 * @return true si el mob es de una zona TIME (spawneado por InstanceFarmZones.xml).
	 */
	public boolean isTimeZoneMob(Npc npc)
	{
		return npc != null && npc.getSpawn() != null && _timeSpawnSet.contains(npc.getSpawn());
	}
	
	public void onMonsterKilled(Npc npc)
	{
		if (npc == null || npc.getInstance() == null || npc.getInstance().getId() == 0)
			return;
		
		final InstanceFarmRun run = _runsByInstance.get(npc.getInstance().getId());
		if (run != null)
			run.onMobKill(npc);
	}
	
	/**
	 * Llamado por Monster.reduceCurrentHp: los skills AoE adentro del Kill Farm le traen todos los mobs al jugador.
	 */
	public void onMonsterHit(Npc npc, net.sf.l2j.gameserver.model.actor.Creature attacker, net.sf.l2j.gameserver.model.L2Skill skill)
	{
		if (skill == null || npc == null || npc.getInstance() == null || npc.getInstance().getId() == 0)
			return;
		
		final InstanceFarmRun run = _runsByInstance.get(npc.getInstance().getId());
		if (run != null)
			run.onMobHit(npc, attacker, skill);
	}
	
	/**
	 * @return true si el mob esta en una instancia de Kill Farm (para no tirar el drop normal al piso, ver KillNormalDrop).
	 */
	public boolean isKillInstanceMob(Npc npc)
	{
		return npc != null && npc.getInstance() != null && npc.getInstance().getId() != 0 && _runsByInstance.containsKey(npc.getInstance().getId());
	}
	
	public void leave(Player player)
	{
		final InstanceFarmRun run = getRun(player);
		if (run == null)
		{
			player.sendMessage("No estas dentro del Kill Farm.");
			return;
		}
		run.end(true);
	}
	
	/**
	 * Muerte (y restart point): cierra el Kill Farm y guarda. En TIME solo guarda (el tiempo no corre estando muerto).
	 */
	public void onPlayerDeath(Player player)
	{
		if (player == null)
			return;
		
		final TimeSession session = _timeSessions.get(player.getObjectId());
		if (session != null)
			session.bank.save();
		
		final InstanceFarmRun run = getRun(player);
		if (run == null || run.isEnded())
			return;
		
		// Lo revive con todo lleno y lo saca a los KillDeathKickSeconds (las kills quedan guardadas).
		run.onOwnerDeath();
	}
	
	/**
	 * "Ir al pueblo" estando muerto adentro: cierra el Kill Farm al toque (el teleport lo hace RequestRestartPoint).
	 */
	public void onRestartPoint(Player player)
	{
		if (player == null)
			return;
		
		final TimeSession session = _timeSessions.get(player.getObjectId());
		if (session != null)
			session.bank.save();
		
		final InstanceFarmRun run = getRun(player);
		if (run != null && !run.isEnded())
			run.end(false);
	}
	
	// =====================================================================
	// Limite por PC (MaxPerHwid del properties, vale para Time y Kill)
	// =====================================================================
	
	/**
	 * @param count : cuantos personajes de esa PC ya estan adentro.
	 * @return true si ya se llego al maximo por PC (MaxPerHwid = 0 es sin limite).
	 */
	private static boolean isHwidLimitReached(int count)
	{
		return Config.INSTANCE_FARM_MAX_PER_HWID > 0 && count >= Config.INSTANCE_FARM_MAX_PER_HWID;
	}
	
	/**
	 * @return cuantos OTROS personajes de esa PC estan en el Time Farm.
	 */
	private int countTimeByKey(String key, Player self)
	{
		if (key == null)
			return 0;
		
		int count = 0;
		for (TimeSession s : _timeSessions.values())
			if (key.equals(s.hwid) && s.player != self)
				count++;
		return count;
	}
	
	/**
	 * @return cuantos OTROS personajes de esa PC estan en el Kill Farm.
	 */
	private int countKillByKey(String key, Player self)
	{
		if (key == null)
			return 0;
		
		int count = 0;
		for (InstanceFarmRun r : _runs.values())
			if (!r.isEnded() && key.equals(r.getHwidKey()) && r.getPlayer() != self)
				count++;
		return count;
	}
	
	public static InstanceFarmManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InstanceFarmManager INSTANCE = new InstanceFarmManager();
	}
}
