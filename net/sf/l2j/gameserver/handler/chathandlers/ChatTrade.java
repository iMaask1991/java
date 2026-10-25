package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;

public class ChatTrade implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		8
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.TRADE_CHAT))
		{
			activeChar.sendMessage("Espera un momento antes de volver a usar el chat trade.");
			return;
		}
		
		if (activeChar.isHwidChatBanned())
		{
			if (((activeChar.getChatBanTimer() - System.currentTimeMillis()) / 1000) >= 60)
				activeChar.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (activeChar.getChatBanTimer() - System.currentTimeMillis()) / (1000 * 60) + " minuto(s).");
			else
				activeChar.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (activeChar.getChatBanTimer() - System.currentTimeMillis()) / 1000 + " segundo(s).");

			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
	      
		VipChatUtil.broadcast(activeChar, type, text);
	}
	 	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}