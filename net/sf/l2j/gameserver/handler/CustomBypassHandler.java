
package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.handler.bypasshandlers.NpcDropBypass;
import net.sf.l2j.gameserver.handler.usercommandhandlers.DressMe;
import net.sf.l2j.gameserver.model.actor.Player;



public class CustomBypassHandler
{

	
	private static CustomBypassHandler _instance = null;
	private final Map<String, ICustomByPassHandler> _handlers;
	
	private CustomBypassHandler()
	{
		_handlers = new HashMap<>();
		
		
		registerCustomBypassHandler(new DressMe());
		registerCustomBypassHandler(new NpcDropBypass());
	}
	
	public static CustomBypassHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new CustomBypassHandler();
		}
		
		return _instance;
	}
	
	public void registerCustomBypassHandler(final ICustomByPassHandler handler)
	{
		for (final String s : handler.getByPassCommands())
		{
			_handlers.put(s, handler);
		}
	}
	
	public void handleBypass(final Player player, final String command)
	{
		String cmd = "";
		String params = "";
		final int iPos = command.indexOf(" ");
		if (iPos != -1)
		{
			cmd = command.substring(7, iPos);
			params = command.substring(iPos + 1);
		}
		else
		{
			cmd = command.substring(7);
		}
		final ICustomByPassHandler ch = _handlers.get(cmd);
		if (ch != null)
		{
			ch.handleCommand(cmd, player, params);
		}
		else
		{

		}
	}
}