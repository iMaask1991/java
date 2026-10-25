package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.RaidInfoBypassHandler;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Voiced command to open raid timers (.raid / .raids) without enabling Community Board.
 */
public class RaidInfoCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"raid",
		"raids",
		"raidboss", // interface ACDC
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (command.equalsIgnoreCase("raid") || command.equalsIgnoreCase("raids") || command.equalsIgnoreCase("raidboss"))
		{
			RaidInfoBypassHandler.show(player, "raidinfo;easy");
			return true;
		}
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
