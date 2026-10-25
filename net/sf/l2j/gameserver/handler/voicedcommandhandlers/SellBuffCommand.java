package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.SellBuffsManager;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Voiced commands for the Sell Buffs system (.sellbuff / .sellbuffs).
 */
public class SellBuffCommand implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"sellbuff",
		"sellbuffs",
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (!Config.SELLBUFF_ENABLED)
		{
			player.sendMessage("Sell Buffs system is disabled.");
			return false;
		}
		
		if (command.equalsIgnoreCase("sellbuff") || command.equalsIgnoreCase("sellbuffs"))
		{
			SellBuffsManager.getInstance().sendSellMenu(player);
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
