package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.ctf.CTFConfig;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMConfig;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.teamvsteam.TvTConfig;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class MultiEventNpc extends Npc
{
	private static final String MULTI_HTML = "data/html/mods/events/multi/";
	private static final String TVT_HTML = "data/html/mods/events/tvt/";
	private static final String DM_HTML = "data/html/mods/events/dm/";
	private static final String LM_HTML = "data/html/mods/events/lm/";
	private static final String CTF_HTML = "data/html/mods/events/ctf/";
	private static final String KTB_HTML = "data/html/mods/events/ktb/";

	public MultiEventNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		handleBypass(player, getObjectId(), command);
	}

	@Override
	public void showChatWindow(Player player, int val)
	{
		showMainMenu(player, getObjectId());
	}

	public static void handleBypass(Player player, int objectId, String command)
	{
		if (player == null || command == null)
			return;

		if (command.equals("event_menu") || command.equals("events"))
		{
			showMainMenu(player, objectId);
			return;
		}
		if (command.equals("event_tvt"))
		{
			showTvT(player, objectId);
			return;
		}
		if (command.equals("event_dm"))
		{
			showDM(player, objectId);
			return;
		}
		if (command.equals("event_lm"))
		{
			showLM(player, objectId);
			return;
		}
		if (command.equals("event_ctf"))
		{
			showCTF(player, objectId);
			return;
		}
		if (command.equals("event_ktb"))
		{
			showKTB(player, objectId);
			return;
		}

		if (command.startsWith("tvt_event_"))
		{
			if (TvTEvent.isParticipating())
				TvTEvent.onBypass(command, player);
			else
				player.sendMessage("TvT is not open.");
		}
		else if (command.startsWith("dm_event_"))
		{
			if (DMEvent.isParticipating())
				DMEvent.onBypass(command, player);
			else
				player.sendMessage("Death Match is not open.");
		}
		else if (command.startsWith("lm_event_"))
		{
			if (LMEvent.isParticipating())
				LMEvent.onBypass(command, player);
			else
				player.sendMessage("Last Man is not open.");
		}
		else if (command.startsWith("ctf_event_"))
		{
			if (CTFEvent.isParticipating())
				CTFEvent.onBypass(command, player);
			else
				player.sendMessage("CTF is not open.");
		}
		else if (command.startsWith("ktb_event_"))
		{
			if (KTBEvent.isParticipating())
				KTBEvent.onBypass(command, player);
			else
				player.sendMessage("Kill The Boss is not open.");
		}
	}

	public static void showMainMenu(Player player, int objectId)
	{
		final String htm = HtmCache.getInstance().getHtm(MULTI_HTML + "Main.htm");
		if (htm == null)
		{
			player.sendMessage("Event menu HTML is missing.");
			return;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setHtml(applyBypasses(htm, objectId));
		html.replace("%bypass%", objectId > 0 ? "bypass npc_" + objectId + "_" : "bypass -h voiced_");
		html.replace("%tvt_status%", statusText(TvTEvent.isParticipating(), TvTEvent.isStarting() || TvTEvent.isStarted()));
		html.replace("%tvt_count%", String.valueOf(TvTEvent.getTeamsPlayerCounts()[0] + TvTEvent.getTeamsPlayerCounts()[1]));
		html.replace("%dm_status%", statusText(DMEvent.isParticipating(), DMEvent.isStarting() || DMEvent.isStarted()));
		html.replace("%dm_count%", String.valueOf(DMEvent.getPlayerCounts()));
		html.replace("%lm_status%", statusText(LMEvent.isParticipating(), LMEvent.isStarting() || LMEvent.isStarted()));
		html.replace("%lm_count%", String.valueOf(LMEvent.getPlayerCounts()));
		html.replace("%ctf_status%", statusText(CTFEvent.isParticipating(), CTFEvent.isStarting() || CTFEvent.isStarted()));
		html.replace("%ctf_count%", String.valueOf(CTFEvent.getTeamsPlayerCounts()[0] + CTFEvent.getTeamsPlayerCounts()[1]));
		html.replace("%ktb_status%", statusText(KTBEvent.isParticipating(), KTBEvent.isStarting() || KTBEvent.isStarted()));
		html.replace("%ktb_count%", String.valueOf(KTBEvent.getPlayerCounts()));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showTvT(Player player, int objectId)
	{
		if (TvTEvent.isParticipating())
		{
			final boolean joined = TvTEvent.isPlayerParticipant(player.getObjectId());
			String htm = HtmCache.getInstance().getHtm(TVT_HTML + (joined ? "RemoveParticipation.htm" : "Participation.htm"));
			if (htm == null)
				return;

			final int[] counts = TvTEvent.getTeamsPlayerCounts();
			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(applyBypasses(htm, objectId), objectId));
			html.replace("%objectId%", String.valueOf(objectId));
			html.replace("%team1name%", TvTConfig.TVT_EVENT_TEAM_1_NAME);
			html.replace("%team1playercount%", String.valueOf(counts[0]));
			html.replace("%team2name%", TvTConfig.TVT_EVENT_TEAM_2_NAME);
			html.replace("%team2playercount%", String.valueOf(counts[1]));
			html.replace("%playercount%", String.valueOf(counts[0] + counts[1]));
			if (!joined)
				html.replace("%fee%", TvTEvent.getParticipationFee());
			player.sendPacket(html);
		}
		else if (TvTEvent.isStarting() || TvTEvent.isStarted())
		{
			final String htm = HtmCache.getInstance().getHtm(TVT_HTML + "Status.htm");
			if (htm == null)
				return;

			final int[] counts = TvTEvent.getTeamsPlayerCounts();
			final int[] points = TvTEvent.getTeamsPoints();
			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(htm, objectId));
			html.replace("%team1name%", TvTConfig.TVT_EVENT_TEAM_1_NAME);
			html.replace("%team1playercount%", String.valueOf(counts[0]));
			html.replace("%team1points%", String.valueOf(points[0]));
			html.replace("%team2name%", TvTConfig.TVT_EVENT_TEAM_2_NAME);
			html.replace("%team2playercount%", String.valueOf(counts[1]));
			html.replace("%team2points%", String.valueOf(points[1]));
			player.sendPacket(html);
		}
		else
			player.sendMessage("TvT is not open.");

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showDM(Player player, int objectId)
	{
		if (DMEvent.isParticipating())
		{
			final boolean joined = DMEvent.isPlayerParticipant(player.getObjectId());
			String htm = HtmCache.getInstance().getHtm(DM_HTML + (joined ? "RemoveParticipation.htm" : "Participation.htm"));
			if (htm == null)
				return;

			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(applyBypasses(htm, objectId), objectId));
			html.replace("%objectId%", String.valueOf(objectId));
			html.replace("%playercount%", String.valueOf(DMEvent.getPlayerCounts()));
			if (!joined)
				html.replace("%fee%", DMEvent.getParticipationFee());
			player.sendPacket(html);
		}
		else if (DMEvent.isStarting() || DMEvent.isStarted())
		{
			final String htm = HtmCache.getInstance().getHtm(DM_HTML + "Status.htm");
			if (htm == null)
				return;

			String htmltext = "";
			if (DMConfig.DM_SHOW_TOP_RANK)
			{
				final String[] topPositions = DMEvent.getFirstPosition(DMConfig.DM_TOP_RANK);
				if (topPositions != null)
				{
					boolean c = true;
					for (int i = 0; i < topPositions.length; i++)
					{
						final String color = c ? "LEVEL" : "FFFFFF";
						final String[] row = topPositions[i].split("\\,");
						htmltext += "<tr><td width=\"35\" align=\"center\"><font color=\"" + color + "\">" + (i + 1) + "</font></td>";
						htmltext += "<td width=\"100\" align=\"left\"><font color=\"" + color + "\">" + row[0] + "</font></td>";
						htmltext += "<td width=\"125\" align=\"right\"><font color=\"" + color + "\">" + row[1] + "</font></td></tr>";
						c = !c;
					}
				}
			}

			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(htm, objectId));
			html.replace("%positions%", htmltext);
			player.sendPacket(html);
		}
		else
			player.sendMessage("Death Match is not open.");

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showLM(Player player, int objectId)
	{
		if (LMEvent.isParticipating())
		{
			final boolean joined = LMEvent.isPlayerParticipant(player.getObjectId());
			String htm = HtmCache.getInstance().getHtm(LM_HTML + (joined ? "RemoveParticipation.htm" : "Participation.htm"));
			if (htm == null)
				return;

			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(applyBypasses(htm, objectId), objectId));
			html.replace("%objectId%", String.valueOf(objectId));
			html.replace("%playercount%", String.valueOf(LMEvent.getPlayerCounts()));
			player.sendPacket(html);
		}
		else if (LMEvent.isStarting() || LMEvent.isStarted())
		{
			final String htm = HtmCache.getInstance().getHtm(LM_HTML + "Status.htm");
			if (htm == null)
				return;

			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(htm, objectId));
			html.replace("%countplayer%", String.valueOf(LMEvent.getPlayerCounts()));
			player.sendPacket(html);
		}
		else
			player.sendMessage("Last Man is not open.");

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showCTF(Player player, int objectId)
	{
		if (CTFEvent.isParticipating())
		{
			final boolean joined = CTFEvent.isPlayerParticipant(player.getObjectId());
			String htm = HtmCache.getInstance().getHtm(CTF_HTML + (joined ? "RemoveParticipation.htm" : "Participation.htm"));
			if (htm == null)
				return;

			final int[] counts = CTFEvent.getTeamsPlayerCounts();
			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(applyBypasses(htm, objectId), objectId));
			html.replace("%objectId%", String.valueOf(objectId));
			html.replace("%team1name%", CTFConfig.CTF_EVENT_TEAM_1_NAME);
			html.replace("%team1playercount%", String.valueOf(counts[0]));
			html.replace("%team2name%", CTFConfig.CTF_EVENT_TEAM_2_NAME);
			html.replace("%team2playercount%", String.valueOf(counts[1]));
			html.replace("%playercount%", String.valueOf(counts[0] + counts[1]));
			player.sendPacket(html);
		}
		else if (CTFEvent.isStarting() || CTFEvent.isStarted())
		{
			final String htm = HtmCache.getInstance().getHtm(CTF_HTML + "Status.htm");
			if (htm == null)
				return;

			final int[] counts = CTFEvent.getTeamsPlayerCounts();
			final int[] points = CTFEvent.getTeamsPoints();
			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(htm, objectId));
			html.replace("%team1name%", CTFConfig.CTF_EVENT_TEAM_1_NAME);
			html.replace("%team1playercount%", String.valueOf(counts[0]));
			html.replace("%team1points%", String.valueOf(points[0]));
			html.replace("%team2name%", CTFConfig.CTF_EVENT_TEAM_2_NAME);
			html.replace("%team2playercount%", String.valueOf(counts[1]));
			html.replace("%team2points%", String.valueOf(points[1]));
			player.sendPacket(html);
		}
		else
			player.sendMessage("CTF is not open.");

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showKTB(Player player, int objectId)
	{
		if (KTBEvent.isParticipating())
		{
			final boolean joined = KTBEvent.isPlayerParticipant(player.getObjectId());
			String htm = HtmCache.getInstance().getHtm(KTB_HTML + (joined ? "RemoveParticipation.htm" : "Participation.htm"));
			if (htm == null)
				return;

			final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
			html.setHtml(withBack(applyBypasses(htm, objectId), objectId));
			html.replace("%objectId%", String.valueOf(objectId));
			html.replace("%playercount%", String.valueOf(KTBEvent.getPlayerCounts()));
			if (!joined)
				html.replace("%fee%", KTBEvent.getParticipationFee());
			player.sendPacket(html);
		}
		else
			player.sendMessage("Kill The Boss is not open.");

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static String statusText(boolean joining, boolean live)
	{
		if (joining)
			return "<font color=\"00FF00\">JOIN</font>";
		if (live)
			return "<font color=\"LEVEL\">LIVE</font>";
		return "<font color=\"999999\">OFF</font>";
	}

	private static String withBack(String html, int objectId)
	{
		final String back = objectId > 0 ? "bypass npc_" + objectId + "_event_menu" : "bypass -h voiced_events";
		final String button = "<br><center><button value=\"Back\" action=\"" + back + "\" width=81 height=24 back=L2UI_NOVA.btn_i_down fore=L2UI_NOVA.btn_i></center>";
		if (html.contains("</body>"))
			return html.replace("</body>", button + "</body>");
		return html + button;
	}

	private static String applyBypasses(String html, int objectId)
	{
		if (objectId > 0)
			return html;

		return html.replace("bypass npc_%objectId%_tvt_event_participation", "bypass -h voiced_tvtjoin")
			.replace("bypass npc_%objectId%_tvt_event_remove_participation", "bypass -h voiced_tvtleave")
			.replace("bypass npc_%objectId%_dm_event_participation", "bypass -h voiced_dmjoin")
			.replace("bypass npc_%objectId%_dm_event_remove_participation", "bypass -h voiced_dmleave")
			.replace("bypass npc_%objectId%_lm_event_participation", "bypass -h voiced_lmjoin")
			.replace("bypass npc_%objectId%_lm_event_remove_participation", "bypass -h voiced_lmleave")
			.replace("bypass npc_%objectId%_ctf_event_participation", "bypass -h voiced_ctfjoin")
			.replace("bypass npc_%objectId%_ctf_event_remove_participation", "bypass -h voiced_ctfleave")
			.replace("bypass npc_%objectId%_ktb_event_participation", "bypass -h voiced_ktbjoin")
			.replace("bypass npc_%objectId%_ktb_event_remove_participation", "bypass -h voiced_ktbleave");
	}
}
