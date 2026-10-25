package net.sf.l2j.gameserver.element;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInfo;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance.ItemState;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.ExCharAttribute;
import net.sf.l2j.gameserver.network.serverpackets.ExItemAttribute;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

/**
 * Elementos de items (estilo High Five):
 * <ul>
 * <li>Piedra (doble click) -> ventana de enchant -> elegis un arma o armadura de grado S.</li>
 * <li>Arma: ataque del elemento de la piedra. Armadura: defensa contra el elemento opuesto.</li>
 * <li>Cada exito sube el valor (primero un salto mas grande, despues de a poco). Si falla, se pierde la piedra y el elemento queda igual.</li>
 * <li>Un solo elemento por item: si ya tiene otro, la piedra se rechaza.</li>
 * <li>Dano: formula H5 en ElementAttributes (ataque en puntos contra defensa en puntos).</li>
 * </ul>
 */
public class ItemElementManager
{
	private static final CLogger LOGGER = new CLogger(ItemElementManager.class.getName());
	
	/** Partes de armadura que aceptan elemento: casco, pechera, pantalon, guantes, botas y armadura entera. */
	private static final int ARMOR_SLOTS = Item.SLOT_HEAD | Item.SLOT_CHEST | Item.SLOT_LEGS | Item.SLOT_GLOVES | Item.SLOT_FEET | Item.SLOT_FULL_ARMOR;
	
	/** Piezas que suman defensa elemental. */
	private static final int[] ARMOR_PAPERDOLL =
	{
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET
	};
	
	/** Jugadores usando una piedra en este momento (anti doble click). */
	private final Set<Integer> _busy = ConcurrentHashMap.newKeySet();
	
	/** Jugadores con la ventana de atributos pendiente de mandar (se agrupan los cambios). */
	private final Set<Integer> _statusPending = ConcurrentHashMap.newKeySet();
	
	protected ItemElementManager()
	{
	}
	
	// ------------------------------------------------------------------------
	// Chequeos
	// ------------------------------------------------------------------------
	
	public boolean isStone(ItemInstance item)
	{
		return item != null && ItemElementData.getInstance().isStone(item.getItemId());
	}
	
	/**
	 * @param item : el item.
	 * @return true si es arma que puede tener elemento.
	 */
	public boolean isWeapon(ItemInstance item)
	{
		return isGradeOk(item) && item.isWeapon();
	}
	
	/**
	 * @param item : el item.
	 * @return true si es pieza de armadura que puede tener elemento.
	 */
	public boolean isArmor(ItemInstance item)
	{
		return isGradeOk(item) && item.getItem().getType2() == Item.TYPE2_SHIELD_ARMOR && (item.getItem().getBodyPart() & ARMOR_SLOTS) != 0;
	}
	
	private static boolean isGradeOk(ItemInstance item)
	{
		return item != null && !item.isShadowItem() && !item.getItem().getCrystalType().isLesser(ItemElementData.getInstance().getMinGrade());
	}
	
	// ------------------------------------------------------------------------
	// Uso de la piedra (llamado desde RequestEnchantItem)
	// ------------------------------------------------------------------------
	
	/**
	 * El jugador eligio un item en la ventana de enchant teniendo la piedra activa.
	 * @param player : el jugador.
	 * @param stone : la piedra.
	 * @param target : el item elegido.
	 */
	public void useStone(Player player, ItemInstance stone, ItemInstance target)
	{
		final ItemElementData data = ItemElementData.getInstance();
		
		if (!data.isEnabled())
		{
			player.sendMessage("El sistema de elementos esta desactivado.");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		final ItemElementData.Stone info = data.getStone(stone.getItemId());
		final ElementType element = (info == null) ? null : info.element;
		if (element == null)
		{
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		final boolean weapon = isWeapon(target);
		if (!weapon && !isArmor(target))
		{
			player.sendMessage("Los elementos solo se aplican en armas y armaduras (casco, pechera, pantalon, guantes, botas) de grado " + data.getMinGrade() + ".");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		// Arma: un solo elemento. Armadura: hasta 3 elementos (como H5), nunca dos opuestos (Fire/Water, Wind/Earth, Holy/Dark).
		final ItemElement current = weapon ? data.get(target.getObjectId()) : data.get(target.getObjectId(), element);
		if (weapon)
		{
			if (current != null && current.getElement() != element)
			{
				player.sendMessage("Esta arma ya tiene el elemento " + current.getElement().getName() + ". Tenes que sacarlo antes de poner otro.");
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
		}
		else if (current == null)
		{
			final List<ItemElement> list = data.getAll(target.getObjectId());
			for (ItemElement e : list)
			{
				if (e.getElement() == element.getOpposite())
				{
					player.sendMessage(target.getItemName() + " tiene piedra de " + e.getElement().getName() + ": no se puede juntar con " + element.getName() + " (son opuestos).");
					player.sendPacket(EnchantResult.CANCELLED);
					return;
				}
			}
			if (list.size() >= data.getArmorMaxElements())
			{
				player.sendMessage(target.getItemName() + " ya tiene " + list.size() + " elementos, que es el maximo.");
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
		}
		
		final int max = weapon ? info.maxWeapon : info.maxArmor;
		final int min = weapon ? info.minWeapon : info.minArmor;
		final int value = (current == null) ? 0 : current.getValue();
		if (value < min)
		{
			player.sendMessage("Para usar " + stone.getItemName() + " el item tiene que tener " + element.getName() + " " + min + " o mas (ahora tiene " + value + ").");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		if (value >= max)
		{
			final int top = weapon ? data.getWeaponMax() : data.getArmorMax();
			player.sendMessage((max >= top) ? "Este item ya tiene el elemento " + element.getName() + " al maximo (" + max + ")." : stone.getItemName() + " sube hasta " + max + ". Para seguir subiendo usa la siguiente piedra.");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		// La piedra tiene que seguir en el inventario
		final ItemInstance stoneInInv = player.getInventory().getItemByObjectId(stone.getObjectId());
		if (stoneInInv == null || stoneInInv.getCount() < 1)
		{
			player.sendMessage("No tenes la piedra.");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		if (!_busy.add(player.getObjectId()))
		{
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		try
		{
			if (!player.destroyItem("ItemElement", stoneInInv.getObjectId(), 1, player, true))
			{
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			if (Rnd.get(100) >= info.chance)
			{
				player.sendPacket(EnchantResult.UNSUCCESS);
				player.sendMessage("La piedra fallo. " + target.getItemName() + " sigue con " + ((value == 0) ? "ningun elemento" : getDescription(weapon, element, value)) + ".");
				return;
			}
			
			final int add = (value == 0) ? (weapon ? data.getWeaponFirst() : data.getArmorFirst()) : (weapon ? data.getWeaponStep() : data.getArmorStep());
			final int newValue = info.full ? max : Math.min(max, value + add);
			
			if (!data.save(target.getObjectId(), new ItemElement(element, newValue), weapon))
			{
				player.sendMessage("Error al guardar el elemento, avisale a un GM.");
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			sendItem(player, target);
			player.sendPacket(EnchantResult.SUCCESS);
			
			if (data.getAnimationSkillId() > 0)
				player.broadcastPacket(new MagicSkillUse(player, player, data.getAnimationSkillId(), 1, 500, 0));
			
			player.sendMessage("Exito! " + target.getItemName() + ": " + getDescription(weapon, element, newValue) + ((newValue >= max) ? " (maximo)." : "."));
		}
		catch (Exception e)
		{
			LOGGER.error("ItemElements: error usando la piedra ({}).", e, player.getName());
			player.sendPacket(EnchantResult.CANCELLED);
		}
		finally
		{
			_busy.remove(player.getObjectId());
		}
	}
	
	/**
	 * @param weapon : true si es arma.
	 * @param element : elemento de la piedra.
	 * @param value : valor.
	 * @return el texto, ej "Fire Attack 150" o "Water Defense 60".
	 */
	public static String getDescription(boolean weapon, ElementType element, int value)
	{
		return weapon ? element.getName() + " Attack " + value : element.getOpposite().getName() + " Defense " + value;
	}
	
	// ------------------------------------------------------------------------
	// Dano
	// ------------------------------------------------------------------------
	
	/**
	 * @param player : el jugador.
	 * @return el elemento del arma equipada, o null.
	 */
	public ItemElement getAttackElement(Player player)
	{
		final ItemInstance weapon = player.getActiveWeaponInstance();
		return (weapon == null) ? null : ItemElementData.getInstance().get(weapon.getObjectId());
	}
	
	/**
	 * @param player : el jugador.
	 * @param attackElement : el elemento con el que lo atacan.
	 * @return la defensa total contra ese elemento en PUNTOS (piedras de armadura + buffs/pasivas), como H5.
	 */
	public int getDefense(Player player, ElementType attackElement)
	{
		return (int) player.getDefenseElementValue(ElementAttributes.toServer(attackElement));
	}
	
	/**
	 * @param player : el jugador.
	 * @param attackElement : el elemento con el que lo atacan.
	 * @return solo la defensa de las piedras de la armadura equipada contra ese elemento.
	 */
	public int getArmorDefense(Player player, ElementType attackElement)
	{
		if (attackElement == null)
			return 0;
		
		final ItemElementData data = ItemElementData.getInstance();
		final ElementType stone = attackElement.getOpposite(); // la piedra que da defensa contra este elemento
		
		int total = 0;
		for (int slot : ARMOR_PAPERDOLL)
		{
			final ItemInstance item = player.getInventory().getPaperdollItem(slot);
			if (item == null)
				continue;
			
			final ItemElement e = data.get(item.getObjectId(), stone);
			if (e != null)
				total += e.getValue();
		}
		return total;
	}
	
	// ------------------------------------------------------------------------
	// Cliente
	// ------------------------------------------------------------------------
	
	/**
	 * Manda el elemento de un item a la interface (si no tiene, manda "sin elemento").
	 * @param player : el duenio.
	 * @param item : el item.
	 */
	public void sendItem(Player player, ItemInstance item)
	{
		writeElements(player, item.getObjectId(), ItemElementData.getInstance().getAll(item.getObjectId()));
		
		// Si el item esta equipado, cambia el ataque/defensa elemental: se actualiza la ventana del ALT+T
		if (item.isEquipped())
			scheduleStatus(player);
	}
	
	/**
	 * Manda los elementos de un item a la interface: primero "sacar todo" (elemento 0) y despues uno por cada elemento.
	 * @param player : el duenio.
	 * @param objectId : objectId del item.
	 * @param list : los elementos del item (puede estar vacia).
	 */
	private static void writeElements(Player player, int objectId, List<ItemElement> list)
	{
		player.sendPacket(new ExItemAttribute(objectId, 0, 0));
		for (ItemElement e : list)
			player.sendPacket(new ExItemAttribute(objectId, e.getElement().getId(), e.getValue()));
	}
	
	/**
	 * Manda la ventana de atributos del ALT+T, una sola vez cada 500ms aunque se pida muchas veces seguidas.
	 * @param player : el jugador.
	 */
	public void scheduleStatus(Player player)
	{
		if (_statusPending.add(player.getObjectId()))
		{
			ThreadPool.schedule(() ->
			{
				_statusPending.remove(player.getObjectId());
				if (player.isOnline())
					sendStatus(player);
			}, 500);
		}
	}
	
	/**
	 * Se llama desde GameClient despues de mandar ItemList / InventoryUpdate: manda los elementos de esos items.
	 * @param client : el cliente.
	 * @param packet : el paquete que se acaba de mandar.
	 */
	public void onPacketSent(GameClient client, L2GameServerPacket packet)
	{
		final ItemElementData data = ItemElementData.getInstance();
		final Player player = client.getPlayer();
		if (player == null)
			return;
		
		if (packet instanceof ItemList)
		{
			sendAll(player);
			
			// Al entrar al juego la interface todavia puede no estar lista: se mandan de nuevo un rato despues
			ThreadPool.schedule(() ->
			{
				if (player.isOnline())
				{
					sendAll(player);
					sendStatus(player);
				}
			}, 3000);
		}
		else if (packet instanceof UserInfo)
		{
			// Cambiaron los stats: se manda la ventana de atributos (una sola vez cada 500ms aunque lleguen muchos UserInfo)
			scheduleStatus(player);
		}
		else if (packet instanceof InventoryUpdate)
		{
			for (ItemInfo info : ((InventoryUpdate) packet).getItems())
			{
				if (info.getChange() != ItemState.ADDED && info.getChange() != ItemState.MODIFIED)
					continue;
				
				final List<ItemElement> list = data.getAll(info.getObjectId());
				if (!list.isEmpty())
					writeElements(player, info.getObjectId(), list);
			}
		}
	}
	
	/**
	 * Manda los datos de la ventana de atributos del ALT+T (AttributeStatusWnd). Los porcentajes van x10 (1 decimal).
	 * <ul>
	 * <li>1 : ataque elemental (elemento, valor)</li>
	 * <li>2-7 : defensa Fire, Water, Wind, Earth, Holy, Dark</li>
	 * <li>10-16 : Phys Crit Damage, Magic Crit Damage, PvP Phys Damage, PvP Phys Skill Damage, PvP Magic Damage, PvE Phys Damage, PvE Magic Damage</li>
	 * <li>20-24 : PvP Phys Defense, PvP Phys Skill Defense, PvP Magic Defense, Phys Crit Damage Resist, Magic Crit Damage Resist</li>
	 * </ul>
	 * @param player : el jugador.
	 */
	public void sendStatus(Player player)
	{
		// Ataque: elemento del arma (o el de los buffs si el arma no tiene) con sus puntos totales
		final byte attackElement = player.getAttackElement();
		final ElementType attackType = ElementAttributes.toItem(attackElement);
		player.sendPacket(new ExCharAttribute(1, (attackType == null) ? 0 : attackType.getId(), (attackType == null) ? 0 : player.getAttackElementValue(attackElement)));
		
		int code = 2;
		for (ElementType e : ElementType.values())
			player.sendPacket(new ExCharAttribute(code++, getDefense(player, e), 0));
		
		// Offense (porcentajes x10)
		final double critBase = net.sf.l2j.Config.PCRIT_POWER;
		final double mcritBase = net.sf.l2j.Config.ENABLE_CUSTOM_CRIT_POWER ? net.sf.l2j.Config.MAGIC_CRITICAL_POWER : net.sf.l2j.Config.MCRIT_POWER;
		player.sendPacket(new ExCharAttribute(10, tenth((player.calcStat(Stats.CRITICAL_DAMAGE, critBase, null, null) / critBase - 1) * 100), 0));
		player.sendPacket(new ExCharAttribute(11, tenth((player.calcStat(Stats.MCRITICAL_DAMAGE, mcritBase, null, null) / mcritBase - 1) * 100), 0));
		player.sendPacket(new ExCharAttribute(12, bonus(player, Stats.PVP_PHYSICAL_DMG), 0));
		player.sendPacket(new ExCharAttribute(13, bonus(player, Stats.PVP_PHYS_SKILL_DMG), 0));
		player.sendPacket(new ExCharAttribute(14, bonus(player, Stats.PVP_MAGICAL_DMG), 0));
		player.sendPacket(new ExCharAttribute(15, tenth((player.calcStat(Stats.PATK_MONSTERS, 1, null, null) * player.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null) - 1) * 100), 0));
		player.sendPacket(new ExCharAttribute(16, bonus(player, Stats.PVE_MAGICAL_DMG), 0));
		
		// Defense (porcentajes x10)
		player.sendPacket(new ExCharAttribute(20, bonus(player, Stats.PVP_PHYSICAL_DEF), 0));
		player.sendPacket(new ExCharAttribute(21, bonus(player, Stats.PVP_PHYS_SKILL_DEF), 0));
		player.sendPacket(new ExCharAttribute(22, bonus(player, Stats.PVP_MAGICAL_DEF), 0));
		player.sendPacket(new ExCharAttribute(23, resist(player, Stats.CRIT_VULN), 0));
		player.sendPacket(new ExCharAttribute(24, resist(player, Stats.MCRITICAL_VULN), 0));
	}
	
	/** Bonus de un stat que multiplica (base 1): 1.125 -> +12.5% (x10 = 125). */
	private static int bonus(Player player, Stats stat)
	{
		return tenth((player.calcStat(stat, 1, null, null) - 1) * 100);
	}
	
	/** Resistencia de un stat de vulnerabilidad (base 1): 0.85 -> 15.0% (x10 = 150). */
	private static int resist(Player player, Stats stat)
	{
		return tenth((1 - player.calcStat(stat, 1, null, null)) * 100);
	}
	
	private static int tenth(double percent)
	{
		return (int) Math.round(percent * 10);
	}
	
	/**
	 * Manda a la interface los elementos de todos los items del inventario que tengan.
	 * @param player : el jugador.
	 */
	public void sendAll(Player player)
	{
		final ItemElementData data = ItemElementData.getInstance();
		for (ItemInstance item : player.getInventory().getItems())
		{
			final List<ItemElement> list = data.getAll(item.getObjectId());
			if (!list.isEmpty())
				writeElements(player, item.getObjectId(), list);
		}
	}
	
	/**
	 * El item se borro del juego: se borra su elemento.
	 * @param objectId : objectId del item.
	 */
	public void onItemDeleted(int objectId)
	{
		ItemElementData.getInstance().delete(objectId);
	}
	
	public static ItemElementManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemElementManager INSTANCE = new ItemElementManager();
	}
}
