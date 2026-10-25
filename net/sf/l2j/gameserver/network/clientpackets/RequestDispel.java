package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;

/**
 * H5 / Essence-style self-cancel buff (Ctrl+Click).<br>
 * Rejects debuffs and non-dispelable effects.
 */
public final class RequestDispel extends L2GameClientPacket
{
	private int _objectId;
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_skillId = readD();
		// HF: level as int. Essence: short level + short subLevel (4 bytes either way).
		_skillLevel = readD() & 0xFFFF;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		cancelBuff(player, _objectId, _skillId, _skillLevel);
	}
	
	public static void cancelBuff(Player player, int objectId, int skillId, int skillLevel)
	{
		if (player == null || skillId <= 0)
			return;
		
		L2Skill skill = null;
		if (skillLevel > 0)
			skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		
		// Fallback: resolve from active effect (Interface may send odd levels).
		if (skill == null && player.getObjectId() == objectId)
		{
			final L2Effect effect = player.getFirstEffect(skillId);
			if (effect != null)
				skill = effect.getSkill();
		}
		
		if (skill == null)
			return;
		
		if (!skill.canBeDispeled() || skill.isDebuff() || skill.isStayAfterDeath())
			return;
		
		if (skill.isDance())
			return;
		
		if (player.getObjectId() == objectId)
		{
			player.stopSkillEffects(skillId);
			return;
		}
		
		final Summon summon = player.getSummon();
		if (summon != null && summon.getObjectId() == objectId)
			summon.stopSkillEffects(skillId);
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:48/4B RequestDispel";
	}
}
