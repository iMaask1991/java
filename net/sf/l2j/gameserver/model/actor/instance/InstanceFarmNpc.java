package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmManager;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmRun;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmZone;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmZoneData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.type.InstanceTimeZone;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * NPC 60020 - Instance Farm Manager (Time / Kill).<br>
 * Toda la validacion (pagar, tiempo guardado, HWID, nivel, etc.) se hace en InstanceFarmManager, no en el HTML.
 */
public class InstanceFarmNpc extends Folk
{
	private static final String HTML_PATH = "data/html/mods/instancefarm/";
	
	public InstanceFarmNpc(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		showPage(player);
	}
	
	/**
	 * El NPC del Kill Farm (KillNpcId, 60023) muestra kill.htm. El resto (60020) muestra main.htm (Time).
	 */
	private void showPage(Player player)
	{
		if (getNpcId() == Config.INSTANCE_FARM_KILL_NPC_ID)
			showKill(player);
		else
			showMain(player);
	}
	
	private static String load(String file)
	{
		final String html = HtmCache.getInstance().getHtm(HTML_PATH + file);
		return (html == null) ? "<html><body>Instance Farm: falta " + HTML_PATH + file + "</body></html>" : html;
	}
	
	/**
	 * Ventana del Instance Solo (Kill Farm). Todo el diseno esta en instancefarm/kill.htm, kill_pack_row.htm y kill_zone_row.htm.
	 */
	private void showKill(Player player)
	{
		final InstanceFarmManager manager = InstanceFarmManager.getInstance();
		final net.sf.l2j.gameserver.instancefarm.InstanceFarmBank bank = net.sf.l2j.gameserver.instancefarm.InstanceFarmBank.get(player);
		final InstanceFarmRun run = manager.getRun(player);
		
		final String packTpl = load("kill_pack_row.htm");
		final StringBuilder packs = new StringBuilder();
		for (int i = 0; i < Config.INSTANCE_FARM_KILL_PACKS.size(); i++)
		{
			final int[] pack = Config.INSTANCE_FARM_KILL_PACKS.get(i);
			packs.append(packTpl.replace("%kills%", String.valueOf(pack[0])).replace("%price%", InstanceFarmManager.getPackPriceText(pack)).replace("%index%", String.valueOf(i)));
		}
		if (packs.length() == 0)
			packs.append(load("kill_pack_empty.htm"));
		
		final String zoneTpl = load("kill_zone_row.htm");
		final StringBuilder zones = new StringBuilder();
		for (InstanceFarmZone zone : InstanceFarmZoneData.getZones())
		{
			if (zone.isEnabled())
				zones.append(zoneTpl.replace("%zoneName%", zone.getName()).replace("%zoneId%", String.valueOf(zone.getId())).replace("%minLevel%", String.valueOf(zone.getMinLevel())));
		}
		if (zones.length() == 0)
			zones.append(load("kill_zone_empty.htm"));
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
		msg.setHtml(load("kill.htm"));
		msg.replace("%killPacks%", packs.toString());
		msg.replace("%killZones%", zones.toString());
		msg.replace("%killBank%", String.format("%,d", (bank == null) ? 0 : bank.getKillCount()));
		msg.replace("%state%", (run != null && !run.isEnded()) ? "Adentro" : "Afuera");
		msg.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(msg);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!Config.INSTANCE_FARM_ENABLED)
		{
			player.sendMessage("El Instance Farm esta desactivado.");
			return;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String action = st.nextToken();
		final int param;
		final int param2;
		try
		{
			param = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
			param2 = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
		}
		catch (NumberFormatException e)
		{
			return;
		}
		
		switch (action)
		{
			case "time_tp":
				InstanceFarmManager.getInstance().teleportTime(player, param, param2);
				return;
			
			case "enter_time":
				InstanceFarmManager.getInstance().enterTime(player, param);
				return;
			
			case "pay_time":
				InstanceFarmManager.getInstance().payTime(player, param);
				return;
			
			case "enter_kill":
				InstanceFarmManager.getInstance().enterKill(player, param);
				return;
			
			case "buy_kills":
				InstanceFarmManager.getInstance().buyKills(player, param);
				showPage(player);
				return;
			
			case "status":
				showPage(player);
				return;
		}
		
		super.onBypassFeedback(player, command);
	}
	
	private void showMain(Player player)
	{
		String html = HtmCache.getInstance().getHtm(HTML_PATH + "main.htm");
		if (html == null)
			html = "<html><body>Instance Farm Manager<br>Falta main.htm</body></html>";
		
		final InstanceFarmManager manager = InstanceFarmManager.getInstance();
		final int timeSeconds = manager.getTimeSeconds(player);
		final InstanceFarmRun run = manager.getRun(player);
		final net.sf.l2j.gameserver.instancefarm.InstanceFarmBank bank = net.sf.l2j.gameserver.instancefarm.InstanceFarmBank.get(player);
		final int kills = bank == null ? 0 : bank.getKillCount();
		
		// ---- TIME ----
		final StringBuilder time = new StringBuilder();
		for (InstanceTimeZone zone : InstanceFarmManager.getTimeZones())
		{
			time.append("<tr><td align=center><font color=\"LEVEL\">").append(zone.getZoneName()).append("</font>");
			if (zone.getMinLevel() > 1)
				time.append(" <font color=\"808080\">(lvl ").append(zone.getMinLevel()).append("+)</font>");
			time.append("</td></tr><tr><td align=center>");
			
			if (timeSeconds > 0)
				time.append("<button value=\"Entrar\" action=\"bypass -h npc_%objectId%_enter_time ").append(zone.getFarmId()).append("\" width=200 height=26 back=L2UI_NOVA.btn_e_down fore=L2UI_NOVA.btn_e>");
			else
				time.append("<button value=\"Pagar y entrar\" action=\"bypass -h npc_%objectId%_pay_time ").append(zone.getFarmId()).append("\" width=200 height=26 back=L2UI_NOVA.btn_e_down fore=L2UI_NOVA.btn_e>");
			
			time.append("</td></tr><tr><td height=4></td></tr>");
		}
		if (time.length() == 0)
			time.append("<tr><td align=center><font color=\"808080\">No hay zonas Time disponibles.</font></td></tr>");
		
		// ---- KILL packs ----
		final StringBuilder packs = new StringBuilder();
		for (int i = 0; i < Config.INSTANCE_FARM_KILL_PACKS.size(); i++)
		{
			final int[] pack = Config.INSTANCE_FARM_KILL_PACKS.get(i);
			packs.append("<tr><td align=center><button value=\"Comprar ").append(pack[0]).append(" kills (").append(InstanceFarmManager.getPackPriceText(pack)).append(")\" action=\"bypass -h npc_%objectId%_buy_kills ").append(i).append("\" width=250 height=26 back=L2UI_NOVA.btn_e_down fore=L2UI_NOVA.btn_e></td></tr>");
		}
		if (packs.length() == 0)
			packs.append("<tr><td align=center><font color=\"808080\">No hay packs configurados.</font></td></tr>");
		
		// ---- KILL zones ----
		final StringBuilder kill = new StringBuilder();
		for (InstanceFarmZone zone : InstanceFarmZoneData.getZones())
		{
			if (!zone.isEnabled())
				continue;
			
			kill.append("<tr><td align=center><button value=\"Entrar a ").append(zone.getName()).append("\" action=\"bypass -h npc_%objectId%_enter_kill ").append(zone.getId()).append("\" width=250 height=26 back=L2UI_NOVA.btn_e_down fore=L2UI_NOVA.btn_e></td></tr>");
		}
		if (kill.length() == 0)
			kill.append("<tr><td align=center><font color=\"808080\">No hay zonas Kill disponibles.</font></td></tr>");
		
		String state = "Afuera";
		if (run != null && !run.isEnded())
			state = "Dentro del Kill Farm";
		else if (manager.isInTimeFarm(player))
			state = "Dentro del Time Farm";
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
		msg.setHtml(html);
		msg.replace("%timeZones%", time.toString());
		msg.replace("%killPacks%", packs.toString());
		msg.replace("%killZones%", kill.toString());
		msg.replace("%objectId%", String.valueOf(getObjectId()));
		msg.replace("%state%", state);
		msg.replace("%timeLeft%", timeSeconds > 0 ? InstanceFarmRun.formatTime(timeSeconds) : "sin tiempo");
		msg.replace("%timePrice%", InstanceFarmManager.getTimePriceText());
		msg.replace("%timeMinutes%", String.valueOf(Config.INSTANCE_FARM_TIME_PAY_MINUTES));
		msg.replace("%killBank%", String.valueOf(kills));
		player.sendPacket(msg);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
