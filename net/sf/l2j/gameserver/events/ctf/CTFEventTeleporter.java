package net.sf.l2j.gameserver.events.ctf;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

public class CTFEventTeleporter implements Runnable
{
	/** The instance of the player to teleport */
	private Player _activeChar = null;
	/** Coordinates of the spot to teleport to */
	private int[] _coordinates = new int[3];
	/** Admin removed this player from event */
	private boolean _adminRemove = false;
	
	/**
	 * Initialize the teleporter and start the delayed task.
	 * @param playerInstance
	 * @param coordinates
	 * @param fastSchedule
	 * @param adminRemove
	 */
	public CTFEventTeleporter(Player playerInstance, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_activeChar = playerInstance;
		_coordinates = coordinates;
		_adminRemove = adminRemove;
		
		long delay = (CTFEvent.isStarted() ? CTFConfig.CTF_EVENT_RESPAWN_TELEPORT_DELAY : CTFConfig.CTF_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;
		
		ThreadPool.schedule(this, fastSchedule ? 0 : delay);
	}
		
	/**
	 * The task method to teleport the player<br>
	 * 1. Unsummon pet if there is one<br>
	 * 2. Remove all effects<br>
	 * 3. Revive and full heal the player<br>
	 * 4. Teleport the player<br>
	 * 5. Broadcast status and user info
	 */
	@Override
	public void run()
	{
		if (_activeChar == null)
			return;
		
		Summon summon = _activeChar.getSummon();
		
		if (summon != null)
			summon.unSummon(_activeChar);
		
	//	if ((CTFConfig.CTF_EVENT_EFFECTS_REMOVAL == 0) || ((CTFConfig.CTF_EVENT_EFFECTS_REMOVAL == 1) && ((_activeChar.getTeam() == 0) || (_activeChar.isInDuel() && (_activeChar.getDuelState() != DuelState.INTERRUPTED)))))
	//	if ((_activeChar.getTeam() == 0) || (_activeChar.isInDuel() && (_activeChar.getDuelState() != DuelState.INTERRUPTED)))
	//		_activeChar.stopAllEffectsExceptThoseThatLastThroughDeath();
		
		if (_activeChar.isInDuel())
			_activeChar.setDuelState(DuelState.INTERRUPTED);
		
		_activeChar.doRevive();

		// Event Gear: set fijo + nick SanSara al entrar, se devuelve todo al salir.
		net.sf.l2j.gameserver.events.eventgear.EventGearManager.onEventTeleport(_activeChar, "CTF", CTFEvent.isStarted() && !_adminRemove);
		
	//	if (_activeChar instanceof FakePlayer && !CTFEvent.isStarted())
	//		_activeChar.teleToLocation(-114584,-251256,-2992, 0);
	//	else
			_activeChar.teleToLocation((_coordinates[0] + Rnd.get(101)) - 50, (_coordinates[1] + Rnd.get(101)) - 50, _coordinates[2], 0);
		
		// Reset flag carrier
		if (CTFEvent.playerIsCarrier(_activeChar))
		{
			CTFEvent.removeFlagCarrier(_activeChar);
			CTFEvent.sysMsgToAllParticipants("The " + CTFEvent.getParticipantEnemyTeam(_activeChar.getObjectId()).getName() + " flag has been returned!");
		}
		
		if (CTFEvent.isStarted() && !_adminRemove)
		{
			_activeChar.setTeam(CTFEvent.getParticipantTeamId(_activeChar.getObjectId()) + 1);
			PvpFlagTaskManager.getInstance().remove(_activeChar);
			_activeChar.updatePvPFlag(0);
		}
		else
			_activeChar.setTeam(0);
		
		_activeChar.setCurrentCp(_activeChar.getMaxCp());
		_activeChar.setCurrentHp(_activeChar.getMaxHp());
		_activeChar.setCurrentMp(_activeChar.getMaxMp());
		
		_activeChar.broadcastStatusUpdate();
		_activeChar.broadcastUserInfo();
	}
}