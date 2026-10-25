package net.sf.l2j.gameserver.model.actor.instance;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.timezone.TimeFarmZoneManager;

/**
 * An instance type extending {@link Folk}, used for teleporters.<br>
 * <br>
 * A teleporter allows {@link Player}s to teleport to a specific location, for a fee.
 */
public final class Gatekeeper extends Folk
{
	private static final long FARM_ZONES_CACHE_MS = 60_000L;
	private static volatile Properties _farmZones;
	private static volatile long _farmZonesAt;
	
	public Gatekeeper(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/teleporter/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		// Generic PK check. Send back the HTM if found and cancel current action.
		if (!Config.KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0 && showPkDenyChatWindow(player, "teleporter"))
			return;
		
		if (command.startsWith("timefarmzone"))
		{
			if (!Config.ENABLE_TIME_ZONE_SYSTEM)
			{
				player.sendMessage("Time Farm Zone is disabled.");
				return;
			}
			
			if (!canInteract(player))
				return;
			
			TimeFarmZoneManager.teleportToCurrentZone(player);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Random farm zone teleport (coords from config/custom/CustomZones.ini).
		if (command.startsWith("random_farmzone"))
		{
			if (!canInteract(player))
				return;
			
			String zone = command.substring("random_farmzone".length()).trim();
			if (zone.startsWith(";") || zone.startsWith(" "))
				zone = zone.substring(1).trim();
			
			final Location loc = getRandomFarmLocation(player, zone);
			if (loc != null)
				player.teleportTo(loc, 0);
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (command.startsWith("goto"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			// No more tokens.
			if (!st.hasMoreTokens())
				return;
			
			// No interaction possible with the NPC.
			if (!canInteract(player))
				return;
			
			// Retrieve the list.
			final TeleportLocation list = TeleportLocationData.getInstance().getTeleportLocation(Integer.parseInt(st.nextToken()));
			if (list == null)
				return;
			
			// Siege is currently in progress in this location.
			if (CastleManager.getInstance().getActiveSiege(list.getX(), list.getY(), list.getZ()) != null)
			{
				player.sendPacket(SystemMessageId.CANNOT_PORT_VILLAGE_IN_SIEGE);
				return;
			}
			
			// The list is for noble, but player isn't noble.
			if (list.isNoble() && !player.isNoble())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/teleporter/nobleteleporter-no.htm");
				html.replace("%objectId%", getObjectId());
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Retrieve price list. Potentially cut it by 2 depending of current date.
			int price = list.getPrice();
			
			if (!list.isNoble())
			{
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
					price /= 2;
			}
			
			// Delete related items, and if successful teleport the player to the location.
			if (player.destroyItemByItemId("Teleport ", (list.isNoble()) ? 6651 : 57, price, this, true))
				player.teleportTo(list, 20);
			
			player.sendPacket(new SystemMessage(SystemMessageId.WILL_BE_MOVED_INTERFACE));
						
						
			if (player.isAutoFarm())
				VoicedAutoFarm.stopAutoFarm(player, "teleport");
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			
			// Show half price HTM depending of current date. If not existing, use the regular "-1.htm".
			if (val == 1)
			{
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					
					String content = HtmCache.getInstance().getHtm("data/html/teleporter/half/" + getNpcId() + ".htm");
					if (content == null)
						content = HtmCache.getInstance().getHtmForce("data/html/teleporter/" + getNpcId() + "-1.htm");
					
					html.setHtml(content);
					html.replace("%objectId%", getObjectId());
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			showChatWindow(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		// Generic PK check. Send back the HTM if found and cancel current action.
		if (!Config.KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0 && showPkDenyChatWindow(player, "teleporter"))
			return;
		
		if (CommunityBoard.openCustomNpcBoard(player, getNpcId()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		showChatWindow(player, getHtmlPath(getNpcId(), val));
	}
	
	/**
	 * Picks a random farm coord from config/custom/CustomZones.ini.
	 * Zones: monastery (Monastery of Silence).
	 * NPC: npc_%objectId%_random_farmzone monastery
	 * CB: _bbsteleport;farmzone;monastery
	 */
	public static Location getRandomFarmLocation(Player player, String zoneName)
	{
		final String propertyKey = resolveFarmZoneKey(zoneName);
		if (propertyKey == null)
		{
			if (player != null)
				player.sendMessage("Invalid farm zone.");
			return null;
		}
		
		final Properties properties = loadFarmZones();
		if (properties == null)
		{
			if (player != null)
				player.sendMessage("Random farm zone config not found.");
			return null;
		}
		
		final String randomSpawns = properties.getProperty(propertyKey);
		if (randomSpawns == null || randomSpawns.isEmpty())
		{
			if (player != null)
				player.sendMessage(propertyKey + " farm zone is not configured.");
			return null;
		}
		
		final String[] spawnPoints = randomSpawns.split(";");
		final String selectedSpawn = spawnPoints[Rnd.get(spawnPoints.length)].trim();
		final String[] coords = selectedSpawn.split(",");
		if (coords.length < 3)
		{
			if (player != null)
				player.sendMessage("Invalid " + propertyKey + " farm zone format.");
			return null;
		}
		
		try
		{
			final int x = Integer.parseInt(coords[0].trim());
			final int y = Integer.parseInt(coords[1].trim());
			final int z = Integer.parseInt(coords[2].trim());
			return new Location(x, y, z);
		}
		catch (NumberFormatException e)
		{
			if (player != null)
				player.sendMessage("Invalid " + propertyKey + " farm zone coordinates.");
			return null;
		}
	}
	
	private static Properties loadFarmZones()
	{
		final long now = System.currentTimeMillis();
		final Properties cached = _farmZones;
		if (cached != null && now - _farmZonesAt < FARM_ZONES_CACHE_MS)
			return cached;
		
		synchronized (Gatekeeper.class)
		{
			if (_farmZones != null && System.currentTimeMillis() - _farmZonesAt < FARM_ZONES_CACHE_MS)
				return _farmZones;
			
			final Properties properties = new Properties();
			try (FileInputStream fis = new FileInputStream("./config/custom/CustomZones.ini"))
			{
				properties.load(fis);
			}
			catch (IOException e)
			{
				return null;
			}
			
			_farmZones = properties;
			_farmZonesAt = System.currentTimeMillis();
			return properties;
		}
	}
	
	private static String resolveFarmZoneKey(String zoneName)
	{
		if (zoneName == null || zoneName.isEmpty())
			return null;
		
		switch (zoneName.toLowerCase())
		{
			case "monastery":
			case "monasteryofsilence":
				return "Monastery";
			default:
				return null;
		}
	}
}