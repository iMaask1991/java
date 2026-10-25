package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.AutoPotManager;
import net.sf.l2j.gameserver.model.actor.Player;

public class VoicedAutoPot implements IVoicedCommandHandler
{
	private static final String[] COMMANDS =
	{
		"on",
		"off"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (activeChar == null)
			return false;
		
		if (command.equalsIgnoreCase("on"))
			AutoPotManager.start(activeChar);
		else if (command.equalsIgnoreCase("off"))
			AutoPotManager.stop(activeChar);
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return COMMANDS;
	}
}
