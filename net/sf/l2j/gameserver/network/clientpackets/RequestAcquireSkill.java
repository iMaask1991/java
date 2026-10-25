package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.SkillTreeData;
import net.sf.l2j.gameserver.data.xml.SpellbookData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.ClassMaster;
import net.sf.l2j.gameserver.model.actor.instance.Fisherman;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.actor.instance.Rebirth;
import net.sf.l2j.gameserver.model.actor.instance.VillageMaster;
import net.sf.l2j.gameserver.model.holder.skillnode.ClanSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.FishingSkillNode;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class RequestAcquireSkill extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLevel;
	private int _skillType;
	
	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLevel = readD();
		_skillType = readD();
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
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		if (skill == null)
			return;
		
		switch (_skillType)
		{
			case 0:
				int skillLvl = player.getSkillLevel(_skillId);
				if (skillLvl >= _skillLevel)
					return;
				
				if (skillLvl != _skillLevel - 1)
					return;
				
				final GeneralSkillNode gsn = player.getTemplate().findSkill(_skillId, _skillLevel);
				if (gsn == null)
					return;
				
				if (player.getSp() < gsn.getCorrectedCost())
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					folk.showSkillList(player);
					return;
				}
				
				final int bookId = SpellbookData.getInstance().getBookForSkill(_skillId, _skillLevel);
				if (bookId > 0 && !player.destroyItemByItemId("SkillLearn", bookId, 1, folk, true))
				{
					player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					folk.showSkillList(player);
					return;
				}
				
				player.removeExpAndSp(0, gsn.getCorrectedCost());
				player.addSkill(skill, true, true);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1).addSkillName(skill));
				player.sendSkillList();
				folk.showSkillList(player);
				break;
			
			case 1:
				// Class Master / Rebirth / Fishing: sequential next level only.
				skillLvl = player.getSkillLevel(_skillId);
				if (skillLvl >= _skillLevel)
					return;
				
				if (skillLvl != _skillLevel - 1)
					return;
				
				final FishingSkillNode fsn = getCommonSkillNode(folk, player, _skillId, _skillLevel);
				if (fsn == null)
					return;
				
				if (!player.destroyItemByItemId("Consume", fsn.getItemId(), fsn.getItemCount(), folk, true))
				{
					player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					refreshCommonSkillList(folk, player);
					return;
				}
				
				player.addSkill(skill, true, true);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1).addSkillName(skill));
				player.sendSkillList();
				refreshCommonSkillList(folk, player);
				break;
			
			case 2:
				if (!player.isClanLeader())
					return;
				
				final ClanSkillNode csn = SkillTreeData.getInstance().getClanSkillFor(player, _skillId, _skillLevel);
				if (csn == null)
					return;
				
				if (player.getClan().getReputationScore() < csn.getCost())
				{
					player.sendPacket(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
					VillageMaster.showPledgeSkillList(player);
					return;
				}
				
				if (Config.LIFE_CRYSTAL_NEEDED && !player.destroyItemByItemId("Consume", csn.getItemId(), 1, folk, true))
				{
					player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					VillageMaster.showPledgeSkillList(player);
					return;
				}
				
				player.getClan().takeReputationScore(csn.getCost());
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(csn.getCost()));
				player.getClan().addNewSkill(skill);
				VillageMaster.showPledgeSkillList(player);
				return;
		}
	}
	
	private static FishingSkillNode getCommonSkillNode(Folk folk, Player player, int skillId, int skillLevel)
	{
		if (folk instanceof ClassMaster)
			return SkillTreeData.getInstance().getClassMasterSkillFor(player, skillId, skillLevel);
		
		if (folk instanceof Rebirth)
			return SkillTreeData.getInstance().getRebirthSkillFor(player, skillId, skillLevel);
		
		return SkillTreeData.getInstance().getFishingSkillFor(player, skillId, skillLevel);
	}
	
	private static void refreshCommonSkillList(Folk folk, Player player)
	{
		if (folk instanceof ClassMaster)
			ClassMaster.showClassMasterSkillList(player);
		else if (folk instanceof Rebirth)
			Rebirth.showRebirthSkillList(player);
		else
			Fisherman.showFishSkillList(player);
	}
}
