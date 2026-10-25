package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.alchemy.AlchemySlot;
import net.sf.l2j.gameserver.alchemy.AttributeAlchemyData;
import net.sf.l2j.gameserver.alchemy.AttributeAlchemyManager;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.element.ElementType;
import net.sf.l2j.gameserver.element.ItemElement;
import net.sf.l2j.gameserver.element.ItemElementData;
import net.sf.l2j.gameserver.element.ItemElementManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * NPC 60022 - Attribute Manager: saca un elemento o un socket de lo que tenes equipado.
 * <ul>
 * <li>Elementos: arma y armaduras (casco, pechera / armadura entera, pantalon, guantes, botas). Se saca de a uno.</li>
 * <li>Sockets: arma, armaduras y joyas. Se saca de a uno (el proximo orbe vuelve a llenar ese socket vacio).</li>
 * </ul>
 * El precio sale de elements.xml y attributeAlchemy.xml (resetItemId / resetCount, 0 = gratis).<br>
 * Todo el diseno (textos, colores, botones) esta en data/html/mods/AttributeReset/: aca solo se rellenan los datos.<br>
 * Toda la validacion se hace aca (el item tiene que estar equipado y ser tuyo), no en el HTML.
 */
public class AttributeResetNpc extends Folk
{
	private static final String HTML_PATH = "data/html/mods/AttributeReset/";

	/** Partes que pueden tener elemento. */
	private static final int[] ELEMENT_SLOTS =
	{
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET
	};

	/** Partes que pueden tener sockets (arma, armaduras, escudo y joyas). */
	private static final int[] SOCKET_SLOTS =
	{
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_NECK,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_RFINGER
	};
	
	public AttributeResetNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		showMain(player);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String action = st.nextToken();
		final int objectId;
		final int param;
		try
		{
			objectId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			param = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
		}
		catch (NumberFormatException e)
		{
			return;
		}

		switch (action)
		{
			case "reset_main":
				showMain(player);
				return;

			case "reset_elements":
				showElements(player);
				return;

			case "reset_sockets":
				showSockets(player);
				return;

			case "reset_elem_ask":
				askElement(player, objectId, param);
				return;

			case "reset_elem_do":
				doElement(player, objectId, param);
				return;

			case "reset_sock_ask":
				askSocket(player, objectId, param);
				return;

			case "reset_sock_do":
				doSocket(player, objectId, param);
				return;
		}

		super.onBypassFeedback(player, command);
	}

	// ------------------------------------------------------------------------
	// Paginas (todo el diseno esta en data/html/mods/AttributeReset/, aca solo se rellenan los datos)
	// ------------------------------------------------------------------------
	
	private void showMain(Player player)
	{
		String html = load("main.htm");
		html = html.replace("%elemPrice%", priceText(ItemElementData.getInstance().getResetItemId(), ItemElementData.getInstance().getResetCount()));
		html = html.replace("%sockPrice%", priceText(AttributeAlchemyData.getInstance().getResetItemId(), AttributeAlchemyData.getInstance().getResetCount()));
		send(player, html);
	}
	
	private void showElements(Player player)
	{
		final ItemElementData data = ItemElementData.getInstance();
		final ItemElementManager manager = ItemElementManager.getInstance();
		final String itemTpl = load("item.htm");
		final String itemEndTpl = load("item_end.htm");
		final String rowTpl = load("elements_row.htm");
		final StringBuilder sb = new StringBuilder();
		
		for (int slot : ELEMENT_SLOTS)
		{
			final ItemInstance item = player.getInventory().getPaperdollItem(slot);
			if (item == null)
				continue;
			
			final boolean weapon = manager.isWeapon(item);
			if (!weapon && !manager.isArmor(item))
				continue;
			
			final List<ItemElement> list = data.getAll(item.getObjectId());
			if (list.isEmpty())
				continue;
			
			sb.append(itemTpl.replace("%itemName%", itemName(item)));
			for (ItemElement e : list)
			{
				sb.append(rowTpl.replace("%element%", e.getElement().getName()).replace("%desc%", ItemElementManager.getDescription(weapon, e.getElement(), e.getValue())).replace("%itemObjId%", String.valueOf(item.getObjectId())).replace("%elemId%", String.valueOf(e.getElement().getId())));
			}
			sb.append(itemEndTpl);
		}
		
		if (sb.length() == 0)
			sb.append(load("elements_empty.htm"));
		
		send(player, load("elements.htm").replace("%rows%", sb.toString()));
	}
	
	private void showSockets(Player player)
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		final AttributeAlchemyManager manager = AttributeAlchemyManager.getInstance();
		final String itemTpl = load("item.htm");
		final String itemEndTpl = load("item_end.htm");
		final String rowTpl = load("sockets_row.htm");
		final StringBuilder sb = new StringBuilder();
		
		for (int slot : SOCKET_SLOTS)
		{
			final ItemInstance item = player.getInventory().getPaperdollItem(slot);
			if (item == null || !manager.isEligible(item))
				continue;
			
			// Arma de 2 manos: ocupa la mano derecha y la izquierda, se muestra una sola vez
			if (slot == Inventory.PAPERDOLL_LHAND && item == player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND))
				continue;
			
			final Map<Integer, AlchemySlot> slots = data.getSlots(item.getObjectId());
			if (slots.isEmpty())
				continue;
			
			sb.append(itemTpl.replace("%itemName%", itemName(item)));
			for (int i = 1; i <= 3; i++)
			{
				final AlchemySlot s = slots.get(i);
				if (s == null)
					continue;
				
				sb.append(rowTpl.replace("%slot%", String.valueOf(i)).replace("%desc%", s.getDisplay()).replace("%tier%", String.valueOf(s.getTier())).replace("%itemObjId%", String.valueOf(item.getObjectId())));
			}
			sb.append(itemEndTpl);
		}
		
		if (sb.length() == 0)
			sb.append(load("sockets_empty.htm"));
		
		send(player, load("sockets.htm").replace("%rows%", sb.toString()));
	}
	
	// ------------------------------------------------------------------------
	// Elementos
	// ------------------------------------------------------------------------

	private void askElement(Player player, int objectId, int elementId)
	{
		final ItemInstance item = getEquipped(player, objectId);
		final ElementType element = ElementType.getById(elementId);
		final ItemElement e = (item == null || element == null) ? null : ItemElementData.getInstance().get(item.getObjectId(), element);
		if (e == null)
		{
			showElements(player);
			return;
		}

		final boolean weapon = ItemElementManager.getInstance().isWeapon(item);
		final ItemElementData data = ItemElementData.getInstance();
		String html = load("confirm_element.htm");
		html = html.replace("%element%", element.getName()).replace("%desc%", ItemElementManager.getDescription(weapon, element, e.getValue())).replace("%itemName%", itemName(item));
		html = html.replace("%price%", priceText(data.getResetItemId(), data.getResetCount())).replace("%itemObjId%", String.valueOf(objectId)).replace("%elemId%", String.valueOf(elementId));
		send(player, html);
	}

	private void doElement(Player player, int objectId, int elementId)
	{
		final ItemElementData data = ItemElementData.getInstance();
		final ItemInstance item = getEquipped(player, objectId);
		final ElementType element = ElementType.getById(elementId);
		if (item == null || element == null || data.get(item.getObjectId(), element) == null)
		{
			player.sendMessage("Ese item ya no tiene ese elemento (tiene que estar equipado).");
			showElements(player);
			return;
		}

		if (!pay(player, data.getResetItemId(), data.getResetCount()))
		{
			showElements(player);
			return;
		}

		data.delete(item.getObjectId(), element);
		ItemElementManager.getInstance().sendItem(player, item);
		player.sendMessage(element.getName() + " sacado de " + item.getItemName() + ".");
		showElements(player);
	}

	// ------------------------------------------------------------------------
	// Sockets
	// ------------------------------------------------------------------------

	private void askSocket(Player player, int objectId, int slotId)
	{
		final ItemInstance item = getEquipped(player, objectId);
		final AlchemySlot s = (item == null) ? null : AttributeAlchemyData.getInstance().getSlots(item.getObjectId()).get(slotId);
		if (s == null)
		{
			showSockets(player);
			return;
		}

		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		String html = load("confirm_socket.htm");
		html = html.replace("%slot%", String.valueOf(slotId)).replace("%desc%", s.getDisplay()).replace("%tier%", String.valueOf(s.getTier())).replace("%itemName%", itemName(item));
		html = html.replace("%price%", priceText(data.getResetItemId(), data.getResetCount())).replace("%itemObjId%", String.valueOf(objectId));
		send(player, html);
	}

	private void doSocket(Player player, int objectId, int slotId)
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		final ItemInstance item = getEquipped(player, objectId);
		if (item == null || data.getSlots(item.getObjectId()).get(slotId) == null)
		{
			player.sendMessage("Ese item ya no tiene ese socket (tiene que estar equipado).");
			showSockets(player);
			return;
		}

		if (!pay(player, data.getResetItemId(), data.getResetCount()))
		{
			showSockets(player);
			return;
		}

		data.deleteSlot(item.getObjectId(), slotId);
		AttributeAlchemyManager.getInstance().refresh(player, item);
		player.sendMessage("Socket " + slotId + " sacado de " + item.getItemName() + ".");
		showSockets(player);
	}

	// ------------------------------------------------------------------------
	// Utiles
	// ------------------------------------------------------------------------

	/**
	 * @param player : el jugador.
	 * @param objectId : objectId del item.
	 * @return el item si es del jugador y lo tiene equipado, o null.
	 */
	private static ItemInstance getEquipped(Player player, int objectId)
	{
		final ItemInstance item = player.getInventory().getItemByObjectId(objectId);
		return (item != null && item.isEquipped()) ? item : null;
	}

	private static boolean pay(Player player, int itemId, int count)
	{
		if (count <= 0)
			return true;

		final boolean paid = (itemId == 57) ? player.reduceAdena("AttributeReset", count, player, true) : player.destroyItemByItemId("AttributeReset", itemId, count, player, true);
		if (!paid)
			player.sendMessage("No te alcanza: necesitas " + priceText(itemId, count) + ".");
		return paid;
	}

	private static String priceText(int itemId, int count)
	{
		if (count <= 0)
			return "Gratis";

		final Item template = ItemTable.getInstance().getTemplate(itemId);
		return String.format("%,d", count) + " " + ((template == null) ? "item " + itemId : template.getName());
	}

	private static String itemName(ItemInstance item)
	{
		return ((item.getEnchantLevel() > 0) ? "+" + item.getEnchantLevel() + " " : "") + item.getItemName();
	}

	/**
	 * @param file : archivo dentro de data/html/mods/AttributeReset/.
	 * @return el html, o un aviso si falta el archivo.
	 */
	private static String load(String file)
	{
		final String html = HtmCache.getInstance().getHtm(HTML_PATH + file);
		return (html == null) ? "<html><body>Attribute Manager: falta " + HTML_PATH + file + "</body></html>" : html;
	}
	
	private void send(Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
		msg.setHtml(html);
		msg.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(msg);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
