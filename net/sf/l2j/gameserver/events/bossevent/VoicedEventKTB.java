package net.sf.l2j.gameserver.events.bossevent;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedEventKTB implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
		{
		"ktbinfo",
		"ktbjoin",
		"ktbleave"
		};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		
		if (command.equals("ktbinfo"))
		{
			if (KTBEvent.isStarting() || KTBEvent.isStarted())
			{
				showKTBStatuPage(activeChar);
				return true;
			}
			activeChar.sendMessage("Kill the Boss event is not in progress.");
			return false;
		}
		else if (command.equals("ktbjoin"))
		{
			if (!KTBEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				KTBEvent.onBypass("ktb_event_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are already registered.");
				return false;
			}
		}
		else if (command.equals("ktbleave"))
		{
			if (KTBEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				KTBEvent.onBypass("ktb_event_remove_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are not registered.");
				return false;
			}
		}

		return true;
	}

	public static void showKTBStatuPage(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		html.setFile("data/html/mods/events/ktb/Status.htm");
		String htmltext = "";
		htmltext = String.valueOf(KTBEvent.getPlayerCounts());
		html.replace("%countplayer%", htmltext);
		activeChar.sendPacket(html);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}