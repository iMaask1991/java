package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class Agathion1 implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		final Player activeChar = (Player) playable;
		
	    if (activeChar.getAgathion() != null)
	    {
	        activeChar.getAgathion().unSummon(activeChar);
	        activeChar.sendMessage("Agathion was removed");
	        return;
	    }
	    else {
		activeChar.doCast(SkillTable.getInstance().getInfo(9001, 1));
		activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, Config.ANIMATION_SUMMON_SKILL_ID_AGATHION, 1, 2000, 0));
	    }
	}
	
}