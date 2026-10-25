/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.tournament.ArenaTask;

/**
 * @author MeGaPacK
 */
public class AdminCustom implements IAdminCommandHandler
{
	public static final AdminCustom getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_tournament"
	};
	
	protected static final Logger _log = Logger.getLogger(AdminCustom.class.getName());
	
	public static boolean _arena_manual = false;
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_tournament"))
		{
			if (ArenaTask._started)
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Tournament: Event Finished.");
				_log.info("----------------------------------------------------------------------------");
				ArenaTask._aborted = true;
				finishEventArena();
				_arena_manual = true;
				
				activeChar.sendMessage("SYS: Finalizaste Tournament");
			}
			else
			{
				_log.info("----------------------------------------------------------------------------");
				_log.info("[Tournament]: Event Started.");
				_log.info("----------------------------------------------------------------------------");
				initEventArena();
				_arena_manual = true;
				activeChar.sendMessage("SYS: Activaste Tournament");
			}
		}
		
		return true;
	}
	
	private static void initEventArena()
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				net.sf.l2j.gameserver.tournament.ArenaTask.SpawnEvent();
			}
		}, 1);
	}
	
	private static void finishEventArena()
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				net.sf.l2j.gameserver.tournament.ArenaTask.finishEvent();
			}
		}, 1);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static class SingletonHolder
	{
		protected static final AdminCustom _instance = new AdminCustom();
	}
}
