package net.sf.l2j.gameserver.events.rewardsoloevent;

import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminSoloEvent implements IAdminCommandHandler
{
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_soloevent"
	};
	
	protected static final Logger _log = Logger.getLogger(AdminSoloEvent.class.getName());
	public static boolean _bestfarm_manual = false;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		
		if (command.equals("admin_soloevent"))
		{
			if (RewardSoloEvent._started)
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Happy Hour]: Event Finished.");
				_log.info("----------------------------------------------------------------------------");
				// Cut boost immediately (don't wait for ThreadPool / waiter race).
				RewardSoloEvent._aborted = true;
				RewardSoloEvent._started = false;
				finishEventSpecialDrop();
				
				activeChar.sendMessage("SYS: Happy Hour finalizado manualmente.");
			}
			else
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Happy Hour]: Event Started.");
				_log.info("----------------------------------------------------------------------------");
				initEventSoloEvent();
				_bestfarm_manual = true;
				activeChar.sendMessage("SYS: Happy Hour activado manualmente.");
			}
		}
		return true;
		
	}
	
	private static void initEventSoloEvent()
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				
				RewardSoloEvent.StartedEvent();
			}
		}, 1L);
	}
	
	private static void finishEventSpecialDrop()
	{
		// Announce / schedule next only if Finish wasn't already done.
		if (!RewardSoloEvent.is_finish())
		{
			ThreadPool.schedule(new Runnable()
			{
				@Override
				public void run()
				{
					RewardSoloEvent.Finish_Event();
				}
			}, 1L);
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
