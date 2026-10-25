package net.sf.l2j.gameserver.skills.conditions;

import java.util.HashSet;
import java.util.Set;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Env;

/**
 * Condiciones de objetivo de H5 (Mobius) que aCis no tenia:
 * <ul>
 * <li>active_effect_id="1339" : el objetivo tiene que tener el efecto de esa skill (Vortex Buster/Crusher/Slug).</li>
 * <li>mindistance="200" : el objetivo tiene que estar a esa distancia o mas (Rush, Rush Impact, Blazing Boost).</li>
 * <li>class_id_restriction="29, 30, 105" : el objetivo tiene que ser de una de esas clases (Mana Gain).</li>
 * </ul>
 */
public class ConditionTargetH5 extends Condition
{
	private final String _type;
	private final int _value;
	private final Set<Integer> _classes;
	
	private ConditionTargetH5(String type, int value, Set<Integer> classes)
	{
		_type = type;
		_value = value;
		_classes = classes;
	}
	
	public static boolean handles(String name)
	{
		return "active_effect_id".equalsIgnoreCase(name) || "mindistance".equalsIgnoreCase(name) || "class_id_restriction".equalsIgnoreCase(name);
	}
	
	public static Condition create(String name, String value)
	{
		if ("class_id_restriction".equalsIgnoreCase(name))
		{
			final Set<Integer> set = new HashSet<>();
			for (String s : value.split("[,;\\s]+"))
				if (!s.isEmpty())
					set.add(Integer.parseInt(s.trim()));
			return new ConditionTargetH5("class", 0, set);
		}
		return new ConditionTargetH5(name.toLowerCase(), Integer.decode(value.trim()), null);
	}
	
	@Override
	boolean testImpl(Env env)
	{
		final Creature target = env.getTarget();
		if (target == null)
			return false;
		
		switch (_type)
		{
			case "active_effect_id":
				return target.getFirstEffect(_value) != null;
			
			case "mindistance":
				return env.getCharacter() != null && !env.getCharacter().isInsideRadius(target, _value, false, false);
			
			case "class":
				return target instanceof Player && _classes.contains(((Player) target).getClassId().getId());
		}
		return true;
	}
}
