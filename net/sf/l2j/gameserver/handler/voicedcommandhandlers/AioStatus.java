package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Voiced .aio — estado AIO y tiempo restante (HTML editable por properties).
 */
public class AioStatus implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"aio"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!command.equalsIgnoreCase("aio"))
			return false;
		
		if (!Config.ENABLE_AIO_COMMAND)
		{
			activeChar.sendMessage("El comando .aio esta desactivado.");
			return true;
		}
		
		showAioHtml(activeChar);
		return true;
	}
	
	private static void showAioHtml(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(Config.AIO_HTML_FILE);
		
		html.replace("%name%", player.getName());
		
		if (player.isAio() && player.getAioEndTime() > System.currentTimeMillis())
		{
			final long remainingMs = player.getAioEndTime() - System.currentTimeMillis();
			final long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(remainingMs));
			final long hours = Math.max(0, TimeUnit.MILLISECONDS.toHours(remainingMs) % 24);
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60);
			
			html.replace("%aio_status%", "Activo");
			html.replace("%aio_end%", new SimpleDateFormat(Config.AIO_DATE_FORMAT).format(new Date(player.getAioEndTime())));
			html.replace("%aio_days%", String.valueOf(days));
			html.replace("%aio_hours%", String.valueOf(hours));
			html.replace("%aio_minutes%", String.valueOf(minutes));
			html.replace("%aio_remaining%", days + " dia(s) " + hours + " hora(s) " + minutes + " min");
		}
		else
		{
			html.replace("%aio_status%", "Inactivo");
			html.replace("%aio_end%", "-");
			html.replace("%aio_days%", "0");
			html.replace("%aio_hours%", "0");
			html.replace("%aio_minutes%", "0");
			html.replace("%aio_remaining%", "Sin AIO");
		}
		
		player.sendPacket(html);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
