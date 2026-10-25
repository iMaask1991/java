package net.sf.l2j.gameserver.element;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.util.StatsSet;

import org.w3c.dom.Document;

/**
 * Atributos elementales de los NPC (como High Five): defensa fija por elemento y, si tiene, ataque elemental.<br>
 * Se carga de data/xml/npcAttributes.xml (sacado de los templates de H5). Los NPC que no estan en el archivo tienen 20 de defensa en todos los elementos y ningun ataque elemental (igual que H5).
 */
public class NpcAttributeData implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(NpcAttributeData.class.getName());

	/** Defensa por defecto de un NPC (H5). */
	public static final int DEFAULT_DEFENSE = 20;

	/** Valores por id de NPC. Indice = elemento del server (1 wind, 2 fire, 3 water, 4 earth, 5 holy, 6 dark). */
	private final Map<Integer, NpcAttribute> _npcs = new HashMap<>();

	public static class NpcAttribute
	{
		public final int[] defense = new int[7];
		public byte attackElement = -1;
		public int attackValue = 0;
	}

	protected NpcAttributeData()
	{
		load();
	}

	@Override
	public void load()
	{
		_npcs.clear();
		parseFile("./data/xml/npcAttributes.xml");
		LOGGER.info("NpcAttributes: {} NPC con atributos H5 cargados.", _npcs.size());
	}

	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "npc", npcNode ->
		{
			final StatsSet set = parseAttributes(npcNode);
			final NpcAttribute a = new NpcAttribute();
			a.defense[1] = set.getInteger("wind", DEFAULT_DEFENSE);
			a.defense[2] = set.getInteger("fire", DEFAULT_DEFENSE);
			a.defense[3] = set.getInteger("water", DEFAULT_DEFENSE);
			a.defense[4] = set.getInteger("earth", DEFAULT_DEFENSE);
			a.defense[5] = set.getInteger("holy", DEFAULT_DEFENSE);
			a.defense[6] = set.getInteger("dark", DEFAULT_DEFENSE);
			a.attackElement = ElementAttributes.parseElement(set.getString("attackType", ""));
			a.attackValue = (a.attackElement > 0) ? set.getInteger("attackValue", 0) : 0;
			_npcs.put(set.getInteger("id"), a);
		}));
	}

	/**
	 * @param npcId : id del NPC.
	 * @return los atributos, o null si usa los de defecto.
	 */
	public NpcAttribute get(int npcId)
	{
		return _npcs.get(npcId);
	}

	public static NpcAttributeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final NpcAttributeData INSTANCE = new NpcAttributeData();
	}
}
