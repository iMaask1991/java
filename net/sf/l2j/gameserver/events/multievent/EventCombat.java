package net.sf.l2j.gameserver.events.multievent;

import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Combat rules for PvP events (TvT, CTF, DM, LM). KTB is PvE and is not included.
 * Clan, party and ally are ignored while both players are in the same running event.
 */
public final class EventCombat
{
	private EventCombat()
	{
	}
	
	/**
	 * @return {@link Boolean#TRUE} if they are event enemies, {@link Boolean#FALSE} if they are teammates in a team event, {@code null} if they are not both in the same PvP event.
	 */
	public static Boolean attackRule(Player player1, Player player2)
	{
		if (TvTEvent.areParticipants(player1, player2))
			return TvTEvent.areEnemies(player1, player2);
		
		if (CTFEvent.areParticipants(player1, player2))
			return CTFEvent.areEnemies(player1, player2);
		
		if (DMEvent.areParticipants(player1, player2))
			return DMEvent.areEnemies(player1, player2);
		
		if (LMEvent.areParticipants(player1, player2))
			return LMEvent.areEnemies(player1, player2);
		
		return null;
	}
}
