package net.sf.l2j.gameserver.skills.basefuncs;

import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Env;

/**
 * Servitor Share (estilo H5): mientras el duenio tenga el efecto de Servitor Share,
 * el servitor suma un porcentaje de los stats del duenio.
 * P. Atk./P. Def. 50%, M. Atk./M. Def. 25%, Max HP/MP 10%, Critical Rate 20%, Atk. Spd. 10%, Casting Spd. 3%.
 */
public final class FuncServitorShare extends Func
{
	public static final int SERVITOR_SHARE_SKILL_ID = 1557;
	
	private final Player _servitorOwner;
	private final double _ratio;
	
	public FuncServitorShare(Stats stat, Object owner, Player servitorOwner, double ratio)
	{
		super(stat, 0x50, owner, new LambdaConst(ratio));
		_servitorOwner = servitorOwner;
		_ratio = ratio;
	}
	
	@Override
	public void calc(Env env)
	{
		if (_servitorOwner == null || _servitorOwner.getFirstEffect(SERVITOR_SHARE_SKILL_ID) == null)
			return;
		
		final double ownerValue;
		switch (stat)
		{
			case POWER_ATTACK:
				ownerValue = _servitorOwner.getPAtk(null);
				break;
			case POWER_DEFENCE:
				ownerValue = _servitorOwner.getPDef(null);
				break;
			case MAGIC_ATTACK:
				ownerValue = _servitorOwner.getMAtk(null, null);
				break;
			case MAGIC_DEFENCE:
				ownerValue = _servitorOwner.getMDef(null, null);
				break;
			case MAX_HP:
				ownerValue = _servitorOwner.getMaxHp();
				break;
			case MAX_MP:
				ownerValue = _servitorOwner.getMaxMp();
				break;
			case POWER_ATTACK_SPEED:
				ownerValue = _servitorOwner.getPAtkSpd();
				break;
			case MAGIC_ATTACK_SPEED:
				ownerValue = _servitorOwner.getMAtkSpd();
				break;
			case CRITICAL_RATE:
				ownerValue = _servitorOwner.getCriticalHit(null, null);
				break;
			default:
				return;
		}
		env.addValue(ownerValue * _ratio);
	}
}
