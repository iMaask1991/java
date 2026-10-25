package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class BossEventNpc extends Npc
{
	private static final String KTB_HTML_PATH = "data/html/mods/events/ktb/";

	public BossEventNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player playerInstance, String command)
	{
		KTBEvent.onBypass(command, playerInstance);
	}

	@Override
	public void showChatWindow(Player playerInstance, int val)
	{
		if (playerInstance == null)
			return;

		if (KTBEvent.isParticipating())
		{
			final boolean isParticipant = KTBEvent.isPlayerParticipant(playerInstance.getObjectId());
			final String htmContent;

			if (!isParticipant)
				htmContent = HtmCache.getInstance().getHtm(KTB_HTML_PATH + "Participation.htm");
			else
				htmContent = HtmCache.getInstance().getHtm(KTB_HTML_PATH + "RemoveParticipation.htm");

			if (htmContent != null)
			{
				final int playerCounts = KTBEvent.getPlayerCounts();
				final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());

				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%playercount%", String.valueOf(playerCounts));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", KTBEvent.getParticipationFee());

				playerInstance.sendPacket(npcHtmlMessage);
			}
		}

		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
