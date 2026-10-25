package net.sf.l2j.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.customoly.CustomOlyArena;
import net.sf.l2j.gameserver.customoly.CustomOlyEvent;
import net.sf.l2j.gameserver.customoly.CustomOlyManager;
import net.sf.l2j.gameserver.customoly.CustomOlyManager.HeroEntry;
import net.sf.l2j.gameserver.customoly.CustomOlyPlayer;
import net.sf.l2j.gameserver.customoly.CustomOlyRules;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class CustomOlyNpc extends Folk
{
	public CustomOlyNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		showMain(player);
	}
	
	private void showMain(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/mods/customoly/main.htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%event%", Config.CUSTOM_OLY_NAME);
		html.replace("%phase%", CustomOlyManager.getInstance().getPhaseText());
		html.replace("%session%", CustomOlyEvent.getInstance().isSessionOpen() ? "Abierto" : "Cerrado");
		html.replace("%q1%", String.valueOf(CustomOlyArena.getInstance().queueSize(1)));
		html.replace("%q2%", String.valueOf(CustomOlyArena.getInstance().queueSize(2)));
		html.replace("%q3%", String.valueOf(CustomOlyArena.getInstance().queueSize(3)));
		CustomOlyPlayer n = CustomOlyManager.getInstance().getNoble(player.getObjectId());
		html.replace("%mypoints%", n == null ? "0" : String.valueOf(n.points));
		html.replace("%myrank%", String.valueOf(CustomOlyManager.getInstance().getRank(player)));
		html.replace("%myclass%", player.getClassId().toString());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.equals("reg1"))
		{
			if (!Config.CUSTOM_OLY_ALLOW_1X1)
				player.sendMessage("1v1 desactivado.");
			else
				CustomOlyArena.getInstance().register1x1(player);
		}
		else if (command.equals("reg2"))
		{
			if (!Config.CUSTOM_OLY_ALLOW_2X2)
				player.sendMessage("2v2 desactivado.");
			else
				CustomOlyArena.getInstance().registerParty(player, 2);
		}
		else if (command.equals("reg3"))
		{
			if (!Config.CUSTOM_OLY_ALLOW_3X3)
				player.sendMessage("3v3 desactivado.");
			else
				CustomOlyArena.getInstance().registerParty(player, 3);
		}
		else if (command.equals("unreg"))
			CustomOlyArena.getInstance().unregister(player);
		else if (command.equals("rank"))
			showRank(player, player.getClassId().getId());
		else if (command.startsWith("rank_"))
		{
			try
			{
				showRank(player, Integer.parseInt(command.substring(5)));
			}
			catch (Exception e)
			{
				showRank(player, player.getClassId().getId());
			}
		}
		else if (command.equals("classes"))
			showClasses(player);
		else if (command.equals("heroes"))
			showHeroes(player);
		else if (command.equals("back"))
		{
			showMain(player);
			return;
		}
		
		if (command.equals("reg1") || command.equals("reg2") || command.equals("reg3") || command.equals("unreg"))
			showMain(player);
	}
	
	private void showRank(Player player, int classId)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/mods/customoly/rank.htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%classname%", CustomOlyRules.className(classId));
		List<CustomOlyPlayer> list = CustomOlyManager.getInstance().getRanking(classId, 30);
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (CustomOlyPlayer n : list)
			sb.append("<tr><td>").append(i++).append("</td><td>").append(n.name).append("</td><td>").append(n.points).append("</td><td>").append(n.wins).append("/").append(n.losses).append("</td><td>").append(n.frozen ? "OK" : "-").append("</td></tr>");
		if (list.isEmpty())
			sb.append("<tr><td colspan=5>Sin participantes.</td></tr>");
		html.replace("%rows%", sb.toString());
		player.sendPacket(html);
	}
	
	private void showClasses(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/mods/customoly/classes.htm");
		html.replace("%objectId%", getObjectId());
		StringBuilder sb = new StringBuilder();
		int col = 0;
		sb.append("<table width=280>");
		for (ClassId cid : CustomOlyManager.getInstance().getThirdClasses())
		{
			if (col == 0)
				sb.append("<tr>");
			sb.append("<td><a action=\"bypass npc_").append(getObjectId()).append("_rank_").append(cid.getId()).append("\">").append(cid.toString()).append("</a></td>");
			col++;
			if (col == 2)
			{
				sb.append("</tr>");
				col = 0;
			}
		}
		if (col != 0)
			sb.append("<td></td></tr>");
		sb.append("</table>");
		html.replace("%list%", sb.toString());
		player.sendPacket(html);
	}
	
	private void showHeroes(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/mods/customoly/heroes.htm");
		html.replace("%objectId%", getObjectId());
		SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm");
		StringBuilder sb = new StringBuilder();
		List<HeroEntry> heroes = CustomOlyManager.getInstance().getActiveHeroes();
		for (HeroEntry h : heroes)
			sb.append("<tr><td>").append(h.name).append("</td><td>").append(CustomOlyRules.className(h.classId)).append("</td><td>").append(fmt.format(new Date(h.endTime))).append("</td></tr>");
		if (heroes.isEmpty())
			sb.append("<tr><td colspan=3>No hay Heroes activos.</td></tr>");
		html.replace("%rows%", sb.toString());
		player.sendPacket(html);
	}
}
