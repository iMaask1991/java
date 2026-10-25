package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

public final class SkillList extends L2GameServerPacket
{
	private final List<Skill> _skills;
	
	static class Skill
	{
		public int id;
		public int level;
		public boolean passive;
		public boolean disabled;
		
		Skill(int pId, int pLevel, boolean pPassive, boolean pDisabled)
		{
			id = pId;
			level = pLevel;
			passive = pPassive;
			disabled = pDisabled;
		}
	}
	
	public SkillList()
	{
		_skills = new ArrayList<>();
	}
	
	public void addSkill(int id, int level, boolean passive, boolean disabled)
	{
		_skills.add(new Skill(id, level, passive, disabled));
	}
	
	@Override
	protected final void writeImpl() {
	    writeC(0x58);
	    List<Skill> sortedSkills = new ArrayList<>();
	    List<Skill> specialSkills = new ArrayList<>();

	    for (Skill skill : _skills) {
	        if (isSpecialSkill(skill.id)) {
	            specialSkills.add(skill);
	        } else {
	            sortedSkills.add(skill);
	        }
	    }

	    sortedSkills.addAll(specialSkills);

	    writeD(sortedSkills.size());

	    for (Skill temp : sortedSkills) {
	        writeD(temp.passive ? 1 : 0);
	        writeD(temp.level);
	        writeD(temp.id);
	        writeC(temp.disabled ? 1 : 0);
	    }
	}

	private static boolean isSpecialSkill(int id) {
	    int[] specialSkillIds = {7029, 1323, 1283, 1262, 1001, 424, 422, 340, 339, 338, 337, 336, 335, 334, 322, 318, 312, 296, 288, 256, 226, 222, 221, 197, 196, 60};
	    for (int skillId : specialSkillIds) {
	        if (id == skillId) {
	            return true;
	        }
	    }
	    return false;
	}

}