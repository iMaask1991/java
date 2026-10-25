package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.Config;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class SummonStatus extends PlayableStatus
{
	/** true mientras el dueno recibe dano que le paso su servitor (para que no lo vuelva a pasar al servitor). */
	private static final ThreadLocal<Boolean> REVERSE = ThreadLocal.withInitial(() -> Boolean.FALSE);
	
	/** @return true si el dano que esta recibiendo el dueno viene de su servitor (Transfer Pain inverso). */
	public static boolean isReverseTransfer()
	{
		return REVERSE.get();
	}
	
	public SummonStatus(Summon activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		if (getActiveChar().isDead())
			return;
		
		final Player owner = getActiveChar().getOwner();
		
		// We deny the duel, no matter if damage has been done or not.
		if (attacker != null)
		{
			final Player attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer != null && (owner == null || owner.getDuelId() != attackerPlayer.getDuelId()))
				attackerPlayer.setDuelState(DuelState.INTERRUPTED);
		}
		
		// Transfer Pain inverso (custom SanSara, Balance.properties): si el dueno tiene Transfer Pain prendido,
		// del dano que recibe el servitor el dueno se come (100 - % de la skill), con tope TPainReverseMaxPercent.
		// El dueno SI puede morir por esto. El servitor muere normal con los golpes directos.
		if (Config.TPAIN_REVERSE && attacker != null && !isHPConsumption && owner != null && attacker != owner && getActiveChar() instanceof Servitor && !owner.isDead() && MathUtil.checkIfInRange(Config.TPAIN_RANGE, owner, getActiveChar(), true))
		{
			final double tpain = owner.calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null);
			if (tpain > 0)
			{
				final double ownerPercent = Math.min(Config.TPAIN_REVERSE_MAX_PERCENT, 100 - Math.min(100, tpain));
				final double ownerDmg = value * ownerPercent / 100.;
				if (ownerDmg >= 1)
				{
					value -= ownerDmg;
					REVERSE.set(Boolean.TRUE);
					try
					{
						owner.reduceCurrentHp(ownerDmg, attacker, null);
					}
					finally
					{
						REVERSE.set(Boolean.FALSE);
					}
				}
			}
		}
		
		super.reduceHp(value, attacker, awake, isDOT, isHPConsumption);
		
		// Since damages have been done, we can send damage message and EVT_ATTACKED notification.
		if (attacker != null)
		{
			if (!isDOT && owner != null)
				owner.sendPacket(SystemMessage.getSystemMessage((getActiveChar() instanceof Servitor) ? SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1 : SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1).addCharName(attacker).addNumber((int) value));
			
			getActiveChar().getAI().notifyEvent(AiEventType.ATTACKED, attacker);
		}
	}
	
	@Override
	public Summon getActiveChar()
	{
		return (Summon) super.getActiveChar();
	}
}