package net.sf.l2j.gameserver.events.ctf;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedCTFEvent implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"ctfinfo",
		"ctfjoin",
		"ctfleave",
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equals("ctfinfo"))
		{
			if (CTFEvent.isStarting() || CTFEvent.isStarted())
			{
				showCTFStatuPage(activeChar);
				return true;
			}
			activeChar.sendMessage("Capture the Flag event is not in progress.");
			return false;
		}
		else if (command.equals("ctfjoin"))
		{
			if (!CTFEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				CTFEvent.onBypass("ctf_event_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are already registered.");
				return false;
			}
		}
		else if (command.equals("ctfleave"))
		{
			if (CTFEvent.isPlayerParticipant(activeChar.getObjectId()))
			{
				CTFEvent.onBypass("ctf_event_remove_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are not registered.");
				return false;
			}
		}
		return true;
	}

	private static void showCTFStatuPage(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
		html.setFile("data/html/mods/events/ctf/Status.htm");
		html.replace("%team1name%", CTFConfig.CTF_EVENT_TEAM_1_NAME);
		html.replace("%team1playercount%", String.valueOf(CTFEvent.getTeamsPlayerCounts()[0]));
		html.replace("%team1points%", String.valueOf(CTFEvent.getTeamsPoints()[0]));
		html.replace("%team2name%", CTFConfig.CTF_EVENT_TEAM_2_NAME);
		html.replace("%team2playercount%", String.valueOf(CTFEvent.getTeamsPlayerCounts()[1]));
		html.replace("%team2points%", String.valueOf(CTFEvent.getTeamsPoints()[1]));
		activeChar.sendPacket(html);	
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}