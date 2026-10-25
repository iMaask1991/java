package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Online implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"online"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{

		if (command.equals("online") && Config.ENABLE_ONLINE)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1);
			sm = new SystemMessage(SystemMessageId.S1);
			sm.addString("======<Online Players>======");
			activeChar.sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.S1);
			activeChar.sendMessage(new StringBuilder().append("There are ").append(World.getInstance().getPlayers().size() * 1 + Config.PLAYERS_ONLINE_TRICK).toString() + " players online.");
			activeChar.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.S1);
			sm.addString("=======================");
			activeChar.sendPacket(sm);
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}