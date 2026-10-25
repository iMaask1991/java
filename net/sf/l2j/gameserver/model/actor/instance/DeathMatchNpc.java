package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.events.deathmatch.DMConfig;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class DeathMatchNpc extends Npc
{
	private static final String DM_HTML_PATH = "data/html/mods/events/dm/";

	public DeathMatchNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player playerInstance, String command)
	{
		DMEvent.onBypass(command, playerInstance);
	}

	@Override
	public void showChatWindow(Player playerInstance, int val)
	{
		if (playerInstance == null)
			return;

		if (DMEvent.isParticipating())
		{
			final boolean isParticipant = DMEvent.isPlayerParticipant(playerInstance.getObjectId());
			final String htmContent;

			if (!isParticipant)
				htmContent = HtmCache.getInstance().getHtm(DM_HTML_PATH + "Participation.htm");
			else
				htmContent = HtmCache.getInstance().getHtm(DM_HTML_PATH + "RemoveParticipation.htm");

			if (htmContent != null)
			{
				final int playerCounts = DMEvent.getPlayerCounts();
				final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(playerCounts));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", DMEvent.getParticipationFee());

				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		else if (DMEvent.isStarting() || DMEvent.isStarted())
		{
			final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			final String htmContent = HtmCache.getInstance().getHtm(DM_HTML_PATH + "Status.htm");

			String htmltext = "";
			if (DMConfig.DM_SHOW_TOP_RANK)
			{
				final String[] topPositions = DMEvent.getFirstPosition(DMConfig.DM_TOP_RANK);
				if (topPositions != null)
				{
					boolean c = true;
					final String c1 = "LEVEL";
					final String c2 = "FFFFFF";
					for (int i = 0; i < topPositions.length; i++)
					{
						final String color = (c ? c1 : c2);
						final String[] row = topPositions[i].split("\\,");
						htmltext += "<tr>";
						htmltext += "<td width=\"35\" align=\"center\"><font color=\"" + color + "\">" + String.valueOf(i + 1) + "</font></td>";
						htmltext += "<td width=\"100\" align=\"left\"><font color=\"" + color + "\">" + row[0] + "</font></td>";
						htmltext += "<td width=\"125\" align=\"right\"><font color=\"" + color + "\">" + row[1] + "</font></td>";
						htmltext += "</tr>";
						c = !c;
					}
				}
			}

			if (htmContent != null)
			{
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%positions%", htmltext);
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}

		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
