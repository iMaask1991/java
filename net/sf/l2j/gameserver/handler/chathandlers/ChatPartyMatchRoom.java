package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyMatchRoom implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		14
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!activeChar.isInPartyMatchRoom())
			return;
		
		final PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
		if (room == null)
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
		for (Player member : room.getPartyMembers())
			member.sendPacket(cs);
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}