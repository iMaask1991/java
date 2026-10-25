package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Premios por cambio de clase (portado del server ruso, ClassRewardsData).
 * <ul>
 * <li>Config: ./config/HadesProject/ClassReward.properties</li>
 * <li>Sets: ./data/xml/ClassRewards.xml</li>
 * </ul>
 * Se recarga en caliente con .class_reload (solo GM).
 */
public final class ClassRewardData
{
	private static final Logger LOGGER = Logger.getLogger(ClassRewardData.class.getName());

	public static final String CONFIG_FILE = "./config/HadesProject/ClassReward.properties";
	public static final String XML_FILE = "./data/xml/ClassRewards.xml";

	// Config
	private boolean _commandEnabled = true;
	private final boolean[] _rewardLevel = new boolean[4];

	// level (1, 2, 3) -> sets
	private final Map<Integer, List<RewardSet>> _setsByLevel = new HashMap<>();
	// set id -> set
	private final Map<Integer, RewardSet> _sets = new HashMap<>();

	protected ClassRewardData()
	{
		load();
	}

	public synchronized void load()
	{
		loadConfig();
		loadXml();
	}

	private void loadConfig()
	{
		final Properties props = new Properties();
		final File file = new File(CONFIG_FILE);
		if (file.exists())
		{
			try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
			{
				props.load(reader);
			}
			catch (Exception e)
			{
				LOGGER.warning("ClassRewardData: error leyendo " + CONFIG_FILE + ": " + e.getMessage());
			}
		}
		else
			LOGGER.warning("ClassRewardData: no existe " + CONFIG_FILE + ", se usan valores por defecto.");

		_commandEnabled = getBool(props, "ClassCommandEnabled", true);
		_rewardLevel[1] = getBool(props, "RewardFirstClass", true);
		_rewardLevel[2] = getBool(props, "RewardSecondClass", true);
		_rewardLevel[3] = getBool(props, "RewardThirdClass", true);
	}

	private static boolean getBool(Properties props, String key, boolean def)
	{
		final String val = props.getProperty(key);
		return (val == null) ? def : Boolean.parseBoolean(val.trim());
	}

	private void loadXml()
	{
		_setsByLevel.clear();
		_sets.clear();

		final File file = new File(XML_FILE);
		if (!file.exists())
		{
			LOGGER.warning("ClassRewardData: no existe " + XML_FILE + ", no hay premios cargados.");
			return;
		}

		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			final Document doc = factory.newDocumentBuilder().parse(file);

			final NodeList levels = doc.getDocumentElement().getElementsByTagName("level");
			for (int i = 0; i < levels.getLength(); i++)
			{
				final Element levelNode = (Element) levels.item(i);
				final int level = Integer.parseInt(levelNode.getAttribute("id").trim());
				final List<RewardSet> list = _setsByLevel.computeIfAbsent(level, k -> new ArrayList<>());

				final NodeList sets = levelNode.getElementsByTagName("set");
				for (int j = 0; j < sets.getLength(); j++)
				{
					final Element setNode = (Element) sets.item(j);
					final int setId = Integer.parseInt(setNode.getAttribute("id").trim());
					if (_sets.containsKey(setId))
					{
						LOGGER.warning("ClassRewardData: set id " + setId + " repetido, se ignora.");
						continue;
					}

					final RewardSet set = new RewardSet(setId, level, setNode.getAttribute("name"), setNode.getAttribute("icon"));

					final NodeList items = setNode.getElementsByTagName("item");
					for (int k = 0; k < items.getLength(); k++)
					{
						final Element itemNode = (Element) items.item(k);
						final int id = Integer.parseInt(itemNode.getAttribute("id").trim());
						final int count = parseInt(itemNode.getAttribute("count"), 1);
						final int enchant = parseInt(itemNode.getAttribute("enchant"), 0);
						final boolean equip = Boolean.parseBoolean(itemNode.getAttribute("equip").trim());
						set.getItems().add(new RewardItem(id, count, enchant, equip));
					}

					list.add(set);
					_sets.put(setId, set);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("ClassRewardData: error leyendo " + XML_FILE + ": " + e.getMessage());
		}

		LOGGER.info("ClassRewardData: cargados " + _sets.size() + " sets de premios por cambio de clase.");
	}

	private static int parseInt(String val, int def)
	{
		if (val == null || val.trim().isEmpty())
			return def;

		return Integer.parseInt(val.trim());
	}

	public boolean isCommandEnabled()
	{
		return _commandEnabled;
	}

	/**
	 * @param level : 1 = 1ra clase, 2 = 2da, 3 = 3ra.
	 * @return true si ese cambio de clase da premio y hay sets cargados para ese nivel.
	 */
	public boolean hasRewardFor(int level)
	{
		if (level < 1 || level > 3 || !_rewardLevel[level])
			return false;

		return !getSets(level).isEmpty();
	}

	public List<RewardSet> getSets(int level)
	{
		final List<RewardSet> list = _setsByLevel.get(level);
		return (list == null) ? Collections.emptyList() : list;
	}

	public RewardSet getSet(int setId)
	{
		return _sets.get(setId);
	}

	public static ClassRewardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ClassRewardData INSTANCE = new ClassRewardData();
	}

	public static final class RewardSet
	{
		private final int _id;
		private final int _level;
		private final String _name;
		private final String _icon;
		private final List<RewardItem> _items = new ArrayList<>();

		public RewardSet(int id, int level, String name, String icon)
		{
			_id = id;
			_level = level;
			_name = (name == null || name.isEmpty()) ? ("Set " + id) : name;
			_icon = (icon == null) ? "" : icon.trim();
		}

		public int getId()
		{
			return _id;
		}

		public int getLevel()
		{
			return _level;
		}

		public String getName()
		{
			return _name;
		}

		public String getIcon()
		{
			return _icon;
		}

		public List<RewardItem> getItems()
		{
			return _items;
		}
	}

	public static final class RewardItem
	{
		private final int _id;
		private final int _count;
		private final int _enchant;
		private final boolean _equip;

		public RewardItem(int id, int count, int enchant, boolean equip)
		{
			_id = id;
			_count = count;
			_enchant = enchant;
			_equip = equip;
		}

		public int getId()
		{
			return _id;
		}

		public int getCount()
		{
			return _count;
		}

		public int getEnchant()
		{
			return _enchant;
		}

		public boolean isEquip()
		{
			return _equip;
		}
	}
}
