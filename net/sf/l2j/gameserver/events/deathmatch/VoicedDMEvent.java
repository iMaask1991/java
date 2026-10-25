package net.sf.l2j.gameserver.events.deathmatch;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedDMEvent implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"dminfo",
		"dmjoin",
		"dmleave",
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equals("dminfo"))
		{
			if (DMEvent.isStarting() || DMEvent.isStarted())
			{
				showDMStatuPage(activeChar);
				return true;
			}
			activeChar.sendMessage("Deathmatch fight is not in progress.");
			return false;
		}
		else if (command.equalsIgnoreCase("dmjoin"))
		{
			if (!DMEvent.isPlayerParticipant(activeChar))
			{
				DMEvent.onBypass("dm_event_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are already registered.");
				return false;
			}
		}
		else if (command.equalsIgnoreCase("dmleave"))
		{
			if (DMEvent.isPlayerParticipant(activeChar))
			{
				DMEvent.onBypass("dm_event_remove_participation", activeChar);
			}
			else
			{
				activeChar.sendMessage("You are not registered.");
				return false;
			}
		}
		return true;
	}

	private static void showDMStatuPage(Player activeChar)
	{
		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
		final String htmContent = HtmCache.getInstance().getHtm("data/html/mods/events/dm/Status.htm");

		String[] topPositions;
		String htmltext = "";
		if (DMConfig.DM_SHOW_TOP_RANK)
		{
			topPositions = DMEvent.getFirstPosition(DMConfig.DM_TOP_RANK);
			Boolean c = true;
			String c1 = "LEVEL";
			String c2 = "FFFFFF";
			if (topPositions != null)
				for (int i = 0; i < topPositions.length; i++)
				{
					String color = (c ? c1 : c2);
					String[] row = topPositions[i].split("\\,");
					htmltext += "<tr>";
					htmltext += "<td width=\"35\" align=\"center\"><font color=\"" + color + "\">" + String.valueOf(i + 1) + "</font></td>";
					htmltext += "<td width=\"100\" align=\"left\"><font color=\"" + color + "\">" + row[0] + "</font></td>";
					htmltext += "<td width=\"125\" align=\"right\"><font color=\"" + color + "\">" + row[1] + "</font></td>";
					htmltext += "</tr>";
					c = !c;
				}
		}
		
    	if (htmContent != null)
    	{
			npcHtmlMessage.setHtml(htmContent);
			npcHtmlMessage.replace("%positions%", htmltext);
			activeChar.sendPacket(npcHtmlMessage);
    	}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}