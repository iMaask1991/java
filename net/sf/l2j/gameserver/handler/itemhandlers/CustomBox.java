package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.BoxData;
import net.sf.l2j.gameserver.data.xml.BoxData.Box;
import net.sf.l2j.gameserver.data.xml.BoxData.Group;
import net.sf.l2j.gameserver.data.xml.BoxData.Reward;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * Handler generico para todas las cajitas definidas en data/xml/custom/boxes.xml.
 * En el XML del item: &lt;set name="handler" val="CustomBox" /&gt;
 */
public class CustomBox implements IItemHandler
{
	/** Tiempo minimo entre aperturas (ms), anti macro / anti spam de announces. */
	private static final long OPEN_DELAY = 300;

	private static final Map<Integer, Long> LAST_OPEN = new ConcurrentHashMap<>();

	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;

		final Player player = (Player) playable;

		final Box box = BoxData.getInstance().getBox(item.getItemId());
		if (box == null)
		{
			player.sendMessage("Esta caja no tiene drop configurado.");
			return;
		}

		// Anti spam
		final long now = System.currentTimeMillis();
		final Long last = LAST_OPEN.get(player.getObjectId());
		if (last != null && now - last < OPEN_DELAY)
			return;
		LAST_OPEN.put(player.getObjectId(), now);

		// Lugar en el inventario para el peor caso
		if (!player.getInventory().validateCapacity(box.getMaxSlots()))
		{
			player.sendMessage("Necesitas al menos " + box.getMaxSlots() + " espacios libres en el inventario para abrir " + box.getName() + ".");
			return;
		}

		// Primero se consume la caja. Si falla, no se entrega nada (anti dupe).
		if (!player.destroyItem("CustomBox", item.getObjectId(), 1, null, true))
			return;

		// Cada grupo tira independiente
		boolean gotSomething = false;
		for (Group group : box.getGroups())
		{
			final List<Reward> rewards = group.roll();
			for (Reward reward : rewards)
			{
				gotSomething = true;
				final int count = reward.rollCount();
				player.addItem("CustomBox", reward.getItemId(), count, player, true);

				if (group.isAnnounce())
				{
					final Item template = ItemTable.getInstance().getTemplate(reward.getItemId());
					final String itemName = (template != null) ? template.getName() : String.valueOf(reward.getItemId());

					// Mismo estilo que los eventos (critical announce, Config.ANNOUNCE_ID_EVENT)
					Announcements.Announce(player.getName() + " abrio " + box.getName() + " y obtuvo " + (count > 1 ? count + " " : "") + itemName + "!");
					LOGGER.info("CustomBox: {} obtuvo {} x{} de {}.", player.getName(), itemName, count, box.getName());
				}
			}
		}
		
		// No salio nada: mensaje solo para el jugador, por chat
		if (!gotSomething)
			player.sendMessage("La " + box.getName() + " estaba vacia...");
	}
}
