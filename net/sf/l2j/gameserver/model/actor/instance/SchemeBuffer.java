package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.BufferTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Custom profile buffer (aCis 409): individual buffs, fighter/mage presets, save/apply profiles 1-3, summon pack.
 * No scheme editor, no rebuff, no coin fees.
 */
public final class SchemeBuffer extends Folk
{
	public SchemeBuffer(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		if (val == 0)
			return "data/html/mods/buffer/index.htm";
		
		return "data/html/mods/buffer/index-" + val + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player.getPvpFlag() > 0 && Config.RESTRICT_USE_BUFFER_ON_PVPFLAG)
		{
			player.sendMessage("You can't use buffer when you are pvp flagged.");
			return;
		}
		
		if (player.isInCombat() && Config.RESTRICT_USE_BUFFER_IN_COMBAT)
		{
			player.sendMessage("You can't use buffer when you are in combat.");
			return;
		}
		
		if (player.isDead())
			return;
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		if (actualCommand.equalsIgnoreCase("Buff"))
		{
			final int page = Integer.parseInt(st.nextToken());
			final int buffId = Integer.parseInt(st.nextToken());
			final int buffLevel = Integer.parseInt(st.nextToken());
			applySingleBuff(player, buffId, buffLevel);
			showChatWindow(player, page);
		}
		else if (actualCommand.equalsIgnoreCase("bDop"))
		{
			final int option = Integer.parseInt(st.nextToken());
			switch (option)
			{
				case 1: // Cancel
					player.stopAllEffects();
					player.sendPacket(new MagicSkillUse(this, player, 1056, 1, 0, 0));
					break;
				case 2: // HP / MP / CP (player + pet/servitor si lo tiene invocado)
					player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
					player.setCurrentCp(player.getMaxCp());
					broadcastPacket(new MagicSkillUse(this, player, 1258, 1, 450, 0));
					
					final Summon pet = player.getSummon();
					if (pet != null && !pet.isDead())
					{
						pet.setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
						broadcastPacket(new MagicSkillUse(this, pet, 1258, 1, 450, 0));
					}
					break;
				case 3: // Summoner pack (replaces old Rebuff)
					buffSummon(player);
					break;
				case 4: // Fighter preset
					applyPresetBuffs(player, Config.FIGHTER_SKILL_LIST);
					break;
				case 5: // Mage preset
					applyPresetBuffs(player, Config.MAGE_SKILL_LIST);
					break;
			}
			showChatWindow(player, 0);
		}
		else if (actualCommand.equalsIgnoreCase("profileBuff"))
		{
			applyProfile(player, Integer.parseInt(st.nextToken()));
			showChatWindow(player, 0);
		}
		else if (actualCommand.equalsIgnoreCase("saveprofile"))
		{
			saveProfile(player, Integer.parseInt(st.nextToken()));
			showChatWindow(player, 1);
		}
		else if (actualCommand.equalsIgnoreCase("summoner"))
		{
			buffSummon(player);
			showChatWindow(player, 0);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private void buffSummon(Player player)
	{
		final Summon servitor = player.getSummon();
		if (servitor == null || servitor.isDead())
		{
			player.sendMessage("You don't have a servitor summoned.");
			return;
		}
		
		applyPresetBuffs(servitor, Config.SUMMONER_SKILL_LIST);
		player.sendMessage("Summon buffs applied.");
	}
	
	private void applySingleBuff(Player player, int buffId, int buffLevel)
	{
		if (buffId == 0)
			return;
		
		// Solo buffs de la lista BufferBuffs (Buffer.properties): evita que con un bypass armado tiren cualquier skill.
		if (!isAllowedSkill(buffId))
			return;
		
		final int level = resolveSkillLevel(buffId);
		final L2Skill skill = SkillTable.getInstance().getInfo(buffId, level);
		if (skill != null)
		{
			skill.getEffects(this, player);
			player.sendPacket(new MagicSkillUse(this, player, buffId, level, 0, 0));
		}
	}
	
	private void applyPresetBuffs(Creature target, List<Integer> skillIds)
	{
		if (skillIds == null || skillIds.isEmpty())
			return;
		
		target.setBatchEffectUpdate(true);
		try
		{
			for (int skillId : skillIds)
			{
				if (!isAllowedSkill(skillId))
					continue;
				
				final L2Skill skill = SkillTable.getInstance().getInfo(skillId, resolveSkillLevel(skillId));
				if (skill != null)
					skill.getEffects(this, target);
			}
		}
		finally
		{
			target.setBatchEffectUpdate(false);
		}
		
		target.updateEffectIcons();
		if (target instanceof Player)
		{
			final Player player = (Player) target;
			player.updateAndBroadcastStatus(2);
			player.sendPacket(new MagicSkillUse(this, player, 1204, 1, 0, 0));
		}
		else if (target instanceof Summon)
		{
			final Summon summon = (Summon) target;
			summon.updateAndBroadcastStatus(1);
			broadcastPacket(new MagicSkillUse(this, summon, 1204, 1, 0, 0));
		}
	}
	
	private void applyProfile(Player player, int slot)
	{
		final List<Integer> skills = BufferTable.getInstance().getScheme(player.getObjectId(), String.valueOf(slot));
		if (skills == null || skills.isEmpty())
		{
			player.sendMessage("Profile " + slot + " is empty. Save it first.");
			return;
		}
		
		applyPresetBuffs(player, skills);
		player.sendMessage("Profile " + slot + " applied.");
	}
	
	private void saveProfile(Player player, int slot)
	{
		final ArrayList<Integer> skills = new ArrayList<>(collectPlayerBuffIds(player));
		if (skills.isEmpty())
		{
			player.sendMessage("You have no buffs to save.");
			return;
		}
		
		final String schemeName = String.valueOf(slot);
		if (!BufferTable.getInstance().setScheme(player.getObjectId(), schemeName, skills))
		{
			player.sendMessage("Maximum profiles amount is already reached.");
			return;
		}
		
		BufferTable.getInstance().persistScheme(player.getObjectId(), schemeName);
		player.sendMessage("Profile " + slot + " saved (" + skills.size() + " buffs).");
	}
	
	private static List<Integer> collectPlayerBuffIds(Player player)
	{
		final List<Integer> skills = new ArrayList<>();
		for (L2Effect effect : player.getAllEffects())
		{
			if (effect == null || effect.getSkill() == null)
				continue;
			
			final L2Skill skill = effect.getSkill();
			final int skillId = skill.getId();
			
			if (!isAllowedSkill(skillId))
				continue;
			
			if (isDisease(skillId))
			{
				if (!skills.contains(skillId))
					skills.add(skillId);
			}
			else
			{
				// Los buffs que salen por chance (triggered) no se guardan: los da el skill que los dispara.
				if (skill.isToggle() || skill.isOffensive() || skill.isHeroSkill() || skill.isTriggeredSkill())
					continue;
				
				final L2SkillType type = skill.getSkillType();
				if (type != L2SkillType.BUFF && type != L2SkillType.CONT && !skill.isDance())
					continue;
				
				if (!skills.contains(skillId))
					skills.add(skillId);
			}
			
			if (skills.size() >= Config.BUFFER_MAX_SKILLS)
				break;
		}
		return skills;
	}
	
	private static boolean isDisease(int skillId)
	{
		return skillId == 4553 || skillId == 4554;
	}
	
	private static boolean isAllowedSkill(int skillId)
	{
		return isDisease(skillId) || (Config.BUFFER_BUFFLIST != null && Config.BUFFER_BUFFLIST.containsKey(skillId));
	}
	
	private static int resolveSkillLevel(int skillId)
	{
		if (isDisease(skillId))
			return 4;
		
		return BufferTable.getRetailBuffLevel(skillId);
	}
}
