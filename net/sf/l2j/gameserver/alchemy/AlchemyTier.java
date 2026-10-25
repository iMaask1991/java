package net.sf.l2j.gameserver.alchemy;

/**
 * Attribute Alchemy - un tier (rango de valor + chance) de un atributo.
 */
public class AlchemyTier
{
	private final int _level;
	private final double _min;
	private final double _max;
	private final double _chance;
	
	public AlchemyTier(int level, double min, double max, double chance)
	{
		_level = level;
		_min = min;
		_max = max;
		_chance = chance;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public double getMin()
	{
		return _min;
	}
	
	public double getMax()
	{
		return _max;
	}
	
	public double getChance()
	{
		return _chance;
	}
}
