package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * Mob del Instance Farm. Es un Monster normal: las kills del Kill Farm se cuentan desde Monster.doDie
 * (sirve para cualquier mob que se ponga con //loc_kill, sea de este tipo o no).
 */
public class InstanceFarmMonster extends Monster
{
	public InstanceFarmMonster(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
}
