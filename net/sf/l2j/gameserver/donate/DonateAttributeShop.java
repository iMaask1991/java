package net.sf.l2j.gameserver.donate;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.alchemy.AlchemyAttribute;
import net.sf.l2j.gameserver.alchemy.AlchemySlot;
import net.sf.l2j.gameserver.alchemy.AlchemyTier;
import net.sf.l2j.gameserver.alchemy.AlchemyType;
import net.sf.l2j.gameserver.alchemy.AttributeAlchemyData;
import net.sf.l2j.gameserver.alchemy.AttributeAlchemyManager;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.element.ElementType;
import net.sf.l2j.gameserver.element.ItemElement;
import net.sf.l2j.gameserver.element.ItemElementData;
import net.sf.l2j.gameserver.element.ItemElementManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Donate Shop - Socket Perfecto y Elemento Perfecto (SanSara).<br>
 * <ul>
 * <li><b>Socket Perfecto</b>: llena UN socket vacio del item equipado con el stat que elija el jugador, en el tier maximo y con el valor mas alto.
 * Nunca pisa un socket ocupado (para cambiarlo primero se saca en el Attribute Manager 60022).</li>
 * <li><b>Elemento Perfecto</b>: pone UN elemento al maximo (arma weaponMax, armadura armorMax) respetando las reglas de siempre:
 * arma 1 solo elemento, armadura hasta armorMaxElements y sin opuestos. Si el item ya tiene ese elemento por debajo del maximo, lo sube al maximo.</li>
 * </ul>
 * El precio sale de attributeAlchemy.xml y elements.xml (donateItemId / donateCount).<br>
 * Todo el diseno esta en data/html/mods/donationmanager/ (alchemy_*.htm y element_*.htm): aca solo se rellenan los datos.<br>
 * Los bypass llegan por RequestBypassToServer (dalchemy... / delement...), asi funciona desde el GM Shop y desde el Donation Manager.
 */
public final class DonateAttributeShop
{
	private static final String HTML_PATH = "data/html/mods/donationmanager/";
	
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
	
	/** Evita doble compra si el jugador hace doble click en "Comprar". */
	private static final Set<Integer> BUSY = ConcurrentHashMap.newKeySet();
	
	private DonateAttributeShop()
	{
	}
	
	/**
	 * @param command : el bypass completo.
	 * @return true si el bypass es de esta tienda.
	 */
	public static boolean isShopBypass(String command)
	{
		return command.startsWith("dalchemy") || command.startsWith("delement");
	}
	
	public static void onBypass(Player player, String command)
	{
		if (player == null)
			return;
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String action = st.nextToken();
		final int objectId;
		final int param;
		final String text;
		try
		{
			objectId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			param = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			text = st.hasMoreTokens() ? st.nextToken() : "";
		}
		catch (NumberFormatException e)
		{
			return;
		}
		
		switch (action)
		{
			case "dalchemy":
				showAlchemyMain(player);
				break;
			case "dalchemy_item":
				showAlchemyItem(player, objectId);
				break;
			case "dalchemy_slot":
				showAlchemyStats(player, objectId, param);
				break;
			case "dalchemy_ask":
				askAlchemy(player, objectId, param, text);
				break;
			case "dalchemy_buy":
				buyAlchemy(player, objectId, param, text);
				break;
			case "delement":
				showElementMain(player);
				break;
			case "delement_item":
				showElementItem(player, objectId);
				break;
			case "delement_ask":
				askElement(player, objectId, param);
				break;
			case "delement_buy":
				buyElement(player, objectId, param);
				break;
		}
	}
	
	// ========================================================================
	// Socket Perfecto
	// ========================================================================
	
	private static void showAlchemyMain(Player player)
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		final AttributeAlchemyManager manager = AttributeAlchemyManager.getInstance();
		final String rowTpl = load("alchemy_item_row.htm");
		final StringBuilder sb = new StringBuilder();
		final Set<Integer> seen = new HashSet<>();
		
		for (int part : SOCKET_SLOTS)
		{
			final ItemInstance item = player.getInventory().getPaperdollItem(part);
			if (item == null || !seen.add(item.getObjectId()))
				continue;
			
			final AlchemyType type = manager.getType(item);
			if (type == null)
				continue;
			
			sb.append(rowTpl.replace("%itemName%", itemName(item)).replace("%type%", typeName(item, type)).replace("%used%", String.valueOf(data.getSlots(item.getObjectId()).size())).replace("%max%", String.valueOf(data.getMaxSlots())).replace("%itemObjId%", String.valueOf(item.getObjectId())));
		}
		
		if (sb.length() == 0)
			sb.append(load("alchemy_empty.htm"));
		
		send(player, load("alchemy_main.htm").replace("%rows%", sb.toString()).replace("%price%", alchemyPrice()));
	}
	
	private static void showAlchemyItem(Player player, int objectId)
	{
		final ItemInstance item = getSocketItem(player, objectId);
		if (item == null)
		{
			showAlchemyMain(player);
			return;
		}
		
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		final Map<Integer, AlchemySlot> slots = data.getSlots(item.getObjectId());
		final String emptyTpl = load("alchemy_slot_row.htm");
		final String fullTpl = load("alchemy_slot_full_row.htm");
		final StringBuilder sb = new StringBuilder();
		
		for (int i = 1; i <= data.getMaxSlots(); i++)
		{
			final AlchemySlot s = slots.get(i);
			if (s == null)
			{
				sb.append(emptyTpl.replace("%slot%", String.valueOf(i)).replace("%itemObjId%", String.valueOf(item.getObjectId())));
				continue;
			}
			
			final AlchemyAttribute attr = s.getAttribute();
			sb.append(fullTpl.replace("%slot%", String.valueOf(i)).replace("%attrName%", (attr == null) ? s.getAttributeId() : attr.getName()).replace("%value%", formatValue(attr, s.getValue())).replace("%unit%", (attr == null) ? "" : attr.getUnit()).replace("%tier%", String.valueOf(s.getTier())).replace("%color%", tierColor(s.getTier())));
		}
		
		send(player, load("alchemy_item.htm").replace("%rows%", sb.toString()).replace("%itemName%", itemName(item)));
	}
	
	private static void showAlchemyStats(Player player, int objectId, int slotId)
	{
		final ItemInstance item = getSocketItem(player, objectId);
		if (item == null || !isFreeSlot(item, slotId))
		{
			showAlchemyItem(player, objectId);
			return;
		}
		
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		final AlchemyType type = AttributeAlchemyManager.getInstance().getType(item);
		final Set<String> used = usedAttributes(item);
		final String rowTpl = load("alchemy_stat_row.htm");
		final StringBuilder sb = new StringBuilder();
		
		for (AlchemyAttribute attr : data.getAttributes(type))
		{
			if (used.contains(attr.getId()))
				continue;
			
			final AlchemyTier top = topTier(attr);
			if (top == null)
				continue;
			
			sb.append(rowTpl.replace("%attrName%", attr.getName()).replace("%value%", formatValue(attr, top.getMax())).replace("%unit%", attr.getUnit()).replace("%itemObjId%", String.valueOf(item.getObjectId())).replace("%slot%", String.valueOf(slotId)).replace("%attrId%", attr.getId()));
		}
		
		send(player, load("alchemy_stats.htm").replace("%rows%", sb.toString()).replace("%itemName%", itemName(item)).replace("%slot%", String.valueOf(slotId)).replace("%itemObjId%", String.valueOf(item.getObjectId())));
	}
	
	private static void askAlchemy(Player player, int objectId, int slotId, String attrId)
	{
		final ItemInstance item = getSocketItem(player, objectId);
		final AlchemyAttribute attr = checkAttribute(item, attrId);
		if (item == null || attr == null || !isFreeSlot(item, slotId))
		{
			showAlchemyItem(player, objectId);
			return;
		}
		
		final AlchemyTier top = topTier(attr);
		String html = load("alchemy_confirm.htm");
		html = html.replace("%itemName%", itemName(item)).replace("%slot%", String.valueOf(slotId)).replace("%attrName%", attr.getName()).replace("%value%", formatValue(attr, top.getMax())).replace("%unit%", attr.getUnit()).replace("%tier%", String.valueOf(top.getLevel()));
		html = html.replace("%price%", alchemyPrice()).replace("%itemObjId%", String.valueOf(item.getObjectId())).replace("%attrId%", attr.getId());
		send(player, html);
	}
	
	private static void buyAlchemy(Player player, int objectId, int slotId, String attrId)
	{
		if (!BUSY.add(player.getObjectId()))
			return;
		
		try
		{
			final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
			final ItemInstance item = getSocketItem(player, objectId);
			final AlchemyAttribute attr = checkAttribute(item, attrId);
			if (item == null || attr == null || !isFreeSlot(item, slotId))
			{
				player.sendMessage("No se pudo: el item tiene que estar equipado y el socket vacio.");
				showAlchemyItem(player, objectId);
				return;
			}
			
			final AlchemyTier top = topTier(attr);
			if (!pay(player, data.getDonateItemId(), data.getDonateCount()))
			{
				showAlchemyItem(player, objectId);
				return;
			}
			
			final AlchemySlot slot = new AlchemySlot(slotId, attr.getId(), top.getLevel(), top.getMax());
			if (!data.saveSlot(item.getObjectId(), slot))
			{
				player.sendMessage("Error al guardar el socket, avisale a un GM.");
				return;
			}
			
			AttributeAlchemyManager.getInstance().refresh(player, item);
			if (data.getAnimationSkillId() > 0)
				player.broadcastPacket(new MagicSkillUse(player, player, data.getAnimationSkillId(), 1, 500, 0));
			player.sendMessage(itemName(item) + " - Socket " + slotId + ": " + slot.getDisplay() + " (Tier " + top.getLevel() + ")");
			
			String html = load("alchemy_ok.htm");
			html = html.replace("%itemName%", itemName(item)).replace("%slot%", String.valueOf(slotId)).replace("%attrName%", attr.getName()).replace("%value%", formatValue(attr, top.getMax())).replace("%unit%", attr.getUnit()).replace("%itemObjId%", String.valueOf(item.getObjectId()));
			send(player, html);
		}
		finally
		{
			BUSY.remove(player.getObjectId());
		}
	}
	
	/**
	 * @return el item si es del jugador, esta equipado y acepta sockets.
	 */
	private static ItemInstance getSocketItem(Player player, int objectId)
	{
		final ItemInstance item = getEquipped(player, objectId);
		return (item != null && AttributeAlchemyManager.getInstance().isEligible(item)) ? item : null;
	}
	
	private static boolean isFreeSlot(ItemInstance item, int slotId)
	{
		return slotId >= 1 && slotId <= AttributeAlchemyData.getInstance().getMaxSlots() && !AttributeAlchemyData.getInstance().getSlots(item.getObjectId()).containsKey(slotId);
	}
	
	private static Set<String> usedAttributes(ItemInstance item)
	{
		final Set<String> used = new HashSet<>();
		for (AlchemySlot s : AttributeAlchemyData.getInstance().getSlots(item.getObjectId()).values())
			used.add(s.getAttributeId().toUpperCase());
		return used;
	}
	
	/**
	 * @return el atributo si existe, es del tipo del item y el item todavia no lo tiene. Si no, null.
	 */
	private static AlchemyAttribute checkAttribute(ItemInstance item, String attrId)
	{
		if (item == null)
			return null;
		
		final AlchemyAttribute attr = AttributeAlchemyData.getInstance().getAttribute(attrId);
		if (attr == null || attr.getType() != AttributeAlchemyManager.getInstance().getType(item) || topTier(attr) == null)
			return null;
		
		return usedAttributes(item).contains(attr.getId()) ? null : attr;
	}
	
	private static AlchemyTier topTier(AlchemyAttribute attr)
	{
		return attr.getTier(attr.getMaxTierLevel());
	}
	
	private static String typeName(ItemInstance item, AlchemyType type)
	{
		switch (type)
		{
			case WEAPON:
				return "Arma";
			case JEWEL:
				return "Joya";
			default:
				return (item.getItem().getBodyPart() == Item.SLOT_L_HAND) ? "Escudo" : "Armadura";
		}
	}
	
	private static String formatValue(AlchemyAttribute attr, double value)
	{
		// Los FLAT sin unidad (Speed) se muestran sin decimales
		if (attr != null && attr.getUnit().isEmpty() && value == Math.floor(value))
			return String.valueOf((int) value);
		return String.format(Locale.US, "%.1f", value);
	}
	
	private static String tierColor(int tier)
	{
		switch (tier)
		{
			case 5:
				return "FF9900";
			case 4:
				return "B080FF";
			case 3:
				return "60A0FF";
			case 2:
				return "80FF80";
			default:
				return "C8C8C8";
		}
	}
	
	private static String alchemyPrice()
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		return priceText(data.getDonateItemId(), data.getDonateCount());
	}
	
	// ========================================================================
	// Elemento Perfecto
	// ========================================================================
	
	private static void showElementMain(Player player)
	{
		final ItemElementData data = ItemElementData.getInstance();
		final ItemElementManager manager = ItemElementManager.getInstance();
		final String rowTpl = load("element_item_row.htm");
		final StringBuilder sb = new StringBuilder();
		
		for (int part : ELEMENT_SLOTS)
		{
			final ItemInstance item = player.getInventory().getPaperdollItem(part);
			if (item == null || (!manager.isWeapon(item) && !manager.isArmor(item)))
				continue;
			
			sb.append(rowTpl.replace("%itemName%", itemName(item)).replace("%elements%", elementList(data.getAll(item.getObjectId()))).replace("%itemObjId%", String.valueOf(item.getObjectId())));
		}
		
		if (sb.length() == 0)
			sb.append(load("element_empty.htm"));
		
		String html = load("element_main.htm").replace("%rows%", sb.toString()).replace("%price%", elementPrice());
		html = html.replace("%weaponMax%", String.valueOf(data.getWeaponMax())).replace("%armorMax%", String.valueOf(data.getArmorMax())).replace("%armorMaxElements%", String.valueOf(data.getArmorMaxElements()));
		send(player, html);
	}
	
	private static void showElementItem(Player player, int objectId)
	{
		final ItemInstance item = getElementItem(player, objectId);
		if (item == null)
		{
			showElementMain(player);
			return;
		}
		
		final ItemElementData data = ItemElementData.getInstance();
		final boolean weapon = ItemElementManager.getInstance().isWeapon(item);
		final int max = weapon ? data.getWeaponMax() : data.getArmorMax();
		final String rowTpl = load("element_row.htm");
		final String blockedTpl = load("element_row_blocked.htm");
		final StringBuilder sb = new StringBuilder();
		
		for (ElementType e : ElementType.values())
		{
			final String reason = blockReason(item, e);
			if (reason != null)
				sb.append(blockedTpl.replace("%element%", e.getName()).replace("%reason%", reason));
			else
				sb.append(rowTpl.replace("%element%", e.getName()).replace("%color%", elementColor(e)).replace("%max%", String.valueOf(max)).replace("%itemObjId%", String.valueOf(item.getObjectId())).replace("%elemId%", String.valueOf(e.getId())));
		}
		
		send(player, load("element_item.htm").replace("%rows%", sb.toString()).replace("%itemName%", itemName(item)).replace("%elements%", elementList(data.getAll(item.getObjectId()))).replace("%itemObjId%", String.valueOf(item.getObjectId())));
	}
	
	private static void askElement(Player player, int objectId, int elementId)
	{
		final ItemInstance item = getElementItem(player, objectId);
		final ElementType element = ElementType.getById(elementId);
		if (item == null || element == null || blockReason(item, element) != null)
		{
			showElementItem(player, objectId);
			return;
		}
		
		final ItemElementData data = ItemElementData.getInstance();
		final int max = ItemElementManager.getInstance().isWeapon(item) ? data.getWeaponMax() : data.getArmorMax();
		String html = load("element_confirm.htm");
		html = html.replace("%itemName%", itemName(item)).replace("%element%", element.getName()).replace("%color%", elementColor(element)).replace("%max%", String.valueOf(max));
		html = html.replace("%price%", elementPrice()).replace("%itemObjId%", String.valueOf(item.getObjectId())).replace("%elemId%", String.valueOf(element.getId()));
		send(player, html);
	}
	
	private static void buyElement(Player player, int objectId, int elementId)
	{
		if (!BUSY.add(player.getObjectId()))
			return;
		
		try
		{
			final ItemElementData data = ItemElementData.getInstance();
			final ItemInstance item = getElementItem(player, objectId);
			final ElementType element = ElementType.getById(elementId);
			final String reason = (item == null || element == null) ? "el item tiene que estar equipado" : blockReason(item, element);
			if (reason != null)
			{
				player.sendMessage("No se pudo: " + reason + ".");
				showElementItem(player, objectId);
				return;
			}
			
			final boolean weapon = ItemElementManager.getInstance().isWeapon(item);
			final int max = weapon ? data.getWeaponMax() : data.getArmorMax();
			if (!pay(player, data.getDonateItemId(), data.getDonateCount()))
			{
				showElementItem(player, objectId);
				return;
			}
			
			if (!data.save(item.getObjectId(), new ItemElement(element, max), weapon))
			{
				player.sendMessage("Error al guardar el elemento, avisale a un GM.");
				return;
			}
			
			ItemElementManager.getInstance().sendItem(player, item);
			if (data.getAnimationSkillId() > 0)
				player.broadcastPacket(new MagicSkillUse(player, player, data.getAnimationSkillId(), 1, 500, 0));
			player.sendMessage(itemName(item) + ": " + ItemElementManager.getDescription(weapon, element, max) + " (maximo).");
			
			String html = load("element_ok.htm");
			html = html.replace("%itemName%", itemName(item)).replace("%element%", element.getName()).replace("%color%", elementColor(element)).replace("%max%", String.valueOf(max)).replace("%itemObjId%", String.valueOf(item.getObjectId()));
			send(player, html);
		}
		finally
		{
			BUSY.remove(player.getObjectId());
		}
	}
	
	/**
	 * @return el item si es del jugador, esta equipado y acepta elementos.
	 */
	private static ItemInstance getElementItem(Player player, int objectId)
	{
		final ItemInstance item = getEquipped(player, objectId);
		if (item == null)
			return null;
		
		final ItemElementManager manager = ItemElementManager.getInstance();
		return (manager.isWeapon(item) || manager.isArmor(item)) ? item : null;
	}
	
	/**
	 * Mismas reglas que la piedra: arma 1 solo elemento, armadura hasta armorMaxElements y sin opuestos.
	 * @return por que no se puede poner ese elemento, o null si se puede.
	 */
	private static String blockReason(ItemInstance item, ElementType element)
	{
		final ItemElementData data = ItemElementData.getInstance();
		final boolean weapon = ItemElementManager.getInstance().isWeapon(item);
		final int max = weapon ? data.getWeaponMax() : data.getArmorMax();
		final List<ItemElement> list = data.getAll(item.getObjectId());
		
		for (ItemElement e : list)
		{
			if (e.getElement() == element)
				return (e.getValue() >= max) ? "ya esta al maximo" : null;
		}
		
		if (weapon)
			return list.isEmpty() ? null : "el arma ya tiene " + list.get(0).getElement().getName();
		
		for (ItemElement e : list)
		{
			if (e.getElement() == element.getOpposite())
				return "opuesto a " + e.getElement().getName();
		}
		
		if (list.size() >= data.getArmorMaxElements())
			return "ya tiene " + list.size() + " elementos";
		
		return null;
	}
	
	private static String elementList(List<ItemElement> list)
	{
		if (list.isEmpty())
			return "Sin elementos";
		
		final StringBuilder sb = new StringBuilder();
		for (ItemElement e : list)
		{
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(e.getElement().getName()).append(' ').append(e.getValue());
		}
		return sb.toString();
	}
	
	private static String elementColor(ElementType element)
	{
		switch (element)
		{
			case FIRE:
				return "FF6040";
			case WATER:
				return "60A0FF";
			case WIND:
				return "B0FFB0";
			case EARTH:
				return "C8A060";
			case HOLY:
				return "FFFFC0";
			default:
				return "B080FF";
		}
	}
	
	private static String elementPrice()
	{
		final ItemElementData data = ItemElementData.getInstance();
		return priceText(data.getDonateItemId(), data.getDonateCount());
	}
	
	// ========================================================================
	// Utiles
	// ========================================================================
	
	/**
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
		
		final boolean paid = (itemId == 57) ? player.reduceAdena("DonateAttribute", count, player, true) : player.destroyItemByItemId("DonateAttribute", itemId, count, player, true);
		if (!paid)
		{
			player.sendMessage("No te alcanza: necesitas " + priceText(itemId, count) + ".");
			send(player, load("noitems.htm"));
		}
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
	
	private static String load(String file)
	{
		final String html = HtmCache.getInstance().getHtm(HTML_PATH + file);
		return (html == null) ? "<html><body>Donate Shop: falta " + HTML_PATH + file + "</body></html>" : html;
	}
	
	private static void send(Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(1);
		msg.setHtml(html);
		player.sendPacket(msg);
	}
}
