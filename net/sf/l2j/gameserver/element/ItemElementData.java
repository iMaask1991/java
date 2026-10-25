package net.sf.l2j.gameserver.element;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.enums.items.CrystalType;

import org.w3c.dom.Document;

/**
 * Elementos de items (estilo High Five) - carga data/xml/custom/elements.xml y guarda el elemento de cada item.<br>
 * La tabla item_elements se carga entera al iniciar (solo tiene los items con elemento).<br>
 * Armas: un solo elemento. Armaduras: hasta 3 elementos (como H5), cada uno en su fila (item_object_id, element_id).
 */
public class ItemElementData implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(ItemElementData.class.getName());
	
	private static final String LOAD_ALL = "SELECT item_object_id, element_id, value FROM item_elements";
	private static final String SAVE = "REPLACE INTO item_elements (item_object_id, element_id, value) VALUES (?,?,?)";
	private static final String DELETE_ITEM = "DELETE FROM item_elements WHERE item_object_id=?";
	private static final String DELETE_ELEMENT = "DELETE FROM item_elements WHERE item_object_id=? AND element_id=?";
	
	// Config (se pisa con lo del XML)
	private boolean _enabled = true;
	private CrystalType _minGrade = CrystalType.S;
	private int _chance = 50;
	private int _weaponFirst = 20;
	private int _weaponStep = 5;
	private int _weaponMax = 300;
	private int _armorFirst = 6;
	private int _armorStep = 6;
	private int _armorMax = 120;
	private int _armorMaxElements = 3;
	private int _resetItemId = 57;
	private int _resetCount = 0;
	private boolean _pve = true;
	private boolean _penalty = true;
	private double _minMultiplier = 0.6;
	private int _animationSkillId = 2024;
	
	/** itemId de la piedra -> datos de la piedra (elemento, chance y rango). */
	private final Map<Integer, Stone> _stones = new HashMap<>();
	
	/**
	 * Una piedra / cristal / energia de elemento (data/xml/custom/elements.xml).<br>
	 * minWeapon / minArmor : valor que tiene que tener ya el item para poder usarla (0 = desde cero).<br>
	 * maxWeapon / maxArmor : hasta donde sube con esta piedra.<br>
	 * full : si es true, en un exito deja el elemento directo en maxWeapon / maxArmor.
	 */
	public static final class Stone
	{
		public final ElementType element;
		public final int chance;
		public final int minWeapon;
		public final int maxWeapon;
		public final int minArmor;
		public final int maxArmor;
		public final boolean full;
		
		Stone(ElementType element, int chance, int minWeapon, int maxWeapon, int minArmor, int maxArmor, boolean full)
		{
			this.element = element;
			this.chance = chance;
			this.minWeapon = minWeapon;
			this.maxWeapon = maxWeapon;
			this.minArmor = minArmor;
			this.maxArmor = maxArmor;
			this.full = full;
		}
	}
	
	/** objectId del item -> sus elementos (lista que no se modifica, se reemplaza entera). */
	private final Map<Integer, List<ItemElement>> _items = new ConcurrentHashMap<>();
	
	protected ItemElementData()
	{
		load();
		loadItems();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/custom/elements.xml");
		LOGGER.info("ItemElements: {} piedras cargadas (enabled={}, chance={}%, arma {}/{} max {}, armadura {}/{} max {} hasta {} elementos).", _stones.size(), _enabled, _chance, _weaponFirst, _weaponStep, _weaponMax, _armorFirst, _armorStep, _armorMax, _armorMaxElements);
	}
	
	public void reload()
	{
		_stones.clear();
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
				_chance = Math.max(0, Math.min(100, set.getInteger("chance", 50)));
				_weaponFirst = set.getInteger("weaponFirst", 20);
				_weaponStep = set.getInteger("weaponStep", 5);
				_weaponMax = set.getInteger("weaponMax", 300);
				_armorFirst = set.getInteger("armorFirst", 6);
				_armorStep = set.getInteger("armorStep", 6);
				_armorMax = set.getInteger("armorMax", 120);
				_armorMaxElements = Math.max(1, Math.min(3, set.getInteger("armorMaxElements", 3)));
				_resetItemId = set.getInteger("resetItemId", 57);
				_resetCount = Math.max(0, set.getInteger("resetCount", 0));
				_pve = set.getBool("pve", true);
				_penalty = set.getBool("penalty", true);
				_minMultiplier = set.getDouble("minMultiplier", 0.6);
				_animationSkillId = set.getInteger("animationSkillId", 2024);
				try
				{
					_minGrade = CrystalType.valueOf(set.getString("minGrade", "S").toUpperCase());
				}
				catch (Exception e)
				{
					_minGrade = CrystalType.S;
				}
			});
			
			forEach(listNode, "stone", stoneNode ->
			{
				final StatsSet set = parseAttributes(stoneNode);
				final ElementType element = ElementType.parse(set.getString("element", ""));
				if (element == null)
				{
					LOGGER.warn("ItemElements: elemento invalido en la piedra {}, se ignora.", set.getInteger("itemId", 0));
					return;
				}
				final int chance = Math.max(0, Math.min(100, set.getInteger("chance", _chance)));
				final int maxWeapon = Math.min(_weaponMax, set.getInteger("maxWeapon", _weaponMax));
				final int maxArmor = Math.min(_armorMax, set.getInteger("maxArmor", _armorMax));
				final int minWeapon = Math.max(0, Math.min(maxWeapon, set.getInteger("minWeapon", 0)));
				final int minArmor = Math.max(0, Math.min(maxArmor, set.getInteger("minArmor", 0)));
				_stones.put(set.getInteger("itemId"), new Stone(element, chance, minWeapon, maxWeapon, minArmor, maxArmor, set.getBool("full", false)));
			});
		});
	}
	
	private void loadItems()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_ALL);
			ResultSet rs = ps.executeQuery())
		{
			final Map<Integer, List<ItemElement>> loaded = new HashMap<>();
			while (rs.next())
			{
				final ElementType element = ElementType.getById(rs.getInt("element_id"));
				if (element != null && rs.getInt("value") > 0)
					loaded.computeIfAbsent(rs.getInt("item_object_id"), k -> new ArrayList<>()).add(new ItemElement(element, rs.getInt("value")));
			}
			for (Map.Entry<Integer, List<ItemElement>> entry : loaded.entrySet())
				_items.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
		}
		catch (Exception e)
		{
			LOGGER.error("ItemElements: no se pudo cargar la tabla item_elements (fijate si corriste el SQL).", e);
		}
		LOGGER.info("ItemElements: {} items con elemento.", _items.size());
	}
	
	// ------------------------------------------------------------------------
	// Items
	// ------------------------------------------------------------------------
	
	/**
	 * @param objectId : objectId del item.
	 * @return el primer elemento del item (en armas es el unico), o null.
	 */
	public ItemElement get(int objectId)
	{
		final List<ItemElement> list = _items.get(objectId);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}
	
	/**
	 * @param objectId : objectId del item.
	 * @param element : el elemento buscado.
	 * @return ese elemento del item, o null si no lo tiene.
	 */
	public ItemElement get(int objectId, ElementType element)
	{
		for (ItemElement e : getAll(objectId))
			if (e.getElement() == element)
				return e;
		return null;
	}
	
	/**
	 * @param objectId : objectId del item.
	 * @return todos los elementos del item (lista vacia si no tiene).
	 */
	public List<ItemElement> getAll(int objectId)
	{
		final List<ItemElement> list = _items.get(objectId);
		return (list == null) ? Collections.emptyList() : list;
	}
	
	/**
	 * Guarda el elemento del item y borra cualquier otro que tenga (armas: un solo elemento).
	 * @param objectId : objectId del item.
	 * @param element : el elemento.
	 * @return true si se guardo.
	 */
	public boolean save(int objectId, ItemElement element)
	{
		return save(objectId, element, true);
	}
	
	/**
	 * Guarda el elemento del item (DB + cache).
	 * @param objectId : objectId del item.
	 * @param element : el elemento.
	 * @param single : true borra los otros elementos del item (armas), false agrega o actualiza solo ese (armaduras).
	 * @return true si se guardo.
	 */
	public synchronized boolean save(int objectId, ItemElement element, boolean single)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if (single)
			{
				try (PreparedStatement ps = con.prepareStatement(DELETE_ITEM))
				{
					ps.setInt(1, objectId);
					ps.executeUpdate();
				}
			}
			try (PreparedStatement ps = con.prepareStatement(SAVE))
			{
				ps.setInt(1, objectId);
				ps.setInt(2, element.getElement().getId());
				ps.setInt(3, element.getValue());
				ps.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.error("ItemElements: no se pudo guardar el elemento del item {}.", e, objectId);
			return false;
		}
		
		final List<ItemElement> list = new ArrayList<>();
		if (!single)
		{
			for (ItemElement old : getAll(objectId))
				if (old.getElement() != element.getElement())
					list.add(old);
		}
		list.add(element);
		_items.put(objectId, Collections.unmodifiableList(list));
		return true;
	}
	
	/**
	 * Borra todos los elementos del item (DB + cache). Si el item no tiene elemento no toca la DB.
	 * @param objectId : objectId del item.
	 * @return true si tenia elemento.
	 */
	public synchronized boolean delete(int objectId)
	{
		if (_items.remove(objectId) == null)
			return false;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ITEM))
		{
			ps.setInt(1, objectId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("ItemElements: no se pudo borrar el elemento del item {}.", e, objectId);
		}
		return true;
	}
	
	/**
	 * Borra un solo elemento del item (DB + cache).
	 * @param objectId : objectId del item.
	 * @param element : el elemento a sacar.
	 * @return true si tenia ese elemento.
	 */
	public synchronized boolean delete(int objectId, ElementType element)
	{
		if (get(objectId, element) == null)
			return false;
		
		final List<ItemElement> list = new ArrayList<>();
		for (ItemElement old : getAll(objectId))
			if (old.getElement() != element)
				list.add(old);
		
		if (list.isEmpty())
			_items.remove(objectId);
		else
			_items.put(objectId, Collections.unmodifiableList(list));
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_ELEMENT))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, element.getId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("ItemElements: no se pudo borrar el elemento {} del item {}.", e, element.getName(), objectId);
		}
		return true;
	}
	
	// ------------------------------------------------------------------------
	// Config
	// ------------------------------------------------------------------------
	
	public ElementType getStoneElement(int itemId)
	{
		final Stone stone = _stones.get(itemId);
		return (stone == null) ? null : stone.element;
	}
	
	public Stone getStone(int itemId)
	{
		return _stones.get(itemId);
	}
	
	public boolean isStone(int itemId)
	{
		return _stones.containsKey(itemId);
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public CrystalType getMinGrade()
	{
		return _minGrade;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public int getWeaponFirst()
	{
		return _weaponFirst;
	}
	
	public int getWeaponStep()
	{
		return _weaponStep;
	}
	
	public int getWeaponMax()
	{
		return _weaponMax;
	}
	
	public int getArmorFirst()
	{
		return _armorFirst;
	}
	
	public int getArmorStep()
	{
		return _armorStep;
	}
	
	public int getArmorMax()
	{
		return _armorMax;
	}
	
	/**
	 * @return cuantos elementos distintos puede tener una pieza de armadura (H5: 3).
	 */
	public int getArmorMaxElements()
	{
		return _armorMaxElements;
	}
	
	/**
	 * @return item que cobra el Donate Shop (donationmanager.properties).
	 */
	public int getDonateItemId()
	{
		return net.sf.l2j.Config.DONATE_ELEMENT_ITEM;
	}
	
	/**
	 * @return cuanto cobra el Donate Shop por cada uno, 0 = gratis (donationmanager.properties).
	 */
	public int getDonateCount()
	{
		return Math.max(0, net.sf.l2j.Config.DONATE_ELEMENT_COUNT);
	}
	
	/**
	 * @return item que cobra el NPC por sacar un elemento (57 = adena).
	 */
	public int getResetItemId()
	{
		return _resetItemId;
	}
	
	/**
	 * @return cuanto cobra el NPC por sacar un elemento (0 = gratis).
	 */
	public int getResetCount()
	{
		return _resetCount;
	}
	
	public boolean isPve()
	{
		return _pve;
	}
	
	public boolean isPenalty()
	{
		return _penalty;
	}
	
	public double getMinMultiplier()
	{
		return _minMultiplier;
	}
	
	public int getAnimationSkillId()
	{
		return _animationSkillId;
	}
	
	public static ItemElementData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemElementData INSTANCE = new ItemElementData();
	}
}
