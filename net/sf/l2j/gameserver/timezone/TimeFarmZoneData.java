package net.sf.l2j.gameserver.timezone;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.model.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class TimeFarmZoneData
{
	private static final CLogger LOGGER = new CLogger(TimeFarmZoneData.class.getName());
	/** LinkedHashMap: preserve XML order and never drop unloaded zones on save. */
	private static final Map<String, ZoneInfo> _zones = new LinkedHashMap<>();
	
	public static class MobSpawn
	{
		public int mobId;
		public Location loc;
		
		public MobSpawn(int mobId, Location loc)
		{
			this.mobId = mobId;
			this.loc = loc;
		}
	}
	
	public static class ZoneInfo
	{
		public String name;
		public int intervalMinutes;
		public final List<MobSpawn> spawns = new ArrayList<>();
		public boolean enabled = true;
	}
	
	public static void load(File file)
	{
		try
		{
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			final Node listNode = doc.getFirstChild();
			_zones.clear();
			
			for (Node zoneNode = listNode.getFirstChild(); zoneNode != null; zoneNode = zoneNode.getNextSibling())
			{
				if (!"zone".equalsIgnoreCase(zoneNode.getNodeName()))
					continue;
				
				final NamedNodeMap attrs = zoneNode.getAttributes();
				final String name = attrs.getNamedItem("name").getNodeValue();
				
				int intervalMinutes = 10;
				if (attrs.getNamedItem("intervalMinutes") != null)
					intervalMinutes = Integer.parseInt(attrs.getNamedItem("intervalMinutes").getNodeValue());
				
				boolean enabled = true;
				if (attrs.getNamedItem("enabled") != null)
					enabled = Boolean.parseBoolean(attrs.getNamedItem("enabled").getNodeValue());
				
				final ZoneInfo zoneInfo = new ZoneInfo();
				zoneInfo.name = name;
				zoneInfo.intervalMinutes = intervalMinutes;
				zoneInfo.enabled = enabled;
				
				for (Node spawnNode = zoneNode.getFirstChild(); spawnNode != null; spawnNode = spawnNode.getNextSibling())
				{
					if (!"spawn".equalsIgnoreCase(spawnNode.getNodeName()))
						continue;
					
					final NamedNodeMap sAttrs = spawnNode.getAttributes();
					final int mobId = Integer.parseInt(sAttrs.getNamedItem("mobId").getNodeValue());
					final int x = Integer.parseInt(sAttrs.getNamedItem("x").getNodeValue());
					final int y = Integer.parseInt(sAttrs.getNamedItem("y").getNodeValue());
					final int z = Integer.parseInt(sAttrs.getNamedItem("z").getNodeValue());
					
					zoneInfo.spawns.add(new MobSpawn(mobId, new Location(x, y, z)));
				}
				
				_zones.put(name, zoneInfo);
				LOGGER.info("TimeFarmZoneData: Loaded zone '{}' with {} spawns (enabled={}).", name, zoneInfo.spawns.size(), enabled);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("TimeFarmZoneData: Failed to load {}.", e, file.getAbsolutePath());
		}
	}
	
	public static void loadZonesFromFile()
	{
		final File file = new File("./data/xml/custom/TimeFarmZones.xml");
		if (file.exists())
			load(file);
		else
			LOGGER.warn("TimeFarmZoneData: File not found: {}", file.getAbsolutePath());
	}
	
	public static void save()
	{
		try
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			sb.append("<list>\n");
			
			for (ZoneInfo zone : _zones.values())
			{
				sb.append(String.format("\t<zone name=\"%s\" intervalMinutes=\"%d\" enabled=\"%b\">\n", zone.name, zone.intervalMinutes, zone.enabled));
				
				for (MobSpawn spawn : zone.spawns)
				{
					sb.append(String.format("\t\t<spawn mobId=\"%d\" x=\"%d\" y=\"%d\" z=\"%d\" />\n", spawn.mobId, spawn.loc.getX(), spawn.loc.getY(), spawn.loc.getZ()));
				}
				
				sb.append("\t</zone>\n");
			}
			
			sb.append("</list>\n");
			
			final File file = new File("./data/xml/custom/TimeFarmZones.xml");
			file.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(file))
			{
				writer.write(sb.toString());
			}
			
			LOGGER.info("TimeFarmZoneData: Saved {} zones to TimeFarmZones.xml.", _zones.size());
		}
		catch (Exception e)
		{
			LOGGER.error("TimeFarmZoneData: Failed to save TimeFarmZones.xml.", e);
		}
	}
	
	public static ZoneInfo getZoneInfo(String name)
	{
		return _zones.get(name);
	}
}
