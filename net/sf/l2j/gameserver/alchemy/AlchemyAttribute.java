package net.sf.l2j.gameserver.alchemy;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.enums.skills.Stats;

/**
 * Attribute Alchemy - definicion de un atributo (stat que puede salir en un socket).<br>
 * El "code" es el numero que viaja al cliente (1-31) y tiene que coincidir con la tabla del ToolTip.uc.
 * <ul>
 * <li>PERCENT : multiplica el stat por (1 + valor/100). Ej: P.Atk +5%.</li>
 * <li>FLAT : suma el valor tal cual. Ej: Run Speed +10, Vampiric Rage +3 (absorbDam ya es en %).</li>
 * <li>REDUCE : multiplica el stat por (1 - valor/100). Para las vulnerabilidades (stunVuln, critVuln...): menos = mas resistencia.</li>
 * </ul>
 */
public class AlchemyAttribute
{
	public enum Mode
	{
		PERCENT,
		FLAT,
		REDUCE
	}
	
	private final int _code;
	private final String _id;
	private final String _name;
	private final AlchemyType _type;
	private final Stats _stat;
	private final Mode _mode;
	private final String _unit;
	private final double _weight;
	private final List<AlchemyTier> _tiers = new ArrayList<>();
	
	public AlchemyAttribute(int code, String id, String name, AlchemyType type, Stats stat, Mode mode, String unit, double weight)
	{
		_code = code;
		_id = id;
		_name = name;
		_type = type;
		_stat = stat;
		_mode = mode;
		_unit = unit;
		_weight = weight;
	}
	
	public int getCode()
	{
		return _code;
	}
	
	public String getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public AlchemyType getType()
	{
		return _type;
	}
	
	public Stats getStat()
	{
		return _stat;
	}
	
	public Mode getMode()
	{
		return _mode;
	}
	
	public String getUnit()
	{
		return _unit;
	}
	
	public double getWeight()
	{
		return _weight;
	}
	
	public void addTier(AlchemyTier tier)
	{
		_tiers.add(tier);
	}
	
	public List<AlchemyTier> getTiers()
	{
		return _tiers;
	}
	
	public AlchemyTier getTier(int level)
	{
		for (AlchemyTier t : _tiers)
			if (t.getLevel() == level)
				return t;
		return null;
	}
	
	public int getMaxTierLevel()
	{
		int max = 0;
		for (AlchemyTier t : _tiers)
			max = Math.max(max, t.getLevel());
		return max;
	}
}
