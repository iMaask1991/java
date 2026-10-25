package Dolls;
/**
 * Terius Dolls.
 */
public class Dolls {
    private int id;
    private int skillId;
    private int skillLvl;

    public Dolls(int id, int skillId, int skillLvl) {
        this.id = id;
        this.skillId = skillId;
        this.skillLvl = skillLvl;
    }

    public int getId() {
        return id;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLvl() {
        return skillLvl;
    }
}
