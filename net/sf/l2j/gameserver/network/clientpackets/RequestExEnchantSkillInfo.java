package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.actor.instance.SkillEnchantManager;
import net.sf.l2j.gameserver.model.holder.skillnode.EnchantSkillNode;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillInfo;

public final class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLevel = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_skillId <= 0 || _skillLevel <= 0)
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final Folk folk = player.getCurrentFolk();
		if (folk == null || !folk.canInteract(player))
			return;
		
		if (player.getSkillLevel(_skillId) >= _skillLevel)
			return;
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		if (skill == null)
			return;
		
		if (!SkillEnchantManager.isCustom(folk) && !folk.getTemplate().canTeach(player.getClassId()))
			return;
		
		final EnchantSkillNode esn = SkillTreeData.getInstance().getEnchantSkillFor(player, _skillId, _skillLevel);
		if (esn == null)
			return;
		
		final int rate = SkillEnchantManager.getChance(player, esn);
		final ExEnchantSkillInfo esi = new ExEnchantSkillInfo(_skillId, _skillLevel, esn.getSp(), esn.getExp(), rate);
		
		final boolean safeMode = SkillEnchantManager.isSafe(player, folk);
		if (safeMode)
		{
			if (Config.SKILL_ENCHANT_SAFE_ITEM_ID > 0 && Config.SKILL_ENCHANT_SAFE_ITEM_COUNT > 0)
				esi.addRequirement(4, Config.SKILL_ENCHANT_SAFE_ITEM_ID, Config.SKILL_ENCHANT_SAFE_ITEM_COUNT, 0);
		}
		else if (Config.ES_SP_BOOK_NEEDED && esn.getItem() != null)
			esi.addRequirement(4, esn.getItem().getId(), esn.getItem().getValue(), 0);
		
		sendPacket(esi);
	}
}
