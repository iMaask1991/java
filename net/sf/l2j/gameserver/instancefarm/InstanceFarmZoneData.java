package net.sf.l2j.gameserver.instancefarm;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.Config;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.model.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Lee las zonas KILL de InstanceFarmZones.xml.<br>
 * //loc_kill y //loc_kill_delete editan el archivo como TEXTO (agregan / sacan una linea &lt;spawn&gt;),
 * asi no se pierden los comentarios ni el formato.
 */
public final class InstanceFarmZoneData
{
	private static final CLogger LOGGER = new CLogger(InstanceFarmZoneData.class.getName());
	
	private static final Pattern ATTR = Pattern.compile("([A-Za-z_]+)\\s*=\\s*\"([^\"]*)\"");
	
	private static volatile Map<Integer, InstanceFarmZone> _zones = new LinkedHashMap<>();
	private static volatile Map<Integer, InstanceFarmTimeZoneDef> _timeZones = new LinkedHashMap<>();
	
	private InstanceFarmZoneData()
	{
	}
	
	public static synchronized void load()
	{
		final Map<Integer, InstanceFarmZone> zones = new LinkedHashMap<>();
		final Map<Integer, InstanceFarmTimeZoneDef> timeZones = new LinkedHashMap<>();
		
		final File file = new File(Config.INSTANCE_FARM_ZONES_FILE);
		if (!file.exists())
		{
			LOGGER.warn("InstanceFarmZoneData: no existe {}.", Config.INSTANCE_FARM_ZONES_FILE);
			_zones = zones;
			_timeZones = timeZones;
			return;
		}
		
		try
		{
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (!"list".equalsIgnoreCase(n.getNodeName()))
					continue;
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("time".equalsIgnoreCase(d.getNodeName()))
					{
						final InstanceFarmTimeZoneDef def = parseTime(d);
						if (def != null)
							timeZones.put(def.getId(), def);
						continue;
					}
					
					if (!"kill".equalsIgnoreCase(d.getNodeName()) && !"zone".equalsIgnoreCase(d.getNodeName()))
						continue;
					
					final NamedNodeMap attrs = d.getAttributes();
					final int id = parseInt(attrs, "id", -1);
					if (id < 0)
						continue;
					
					final String type = parseString(attrs, "type", "KILL");
					if (!"KILL".equalsIgnoreCase(type))
					{
						LOGGER.warn("InstanceFarmZoneData: zona {} tipo {} ignorada (usa <time> o <kill>).", id, type);
						continue;
					}
					
					final String name = parseString(attrs, "name", "Kill Farm " + id);
					final boolean enabled = Boolean.parseBoolean(parseString(attrs, "enabled", "true"));
					final int x = parseInt(attrs, "x", parseInt(attrs, "centerX", 0));
					final int y = parseInt(attrs, "y", parseInt(attrs, "centerY", 0));
					final int z = parseInt(attrs, "z", parseInt(attrs, "centerZ", 0));
					final int respawn = Math.max(1, parseInt(attrs, "respawnSeconds", 5));
					final int defaultMobId = parseInt(attrs, "defaultMobId", parseInt(attrs, "mobId", 0));
					final int minLevel = Math.max(1, parseInt(attrs, "minLevel", 1));
					
					zones.put(id, new InstanceFarmZone(id, name, enabled, new Location(x, y, z), respawn, defaultMobId, minLevel, parseSpawns(d, id, defaultMobId)));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmZoneData: error leyendo {}.", e, Config.INSTANCE_FARM_ZONES_FILE);
		}
		
		_zones = zones;
		_timeZones = timeZones;
		for (InstanceFarmTimeZoneDef def : timeZones.values())
			LOGGER.info("InstanceFarmZoneData: TIME zona {} '{}' con {} spawn(s){}.", def.getId(), def.getName(), def.getSpawns().size(), def.isEnabled() ? "" : " (desactivada)");
		for (InstanceFarmZone zone : zones.values())
			LOGGER.info("InstanceFarmZoneData: KILL zona {} '{}' con {} spawn(s){}.", zone.getId(), zone.getName(), zone.getSpawns().size(), zone.isEnabled() ? "" : " (desactivada)");
	}
	
	private static InstanceFarmTimeZoneDef parseTime(Node d)
	{
		final NamedNodeMap attrs = d.getAttributes();
		final int id = parseInt(attrs, "id", -1);
		if (id < 0)
			return null;
		
		final String name = parseString(attrs, "name", "Time Farm " + id);
		final boolean enabled = Boolean.parseBoolean(parseString(attrs, "enabled", "true"));
		final int x = parseInt(attrs, "x", 0);
		final int y = parseInt(attrs, "y", 0);
		final int z = parseInt(attrs, "z", 0);
		final int radius = parseInt(attrs, "radius", 0);
		final int minZ = parseInt(attrs, "minZ", z - 1000);
		final int maxZ = parseInt(attrs, "maxZ", z + 1000);
		final int minLevel = Math.max(1, parseInt(attrs, "minLevel", 1));
		final boolean inviteParty = Boolean.parseBoolean(parseString(attrs, "inviteParty", "false"));
		final boolean noDropProtection = Boolean.parseBoolean(parseString(attrs, "noDropProtection", "false"));
		final boolean loseBuff = Boolean.parseBoolean(parseString(attrs, "loseBuff", "true"));
		final int respawn = Math.max(1, parseInt(attrs, "respawnSeconds", 30));
		final int defaultMobId = parseInt(attrs, "defaultMobId", 0);
		
		final java.util.Set<Integer> blocked = new java.util.HashSet<>();
		for (String classId : parseString(attrs, "blockedClasses", "").split("[;,]"))
		{
			try
			{
				if (!classId.trim().isEmpty())
					blocked.add(Integer.parseInt(classId.trim()));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn("InstanceFarmZoneData: TIME zona {} blockedClasses '{}' invalido.", id, classId);
			}
		}
		
		final List<int[]> nodes = new ArrayList<>();
		final List<Location> enters = new ArrayList<>();
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			final NamedNodeMap ca = c.getAttributes();
			if ("node".equalsIgnoreCase(c.getNodeName()))
				nodes.add(new int[]
				{
					parseInt(ca, "x", 0),
					parseInt(ca, "y", 0)
				});
			else if ("enter".equalsIgnoreCase(c.getNodeName()))
				enters.add(new Location(parseInt(ca, "x", 0), parseInt(ca, "y", 0), parseInt(ca, "z", 0)));
		}
		
		if (nodes.size() < 3 && radius <= 0)
		{
			LOGGER.warn("InstanceFarmZoneData: TIME zona {} sin forma (pone radius=\"...\" o 3+ <node>), se ignora.", id);
			return null;
		}
		
		if (enters.isEmpty())
			enters.add(new Location(x, y, z));
		
		return new InstanceFarmTimeZoneDef(id, name, enabled, new Location(x, y, z), radius, minZ, maxZ, nodes, minLevel, inviteParty, noDropProtection, loseBuff, blocked, enters, respawn, defaultMobId, parseSpawns(d, id, defaultMobId));
	}
	
	private static List<InstanceFarmZone.KillSpawn> parseSpawns(Node d, int id, int defaultMobId)
	{
		final List<InstanceFarmZone.KillSpawn> spawns = new ArrayList<>();
		for (Node s = d.getFirstChild(); s != null; s = s.getNextSibling())
		{
			if (!"spawn".equalsIgnoreCase(s.getNodeName()))
				continue;
			
			final NamedNodeMap sa = s.getAttributes();
			int mobId = parseInt(sa, "mobId", 0);
			if (mobId <= 0)
				mobId = defaultMobId;
			
			if (mobId <= 0)
			{
				LOGGER.warn("InstanceFarmZoneData: zona {} tiene un spawn sin mobId y sin defaultMobId, se ignora.", id);
				continue;
			}
			
			spawns.add(new InstanceFarmZone.KillSpawn(mobId, parseInt(sa, "x", 0), parseInt(sa, "y", 0), parseInt(sa, "z", 0), parseInt(sa, "heading", 0)));
		}
		return spawns;
	}
	
	public static InstanceFarmTimeZoneDef getTimeZone(int id)
	{
		return _timeZones.get(id);
	}
	
	public static Collection<InstanceFarmTimeZoneDef> getTimeZones()
	{
		return _timeZones.values();
	}
	
	public static InstanceFarmZone getZone(int id)
	{
		return _zones.get(id);
	}
	
	public static Collection<InstanceFarmZone> getZones()
	{
		return _zones.values();
	}
	
	/**
	 * Agrega una linea &lt;spawn&gt; dentro de la zona y recarga.
	 * @return null si salio bien, o el error.
	 */
	public static synchronized String addSpawn(boolean time, int zoneId, int mobId, int x, int y, int z, int heading)
	{
		try
		{
			final File file = new File(Config.INSTANCE_FARM_ZONES_FILE);
			String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			final String nl = text.contains("\r\n") ? "\r\n" : "\n";
			
			final Matcher m = findZone(text, time, zoneId);
			if (m == null)
				return "No encontre la zona " + (time ? "TIME " : "KILL ") + zoneId + " en el XML.";
			
			final String line = "\t\t<spawn mobId=\"" + mobId + "\" x=\"" + x + "\" y=\"" + y + "\" z=\"" + z + "\" heading=\"" + heading + "\" />";
			
			if (m.group(0).endsWith("/>"))
			{
				// <zone ... />  ->  <zone ...> spawn </zone>
				final String open = m.group(0).substring(0, m.group(0).length() - 2).replaceAll("\\s+$", "") + ">";
				text = text.substring(0, m.start()) + open + nl + line + nl + "\t</" + m.group(1) + ">" + text.substring(m.end());
			}
			else
			{
				final int close = text.indexOf("</" + m.group(1) + ">", m.end());
				if (close < 0)
					return "La zona " + zoneId + " no tiene </" + m.group(1) + ">.";
				
				final int lineStart = text.lastIndexOf('\n', close) + 1;
				text = text.substring(0, lineStart) + line + nl + text.substring(lineStart);
			}
			
			Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmZoneData: error guardando spawn.", e);
			return "Error escribiendo el XML: " + e.getMessage();
		}
		
		load();
		return null;
	}
	
	/**
	 * Saca la linea &lt;spawn&gt; con esas coordenadas de la zona y recarga.
	 * @return null si salio bien, o el error.
	 */
	public static synchronized String removeSpawn(boolean time, int zoneId, int x, int y, int z)
	{
		try
		{
			final File file = new File(Config.INSTANCE_FARM_ZONES_FILE);
			final String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			
			final Matcher m = findZone(text, time, zoneId);
			if (m == null || m.group(0).endsWith("/>"))
				return "La zona " + zoneId + " no tiene spawns en el XML.";
			
			final int close = text.indexOf("</" + m.group(1) + ">", m.end());
			if (close < 0)
				return "La zona " + zoneId + " no tiene </" + m.group(1) + ">.";
			
			int pos = m.end();
			while (true)
			{
				final int start = text.indexOf("<spawn", pos);
				if (start < 0 || start > close)
					break;
				
				final int end = text.indexOf('>', start);
				if (end < 0)
					break;
				
				final String tag = text.substring(start, end + 1);
				final Map<String, String> attrs = new LinkedHashMap<>();
				final Matcher am = ATTR.matcher(tag);
				while (am.find())
					attrs.put(am.group(1).toLowerCase(), am.group(2));
				
				if (String.valueOf(x).equals(attrs.get("x")) && String.valueOf(y).equals(attrs.get("y")) && String.valueOf(z).equals(attrs.get("z")))
				{
					// Borra la linea entera (indentacion + salto de linea).
					int lineStart = text.lastIndexOf('\n', start) + 1;
					if (!text.substring(lineStart, start).trim().isEmpty())
						lineStart = start;
					
					int lineEnd = end + 1;
					final int nextNl = text.indexOf('\n', lineEnd);
					if (nextNl >= 0 && text.substring(lineEnd, nextNl).trim().isEmpty())
						lineEnd = nextNl + 1;
					
					final String result = text.substring(0, lineStart) + text.substring(lineEnd);
					Files.write(file.toPath(), result.getBytes(StandardCharsets.UTF_8));
					load();
					return null;
				}
				pos = end + 1;
			}
		}
		catch (Exception e)
		{
			LOGGER.error("InstanceFarmZoneData: error borrando spawn.", e);
			return "Error escribiendo el XML: " + e.getMessage();
		}
		
		return "No encontre ese spawn en el XML.";
	}
	
	private static Matcher findZone(String text, boolean time, int zoneId)
	{
		final Matcher m = Pattern.compile(time ? "<(time)\\b[^>]*>" : "<(kill|zone)\\b[^>]*>", Pattern.DOTALL).matcher(text);
		while (m.find())
		{
			final Matcher am = ATTR.matcher(m.group(0));
			while (am.find())
			{
				if (am.group(1).equalsIgnoreCase("id") && am.group(2).trim().equals(String.valueOf(zoneId)))
				{
					// Que no este comentado.
					final int commentOpen = text.lastIndexOf("<!--", m.start());
					final int commentClose = text.lastIndexOf("-->", m.start());
					if (commentOpen > commentClose)
						break;
					return m;
				}
			}
		}
		return null;
	}
	
	private static String parseString(NamedNodeMap attrs, String name, String fallback)
	{
		final Node node = attrs.getNamedItem(name);
		return node == null ? fallback : node.getNodeValue().trim();
	}
	
	private static int parseInt(NamedNodeMap attrs, String name, int fallback)
	{
		final Node node = attrs.getNamedItem(name);
		if (node == null)
			return fallback;
		
		try
		{
			return Integer.parseInt(node.getNodeValue().trim());
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
