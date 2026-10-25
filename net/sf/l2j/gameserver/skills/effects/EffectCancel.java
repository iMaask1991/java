package net.sf.l2j.gameserver.skills.effects;

import java.util.Vector;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;

import net.sf.l2j.gameserver.enums.skills.L2EffectType;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.taskmanager.CustomCancelTaskManager;

/**
 * @author DS
 */
public class EffectCancel extends L2Effect
{
	public EffectCancel(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.CANCEL;
	}
	
	@Override
	public boolean onStart()
	{
		return cancel(getEffector(), getEffected(), this);
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
	
	private static boolean cancel(Creature caster, Creature target, L2Effect effect)
	{
		if (!(target instanceof Player) || target.isDead())
			return false;
		
		final int cancelLvl = effect.getSkill().getMagicLevel();
		int count = effect.getSkill().getMaxNegatedEffects();
		
		double rate = effect.getEffectPower();
		
		// Resistance/vulnerability
		final double res = Formulas.calcSkillVulnerability(caster, target, effect.getSkill(), effect.getSkillType());
		rate *= res;
		
		// Devolucion de buffs (igual que el Cancel normal): solo PvP y fuera de olimpiada.
		final boolean returnCancel = Config.RETURN_CANCEL_BUFF && caster instanceof Player && !((Player) caster).isInOlympiadMode();
		final Vector<L2Skill> cancelledBuffs = new Vector<>();

		L2Effect eff;
		int lastCanceledSkillId = 0;
		final L2Effect[] effects = target.getAllEffects();
		for (int i = effects.length; --i >= 0;)
		{
			eff = effects[i];
			if (eff == null)
				continue;
			
			// Como el Cancel normal: los toggles no se cancelan.
			if (eff.getSkill().isToggle())
				continue;
			
			// first pass - dances/songs only
			if (!eff.getSkill().isDance())
				continue;
			
			if (eff.getSkill().getId() == lastCanceledSkillId)
			{
				eff.exit(); // this skill already canceled
				continue;
			}
			
			if (!calcCancelSuccess(eff, cancelLvl, (int) rate))
				continue;
			
			if (returnCancel && !cancelledBuffs.contains(eff.getSkill()))
				cancelledBuffs.add(eff.getSkill());
			
			lastCanceledSkillId = eff.getSkill().getId();
			eff.exit();
			count--;
			
			if (count == 0)
				break;
		}
		
		if (count != 0)
		{
			lastCanceledSkillId = 0;
			for (int i = effects.length; --i >= 0;)
			{
				eff = effects[i];
				if (eff == null)
					continue;
				
				// Como el Cancel normal: los toggles no se cancelan.
				if (eff.getSkill().isToggle())
					continue;
				
				// second pass - all except dances/songs
				if (eff.getSkill().isDance())
					continue;
				
				if (eff.getSkill().getId() == lastCanceledSkillId)
				{
					eff.exit(); // this skill already canceled
					continue;
				}
				
				if (!calcCancelSuccess(eff, cancelLvl, (int) rate))
					continue;
				
				if (returnCancel && !cancelledBuffs.contains(eff.getSkill()))
					cancelledBuffs.add(eff.getSkill());
				
				lastCanceledSkillId = eff.getSkill().getId();
				eff.exit();
				count--;
				
				if (count == 0)
					break;
			}
		}
		
		// Devolver los buffs cancelados despues de RETURN_CANCEL_TIME segundos (igual que el Cancel normal).
		if (returnCancel && !cancelledBuffs.isEmpty())
			ThreadPool.schedule(new CustomCancelTaskManager((Player) target, cancelledBuffs), Config.RETURN_CANCEL_TIME * 1000L);
		
		return true;
	}
	
	private static boolean calcCancelSuccess(L2Effect effect, int cancelLvl, int baseRate)
	{
		int rate = 2 * (cancelLvl - effect.getSkill().getMagicLevel());
		rate += effect.getPeriod() / 120;
		rate += baseRate;
		
		if (rate < 25)
			rate = 25;
		else if (rate > 75)
			rate = 75;
		
		return Rnd.get(100) < rate;
	}
}
