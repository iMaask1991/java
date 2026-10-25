package net.sf.l2j.gameserver.events.multievent;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.MultiEventNpc;

public class VoicedMultiEvent implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"events",
		"event_tvt",
		"event_dm",
		"event_lm",
		"event_ctf",
		"event_ktb"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		MultiEventNpc.handleBypass(activeChar, 0, command);
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
