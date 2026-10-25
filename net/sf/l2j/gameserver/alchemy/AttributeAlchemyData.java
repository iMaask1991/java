package net.sf.l2j.gameserver.alchemy;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.enums.skills.Stats;

import org.w3c.dom.Document;

/**
 * Attribute Alchemy (Sockets) - carga data/xml/custom/attributeAlchemy.xml y guarda los sockets de cada item.<br>
 * Basado en el "Attribute Alchemy System" de Schuster (L2JBrasil), adaptado a HadesSource:
 * <ul>
 * <li>Se usa con el orbe (doble click) y la ventana de enchant del cliente, sin HTML.</li>
 * <li>Los sockets viajan al cliente en el campo "mana" del item y los dibuja el ToolTip.uc.</li>
 * </ul>
 */
public class AttributeAlchemyData implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(AttributeAlchemyData.class.getName());
	
	private static final String LOAD_SLOTS = "SELECT slot_id, attribute_id, tier, value FROM character_attribute_alchemy WHERE item_object_id=?";
	private static final String SAVE_SLOT = "REPLACE INTO character_attribute_alchemy (item_object_id, slot_id, attribute_id, tier, value, updated_at) VALUES (?,?,?,?,?,?)";
	private static final String DELETE_ITEM = "DELETE FROM character_attribute_alchemy WHERE item_object_id=?";
	private static final String DELETE_SLOT = "DELETE FROM character_attribute_alchemy WHERE item_object_id=? AND slot_id=?";
	
	// Config (se pisa con lo del XML)
	private boolean _enabled = true;
	private int _maxSlots = 3;
	private CrystalType _minGrade = CrystalType.S;
	private final Map<AlchemyType, Integer> _orbs = new java.util.EnumMap<>(AlchemyType.class);
	private int _orbCount = 1;
	private int _animationSkillId = 2024;
	private int _resetItemId = 57;
	private int _resetCount = 0;
	
	private final Map<String, AlchemyAttribute> _attributes = new LinkedHashMap<>();
	
	/** Cache de sockets por objectId del item (se carga de la DB la primera vez). */
	private final Map<Integer, Map<Integer, AlchemySlot>> _itemSlots = new ConcurrentHashMap<>();
	
	protected AttributeAlchemyData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/custom/attributeAlchemy.xml");
		LOGGER.info("AttributeAlchemy: {} atributos cargados (enabled={}, slots={}, orbes={}, grado min={}).", _attributes.size(), _enabled, _maxSlots, _orbs, _minGrade);
	}
	
	public void reload()
	{
		_attributes.clear();
		_orbs.clear();
		load();
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "config", configNode ->
			{
				final StatsSet set = parseAttributes(configNode);
				_enabled = set.getBool("enabled", true);
				_maxSlots = Math.max(1, Math.min(3, set.getInteger("maxSlots", 3)));
				_orbCount = Math.max(1, set.getInteger("orbCount", 1));
				_animationSkillId = set.getInteger("animationSkillId", 2024);
				_resetItemId = set.getInteger("resetItemId", 57);
				_resetCount = Math.max(0, set.getInteger("resetCount", 0));
				try
				{
					_minGrade = CrystalType.valueOf(set.getString("minGrade", "S").toUpperCase());
				}
				catch (Exception e)
				{
					_minGrade = CrystalType.S;
				}
			});
			
			forEach(listNode, "orb", orbNode ->
			{
				final StatsSet set = parseAttributes(orbNode);
				_orbs.put(AlchemyType.fromString(set.getString("type", "WEAPON")), set.getInteger("itemId"));
			});
			
			forEach(listNode, "attribute", attrNode ->
			{
				final StatsSet set = parseAttributes(attrNode);
				final int code = set.getInteger("code", 0);
				final String id = set.getString("id", "").toUpperCase();
				final String statName = set.getString("stat", "");
				
				if (code < 1 || code > 31 || id.isEmpty())
				{
					LOGGER.warn("AttributeAlchemy: atributo invalido (code {} / id {}), se ignora. El code tiene que ser 1-31.", code, id);
					return;
				}
				
				Stats stat;
				try
				{
					stat = Stats.valueOfXml(statName);
				}
				catch (Exception e)
				{
					try
					{
						stat = Stats.valueOf(statName.toUpperCase());
					}
					catch (Exception ex)
					{
						LOGGER.warn("AttributeAlchemy: stat desconocido '{}' en el atributo {}, se ignora.", statName, id);
						return;
					}
				}
				
				AlchemyAttribute.Mode mode;
				try
				{
					mode = AlchemyAttribute.Mode.valueOf(set.getString("mode", "PERCENT").toUpperCase());
				}
				catch (Exception e)
				{
					LOGGER.warn("AttributeAlchemy: mode invalido en el atributo {} (PERCENT, FLAT o REDUCE), se ignora.", id);
					return;
				}
				
				final AlchemyType type = AlchemyType.fromString(set.getString("type", "WEAPON"));
				final AlchemyAttribute attr = new AlchemyAttribute(code, id, set.getString("name", id), type, stat, mode, set.getString("unit", "%"), set.getDouble("weight", 100));
				
				forEach(attrNode, "tier", tierNode ->
				{
					final StatsSet t = parseAttributes(tierNode);
					attr.addTier(new AlchemyTier(t.getInteger("level"), t.getDouble("min"), t.getDouble("max"), t.getDouble("chance")));
				});
				
				if (attr.getTiers().isEmpty())
				{
					LOGGER.warn("AttributeAlchemy: el atributo {} no tiene tiers, se ignora.", id);
					return;
				}
				
				_attributes.put(id, attr);
			});
		});
	}
	
	// ------------------------------------------------------------------------
	// Config
	// ------------------------------------------------------------------------
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public int getMaxSlots()
	{
		return _maxSlots;
	}
	
	public CrystalType getMinGrade()
	{
		return _minGrade;
	}
	
	/**
	 * @param type : el tipo de item.
	 * @return el itemId del orbe para ese tipo, o 0 si no hay.
	 */
	public int getOrbItemId(AlchemyType type)
	{
		return _orbs.getOrDefault(type, 0);
	}
	
	/**
	 * @param itemId : un itemId.
	 * @return true si ese item es el orbe de algun tipo.
	 */
	public boolean isOrbItem(int itemId)
	{
		return itemId > 0 && _orbs.containsValue(itemId);
	}
	
	public int getOrbCount()
	{
		return _orbCount;
	}
	
	public int getAnimationSkillId()
	{
		return _animationSkillId;
	}
	
	// ------------------------------------------------------------------------
	// Atributos
	// ------------------------------------------------------------------------
	
	public AlchemyAttribute getAttribute(String id)
	{
		return (id == null) ? null : _attributes.get(id.toUpperCase());
	}
	
	public List<AlchemyAttribute> getAttributes()
	{
		return new ArrayList<>(_attributes.values());
	}
	
	/**
	 * @param type : el tipo de item.
	 * @return los atributos que pueden salir en ese tipo de item.
	 */
	public List<AlchemyAttribute> getAttributes(AlchemyType type)
	{
		final List<AlchemyAttribute> list = new ArrayList<>();
		for (AlchemyAttribute a : _attributes.values())
			if (a.getType() == type)
				list.add(a);
		return list;
	}
	
	// ------------------------------------------------------------------------
	// Sockets de los items (cache + DB)
	// ------------------------------------------------------------------------
	
	/**
	 * @param objectId : objectId del item.
	 * @return los sockets del item (slot -> socket). Nunca null.
	 */
	public Map<Integer, AlchemySlot> getSlots(int objectId)
	{
		return _itemSlots.computeIfAbsent(objectId, this::loadSlots);
	}
	
	/**
	 * @param objectId : objectId del item.
	 * @return true si el item tiene al menos un socket cargado.
	 */
	public boolean hasSlots(int objectId)
	{
		return !getSlots(objectId).isEmpty();
	}
	
	private Map<Integer, AlchemySlot> loadSlots(int objectId)
	{
		final Map<Integer, AlchemySlot> slots = new ConcurrentHashMap<>();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SLOTS))
		{
			ps.setInt(1, objectId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final AlchemySlot slot = new AlchemySlot(rs.getInt("slot_id"), rs.getString("attribute_id"), rs.getInt("tier"), rs.getDouble("value"));
					slots.put(slot.getSlot(), slot);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("AttributeAlchemy: no se pudieron cargar los sockets del item {}.", e, objectId);
		}
		return slots;
	}
	
	/**
	 * Guarda (o reemplaza) un socket del item en la DB y en la cache.
	 * @param objectId : objectId del item.
	 * @param slot : el socket.
	 * @return true si se guardo bien.
	 */
	public boolean saveSlot(int objectId, AlchemySlot slot)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_SLOT))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, slot.getSlot());
			ps.setString(3, slot.getAttributeId());
			ps.setInt(4, slot.getTier());
			ps.setDouble(5, slot.getValue());
			ps.setLong(6, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("AttributeAlchemy: no se pudo guardar el socket {} del item {}.", e, slot.getSlot(), objectId);
			return false;
		}
		
		getSlots(objectId).put(slot.getSlot(), slot);
		return true;
	}
	
	/**
	 * Borra todos los sockets del item (DB + cache). Se llama cuando el item se borra o con //clearalchemy.
	 * @param objectId : objectId del item.
	 */
	public void deleteItem(int objectId)
	{
		_itemSlots.remove(objectId);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ITEM))
		{
			ps.setInt(1, objectId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("AttributeAlchemy: no se pudieron borrar los sockets del item {}.", e, objectId);
		}
	}
	
	/**
	 * Borra un solo socket del item (DB + cache). El proximo orbe vuelve a llenar ese socket vacio.
	 * @param objectId : objectId del item.
	 * @param slotId : el socket (1-3).
	 * @return true si el item tenia ese socket.
	 */
	public boolean deleteSlot(int objectId, int slotId)
	{
		if (getSlots(objectId).remove(slotId) == null)
			return false;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SLOT))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, slotId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("AttributeAlchemy: no se pudo borrar el socket {} del item {}.", e, slotId, objectId);
		}
		return true;
	}
	
	/**
	 * @return item que cobra el Donate Shop (donationmanager.properties).
	 */
	public int getDonateItemId()
	{
		return net.sf.l2j.Config.DONATE_ALCHEMY_ITEM;
	}
	
	/**
	 * @return cuanto cobra el Donate Shop por cada uno, 0 = gratis (donationmanager.properties).
	 */
	public int getDonateCount()
	{
		return Math.max(0, net.sf.l2j.Config.DONATE_ALCHEMY_COUNT);
	}
	
	/**
	 * @return item que cobra el NPC por sacar un socket (57 = adena).
	 */
	public int getResetItemId()
	{
		return _resetItemId;
	}
	
	/**
	 * @return cuanto cobra el NPC por sacar un socket (0 = gratis).
	 */
	public int getResetCount()
	{
		return _resetCount;
	}
	
	/**
	 * Saca el item de la cache (sin tocar la DB).
	 * @param objectId : objectId del item.
	 */
	public void uncache(int objectId)
	{
		_itemSlots.remove(objectId);
	}
	
	public static Map<Integer, AlchemySlot> emptySlots()
	{
		return Collections.emptyMap();
	}
	
	public static AttributeAlchemyData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttributeAlchemyData INSTANCE = new AttributeAlchemyData();
	}
}
