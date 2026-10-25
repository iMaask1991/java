package net.sf.l2j.gameserver;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class Announcements
{
	private static Announcements _instance;
	

	
	
	public static Announcements getInstance()
	{
		if (_instance == null)
		{
			_instance = new Announcements();
		}
		return _instance;
	}
	
	public static void announceToPlayers(String message)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			player.sendMessage(message);
		}
	}
	
	public static void Announce(String text)
	{
		// AnnounceIdEvents (ModsHadesProject.properties master). 18 = SuperNotif.
		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && (player.isOnline()))
			{
				player.sendPacket(cs);
			}
		}
		cs = null;
	}
	
}
