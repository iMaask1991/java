package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatHeroVoice implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		17
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!activeChar.isHero())
			return;
		
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.HERO_VOICE))
			return;

		if (activeChar.ChatProtection(activeChar.getHWID()) && activeChar.isChatBlocked() && ((activeChar.getChatBanTimer() - 1500) > System.currentTimeMillis()))
		{
			if (((activeChar.getChatBanTimer() - System.currentTimeMillis()) / 1000) >= 60)
				activeChar.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (activeChar.getChatBanTimer() - System.currentTimeMillis()) / (1000 * 60) + " minuto(s).");
			else
				activeChar.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (activeChar.getChatBanTimer() - System.currentTimeMillis()) / 1000 + " segundo(s).");

			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		final CreatureSay csFake = activeChar.isEventDisguised() ? new CreatureSay(activeChar.getObjectId(), type, net.sf.l2j.gameserver.events.eventgear.EventGearConfig.FAKE_NAME, text) : null;
		for (Player player : World.getInstance().getPlayers())
			player.sendPacket((csFake != null && net.sf.l2j.gameserver.events.eventgear.EventGearManager.hideFor(activeChar, player)) ? csFake : cs);
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}