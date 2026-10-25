package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.handler.ICustomByPassHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;



public class DressMe implements IVoicedCommandHandler, ICustomByPassHandler
{
	private static String[] _voicedCommands =
	{
		Config.DRESS_ME_COMMAND
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{

		
		if (command.startsWith(Config.DRESS_ME_COMMAND))
		{
			showHtm(activeChar);
		}
		

		return true;
	}
	
	private static void showHtm(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm("data/html/dressme/index.htm");
		
		htm.setHtml(text);
		
		{
			htm.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
			htm.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
			
		}
		
		player.sendPacket(htm);
	}
	
	static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"dressme_back"
		};
	}
	
	private enum CommandEnum
	{
		dressme_back,
	}
	
	@Override
	public void handleCommand(String command, Player player, String parameters)
	{
		CommandEnum comm = CommandEnum.valueOf(command);
		
		if (comm == null)
		{
			return;
		}
		
		switch (comm)
		{
			case dressme_back:
			{
				showHtm(player);
			}
				break;
		}
	}
}