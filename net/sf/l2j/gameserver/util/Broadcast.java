package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public final class Broadcast
{
	private Broadcast()
	{
	}

	public static void gameAnnounceToOnlinePlayers(String text)
	{
		// AnnounceIdEvents (ModsHadesProject.properties master). 18 = SuperNotif.
		final CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", text);
		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
				player.sendPacket(cs);
		}
	}

	public static void gameAnnounceToAll(String text)
	{
		Announcements.Announce(text);
	}
	
	public static void announceToOnlinePlayers(String text)
	{
		gameAnnounceToOnlinePlayers(text);
	}
}
