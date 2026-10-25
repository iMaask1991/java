package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmManager;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmRun;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * .time : muestra el tiempo guardado del Time Farm (por HWID), adentro o afuera.
 */
public class VoicedTimeLeft implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"time"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (!Config.INSTANCE_FARM_ENABLED)
		{
			activeChar.sendMessage("El Time Farm esta desactivado.");
			return true;
		}
		
		final int seconds = InstanceFarmManager.getInstance().getTimeSeconds(activeChar);
		if (seconds <= 0)
		{
			activeChar.sendMessage("Time Farm: no tenes tiempo guardado. Para entrar tenes que pagar en el TimeZone Gatekeeper.");
			return true;
		}
		
		final String left = InstanceFarmRun.formatTime(seconds);
		activeChar.sendMessage("Time Farm: te quedan " + left + (InstanceFarmManager.getInstance().isInTimeFarm(activeChar) ? "." : " guardado (solo corre estando adentro)."));
		activeChar.sendPacket(new ExShowScreenMessage("Tiempo: " + left, 3000));
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
