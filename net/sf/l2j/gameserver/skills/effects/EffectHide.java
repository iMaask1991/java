package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.skills.L2EffectType;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Env;

/**
 * H5 Hide (Mobius handlers.skill.effects.Hide): el jugador queda invisible para los demas.
 * Los que lo tenian de target lo pierden (y los mobs se olvidan de el).
 * Se corta con cualquier accion que no sea moverse o al recibir dano (removedOnAnyActionExceptMove / removedOnDamage de la skill).
 */
public class EffectHide extends L2Effect
{
	public EffectHide(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}
	
	@Override
	public boolean onStart()
	{
		super.onStart();
		
		if (getEffected() instanceof Player)
		{
			final Player player = (Player) getEffected();
			player.getAppearance().setInvisible();
			player.abortAttack();
			
			for (Creature cha : player.getKnownType(Creature.class))
			{
				if (cha == null || cha.getTarget() != player)
					continue;
				
				if (cha instanceof Attackable)
					((Attackable) cha).stopHating(player);
				
				cha.setTarget(null);
				cha.abortAttack();
				cha.abortCast();
				if (cha.hasAI())
					cha.getAI().setIntention(IntentionType.IDLE);
			}
			
			player.broadcastUserInfo();
		}
		return true;
	}
	
	@Override
	public void onExit()
	{
		if (getEffected() instanceof Player)
		{
			final Player player = (Player) getEffected();
			if (!player.isInObserverMode())
			{
				player.getAppearance().setVisible();
				player.broadcastUserInfo();
			}
		}
		super.onExit();
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
