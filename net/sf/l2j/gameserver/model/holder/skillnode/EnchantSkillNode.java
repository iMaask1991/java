package net.sf.l2j.gameserver.model.holder.skillnode;

import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * A datatype used by enchant skill types. It extends {@link IntIntHolder}.
 */
public class EnchantSkillNode extends IntIntHolder
{
	private final int _exp;
	private final int _sp;
	
	private final int[] _enchantRates = new int[5];
	
	private IntIntHolder _item;
	
	public EnchantSkillNode(StatsSet set)
	{
		super(set.getInteger("id"), set.getInteger("lvl"));
		
		_exp = set.getInteger("exp");
		_sp = set.getInteger("sp");
		
		_enchantRates[0] = set.getInteger("rate76");
		_enchantRates[1] = set.getInteger("rate77");
		_enchantRates[2] = set.getInteger("rate78");
		_enchantRates[3] = set.getInteger("rate79");
		_enchantRates[4] = set.getInteger("rate80");
		
		if (set.containsKey("itemNeeded"))
			_item = set.getIntIntHolder("itemNeeded");
	}
	
	public int getExp()
	{
		return _exp;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public int getEnchantRate(int level)
	{
		int idx = level - 76;
		if (idx < 0)
			idx = 0;
		else if (idx >= _enchantRates.length)
			idx = _enchantRates.length - 1;
		
		return _enchantRates[idx];
	}
	
	/**
	 * Enchant step for skill level (formato H5: ruta N = niveles N01-N30). 101/201/301 = +1 ... 130/230/330 = +30. Unenchanted = 0.
	 */
	public static int getEnchantStep(int skillLevel)
	{
		if (skillLevel < 101)
			return 0;
		
		return skillLevel % 100;
	}
	
	/**
	 * @return true si el nivel es el primero de una ruta (101, 201, 301 ...).
	 */
	public static boolean isRouteStart(int skillLevel)
	{
		return skillLevel > 100 && skillLevel % 100 == 1;
	}
	
	public IntIntHolder getItem()
	{
		return _item;
	}
}