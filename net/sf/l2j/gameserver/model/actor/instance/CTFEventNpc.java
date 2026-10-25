package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.events.ctf.CTFConfig;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class CTFEventNpc extends Npc
{
	private static final String CTF_HTML_PATH = "data/html/mods/events/ctf/";

	public CTFEventNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player playerInstance, String command)
	{
		CTFEvent.onBypass(command, playerInstance);
	}

	@Override
	public void showChatWindow(Player playerInstance, int val)
	{
		if (playerInstance == null)
			return;

		if (CTFEvent.isParticipating())
		{
			final boolean isParticipant = CTFEvent.isPlayerParticipant(playerInstance.getObjectId());
			final String htmContent;

			if (!isParticipant)
				htmContent = HtmCache.getInstance().getHtm(CTF_HTML_PATH + "Participation.htm");
			else
				htmContent = HtmCache.getInstance().getHtm(CTF_HTML_PATH + "RemoveParticipation.htm");

			if (htmContent != null)
			{
				final int[] teamsPlayerCounts = CTFEvent.getTeamsPlayerCounts();
				final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", CTFConfig.CTF_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", CTFConfig.CTF_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1]));
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		else if (CTFEvent.isStarting() || CTFEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(CTF_HTML_PATH + "Status.htm");

			if (htmContent != null)
			{
				final int[] teamsPlayerCounts = CTFEvent.getTeamsPlayerCounts();
				final int[] teamsPointsCounts = CTFEvent.getTeamsPoints();
				final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%team1name%", CTFConfig.CTF_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", CTFConfig.CTF_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1]));
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
