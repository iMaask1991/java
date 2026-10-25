package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.model.actor.Player;

public interface ICustomByPassHandler
{
	public String[] getByPassCommands();
	
	public void handleCommand(String command, Player player, String parameters);
}
