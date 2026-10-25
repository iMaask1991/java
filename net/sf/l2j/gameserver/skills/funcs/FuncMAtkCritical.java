package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

public class FuncMAtkCritical extends Func
{
	static final FuncMAtkCritical _fac_instance = new FuncMAtkCritical();
	
	public static Func getInstance()
	{
		return _fac_instance;
	}
	
	private FuncMAtkCritical()
	{
		super(Stats.MCRITICAL_RATE, 0x09, null, null);
	}
	
	@Override
	public void calc(Env env)
	{
		// Retail: WIT multiplies m.crit. Unarmed players keep the base (no WIT bonus).
		if (!(env.getCharacter() instanceof Player) || env.getCharacter().getActiveWeaponInstance() != null)
		{
			int wit = env.getCharacter().getWIT();
			if (wit < 0)
				wit = 0;
			else if (wit >= Formulas.WIT_BONUS.length)
				wit = Formulas.WIT_BONUS.length - 1;
			
			env.mulValue(Formulas.WIT_BONUS[wit]);
		}
		
		env.setBaseValue(env.getValue());
	}
}
