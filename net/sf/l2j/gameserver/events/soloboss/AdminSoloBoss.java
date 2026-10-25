package net.sf.l2j.gameserver.events.soloboss;

import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminSoloBoss implements IAdminCommandHandler
{
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_soloboss"
	};
	
	protected static final Logger _log = Logger.getLogger(AdminSoloBoss.class.getName());
	public static boolean _bestfarm_manual = false;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		
		if (command.equals("admin_soloboss"))
		{
			if (SoloBoss._started)
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Solo Boss]: Event Finished.");
				_log.info("----------------------------------------------------------------------------");
				SoloBoss._aborted = true;
				finishEventSoloBoss();
				
				activeChar.sendMessage("SYS: Voce Finalizou o Solo Boss Manualmente..");
			}
			else
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Solo Boss]: Event Started.");
				_log.info("----------------------------------------------------------------------------");
				initEventSoloBoss();
				_bestfarm_manual = true;
				activeChar.sendMessage("SYS: Voce ativou o Solo Boss Manualmente..");
			}
		}
		return true;
		
	}
	
	private static void initEventSoloBoss()
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				
				SoloBoss.bossSpawnMonster();
			}
		}, 1L);
	}
	
	private static void finishEventSoloBoss()
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				
				SoloBoss.Finish_Event();
				
			}
		}, 1L);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}