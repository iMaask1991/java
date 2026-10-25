package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.SpawnZoneType;
import net.sf.l2j.gameserver.enums.ZoneId;

public class TournamentZone extends SpawnZoneType
{
	public TournamentZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(Creature character)
	{
		character.setInsideZone(ZoneId.PEACE, true);
		character.setInsideZone(ZoneId.TOURNAMENT, true);
		
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			player.setTournamentTeleport(true);
			if (!player.isOlympiadProtection())
			{
				player.sendMessage("You have entered a Tournament zone.");
				player.getAppearance().setInvisible();
				player.broadcastUserInfo();
			}
			
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		character.setInsideZone(ZoneId.TOURNAMENT, false);
		character.setInsideZone(ZoneId.PEACE, false);
		
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			
			if (!player.isArenaProtection() && !player.isOlympiadProtection())
			{
				player.setTournamentTeleport(false);
				
			if (!player.isInObserverMode() && !player.isOlympiadProtection() && !player.isGM())
					player.getAppearance().setVisible();
							
				player.sendMessage("You left a Tournament zone.");
				player.broadcastUserInfo();
			}
			
		}
	}
	


}
