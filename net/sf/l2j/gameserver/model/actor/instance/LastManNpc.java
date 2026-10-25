package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class LastManNpc extends Npc
{
	private static final String LM_HTML_PATH = "data/html/mods/events/lm/";

	public LastManNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player playerInstance, String command)
	{
		LMEvent.onBypass(command, playerInstance);
	}

	@Override
	public void showChatWindow(Player playerInstance, int val)
	{
		if (playerInstance == null)
			return;

		if (LMEvent.isParticipating())
		{
			final boolean isParticipant = LMEvent.isPlayerParticipant(playerInstance.getObjectId());
			final String htmContent;

			if (!isParticipant)
				htmContent = HtmCache.getInstance().getHtm(LM_HTML_PATH + "Participation.htm");
			else
				htmContent = HtmCache.getInstance().getHtm(LM_HTML_PATH + "RemoveParticipation.htm");

			if (htmContent != null)
			{
				final int playerCounts = LMEvent.getPlayerCounts();
				final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(playerCounts));
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		else if (LMEvent.isStarting() || LMEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(LM_HTML_PATH + "Status.htm");

			if (htmContent != null)
			{
				final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%countplayer%", String.valueOf(LMEvent.getPlayerCounts()));
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
