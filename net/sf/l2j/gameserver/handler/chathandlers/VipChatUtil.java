package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.BlockList;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public final class VipChatUtil
{
	private VipChatUtil()
	{
	}
	
	public static String getChatName(Player player)
	{
		if (Config.ENABLE_VIP_CHAT_PREFIX && player.isVip())
			return Config.VIP_CHAT_PREFIX + player.getName();
		
		return player.getName();
	}
	
	public static void broadcast(Player activeChar, int type, String text)
	{
		// objectId 0 if GM is hidden: client hides !/+ from stealthed chars (including yourself).
		final int objectId = (activeChar.isGM() && activeChar.getAppearance().getInvisible()) ? 0 : activeChar.getObjectId();
		final CreatureSay cs = new CreatureSay(objectId, type, getChatName(activeChar), text);
		final CreatureSay csFake = activeChar.isEventDisguised() ? new CreatureSay(objectId, type, net.sf.l2j.gameserver.events.eventgear.EventGearConfig.FAKE_NAME, text) : null;
		final boolean global = (Config.ENABLE_VIP_CHAT_PREFIX && activeChar.isVip() && Config.VIP_CHAT_GLOBAL) || Config.GLOBAL_SHOUT_TRADE_CHAT;
		
		if (global)
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if (!BlockList.isInBlockList(player, activeChar))
					player.sendPacket((csFake != null && net.sf.l2j.gameserver.events.eventgear.EventGearManager.hideFor(activeChar, player)) ? csFake : cs);
			}
			return;
		}
		
		final int region = MapRegionData.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());
		for (Player player : World.getInstance().getPlayers())
		{
			if (!BlockList.isInBlockList(player, activeChar) && region == MapRegionData.getInstance().getMapRegion(player.getX(), player.getY()))
				player.sendPacket((csFake != null && net.sf.l2j.gameserver.events.eventgear.EventGearManager.hideFor(activeChar, player)) ? csFake : cs);
		}
	}
}
