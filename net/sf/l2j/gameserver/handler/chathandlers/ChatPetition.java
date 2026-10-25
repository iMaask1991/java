package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.data.manager.PetitionManager;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;

public class ChatPetition implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		6,
		7
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT);
			return;
		}

		if (activeChar.ChatProtection(activeChar.getHWID()) && activeChar.isChatBlocked() && ((activeChar.getChatBanTimer() - 1500) > System.currentTimeMillis()))
		{
			if (((activeChar.getChatBanTimer() - System.currentTimeMillis()) / 1000) >= 60)
				activeChar.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (activeChar.getChatBanTimer() - System.currentTimeMillis()) / (1000 * 60) + " minuto(s).");
			else
				activeChar.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (activeChar.getChatBanTimer() - System.currentTimeMillis()) / 1000 + " segundo(s).");

			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		PetitionManager.getInstance().sendActivePetitionMessage(activeChar, text);
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}