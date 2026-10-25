package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmBank;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmManager;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmRun;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * .instance : ventana del Kill Farm con kills compradas, mobs matados, kills que quedan y progreso.<br>
 * Funciona adentro y afuera. El diseno esta en data/html/mods/instancefarm/stats.htm (boton Refresh = bypass voiced_instance).
 */
public class VoicedInstanceFarmStats implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"instance"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.INSTANCE_FARM_ENABLED)
		{
			activeChar.sendMessage("El Kill Farm esta desactivado.");
			return true;
		}
		
		final InstanceFarmBank bank = InstanceFarmBank.get(activeChar);
		if (bank == null)
			return true;
		
		final int bought = bank.getTotalBought();
		final int killed = bank.getTotalKilled();
		final int left = bank.getKillCount();
		final int progress = (bought > 0) ? Math.min(100, (int) (killed * 100L / bought)) : 0;
		
		final InstanceFarmRun run = InstanceFarmManager.getInstance().getRun(activeChar);
		final boolean inside = run != null && !run.isEnded();
		
		String html = HtmCache.getInstance().getHtm("data/html/mods/instancefarm/stats.htm");
		if (html == null)
			html = "<html><body>Kill Farm: falta data/html/mods/instancefarm/stats.htm</body></html>";
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(html);
		msg.replace("%bought%", String.format("%,d", bought));
		msg.replace("%killed%", String.format("%,d", killed));
		msg.replace("%left%", String.format("%,d", left));
		msg.replace("%progress%", String.valueOf(progress));
		msg.replace("%progressColor%", progress >= 70 ? "00FF00" : progress >= 30 ? "FFFF00" : "FF4040");
		msg.replace("%state%", inside ? "Adentro: " + run.getZone().getName() : "Afuera");
		msg.replace("%stateColor%", inside ? "00FF00" : "878686");
		activeChar.sendPacket(msg);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
