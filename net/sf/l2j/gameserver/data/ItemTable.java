package net.sf.l2j.gameserver.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.skills.DocumentItem;

public class ItemTable
{
	private static final Logger _log = Logger.getLogger(ItemTable.class.getName());
	
	public static final Map<String, Integer> _slots = new HashMap<>();
	
	private volatile Item[] _allTemplates;
	private static final Map<Integer, Armor> _armors = new HashMap<>();
	private static final Map<Integer, EtcItem> _etcItems = new HashMap<>();
	private static final Map<Integer, Weapon> _weapons = new HashMap<>();
	
	static
	{
		_slots.put("chest", Item.SLOT_CHEST);
		_slots.put("fullarmor", Item.SLOT_FULL_ARMOR);
		_slots.put("alldress", Item.SLOT_ALLDRESS);
		_slots.put("head", Item.SLOT_HEAD);
		_slots.put("hair", Item.SLOT_HAIR);
		_slots.put("face", Item.SLOT_FACE);
		_slots.put("hairall", Item.SLOT_HAIRALL);
		_slots.put("underwear", Item.SLOT_UNDERWEAR);
		_slots.put("back", Item.SLOT_BACK);
		_slots.put("neck", Item.SLOT_NECK);
		_slots.put("legs", Item.SLOT_LEGS);
		_slots.put("feet", Item.SLOT_FEET);
		_slots.put("gloves", Item.SLOT_GLOVES);
		_slots.put("chest,legs", Item.SLOT_CHEST | Item.SLOT_LEGS);
		_slots.put("rhand", Item.SLOT_R_HAND);
		_slots.put("lhand", Item.SLOT_L_HAND);
		_slots.put("lrhand", Item.SLOT_LR_HAND);
		_slots.put("rear;lear", Item.SLOT_R_EAR | Item.SLOT_L_EAR);
		_slots.put("rfinger;lfinger", Item.SLOT_R_FINGER | Item.SLOT_L_FINGER);
		_slots.put("none", Item.SLOT_NONE);
		_slots.put("wolf", Item.SLOT_WOLF); // for wolf
		_slots.put("hatchling", Item.SLOT_HATCHLING); // for hatchling
		_slots.put("strider", Item.SLOT_STRIDER); // for strider
		_slots.put("babypet", Item.SLOT_BABYPET); // for babypet
	}
	
	public static ItemTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ItemTable()
	{
		load();
	}
	
	private void load()
	{
		applyItems(parseItems());
	}
	
	private static ParsedItems parseItems()
	{
		final ParsedItems parsed = new ParsedItems();
		final File dir = new File("./data/xml/items");
		final File[] files = dir.listFiles();
		if (files == null)
			throw new IllegalStateException("Item XML folder not found: " + dir.getAbsolutePath());
		
		for (File file : files)
		{
			if (!file.isFile())
				continue;
			
			DocumentItem document = new DocumentItem(file);
			document.parse();
			
			for (Item item : document.getItemList())
			{
				if (parsed.highest < item.getItemId())
					parsed.highest = item.getItemId();
				
				if (item instanceof EtcItem)
					parsed.etcItems.put(item.getItemId(), (EtcItem) item);
				else if (item instanceof Armor)
					parsed.armors.put(item.getItemId(), (Armor) item);
				else
					parsed.weapons.put(item.getItemId(), (Weapon) item);
			}
		}
		return parsed;
	}
	
	private void applyItems(ParsedItems parsed)
	{
		_armors.keySet().removeIf(k -> !parsed.armors.containsKey(k));
		_armors.putAll(parsed.armors);
		_etcItems.keySet().removeIf(k -> !parsed.etcItems.containsKey(k));
		_etcItems.putAll(parsed.etcItems);
		_weapons.keySet().removeIf(k -> !parsed.weapons.containsKey(k));
		_weapons.putAll(parsed.weapons);
		
		_log.info("ItemTable: Highest used itemID : " + parsed.highest);
		
		final Item[] templates = new Item[parsed.highest + 1];
		for (Armor item : _armors.values())
			templates[item.getItemId()] = item;
		for (Weapon item : _weapons.values())
			templates[item.getItemId()] = item;
		for (EtcItem item : _etcItems.values())
			templates[item.getItemId()] = item;
		_allTemplates = templates;
	}
	
	/**
	 * @param id : int designating the item
	 * @return the item corresponding to the item ID.
	 */
	public Item getTemplate(int id)
	{
		if (id >= _allTemplates.length)
			return null;
		
		return _allTemplates[id];
	}
	/**
	 * Dummy item is created by setting the ID of the object in the world at null value
	 * @param itemId : int designating the item
	 * @return ItemInstance designating the dummy item created
	 */
	public ItemInstance createDummyItem(int itemId)
	{
		final Item item = getTemplate(itemId);
		if (item == null)
			return null;

		return new ItemInstance(0, item);
	}	
	public void reload()
	{
		final ParsedItems parsed = parseItems();
		if (parsed.highest <= 0)
			throw new IllegalStateException("Item reload aborted: 0 items parsed.");
		
		applyItems(parsed);
	}
	
	private static class ParsedItems
	{
		int highest;
		final Map<Integer, Armor> armors = new HashMap<>();
		final Map<Integer, EtcItem> etcItems = new HashMap<>();
		final Map<Integer, Weapon> weapons = new HashMap<>();
	}
	
	private static class SingletonHolder
	{
		protected static final ItemTable _instance = new ItemTable();
	}
}