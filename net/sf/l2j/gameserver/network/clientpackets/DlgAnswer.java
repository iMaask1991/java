package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.soloboss.SoloBoss;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;

/**
 * @author Dezmond_snz Format: cddd
 */
public final class DlgAnswer extends L2GameClientPacket
{
	private int _messageId;
	private int _answer;
	private int _requesterId;
	
	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requesterId = readD();
	}
	
	@Override
	public void runImpl()
	{
		final Player activeChar = getClient().getPlayer();
		if (activeChar == null)
			return;
		
		if (_messageId == SystemMessageId.RESSURECTION_REQUEST_BY_S1.getId() || _messageId == SystemMessageId.DO_YOU_WANT_TO_BE_RESTORED.getId())
			activeChar.reviveAnswer(_answer);
		else if (Announcement.isSummoning == true && this._answer == 1) {
			activeChar.teleportTo(Config.Tournament_locx, Config.Tournament_locy, Config.Tournament_locz, 125);
		}
		else if (_messageId == SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
			activeChar.teleportAnswer(_answer, _requesterId);
		else if (_messageId == 1983 && Config.ALLOW_WEDDING)
			activeChar.engageAnswer(_answer);
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId())
			activeChar.activateGate(_answer, 1);
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId())
			activeChar.activateGate(_answer, 0);
		else if (SoloBoss.boss_teleporter && _answer == 1)
		{
			if (activeChar.isInObserverMode() || activeChar.isDead() || DMEvent.isPlayerParticipant(activeChar.getObjectId()) && DMEvent.isStarted() || KTBEvent.isPlayerParticipant(activeChar.getObjectId()) && KTBEvent.isStarted() || LMEvent.isPlayerParticipant(activeChar.getObjectId()) && LMEvent.isStarted() || activeChar.isAlikeDead() || activeChar.isInArenaEvent() || activeChar.isArenaProtection() || activeChar.isOlympiadProtection() || activeChar.isInStoreMode() || activeChar.isRooted() || activeChar.getKarma() > 0 || activeChar.isInOlympiadMode() || activeChar.isOff() || activeChar.isOffShop() || activeChar.isFestivalParticipant() || activeChar.isArenaAttack() || activeChar.isInsideZone(ZoneId.BOSS) || activeChar.isInsideZone(ZoneId.ARENA_EVENT) || activeChar.isInsideZone(ZoneId.PVP) || activeChar.isInsideZone(ZoneId.SIEGE))
				return;
			
			activeChar.teleportTo(Config.SOLO_BOSS_ID_ONE_LOC[0], Config.SOLO_BOSS_ID_ONE_LOC[1], Config.SOLO_BOSS_ID_ONE_LOC[2], 825);
		}
	}
}