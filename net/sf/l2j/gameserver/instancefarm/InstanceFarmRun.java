package net.sf.l2j.gameserver.instancefarm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.events.instance.InstanceHolder;
import net.sf.l2j.gameserver.events.instance.InstanceManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;

/**
 * Una corrida de KILL Farm: instancia privada de UN jugador.<br>
 * Cada mob muerto descuenta 1 kill del banco del HWID. Con 0 kills se termina y lo saca.
 */
public class InstanceFarmRun
{
	private static final CLogger LOGGER = new CLogger(InstanceFarmRun.class.getName());
	
	private final Player _player;
	private final InstanceFarmZone _zone;
	private final InstanceHolder _instance;
	private final InstanceFarmBank _bank;
	private final List<L2Spawn> _spawns = new ArrayList<>();
	private String _hwidKey;
	
	private volatile boolean _ended;
	/** El jugador murio adentro: en unos segundos se lo revive y se lo saca. */
	private volatile boolean _dying;
	private boolean _emptied;
	private int _ticks;
	
	private ScheduledFuture<?> _tickTask;
	private ScheduledFuture<?> _kickTask;
	private ScheduledFuture<?> _spawnTask;
	
	/** Contador en pantalla: cuando se mando el ultimo y si hay uno pendiente (para no parpadear al matar varios juntos). */
	private long _lastCounterTime;
	private ScheduledFuture<?> _counterTask;
	
	public InstanceFarmRun(Player player, InstanceFarmZone zone, InstanceHolder instance, InstanceFarmBank bank)
	{
		_player = player;
		_zone = zone;
		_instance = instance;
		_bank = bank;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public InstanceFarmZone getZone()
	{
		return _zone;
	}
	
	public InstanceHolder getInstance()
	{
		return _instance;
	}
	
	public InstanceFarmBank getBank()
	{
		return _bank;
	}
	
	public String getHwidKey()
	{
		return _hwidKey;
	}
	
	public void setHwidKey(String hwidKey)
	{
		_hwidKey = hwidKey;
	}
	
	public boolean isEnded()
	{
		return _ended;
	}
	
	public int getKillsLeft()
	{
		return _bank.getKillCount();
	}
	
	public void start()
	{
		_tickTask = ThreadPool.scheduleAtFixedRate(this::onTick, 5000, 5000);
		
		final int delay = Config.INSTANCE_FARM_KILL_SPAWN_DELAY_SECONDS;
		if (delay <= 0)
		{
			spawnMobs();
			return;
		}
		
		_player.sendMessage("Los mobs aparecen en " + delay + " segundo(s)...");
		_player.sendPacket(new ExShowScreenMessage("Los mobs aparecen en " + delay + "s", 3000));
		_spawnTask = ThreadPool.schedule(this::spawnMobs, delay * 1000L);
	}
	
	private void onTick()
	{
		if (_ended)
			return;
		
		if (_player == null || !_player.isOnline())
		{
			end(false);
			return;
		}
		
		// Guardado de seguridad cada 30s por si se cae el server.
		if (++_ticks % 6 == 0)
			_bank.save();
	}
	
	private synchronized void spawnMobs()
	{
		_spawnTask = null;
		if (_ended)
			return;
		
		for (InstanceFarmZone.KillSpawn ks : _zone.getSpawns())
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(ks.getMobId());
			if (template == null)
			{
				LOGGER.warn("InstanceFarmRun: no existe el npc {} (zona {}).", ks.getMobId(), _zone.getId());
				continue;
			}
			
			try
			{
				// Los mobs salen mirando hacia donde entra el jugador (KillFacePlayer).
				int heading = ks.getHeading();
				if (Config.INSTANCE_FARM_KILL_FACE_PLAYER)
				{
					final Location enter = _zone.getEnterLoc();
					heading = MathUtil.calculateHeadingFrom(ks.getX(), ks.getY(), enter.getX(), enter.getY());
				}
				
				final L2Spawn spawn = new L2Spawn(template);
				spawn.setLoc(ks.getX(), ks.getY(), ks.getZ(), heading);
				spawn.setRespawnDelay(_zone.getRespawnSeconds());
				spawn.setRespawnState(true);
				SpawnTable.getInstance().addSpawn(spawn, false);
				
				final Npc npc = spawn.doSpawn(false);
				if (npc != null)
				{
					// No silencioso: el mob nace en el mundo normal (instancia 0) y hay que re-spawnearlo en la instancia
					// del jugador para que le llegue al cliente. Con "true" el jugador nunca lo veia.
					npc.setInstance(_instance, false);
					if (Config.INSTANCE_FARM_KILL_FACE_PLAYER && npc instanceof Attackable)
						((Attackable) npc).setNoRndWalk(true);
				}
				
				_spawns.add(spawn);
			}
			catch (Exception e)
			{
				LOGGER.error("InstanceFarmRun: error spawneando {}.", e, ks.getMobId());
			}
		}
		
		showCounter();
	}
	
	/**
	 * Llamado cuando muere un monster dentro de esta instancia.
	 * @param npc : el mob que murio.
	 */
	public synchronized void onMobKill(Npc npc)
	{
		if (_ended || _emptied)
			return;
		
		final int left = _bank.consumeKill();
		if (left < 0)
			return;
		
		giveRewards();
		
		// El mob desaparece al toque (sin cadaver) y reaparece en su lugar con el respawn de la zona.
		if (Config.INSTANCE_FARM_KILL_NO_CORPSE && npc != null)
			ThreadPool.schedule(() -> removeCorpse(npc), 300);
		
		if (left <= 0)
		{
			_emptied = true;
			_bank.save();
			ThreadPool.schedule(this::despawnMobs, 300);
			scheduleKick("Se terminaron tus kills!");
			return;
		}
		
		showCounter();
	}
	
	/**
	 * Recompensa directa al inventario por cada mob (KillRewards del properties).
	 */
	private void giveRewards()
	{
		if (_player == null || !_player.isOnline())
			return;
		
		for (double[] reward : Config.INSTANCE_FARM_KILL_REWARDS)
		{
			if (Rnd.get(100000) >= reward[3] * 1000)
				continue;
			
			final int count = Rnd.get((int) reward[1], (int) reward[2]);
			if (count > 0)
				_player.addItem("InstanceFarmReward", (int) reward[0], count, _player, true);
		}
	}
	
	public static void removeCorpse(Npc npc)
	{
		if (!npc.isDead() || npc.isDecayed())
			return;
		
		DecayTaskManager.getInstance().cancel(npc);
		npc.onDecay();
	}
	
	/**
	 * Un mob de la instancia recibio un golpe. Si fue con un skill en area (AoE), se le vienen encima todos los mobs (KillAoeAggro).
	 * @param mob : el mob golpeado.
	 * @param attacker : quien pego.
	 * @param skill : el skill (null = golpe normal).
	 */
	public void onMobHit(Npc mob, Creature attacker, L2Skill skill)
	{
		if (_ended || !Config.INSTANCE_FARM_KILL_AOE_AGGRO || skill == null || attacker == null || !skill.isAOE())
			return;
		
		final Creature target = (attacker.getActingPlayer() != null) ? attacker.getActingPlayer() : attacker;
		for (L2Spawn spawn : new ArrayList<>(_spawns))
		{
			final Npc other = spawn.getNpc();
			if (other == null || other == mob || other.isDead() || !(other instanceof Attackable))
				continue;
			
			final Attackable a = (Attackable) other;
			if (a.getAggroList().containsKey(target))
				continue;
			
			a.setRunning();
			a.addDamageHate(target, 0, 999);
		}
	}
	
	/**
	 * Murio adentro: a los KillDeathKickSeconds se lo revive con HP/MP/CP llenos y se lo saca. Las kills quedan guardadas.
	 */
	public void onOwnerDeath()
	{
		if (_ended || _dying)
			return;
		
		_dying = true;
		_bank.save();
		
		if (_player != null && _player.isOnline())
		{
			_player.sendMessage("Moriste en el Kill Farm. En " + Config.INSTANCE_FARM_KILL_DEATH_KICK_SECONDS + " segundos te revivimos y te sacamos. Tus kills quedan guardadas.");
			_player.sendPacket(new ExShowScreenMessage("Moriste! Te sacamos en " + Config.INSTANCE_FARM_KILL_DEATH_KICK_SECONDS + "s", 3000));
		}
		
		ThreadPool.schedule(() ->
		{
			if (_ended)
				return;
			
			if (_player != null && _player.isOnline() && _player.isDead())
			{
				_player.doRevive();
				_player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
				_player.setCurrentCp(_player.getMaxCp());
			}
			end(_player != null && _player.isOnline());
		}, Config.INSTANCE_FARM_KILL_DEATH_KICK_SECONDS * 1000L);
	}
	
	/**
	 * Deslogueo adentro: se cierra la instancia y queda guardado afuera (en la salida del properties o en el pueblo).
	 */
	public void endForLogout()
	{
		end(false);
		
		if (_player == null)
			return;
		
		if (Config.INSTANCE_FARM_EXIT_X == 0 && Config.INSTANCE_FARM_EXIT_Y == 0)
		{
			final Location town = MapRegionData.getInstance().getLocationToTeleport(_player, TeleportType.TOWN);
			if (town != null)
				_player.setXYZInvisible(town);
		}
		else
			_player.setXYZInvisible(Config.INSTANCE_FARM_EXIT_X, Config.INSTANCE_FARM_EXIT_Y, Config.INSTANCE_FARM_EXIT_Z);
	}
	
	public void onKillsAdded()
	{
		if (!_ended)
			showCounter();
	}
	
	/**
	 * Muestra "Kills: X". Como maximo una vez cada CounterUpdateMs: si se matan varios juntos, se junta todo en un solo
	 * mensaje al final (asi no parpadea). El mensaje dura un poco mas que ese intervalo para que quede fijo en pantalla.
	 */
	private synchronized void showCounter()
	{
		if (!Config.INSTANCE_FARM_KILL_SHOW_COUNTER || _player == null || !_player.isOnline())
			return;
		
		final long wait = _lastCounterTime + Config.INSTANCE_FARM_COUNTER_UPDATE_MS - System.currentTimeMillis();
		if (wait > 0)
		{
			if (_counterTask == null)
				_counterTask = ThreadPool.schedule(this::sendCounterNow, wait);
			return;
		}
		
		sendCounterNow();
	}
	
	private synchronized void sendCounterNow()
	{
		_counterTask = null;
		if (_ended || _player == null || !_player.isOnline())
			return;
		
		_lastCounterTime = System.currentTimeMillis();
		_player.sendPacket(counterMessage("Kills: " + _bank.getKillCount(), Config.INSTANCE_FARM_COUNTER_UPDATE_MS + 3000));
	}
	
	private void scheduleKick(String reason)
	{
		if (_player != null && _player.isOnline())
		{
			_player.sendMessage(reason + " Vas a ser teletransportado afuera.");
			_player.sendPacket(new ExShowScreenMessage(reason, 4000));
		}
		
		final int delay = Config.INSTANCE_FARM_KILL_KICK_DELAY_SECONDS;
		if (delay <= 0)
			end(true);
		else if (_kickTask == null)
			_kickTask = ThreadPool.schedule(() -> end(true), delay * 1000L);
	}
	
	public synchronized void end(boolean teleportOut)
	{
		if (_ended)
			return;
		
		_ended = true;
		
		if (_tickTask != null)
		{
			_tickTask.cancel(false);
			_tickTask = null;
		}
		if (_kickTask != null)
		{
			_kickTask.cancel(false);
			_kickTask = null;
		}
		if (_spawnTask != null)
		{
			_spawnTask.cancel(false);
			_spawnTask = null;
		}
		
		_bank.save();
		despawnMobs();
		
		if (_player != null)
		{
			_player.setInstance(InstanceManager.getInstance().getInstance(0), true);
			
			if (teleportOut && _player.isOnline())
				InstanceFarmManager.teleportOut(_player);
		}
		
		InstanceFarmManager.getInstance().removeRun(this);
		
		if (_instance != null && _instance.getId() != 0)
			InstanceManager.getInstance().deleteInstance(_instance.getId());
	}
	
	private synchronized void despawnMobs()
	{
		for (L2Spawn spawn : _spawns)
		{
			try
			{
				spawn.setRespawnState(false);
				if (spawn.getNpc() != null)
					spawn.getNpc().deleteMe();
				SpawnTable.getInstance().deleteSpawn(spawn, false);
			}
			catch (Exception e)
			{
				// limpieza
			}
		}
		_spawns.clear();
	}
	
	/**
	 * Mensaje en pantalla de los contadores (Kills / Tiempo): letra chica o grande y posicion, del properties
	 * (CounterSmallFont / CounterPosition).
	 */
	public static ExShowScreenMessage counterMessage(String text, int time)
	{
		ExShowScreenMessage.SMPOS pos;
		try
		{
			pos = ExShowScreenMessage.SMPOS.valueOf(Config.INSTANCE_FARM_COUNTER_POSITION);
		}
		catch (Exception e)
		{
			pos = ExShowScreenMessage.SMPOS.BOTTOM_RIGHT;
		}
		
		// type 1, sin system message, posicion, no oculto, size (1 = letra chica), sin efecto, tiempo, sin fade.
		return new ExShowScreenMessage(1, -1, pos.ordinal(), false, Config.INSTANCE_FARM_COUNTER_SMALL ? 1 : 0, 0, 0, false, time, false, text);
	}
	
	public static String formatTime(long seconds)
	{
		// Siempre en minutos:segundos (60:00, 45:30...), aunque pase de 1 hora.
		final long s = Math.max(0, seconds);
		return String.format("%02d:%02d", s / 60, s % 60);
	}
}
