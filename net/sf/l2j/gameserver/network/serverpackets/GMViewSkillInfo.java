package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;

public class GMViewSkillInfo extends L2GameServerPacket {
    private final Player _player;
    private final List<L2Skill> _skills;
    private final boolean _isWearingFormalWear;
    private final boolean _isClanDisabled;

    public GMViewSkillInfo(Player player) {
        _player = player;
        Collection<L2Skill> allSkills = player.getSkills().values();
        _skills = new ArrayList<>(allSkills.size());
        List<L2Skill> toggleSkills = new ArrayList<>();

        _isWearingFormalWear = player.isWearingFormalWear();
        _isClanDisabled = player.getClan() != null && player.getClan().getReputationScore() < 0;

        // Separar habilidades toggle de las no-toggle
        for (L2Skill skill : allSkills) {
            if (skill.isToggle()) {
                toggleSkills.add(skill);
            } else {
                _skills.add(skill);
            }
        }

        // Agregar las habilidades toggle al final de la lista
        _skills.addAll(toggleSkills);
    }

    @Override
    protected final void writeImpl() {
        writeC(0x91);
        writeS(_player.getName());
        writeD(_skills.size());

        for (L2Skill skill : _skills) {
            writeD(skill.isPassive() ? 1 : 0);
            writeD(skill.getLevel());
            writeD(skill.getId());
            writeC(_isWearingFormalWear || (skill.isClanSkill() && _isClanDisabled) ? 1 : 0);
        }
    }
}
