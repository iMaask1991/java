package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.instancemanager.BalanceClassManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.basefuncs.Func;

/**
 * Suma el bonus fijo de ClassBalance.properties (BALANCE POR CLASE) segun la clase ACTIVA del jugador.<br>
 * Se engancha una sola vez al Player (addFuncsToNewCharacter) y consulta la clase en cada calculo,
 * asi que al cambiar de subclass / 3ra clase el valor cambia solo, sin sumarse.
 */
public class FuncBalanceClass extends Func
{
	private static final FuncBalanceClass[] INSTANCES = new FuncBalanceClass[BalanceClassManager.STATS.length];

	static
	{
		for (int i = 0; i < INSTANCES.length; i++)
			INSTANCES[i] = new FuncBalanceClass(BalanceClassManager.STATS[i], i);
	}

	private final int _index;

	private FuncBalanceClass(Stats stat, int index)
	{
		// 0x40 = mismo orden que los "add" de skills/items: suma fija despues de los multiplicadores.
		super(stat, 0x40, null, null);
		_index = index;
	}

	public static FuncBalanceClass[] getInstances()
	{
		return INSTANCES;
	}

	@Override
	public void calc(Env env)
	{
		final Creature creature = env.getCharacter();
		if (!(creature instanceof Player))
			return;

		final Player player = (Player) creature;

		// La velocidad de corrida no se toca montado (strider, wyvern, etc).
		if (stat == Stats.RUN_SPEED && player.isMounted())
			return;

		final int bonus = BalanceClassManager.getInstance().getBonus(player, _index);
		if (bonus != 0)
			env.addValue(bonus);
	}
}
