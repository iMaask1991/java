package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmManager;
import net.sf.l2j.gameserver.model.actor.Player;

public class VoicedInstanceFarmExit implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"exit"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!"exit".equalsIgnoreCase(command))
			return false;
		
		if (!Config.INSTANCE_FARM_ENABLED)
		{
			activeChar.sendMessage("Instance Farm is disabled.");
			return true;
		}
		
		if (!InstanceFarmManager.getInstance().isInRun(activeChar))
		{
			activeChar.sendMessage("You are not inside an Instance Farm. Use .exit only inside.");
			return true;
		}
		
		InstanceFarmManager.getInstance().leave(activeChar);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
