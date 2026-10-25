package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.location.Location;

import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.enums.skills.FlyType;

public class InstantJump implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.INSTANT_JUMP
	};
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets)
	{
		Creature target = (Creature) targets[0];
		
		int px = target.getX();
		int py = target.getY();
		double ph = MathUtil.convertHeadingToDegree(target.getHeading());
		
		ph += 180;
		
		if (ph > 360)
			ph -= 360;
		
		ph = (Math.PI * ph) / 180;
		
		// H5: 25 de distancia atras del objetivo. En Interlude, si el objetivo es grande (mobs), 25 queda adentro
		// de su cuerpo y el cliente te empuja para cualquier lado: se usa el radio de los dos para quedar justo atras.
		final double dist = Math.max(25, target.getCollisionRadius() + activeChar.getCollisionRadius());
		int x = (int) (px + (dist * Math.cos(ph)));
		int y = (int) (py + (dist * Math.sin(ph)));
		int z = target.getZ();
		
		// Que no te mande atras de una pared (H5: GeoEngine.getValidLocation).
		final Location loc = GeoEngine.getInstance().canMoveToTargetLoc(target.getX(), target.getY(), target.getZ(), x, y, z);
		if (loc != null)
		{
			x = loc.getX();
			y = loc.getY();
			z = loc.getZ();
		}
		
		// Cancel current actions.
		activeChar.getAI().setIntention(IntentionType.IDLE);
		activeChar.stopMove(null);
		activeChar.abortAttack();
		activeChar.abortCast();
		
		// H5 (Shadow Step): aparece atras del objetivo, mirando para el mismo lado que el.
		activeChar.broadcastPacket(new FlyToLocation(activeChar, x, y, z, FlyType.DUMMY));
		activeChar.setXYZ(x, y, z);
		activeChar.getPosition().setHeading(target.getHeading());
		activeChar.broadcastPacket(new ValidateLocation(activeChar));
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}