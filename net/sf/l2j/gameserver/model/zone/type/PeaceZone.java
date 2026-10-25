package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.ZoneType;

/**
 * A zone extending {@link ZoneType}, notably used for peace behavior (pvp related).
 */
public class PeaceZone extends ZoneType
{
	public PeaceZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		character.setInsideZone(ZoneId.PEACE, true);

		if (character instanceof Player)
		{
			final Player player = (Player) character;
			if (player.isAutoFarm())
				VoicedAutoFarm.stopAutoFarm(player, "zona peace");
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.PEACE, false);
	}
}