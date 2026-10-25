package net.sf.l2j.gameserver.handler.bypasshandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.ICustomByPassHandler;
import net.sf.l2j.gameserver.model.actor.NpcDropViewer;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Pagination bypass for shift+click drop viewer.
 * Usage: bypass -h custom_droplist &lt;npcId&gt; &lt;page&gt;
 */
public final class NpcDropBypass implements ICustomByPassHandler
{
	private static final String[] COMMANDS =
	{
		"droplist"
	};
	
	@Override
	public String[] getByPassCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public void handleCommand(String command, Player player, String parameters)
	{
		if (player == null || parameters == null || parameters.isEmpty())
			return;
		
		final StringTokenizer st = new StringTokenizer(parameters);
		if (!st.hasMoreTokens())
			return;
		
		final int npcId = parseInt(st.nextToken(), 0);
		final int page = st.hasMoreTokens() ? parseInt(st.nextToken(), 1) : 1;
		if (npcId <= 0)
			return;
		
		NpcDropViewer.show(player, npcId, page);
	}
	
	private static int parseInt(String value, int def)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
			return def;
		}
	}
}
