package net.sf.l2j.gameserver.events.lastman;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

public class LMEventTeleporter implements Runnable
{
	/** The instance of the player to teleport */
	private Player _activeChar = null;
	/** Coordinates of the spot to teleport to */
	private int[] _coordinates = new int[3];
	/** Admin removed this player from event */
	private boolean _adminRemove = false;
	
	/**
	 * Initialize the teleporter and start the delayed task<br><br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 * @param coordinates as int[]<br>
	 * @param fastSchedule 
	 * @param adminRemove as boolean<br>
	 */
	public LMEventTeleporter(Player activeChar, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_activeChar = activeChar;
		_coordinates = coordinates;
		_adminRemove = adminRemove;
		
		loadTeleport(fastSchedule);
	}
	
	/**
	 * Initialize the teleporter and start the delayed task<br><br>
	 *
	 * @param activeChar as L2PcInstance<br>
	 * @param fastSchedule 
	 * @param adminRemove as boolean<br>
	 */
	public LMEventTeleporter(Player activeChar, boolean fastSchedule, boolean adminRemove)
	{
		_activeChar = activeChar;
		_coordinates = LMConfig.LM_EVENT_PLAYER_COORDINATES.get(Rnd.get(LMConfig.LM_EVENT_PLAYER_COORDINATES.size()));
		_adminRemove = adminRemove;
		
		loadTeleport(fastSchedule);
	}

	private void loadTeleport(boolean fastSchedule)
	{
		long delay = (LMEvent.isStarted() ? LMConfig.LM_EVENT_RESPAWN_TELEPORT_DELAY : LMConfig.LM_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;
		ThreadPool.schedule(this, fastSchedule ? 0 : delay);
	}
		
	/**
	 * The task method to teleport the player<br>
	 * 1. Unsummon pet if there is one<br>
	 * 2. Remove all effects<br>
	 * 3. Revive and full heal the player<br>
	 * 4. Teleport the player<br>
	 * 5. Broadcast status and user info<br><br>
	 *
	 */
	@Override
	public void run()
	{
		if (_activeChar == null) 
			return;
		
		Summon summon = _activeChar.getSummon();
		
		if (summon != null)
			summon.unSummon(_activeChar);

		if (LMConfig.LM_EVENT_EFFECTS_REMOVAL == 0 || (LMConfig.LM_EVENT_EFFECTS_REMOVAL == 1 && (_activeChar.getTeam() == 0 || (_activeChar.isInDuel() && _activeChar.getDuelState() != DuelState.INTERRUPTED))))
			_activeChar.stopAllEffectsExceptThoseThatLastThroughDeath();

		if (_activeChar.isInDuel())
			_activeChar.setDuelState(DuelState.INTERRUPTED);

		_activeChar.doRevive();

		// Event Gear: set fijo + nick SanSara al entrar, se devuelve todo al salir.
		net.sf.l2j.gameserver.events.eventgear.EventGearManager.onEventTeleport(_activeChar, "LM", LMEvent.isStarted() && !_adminRemove);

	//	if (_activeChar instanceof FakePlayer && !LMEvent.isStarted())
	//		_activeChar.teleToLocation(60608, -94016, -1344, 0);
	//	else
			_activeChar.teleToLocation(_coordinates[0] + Rnd.get(101) - 50, _coordinates[1] + Rnd.get(101) - 50, _coordinates[2], 0);

		if (LMEvent.isStarted() && !_adminRemove)
		{
			_activeChar.setTeam(2);
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