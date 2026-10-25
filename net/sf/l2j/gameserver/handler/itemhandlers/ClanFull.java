package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class ClanFull implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		final Player activeChar = (Player) playable;
		
	 	if (activeChar.getClan() == null)
	 	{
	 		activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
	 		return;
	 	}
	 	else if (!activeChar.isClanLeader())
	 	{
	 		activeChar.sendPacket(SystemMessageId.NOT_AUTHORIZED_TO_BESTOW_RIGHTS);
	 		return;
	 	}
	 	else if (activeChar.getClan().getLevel() == 8)
	 	{
	 		activeChar.sendMessage("Your clan is already level 8.");
	 	}
	 	else
	 	{
	 		activeChar.getClan().changeLevel(8);
	 		activeChar.getClan().addReputationScore(20000);
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(370, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(371, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(372, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(373, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(374, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(375, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(376, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(377, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(378, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(379, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(380, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(381, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(382, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(383, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(384, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(385, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(386, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(387, 3));
			activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(388, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(389, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(390, 3));
	 		activeChar.getClan().addNewSkill(SkillTable.getInstance().getInfo(391, 1));	 		
	 		activeChar.getClan().broadcastClanStatus();
	 		activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5103, 1, 1000, 0));
	 		playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
	 	}
	}
	
}