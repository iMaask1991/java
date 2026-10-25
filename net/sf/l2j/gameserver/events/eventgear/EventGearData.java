package net.sf.l2j.gameserver.events.eventgear;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Sets del Event Gear por clase (2da y 3ra profesion).<br>
 * Archivo: data/xml/custom/EventGear.xml
 */
public final class EventGearData
{
	private static final Logger LOG = Logger.getLogger(EventGearData.class.getName());

	public static final String FILE = "./data/xml/custom/EventGear.xml";

	/** Un item del set: id + enchant (-1 = usa DefaultEnchant del properties). */
	public static final class GearItem
	{
		public final int id;
		public final int enchant;

		GearItem(int id, int enchant)
		{
			this.id = id;
			this.enchant = enchant;
		}
	}

	/** Un item apilable (shots, flechas): id + cantidad. */
	public static final class GearStack
	{
		public final int id;
		public final int count;

		GearStack(int id, int count)
		{
			this.id = id;
			this.count = count;
		}
	}

	public static final class GearSet
	{
		public final String name;
		public final List<GearItem> items = new ArrayList<>();
		public final List<GearStack> stacks = new ArrayList<>();
		public int armorSkin;
		public int weaponSkin;
		public int hairSkin;
		public int faceSkin;
		public int shieldSkin;
		public int raceSkin = -1;
		public int classSkin = -1;

		GearSet(String name)
		{
			this.name = name;
		}
	}

	private static final Map<Integer, GearSet> BY_CLASS = new HashMap<>();
	private static final List<GearSet> SETS = new ArrayList<>();

	private EventGearData()
	{
	}

	public static synchronized void load()
	{
		BY_CLASS.clear();
		SETS.clear();

		final File file = new File(FILE);
		if (!file.exists())
		{
			LOG.warning("EventGear: no se encontro " + FILE + ".");
			return;
		}

		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			final Document doc = factory.newDocumentBuilder().parse(file);

			final NodeList sets = doc.getDocumentElement().getElementsByTagName("set");
			for (int i = 0; i < sets.getLength(); i++)
			{
				final Element setEl = (Element) sets.item(i);
				final GearSet set = new GearSet(setEl.getAttribute("name"));

				final NodeList children = setEl.getChildNodes();
				for (int j = 0; j < children.getLength(); j++)
				{
					final Node n = children.item(j);
					if (n.getNodeType() != Node.ELEMENT_NODE)
						continue;

					final Element el = (Element) n;
					switch (el.getNodeName())
					{
						case "item":
							set.items.add(new GearItem(intAttr(el, "id", 0), intAttr(el, "enchant", -1)));
							break;
						case "stack":
							set.stacks.add(new GearStack(intAttr(el, "id", 0), intAttr(el, "count", 1)));
							break;
						case "skin":
							set.armorSkin = intAttr(el, "armor", 0);
							set.weaponSkin = intAttr(el, "weapon", 0);
							set.hairSkin = intAttr(el, "hair", 0);
							set.faceSkin = intAttr(el, "face", 0);
							set.shieldSkin = intAttr(el, "shield", 0);
							set.raceSkin = intAttr(el, "race", -1);
							set.classSkin = intAttr(el, "class", -1);
							break;
					}
				}

				for (String token : setEl.getAttribute("classes").split(","))
				{
					token = token.trim();
					if (token.isEmpty())
						continue;

					try
					{
						final int classId = Integer.parseInt(token);
						if (BY_CLASS.put(classId, set) != null)
							LOG.warning("EventGear: la clase " + classId + " esta en mas de un set, queda la del set '" + set.name + "'.");
					}
					catch (NumberFormatException e)
					{
						LOG.warning("EventGear: id de clase invalido '" + token + "' en el set '" + set.name + "'.");
					}
				}
				SETS.add(set);
			}
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: error leyendo " + FILE + ": " + e.getMessage());
		}

		LOG.info("EventGear: " + SETS.size() + " sets cargados para " + BY_CLASS.size() + " clases.");
	}

	private static int intAttr(Element el, String name, int def)
	{
		final String v = el.getAttribute(name);
		if (v == null || v.trim().isEmpty())
			return def;

		try
		{
			return Integer.parseInt(v.trim());
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}

	public static GearSet getSet(int classId)
	{
		return BY_CLASS.get(classId);
	}
}
