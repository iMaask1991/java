package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * Loads multisell purchase announcements from data/xml/announceMultiSell.xml
 */
public class AnnounceMultiSellData implements IXmlReader
{
	private static final String FILE = "./data/xml/announceMultiSell.xml";
	private static final String DEFAULT_MSG = "[Shop] %playername% obtuvo %itemname%.";
	
	private final Map<Integer, List<AnnounceEntry>> _entries = new HashMap<>();
	
	public AnnounceMultiSellData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_entries.clear();
		parseFile(FILE);
		LOGGER.info("Loaded {} announce multisell entries.", _entries.values().stream().mapToInt(List::size).sum());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "multisell", multisellNode ->
		{
			final NamedNodeMap attrs = multisellNode.getAttributes();
			final int listId = MultisellData.getListHash(String.valueOf(parseInteger(attrs, "id")));
			final int itemId = parseInteger(attrs, "itemId", -1);
			final Boolean fireworkAttr = parseBoolean(attrs, "firework", false);
			final boolean firework = Boolean.TRUE.equals(fireworkAttr);
			final String msg = parseString(attrs, "msg", DEFAULT_MSG);
			final int typeMsg = normalizeTypeMsg(parseInteger(attrs, "typemsg", Say2.ANNOUNCEMENT));
			
			_entries.computeIfAbsent(listId, key -> new ArrayList<>()).add(new AnnounceEntry(listId, itemId, firework, msg, typeMsg));
		}));
	}
	
	public void tryAnnounce(Player player, int listId, int itemId, int amount, int enchantLevel)
	{
		if (!Config.ANNOUNCE_MULTISELL || player == null)
			return;
		
		final List<AnnounceEntry> entries = _entries.get(listId);
		if (entries != null && !entries.isEmpty())
		{
			for (AnnounceEntry entry : entries)
			{
				if (entry.itemId > 0 && entry.itemId != itemId)
					continue;
				
				broadcast(player, itemId, amount, enchantLevel, entry.firework, entry.msg, entry.typeMsg);
				return;
			}
		}
		
		if (isConfiguredList(listId))
			broadcast(player, itemId, amount, enchantLevel, false, DEFAULT_MSG, Say2.ANNOUNCEMENT);
	}
	
	private static boolean isConfiguredList(int listId)
	{
		for (int id : Config.ANNOUNCE_MULTISELL_LIST_IDS)
		{
			if (MultisellData.getListHash(String.valueOf(id)) == listId)
				return true;
		}
		return false;
	}
	
	private static void broadcast(Player player, int itemId, int amount, int enchantLevel, boolean firework, String msgTemplate, int typeMsg)
	{
		try
		{
			if (firework)
				player.broadcastPacket(new MagicSkillUse(player, player, 2024, 1, 1, 0));
			
			final Item template = ItemTable.getInstance().getTemplate(itemId);
			final String itemName = (template != null) ? template.getName() : "Item";
			final String quantityPrefix = (amount > 1) ? amount + " " : "";
			final String enchantSuffix = (enchantLevel > 0) ? " +" + enchantLevel : "";
			
			String message = (msgTemplate != null && !msgTemplate.isEmpty()) ? msgTemplate : DEFAULT_MSG;
			message = message.replace("%playername%", player.getName());
			message = message.replace("%itemname%", quantityPrefix + itemName + enchantSuffix);
			
			final CreatureSay cs = new CreatureSay(0, typeMsg, "", message);
			// snapshot para evitar ConcurrentModificationException si alguien entra/sale
			for (Player online : World.getInstance().getPlayers().toArray(new Player[0]))
			{
				if (online != null && online.isOnline())
					online.sendPacket(cs);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("AnnounceMultiSell broadcast failed for item {}: {}", itemId, e.getMessage());
		}
	}
	
	private static int normalizeTypeMsg(int typeMsg)
	{
		if (typeMsg < 0 || typeMsg > Say2.CRITICAL_ANNOUNCE)
			return Say2.ANNOUNCEMENT;
		
		return typeMsg;
	}
	
	public static AnnounceMultiSellData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class AnnounceEntry
	{
		private final int listId;
		private final int itemId;
		private final boolean firework;
		private final String msg;
		private final int typeMsg;
		
		private AnnounceEntry(int listId, int itemId, boolean firework, String msg, int typeMsg)
		{
			this.listId = listId;
			this.itemId = itemId;
			this.firework = firework;
			this.msg = msg;
			this.typeMsg = typeMsg;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AnnounceMultiSellData INSTANCE = new AnnounceMultiSellData();
	}
}
