package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;

/**
 * The Class ConditionPlayerIsNoble.
 */
public class ConditionPlayerIsNoble extends Condition
{
	private final boolean _val;
	
	/**
	 * Instantiates a new condition player is hero.
	 * @param val the val
	 */
	public ConditionPlayerIsNoble(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if (env.getPlayer() == null)
			return false;
		
		return (env.getPlayer().isNoble() == _val);
	}
}