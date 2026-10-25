package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.CommandChannel;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyRoomAll implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		16
	};
	
	@Override
	public void handleChat(int type, Player player, String target, String text)
	{
		final Party party = player.getParty();
		if (party == null || !party.isLeader(player))
			return;
		
		final CommandChannel channel = party.getCommandChannel();
		if (channel == null)
			return;

		if (player.ChatProtection(player.getHWID()) && player.isChatBlocked() && ((player.getChatBanTimer() - 1500) > System.currentTimeMillis()))
		{
			if (((player.getChatBanTimer() - System.currentTimeMillis()) / 1000) >= 60)
				player.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (player.getChatBanTimer() - System.currentTimeMillis()) / (1000 * 60) + " minuto(s).");
			else
				player.sendChatMessage(0, Say2.TELL, "SYS", "Tu chat fue suspendido por " + (player.getChatBanTimer() - System.currentTimeMillis()) / 1000 + " segundo(s).");

			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		channel.broadcastCreatureSay(new CreatureSay(player.getObjectId(), type, player.getName(), text), player);
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}