package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.StatsSet;

import org.w3c.dom.Document;

/**
 * Custom Boxes (cajitas) - carga data/xml/custom/boxes.xml
 * <ul>
 * <li>Cada grupo tira independiente del resto.</li>
 * <li>mode="one" : una tirada con el chance del grupo; si sale, entrega UN item random del grupo.</li>
 * <li>mode="each" : cada item tira su propio chance por separado (pueden salir varios juntos).</li>
 * </ul>
 */
public class BoxData implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(BoxData.class.getName());

	private final Map<Integer, Box> _boxes = new HashMap<>();

	protected BoxData()
	{
		load();
	}

	@Override
	public void load()
	{
		parseFile("./data/xml/custom/boxes.xml");
		LOGGER.info("Loaded {} custom boxes.", _boxes.size());
	}

	public void reload()
	{
		_boxes.clear();
		load();
	}

	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "box", boxNode ->
		{
			final StatsSet boxSet = parseAttributes(boxNode);
			final Box box = new Box(boxSet.getInteger("itemId"), boxSet.getString("name", "Box"));

			forEach(boxNode, "group", groupNode ->
			{
				final StatsSet groupSet = parseAttributes(groupNode);
				final Group group = new Group(groupSet.getString("name", "Group"), groupSet.getDouble("chance", 100), "each".equalsIgnoreCase(groupSet.getString("mode", "one")), groupSet.getBool("announce", false));

				forEach(groupNode, "item", itemNode ->
				{
					final StatsSet itemSet = parseAttributes(itemNode);
					final int id = itemSet.getInteger("id");
					if (id <= 0)
					{
						LOGGER.warn("BoxData: item con id invalido en box {} / grupo {}, se ignora.", box.getItemId(), group.getName());
						return;
					}

					final int count = itemSet.getInteger("count", 1);
					final int min = itemSet.getInteger("min", count);
					final int max = itemSet.getInteger("max", Math.max(min, count));
					final double chance = itemSet.getDouble("chance", group.getChance());
					group.addReward(new Reward(id, min, max, chance));
				});

				if (group.getRewards().isEmpty())
					LOGGER.warn("BoxData: grupo {} de la box {} quedo vacio.", group.getName(), box.getItemId());
				else
					box.addGroup(group);
			});

			_boxes.put(box.getItemId(), box);
		}));
	}

	public Box getBox(int itemId)
	{
		return _boxes.get(itemId);
	}

	public static BoxData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final BoxData INSTANCE = new BoxData();
	}

	// ============================== MODELO ==============================

	public static class Box
	{
		private final int _itemId;
		private final String _name;
		private final List<Group> _groups = new ArrayList<>();
		private int _maxSlots;

		public Box(int itemId, String name)
		{
			_itemId = itemId;
			_name = name;
		}

		public void addGroup(Group group)
		{
			_groups.add(group);
			_maxSlots += group.isEach() ? group.getRewards().size() : 1;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public String getName()
		{
			return _name;
		}

		public List<Group> getGroups()
		{
			return Collections.unmodifiableList(_groups);
		}

		/** Peor caso de slots de inventario que puede ocupar una apertura. */
		public int getMaxSlots()
		{
			return _maxSlots;
		}
	}

	public static class Group
	{
		private final String _name;
		private final double _chance;
		private final boolean _each;
		private final boolean _announce;
		private final List<Reward> _rewards = new ArrayList<>();

		public Group(String name, double chance, boolean each, boolean announce)
		{
			_name = name;
			_chance = chance;
			_each = each;
			_announce = announce;
		}

		public void addReward(Reward reward)
		{
			_rewards.add(reward);
		}

		/**
		 * Tira el grupo y devuelve lo que salio (puede venir vacio = mala suerte).
		 */
		public List<Reward> roll()
		{
			if (_each)
			{
				final List<Reward> result = new ArrayList<>();
				for (Reward reward : _rewards)
				{
					if (rollChance(reward.getChance()))
						result.add(reward);
				}
				return result;
			}

			if (!rollChance(_chance))
				return Collections.emptyList();

			return Collections.singletonList(Rnd.get(_rewards));
		}

		public String getName()
		{
			return _name;
		}

		public double getChance()
		{
			return _chance;
		}

		public boolean isEach()
		{
			return _each;
		}

		public boolean isAnnounce()
		{
			return _announce;
		}

		public List<Reward> getRewards()
		{
			return _rewards;
		}
	}

	public static class Reward
	{
		private final int _itemId;
		private final int _min;
		private final int _max;
		private final double _chance;

		public Reward(int itemId, int min, int max, double chance)
		{
			_itemId = itemId;
			_min = min;
			_max = max;
			_chance = chance;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int rollCount()
		{
			return (_min >= _max) ? _min : Rnd.get(_min, _max);
		}

		public double getChance()
		{
			return _chance;
		}
	}

	/** Chance en porcentaje con decimales (0.5 = medio por ciento). */
	protected static boolean rollChance(double chance)
	{
		if (chance >= 100)
			return true;
		if (chance <= 0)
			return false;

		return Rnd.nextDouble() * 100 < chance;
	}
}
