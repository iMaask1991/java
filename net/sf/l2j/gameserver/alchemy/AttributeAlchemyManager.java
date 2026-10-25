package net.sf.l2j.gameserver.alchemy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.basefuncs.FuncAdd;
import net.sf.l2j.gameserver.skills.basefuncs.FuncMul;
import net.sf.l2j.gameserver.skills.basefuncs.LambdaConst;

/**
 * Attribute Alchemy (Sockets) - logica del orbe, stats del item y datos para el cliente.
 * <ul>
 * <li>Si hay un socket vacio, el orbe llena ese (primero el 1, despues el 2 y despues el 3).</li>
 * <li>Si estan todos llenos, re-rollea el de tier mas bajo. Un socket en tier maximo no se toca nunca.</li>
 * <li>Si todos estan en tier maximo, no se gasta el orbe.</li>
 * <li>Un item nunca tiene el mismo atributo en dos sockets.</li>
 * </ul>
 */
public class AttributeAlchemyManager
{
	private static final CLogger LOGGER = new CLogger(AttributeAlchemyManager.class.getName());
	
	/** Orden de calculo de los stats de los sockets (despues de los stats base y del enchant del item). */
	private static final int FUNC_ORDER = 0x40;
	
	/** Partes de armadura que aceptan Guardian Orb: casco, pechera, pantalon, guantes, botas, armadura entera y escudo. */
	private static final int ARMOR_SLOTS = Item.SLOT_HEAD | Item.SLOT_CHEST | Item.SLOT_LEGS | Item.SLOT_GLOVES | Item.SLOT_FEET | Item.SLOT_FULL_ARMOR | Item.SLOT_L_HAND;
	
	/** Joyas que aceptan Fortune Orb: aros, collar y anillos. */
	private static final int JEWEL_SLOTS = Item.SLOT_LR_EAR | Item.SLOT_NECK | Item.SLOT_LR_FINGER;
	
	/** Jugadores que estan usando un orbe en este momento (anti doble click). */
	private final Set<Integer> _busy = ConcurrentHashMap.newKeySet();
	
	protected AttributeAlchemyManager()
	{
	}
	
	// ------------------------------------------------------------------------
	// Chequeos
	// ------------------------------------------------------------------------
	
	/**
	 * @param item : el item usado (scroll / orbe).
	 * @return true si el item es el orbe de alquimia.
	 */
	public boolean isOrb(ItemInstance item)
	{
		return item != null && AttributeAlchemyData.getInstance().isOrbItem(item.getItemId());
	}
	
	/**
	 * Items de grado S (o el grado minimo del XML), que no sean shadow:
	 * <ul>
	 * <li>armas -> WEAPON</li>
	 * <li>casco, pechera, pantalon, guantes, botas y escudo -> ARMOR</li>
	 * <li>aros, collar y anillos -> JEWEL</li>
	 * </ul>
	 * @param item : el item.
	 * @return el tipo de sockets del item, o null si no puede tener sockets.
	 */
	public AlchemyType getType(ItemInstance item)
	{
		if (item == null || item.isShadowItem())
			return null;
		
		final Item template = item.getItem();
		if (template.getCrystalType().isLesser(AttributeAlchemyData.getInstance().getMinGrade()))
			return null;
		
		if (item.isWeapon())
			return AlchemyType.WEAPON;
		
		final int bodyPart = template.getBodyPart();
		if (template.getType2() == Item.TYPE2_SHIELD_ARMOR && (bodyPart & ARMOR_SLOTS) != 0)
			return AlchemyType.ARMOR;
		
		if (template.getType2() == Item.TYPE2_ACCESSORY && (bodyPart & JEWEL_SLOTS) != 0)
			return AlchemyType.JEWEL;
		
		return null;
	}
	
	/**
	 * @param item : el item.
	 * @return true si el item puede tener sockets.
	 */
	public boolean isEligible(ItemInstance item)
	{
		return getType(item) != null;
	}
	
	// ------------------------------------------------------------------------
	// Uso del orbe (llamado desde RequestEnchantItem)
	// ------------------------------------------------------------------------
	
	/**
	 * El jugador eligio un item en la ventana de enchant teniendo el orbe activo.
	 * @param player : el jugador.
	 * @param orb : el orbe (item activo de la ventana de enchant).
	 * @param target : el item elegido.
	 */
	public void useOrb(Player player, ItemInstance orb, ItemInstance target)
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		
		if (!data.isEnabled())
		{
			player.sendMessage("El sistema de Sockets esta desactivado.");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		final AlchemyType type = getType(target);
		if (type == null)
		{
			player.sendMessage("Los sockets solo se aplican en armas, armaduras y joyas de grado " + data.getMinGrade() + ".");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		if (orb.getItemId() != data.getOrbItemId(type))
		{
			player.sendMessage("Este orbe no sirve para " + type.getDesc() + ".");
			player.sendPacket(EnchantResult.CANCELLED);
			return;
		}
		
		// El orbe tiene que seguir en el inventario y alcanzar
		final ItemInstance orbInInv = player.getInventory().getItemByObjectId(orb.getObjectId());
		if (orbInInv == null || orbInInv.getCount() < data.getOrbCount())
		{
			player.sendMessage("No tenes suficientes orbes.");
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
			final Map<Integer, AlchemySlot> slots = data.getSlots(target.getObjectId());
			
			// 1. Elegir el socket
			final int slotId = pickSlot(slots);
			if (slotId == 0)
			{
				player.sendMessage("Todos los sockets de este item ya estan en el tier maximo.");
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			// 2. Sortear atributo, tier y valor
			final AlchemyAttribute attr = rollAttribute(type, slots, slotId);
			if (attr == null)
			{
				player.sendMessage("No hay atributos configurados para el orbe.");
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			final AlchemyTier tier = rollTier(attr);
			final double value = rollValue(tier);
			
			// 3. Gastar el orbe
			if (!player.destroyItem("AttributeAlchemy", orbInInv.getObjectId(), data.getOrbCount(), player, true))
			{
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			// 4. Guardar
			final AlchemySlot result = new AlchemySlot(slotId, attr.getId(), tier.getLevel(), value);
			if (!data.saveSlot(target.getObjectId(), result))
			{
				player.sendMessage("Error al guardar el socket, avisale a un GM.");
				player.sendPacket(EnchantResult.CANCELLED);
				return;
			}
			
			// 5. Refrescar item, stats y cliente
			refresh(player, target);
			player.sendPacket(EnchantResult.SUCCESS);
			
			if (data.getAnimationSkillId() > 0)
				player.broadcastPacket(new MagicSkillUse(player, player, data.getAnimationSkillId(), 1, 500, 0));
			
			player.sendMessage("Socket " + slotId + ": " + result.getDisplay() + " (Tier " + tier.getLevel() + ")");
		}
		catch (Exception e)
		{
			LOGGER.error("AttributeAlchemy: error usando el orbe ({}).", e, player.getName());
			player.sendPacket(EnchantResult.CANCELLED);
		}
		finally
		{
			_busy.remove(player.getObjectId());
		}
	}
	
	/**
	 * @param slots : los sockets actuales del item.
	 * @return el socket a sortear (1..max), o 0 si estan todos en tier maximo.
	 */
	private static int pickSlot(Map<Integer, AlchemySlot> slots)
	{
		final int max = AttributeAlchemyData.getInstance().getMaxSlots();
		
		// Primer socket vacio
		for (int i = 1; i <= max; i++)
			if (!slots.containsKey(i))
				return i;
		
		// Todos llenos: el de tier mas bajo que no este en el maximo (si empatan, uno al azar)
		int lowest = Integer.MAX_VALUE;
		final List<Integer> candidates = new ArrayList<>();
		for (int i = 1; i <= max; i++)
		{
			final AlchemySlot slot = slots.get(i);
			final AlchemyAttribute attr = slot.getAttribute();
			if (attr != null && slot.getTier() >= attr.getMaxTierLevel())
				continue;
			
			if (slot.getTier() < lowest)
			{
				lowest = slot.getTier();
				candidates.clear();
			}
			if (slot.getTier() == lowest)
				candidates.add(i);
		}
		
		return candidates.isEmpty() ? 0 : Rnd.get(candidates);
	}
	
	/**
	 * Sortea un atributo por peso, sin repetir los que ya estan en los otros sockets.
	 * @param slots : los sockets actuales.
	 * @param slotId : el socket que se va a sortear.
	 * @return el atributo, o null si no hay.
	 */
	private static AlchemyAttribute rollAttribute(AlchemyType type, Map<Integer, AlchemySlot> slots, int slotId)
	{
		final List<AlchemyAttribute> pool = AttributeAlchemyData.getInstance().getAttributes(type);
		for (AlchemySlot other : slots.values())
			if (other.getSlot() != slotId)
				pool.removeIf(a -> a.getId().equalsIgnoreCase(other.getAttributeId()));
		
		if (pool.isEmpty())
			return null;
		
		double total = 0;
		for (AlchemyAttribute a : pool)
			total += a.getWeight();
		
		double roll = Rnd.nextDouble() * total;
		for (AlchemyAttribute a : pool)
		{
			roll -= a.getWeight();
			if (roll <= 0)
				return a;
		}
		return pool.get(pool.size() - 1);
	}
	
	private static AlchemyTier rollTier(AlchemyAttribute attr)
	{
		double total = 0;
		for (AlchemyTier t : attr.getTiers())
			total += t.getChance();
		
		double roll = Rnd.nextDouble() * total;
		for (AlchemyTier t : attr.getTiers())
		{
			roll -= t.getChance();
			if (roll <= 0)
				return t;
		}
		return attr.getTiers().get(0);
	}
	
	/**
	 * Valor al azar dentro del rango del tier, con 1 decimal (asi el cliente lo muestra exacto).
	 * @param tier : el tier.
	 * @return el valor.
	 */
	private static double rollValue(AlchemyTier tier)
	{
		if (tier.getMin() >= tier.getMax())
			return tier.getMin();
		
		final double value = tier.getMin() + Rnd.nextDouble() * (tier.getMax() - tier.getMin());
		return new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}
	
	// ------------------------------------------------------------------------
	// Stats y refresco
	// ------------------------------------------------------------------------
	
	/**
	 * Funcs de los sockets del item. Se suman en ItemInstance.getStatFuncs().
	 * @param item : el item.
	 * @return la lista de funcs (vacia si no tiene sockets).
	 */
	public List<Func> getStatFuncs(ItemInstance item)
	{
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		if (!data.isEnabled() || !isEligible(item))
			return Collections.emptyList();
		
		final Map<Integer, AlchemySlot> slots = data.getSlots(item.getObjectId());
		if (slots.isEmpty())
			return Collections.emptyList();
		
		final List<Func> funcs = new ArrayList<>(slots.size());
		for (AlchemySlot slot : slots.values())
		{
			final AlchemyAttribute attr = slot.getAttribute();
			if (attr == null)
				continue;
			
			switch (attr.getMode())
			{
				case PERCENT:
					funcs.add(new FuncMul(attr.getStat(), FUNC_ORDER, item, new LambdaConst(1.0 + slot.getValue() / 100.0)));
					break;
				
				case REDUCE:
					funcs.add(new FuncMul(attr.getStat(), FUNC_ORDER, item, new LambdaConst(Math.max(0.0, 1.0 - slot.getValue() / 100.0))));
					break;
				
				case FLAT:
					funcs.add(new FuncAdd(attr.getStat(), FUNC_ORDER, item, new LambdaConst(slot.getValue())));
					break;
			}
		}
		return funcs;
	}
	
	/**
	 * Vuelve a aplicar los stats del item si esta equipado y le manda el item actualizado al cliente.
	 * @param player : el duenio.
	 * @param item : el item.
	 */
	public void refresh(Player player, ItemInstance item)
	{
		if (item.isEquipped())
		{
			player.removeStatsByOwner(item);
			player.addStatFuncs(item.getStatFuncs(player));
			player.broadcastUserInfo();
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendPacket(iu);
	}
	
	// ------------------------------------------------------------------------
	// Cliente
	// ------------------------------------------------------------------------
	
	/**
	 * Valor que va en el campo "mana" del item en ItemList / InventoryUpdate.<br>
	 * Para items con sockets van los sockets 1 y 2 empaquetados (16 bits cada uno, socket 1 abajo y socket 2 arriba).<br>
	 * El socket 3 va en customType2 (ver getClientType2).
	 * <ul>
	 * <li>bits 0-4 : code del atributo (1-31, 0 = vacio)</li>
	 * <li>bits 5-7 : tier (1-5)</li>
	 * <li>bits 8-15 : valor x10 (0-255, o sea hasta 25.5)</li>
	 * </ul>
	 * Para el resto de los items devuelve la mana normal (armas shadow).
	 * @param item : el item.
	 * @return el valor para el paquete.
	 */
	public int getClientMana(ItemInstance item)
	{
		if (item == null)
			return 0;
		
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		if (!data.isEnabled() || !isEligible(item))
			return item.getMana();
		
		final Map<Integer, AlchemySlot> slots = data.getSlots(item.getObjectId());
		if (slots.isEmpty())
			return item.getMana();
		
		int packed = 0;
		for (AlchemySlot slot : slots.values())
		{
			if (slot.getSlot() < 1 || slot.getSlot() > 2)
				continue;
			
			packed |= packSlot(slot) << (16 * (slot.getSlot() - 1));
		}
		return packed;
	}
	
	/**
	 * Valor que va en customType1 del item (ItemList / InventoryUpdate). No se usa para sockets.
	 * @param item : el item.
	 * @return el valor normal del item.
	 */
	public int getClientType1(ItemInstance item)
	{
		return (item == null) ? 0 : item.getCustomType1();
	}
	
	/**
	 * Valor que va en customType2 del item (ItemList / InventoryUpdate).<br>
	 * Para items que pueden tener sockets va el socket 3 (16 bits, mismo formato que en getClientMana; 0 = vacio).<br>
	 * El cliente lo lee sin signo en item.Damaged.
	 * @param item : el item.
	 * @return el valor para el paquete.
	 */
	public int getClientType2(ItemInstance item)
	{
		if (item == null)
			return 0;
		
		final AttributeAlchemyData data = AttributeAlchemyData.getInstance();
		if (!data.isEnabled() || !isEligible(item))
			return item.getCustomType2();
		
		final AlchemySlot slot = data.getSlots(item.getObjectId()).get(3);
		return (slot == null) ? 0 : packSlot(slot);
	}
	
	/**
	 * Empaqueta un socket en 16 bits: bits 0-4 code, bits 5-7 tier, bits 8-15 valor x10.
	 * @param slot : el socket.
	 * @return los 16 bits (0 si el atributo ya no existe en el XML).
	 */
	private static int packSlot(AlchemySlot slot)
	{
		final AlchemyAttribute attr = slot.getAttribute();
		if (attr == null)
			return 0;
		
		final int value = Math.max(0, Math.min(255, (int) Math.round(slot.getValue() * 10)));
		return (attr.getCode() & 0x1F) | ((slot.getTier() & 0x07) << 5) | (value << 8);
	}
	
	public static AttributeAlchemyManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttributeAlchemyManager INSTANCE = new AttributeAlchemyManager();
	}
}
