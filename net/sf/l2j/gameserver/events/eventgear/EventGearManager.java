package net.sf.l2j.gameserver.events.eventgear;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.events.eventgear.EventGearData.GearItem;
import net.sf.l2j.gameserver.events.eventgear.EventGearData.GearSet;
import net.sf.l2j.gameserver.events.eventgear.EventGearData.GearStack;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.enums.items.ActionType;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

/**
 * Event Gear (basado en la Balance Zone del server ruso).<br>
 * Al entrar a la pelea de TvT / DM / LM / CTF: se guarda y desequipa el equipo propio, se da el set fijo de la clase
 * (xml), los jugadores ven el nick "SanSara" (los GMs ven el real), sin clan, title = kills y skin del evento.<br>
 * Al salir (fin, muerte en LM, logout, admin): se borra lo prestado y se vuelve a equipar lo propio.<br>
 * Todo queda guardado en la tabla event_gear_state, asi si se cae el server se arregla solo al loguear.
 */
public final class EventGearManager
{
	private static final Logger LOG = Logger.getLogger(EventGearManager.class.getName());

	private static final int KIND_BORROWED = 1;
	private static final int KIND_EQUIPPED = 2;
	private static final int KIND_STACK = 3;

	private static final String CREATE = "CREATE TABLE IF NOT EXISTS event_gear_state (char_id INT NOT NULL, kind TINYINT NOT NULL, ref_id INT NOT NULL, val INT NOT NULL DEFAULT 0, PRIMARY KEY (char_id, kind, ref_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	private static final String SELECT = "SELECT kind, ref_id, val FROM event_gear_state WHERE char_id=?";
	private static final String DELETE = "DELETE FROM event_gear_state WHERE char_id=?";
	private static final String INSERT = "REPLACE INTO event_gear_state (char_id, kind, ref_id, val) VALUES (?,?,?,?)";

	/** Estado de un jugador adentro del evento. */
	private static final class State
	{
		final Set<Integer> borrowed = new LinkedHashSet<>();
		final List<Integer> equipped = new ArrayList<>();
		final Map<Integer, Integer> stackBase = new HashMap<>();
		final Set<Integer> autoShots = new LinkedHashSet<>();
		int kills;
	}

	private static final Map<Integer, State> STATES = new ConcurrentHashMap<>();
	private static final Set<Integer> LOCKED_ITEMS = ConcurrentHashMap.newKeySet();

	private EventGearManager()
	{
	}

	public static void init()
	{
		EventGearConfig.load();
		EventGearData.load();

		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement())
		{
			st.executeUpdate(CREATE);
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: no se pudo crear la tabla event_gear_state: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Consultas
	// ------------------------------------------------------------------

	public static boolean isDisguised(Player player)
	{
		return player != null && player.isEventDisguised();
	}

	/**
	 * @param target : el jugador que se dibuja.
	 * @param viewer : el que lo esta mirando (puede ser null).
	 * @return true si al viewer hay que mostrarle el nick falso del target.
	 */
	public static boolean hideFor(Player target, Player viewer)
	{
		if (!isDisguised(target))
			return false;

		if (viewer == target)
			return false;

		if (viewer != null && viewer.isGM() && EventGearConfig.GM_SEE_REAL_NAME)
			return false;

		return true;
	}

	public static String getVisibleName(Player target, Player viewer)
	{
		return hideFor(target, viewer) ? EventGearConfig.FAKE_NAME : target.getName();
	}

	/** Nombre para los mensajes del evento ("X Hunted Player Y"). */
	public static String getDisplayName(Player player)
	{
		if (player == null)
			return "";

		return isDisguised(player) ? EventGearConfig.FAKE_NAME : player.getName();
	}

	public static String getEventTitle(Player player)
	{
		final State state = STATES.get(player.getObjectId());
		final int kills = (state == null) ? 0 : state.kills;
		return EventGearConfig.TITLE_FORMAT.replace("%kills%", String.valueOf(kills));
	}

	public static boolean isEventItem(ItemInstance item)
	{
		return item != null && LOCKED_ITEMS.contains(item.getObjectId());
	}

	/**
	 * Para los packets de drop/trade/WH/venta/enchant/etc.
	 * @param player : el jugador.
	 * @return true (y avisa) si el jugador esta en el evento y la accion se tiene que cancelar.
	 */
	public static boolean blockAction(Player player)
	{
		if (!isDisguised(player))
			return false;

		player.sendMessage(EventGearConfig.MSG_BLOCKED);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return true;
	}

	/** Equipar/desequipar durante el evento. */
	public static boolean blockEquip(Player player)
	{
		if (!isDisguised(player))
			return false;

		player.sendMessage(EventGearConfig.MSG_NO_EQUIP);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return true;
	}

	/** .dressme y skins durante el evento. */
	public static boolean blockSkin(Player player)
	{
		if (!isDisguised(player))
			return false;

		player.sendMessage(EventGearConfig.MSG_NO_SKIN);
		return true;
	}

	// ------------------------------------------------------------------
	// Hooks de los eventos
	// ------------------------------------------------------------------

	/**
	 * Lo llaman los teleporters de TvT / DM / LM / CTF.
	 * @param player : el jugador teleportado.
	 * @param event : "TvT", "DM", "LM" o "CTF".
	 * @param inFight : true si va a la pelea, false si vuelve (fin, admin, sin creditos).
	 */
	public static void onEventTeleport(Player player, String event, boolean inFight)
	{
		if (player == null)
			return;

		try
		{
			if (inFight && EventGearConfig.isEnabledFor(event))
				apply(player);
			else
				restore(player, true);
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: error con " + player.getName() + ": " + e.getMessage());
		}
	}

	public static void onEventKill(Player killer)
	{
		if (killer == null)
			return;

		final State state = STATES.get(killer.getObjectId());
		if (state == null)
			return;

		state.kills++;
		killer.broadcastTitleInfo();
	}

	// ------------------------------------------------------------------
	// Entrar / salir
	// ------------------------------------------------------------------

	public static synchronized void apply(Player player)
	{
		if (player.isEventDisguised() || STATES.containsKey(player.getObjectId()))
			return;

		// Si quedo algo colgado de antes (crash), primero se arregla.
		restoreFromDatabase(player);

		final State state = new State();
		final PcInventory inv = player.getInventory();
		final GearSet set = EventGearData.getSet(player.getClassId().getId());

		if (set != null)
		{
			// 1. Guardar y desequipar todo lo propio.
			for (int slot = 0; slot < Inventory.PAPERDOLL_TOTALSLOTS; slot++)
			{
				final ItemInstance item = inv.getPaperdollItem(slot);
				if (item != null && !state.equipped.contains(item.getObjectId()))
					state.equipped.add(item.getObjectId());
			}
			for (int slot = 0; slot < Inventory.PAPERDOLL_TOTALSLOTS; slot++)
			{
				if (inv.getPaperdollItem(slot) != null)
					inv.unEquipItemInSlot(slot);
			}

			// 2. Dar y equipar el set del evento.
			for (GearItem gi : set.items)
			{
				final ItemInstance item = inv.addItem("EventGear", gi.id, 1, player, null);
				if (item == null)
				{
					LOG.warning("EventGear: el item " + gi.id + " no existe (set '" + set.name + "').");
					continue;
				}

				state.borrowed.add(item.getObjectId());
				LOCKED_ITEMS.add(item.getObjectId());

				final int enchant = (gi.enchant >= 0) ? gi.enchant : EventGearConfig.DEFAULT_ENCHANT;
				if (enchant > 0)
					item.setEnchantLevel(enchant);

				if (item.isEquipable())
					inv.equipItem(item);
			}

			// 3. Shots / flechas: se anota cuanto tenia antes para devolverle solo lo suyo.
			for (GearStack gs : set.stacks)
			{
				if (state.stackBase.containsKey(gs.id))
					continue;

				final ItemInstance current = inv.getItemByItemId(gs.id);
				state.stackBase.put(gs.id, (current == null) ? 0 : current.getCount());
				final ItemInstance added = inv.addItem("EventGear", gs.id, gs.count, player, null);
				
				// Shots del evento: se activan solos (y se apagan al salir si no los tenia activados).
				if (added != null && (added.getItem().getDefaultAction() == ActionType.soulshot || added.getItem().getDefaultAction() == ActionType.spiritshot) && !player.getAutoSoulShot().contains(gs.id))
				{
					player.addAutoSoulShot(gs.id);
					state.autoShots.add(gs.id);
				}
			}

			player.setEventDisguise(true, set.armorSkin, set.weaponSkin, set.hairSkin, set.faceSkin, set.shieldSkin, set.raceSkin, set.classSkin);
		}
		else
		{
			// Clase sin set en el xml: juega con su equipo, pero igual con nick SanSara y sin skins.
			player.setEventDisguise(true, 0, 0, 0, 0, 0, -1, -1);
		}

		STATES.put(player.getObjectId(), state);
		saveToDatabase(player, state);

		player.sendPacket(new ItemList(player, false));
		for (int shotId : state.autoShots)
			player.sendPacket(new ExAutoSoulShot(shotId, 1));
		if (!state.autoShots.isEmpty())
			player.rechargeShots(true, true);
		player.broadcastUserInfo();
		if (set != null)
			player.sendMessage(EventGearConfig.MSG_ENTER);
	}

	/**
	 * Saca el set del evento y devuelve el equipo propio. Si el jugador no estaba en el evento no hace nada.
	 * @param player : el jugador.
	 * @param sendPackets : false en logout (el cliente ya no esta).
	 */
	public static synchronized void restore(Player player, boolean sendPackets)
	{
		if (player == null)
			return;

		final State state = STATES.remove(player.getObjectId());
		final boolean wasDisguised = player.isEventDisguised();
		player.setEventDisguise(false, 0, 0, 0, 0, 0, -1, -1);

		if (state == null)
		{
			if (wasDisguised && sendPackets)
				player.broadcastUserInfo();
			return;
		}

		final boolean hadGear = !state.borrowed.isEmpty() || !state.stackBase.isEmpty();
		for (int shotId : state.autoShots)
		{
			player.removeAutoSoulShot(shotId);
			if (sendPackets)
				player.sendPacket(new ExAutoSoulShot(shotId, 0));
		}
		giveBack(player, state.borrowed, state.equipped, state.stackBase);
		deleteFromDatabase(player);

		if (sendPackets)
		{
			player.sendPacket(new ItemList(player, false));
			player.broadcastUserInfo();
			if (hadGear)
				player.sendMessage(EventGearConfig.MSG_EXIT);
		}
	}

	/** EnterWorld: si el server se cayo con el jugador adentro del evento, se le devuelve todo. */
	public static void onEnterWorld(Player player)
	{
		if (player == null)
			return;

		try
		{
			if (restoreFromDatabase(player))
			{
				player.sendPacket(new ItemList(player, false));
				player.broadcastUserInfo();
				player.sendMessage(EventGearConfig.MSG_EXIT);
			}
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: error restaurando a " + player.getName() + ": " + e.getMessage());
		}
	}

	private static void giveBack(Player player, Set<Integer> borrowed, List<Integer> equipped, Map<Integer, Integer> stackBase)
	{
		final PcInventory inv = player.getInventory();

		// 1. Desequipar todo (set del evento + flechas).
		for (int slot = 0; slot < Inventory.PAPERDOLL_TOTALSLOTS; slot++)
		{
			if (inv.getPaperdollItem(slot) != null)
				inv.unEquipItemInSlot(slot);
		}

		// 2. Borrar lo prestado.
		for (int objectId : borrowed)
		{
			LOCKED_ITEMS.remove(objectId);
			final ItemInstance item = inv.getItemByObjectId(objectId);
			if (item != null)
				inv.destroyItem("EventGear", item, player, null);
		}

		// 3. Shots / flechas: se saca solo lo que sobro del evento.
		for (Map.Entry<Integer, Integer> e : stackBase.entrySet())
		{
			final ItemInstance item = inv.getItemByItemId(e.getKey());
			if (item == null)
				continue;

			final int extra = item.getCount() - e.getValue();
			if (extra > 0)
				inv.destroyItem("EventGear", item, extra, player, null);
		}

		// 4. Volver a equipar lo propio.
		for (int objectId : equipped)
		{
			final ItemInstance item = inv.getItemByObjectId(objectId);
			if (item != null && !item.isEquipped() && item.isEquipable())
				inv.equipItem(item);
		}
	}

	// ------------------------------------------------------------------
	// Base de datos (por si se cae el server)
	// ------------------------------------------------------------------

	private static void saveToDatabase(Player player, State state)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(DELETE))
			{
				ps.setInt(1, player.getObjectId());
				ps.executeUpdate();
			}

			try (PreparedStatement ps = con.prepareStatement(INSERT))
			{
				for (int objectId : state.borrowed)
					addRow(ps, player, KIND_BORROWED, objectId, 0);
				for (int objectId : state.equipped)
					addRow(ps, player, KIND_EQUIPPED, objectId, 0);
				for (Map.Entry<Integer, Integer> e : state.stackBase.entrySet())
					addRow(ps, player, KIND_STACK, e.getKey(), e.getValue());

				ps.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: no se pudo guardar el estado de " + player.getName() + ": " + e.getMessage());
		}
	}

	private static void addRow(PreparedStatement ps, Player player, int kind, int refId, int val) throws Exception
	{
		ps.setInt(1, player.getObjectId());
		ps.setInt(2, kind);
		ps.setInt(3, refId);
		ps.setInt(4, val);
		ps.addBatch();
	}

	private static void deleteFromDatabase(Player player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE))
		{
			ps.setInt(1, player.getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: no se pudo borrar el estado de " + player.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * @param player : el jugador.
	 * @return true si habia algo guardado y se le devolvio el equipo.
	 */
	private static boolean restoreFromDatabase(Player player)
	{
		final Set<Integer> borrowed = new LinkedHashSet<>();
		final List<Integer> equipped = new ArrayList<>();
		final Map<Integer, Integer> stackBase = new HashMap<>();

		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					switch (rs.getInt("kind"))
					{
						case KIND_BORROWED:
							borrowed.add(rs.getInt("ref_id"));
							break;
						case KIND_EQUIPPED:
							equipped.add(rs.getInt("ref_id"));
							break;
						case KIND_STACK:
							stackBase.put(rs.getInt("ref_id"), rs.getInt("val"));
							break;
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: no se pudo leer el estado de " + player.getName() + ": " + e.getMessage());
			return false;
		}

		if (borrowed.isEmpty() && equipped.isEmpty() && stackBase.isEmpty())
			return false;

		giveBack(player, borrowed, equipped, stackBase);
		deleteFromDatabase(player);
		LOG.info("EventGear: se devolvio el equipo de " + player.getName() + " (habia quedado del evento).");
		return true;
	}
}
