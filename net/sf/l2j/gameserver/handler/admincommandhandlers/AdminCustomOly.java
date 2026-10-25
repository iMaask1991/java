package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.customoly.CustomOlyEvent;
import net.sf.l2j.gameserver.customoly.CustomOlyManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminCustomOly implements IAdminCommandHandler
{
	private static final String[] COMMANDS =
	{
		"admin_customoly",
		"admin_customoly_start",
		"admin_customoly_stop",
		"admin_customoly_season",
		"admin_customoly_heroes",
		"admin_customoly_reset"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_customoly"))
		{
			activeChar.sendMessage("Custom Oly: //customoly_start | _stop | _season | _heroes | _reset");
			activeChar.sendMessage("Phase: " + CustomOlyManager.getInstance().getPhaseText());
		}
		else if (command.equals("admin_customoly_start"))
		{
			CustomOlyEvent.getInstance().forceStart();
			activeChar.sendMessage("Custom Oly: ventana NPC abierta.");
		}
		else if (command.equals("admin_customoly_stop"))
		{
			CustomOlyEvent.getInstance().forceStop();
			activeChar.sendMessage("Custom Oly: ventana NPC cerrada.");
		}
		else if (command.equals("admin_customoly_season"))
		{
			CustomOlyManager.getInstance().startSeason();
			activeChar.sendMessage("Custom Oly: nueva season.");
		}
		else if (command.equals("admin_customoly_heroes"))
		{
			CustomOlyManager.getInstance().giveHeroes();
			activeChar.sendMessage("Custom Oly: Heroes entregados ahora.");
		}
		else if (command.equals("admin_customoly_reset"))
		{
			CustomOlyManager.getInstance().resetPoints();
			activeChar.sendMessage("Custom Oly: puntos reseteados.");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
}
