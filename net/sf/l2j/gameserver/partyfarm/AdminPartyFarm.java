package net.sf.l2j.gameserver.partyfarm;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminPartyFarm implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_partyfarm"
	};
	
	protected static final Logger _log = Logger.getLogger(AdminPartyFarm.class.getName());
	public static boolean _partyfarm_manual = false;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (!command.equals("admin_partyfarm"))
			return false;
		
		if (PartyFarm._started)
		{
			_log.info(Config.NAME_EVENT + ": Event finished manually.");
			PartyFarm._aborted = true;
			finishEventPartyFarm();
			_partyfarm_manual = true;
			activeChar.sendMessage("SYS: Finalizaste " + Config.NAME_EVENT);
		}
		else
		{
			_log.info(Config.NAME_EVENT + ": Event started manually.");
			initEventPartyFarm();
			_partyfarm_manual = true;
			activeChar.sendMessage("SYS: Activaste " + Config.NAME_EVENT);
		}
		return true;
	}
	
	private static void initEventPartyFarm()
	{
		ThreadPool.schedule(() -> PartyFarm.startEvent(), 1L);
	}
	
	private static void finishEventPartyFarm()
	{
		ThreadPool.schedule(() -> PartyFarm.finishEvent(), 1L);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
