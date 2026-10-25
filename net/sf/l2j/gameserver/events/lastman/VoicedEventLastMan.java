package net.sf.l2j.gameserver.events.lastman;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedEventLastMan implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"lminfo",
		"lmjoin",
		"lmleave",
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		
		if (command.equals("lminfo"))
		{
			if (LMEvent.isStarting() || LMEvent.isStarted())
			{
				showLMStatuPage(activeChar);
				return true;
			}
			activeChar.sendMessage("Last Man fight is not in progress.");
			return false;
		}
		else if (command.equalsIgnoreCase("lmjoin"))
		{
			if (!LMEvent.isPlayerParticipant(activeChar))
			{
				LMEvent.onBypass("lm_event_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are already registered.");
				return false;
			}
		}
		else if (command.equalsIgnoreCase("lmleave"))
		{
			if (LMEvent.isPlayerParticipant(activeChar))
			{
				LMEvent.onBypass("lm_event_remove_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are not registered.");
				return false;
			}
		}
		else if (command.equalsIgnoreCase("port"))
		{
			if (LMEvent.isPlayerParticipant(activeChar) && LMEvent.isStarted())
			{
				new LMEventTeleporter(activeChar, false, false);
			}
			else
			{
				activeChar.sendMessage("You are not registered.");
				return false;
			}
		}
		return true;
	}

	private static void showLMStatuPage(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		html.setFile("data/html/mods/events/lm/Status.htm");
		String htmltext = "";
		htmltext = String.valueOf(LMEvent.getPlayerCounts());
		html.replace("%countplayer%", htmltext);
		activeChar.sendPacket(html);
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}