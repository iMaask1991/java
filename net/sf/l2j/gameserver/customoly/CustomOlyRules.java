package net.sf.l2j.gameserver.customoly;

import java.util.List;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public class CustomOlyRules
{
	public static String canJoin(Player player, int teamSize)
	{
		if (!Config.CUSTOM_OLY_ENABLED)
			return "El evento esta desactivado.";
		if (CustomOlyManager.getInstance().getPhase() != CustomOlyPhase.COMPETITION)
			return "La competencia no esta abierta.";
		if (!CustomOlyEvent.getInstance().isSessionOpen())
			return "El NPC no esta en ventana de registro.";
		if (!player.isNoble())
			return "Tenés que ser Noblesse.";
		if (player.getClassId().level() != 3)
			return "Solo clases de 3ra.";
		if (player.getBaseClass() != player.getClassId().getId())
			return "Solo Main Class.";
		if (CustomOlyManager.getInstance().isFrozen(player))
			return "Ya clasificaste. Esperá la proxima season.";
		if (player.isAio())
			return "AIO no puede participar.";
		if (player.isHero() && !CustomOlyManager.getInstance().isOlyAwardedHero(player.getObjectId()))
			return "Hero por donate no puede participar.";
		if (player.isCursedWeaponEquipped() || player.isInStoreMode() || player.getKarma() > 0)
			return "No cumplis los requisitos.";
		if (OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode())
			return "Estas en Olympiad retail.";
		if (player.isInArenaEvent() || player.isArenaAttack() || player.isArenaProtection())
			return "Ya estas en un evento de arena.";
		
		if (teamSize == 1 && player.isInParty())
			return "Salite de la party para 1v1.";
		
		if (teamSize > 1)
		{
			if (!player.isInParty() || !player.getParty().isLeader(player))
				return "El lider de party tiene que registrar.";
			if (player.getParty().getMembersCount() != teamSize)
				return "La party tiene que ser de " + teamSize + ".";
			for (Player m : player.getParty().getMembers())
			{
				if (m == player)
					continue;
				String err = canJoinMember(m);
				if (err != null)
					return m.getName() + ": " + err;
			}
		}
		
		String gear = checkGear(player);
		if (gear != null)
			return gear;
		
		if (teamSize > 1)
		{
			for (Player m : player.getParty().getMembers())
			{
				String gearM = checkGear(m);
				if (gearM != null)
					return m.getName() + ": " + gearM;
			}
		}
		
		if (!Config.CUSTOM_OLY_ALLOW_SAME_HWID && CustomOlyArena.getInstance().isHwidBusy(player))
			return "Solo 1 personaje por PC.";
		
		return null;
	}
	
	private static String canJoinMember(Player player)
	{
		if (!player.isNoble())
			return "no es Noblesse";
		if (player.getClassId().level() != 3)
			return "no es 3ra";
		if (player.getBaseClass() != player.getClassId().getId())
			return "no es Main Class";
		if (CustomOlyManager.getInstance().isFrozen(player))
			return "ya clasifico";
		if (player.isAio() || player.isCursedWeaponEquipped() || player.isInStoreMode() || player.getKarma() > 0)
			return "no cumple requisitos";
		if (player.isHero() && !CustomOlyManager.getInstance().isOlyAwardedHero(player.getObjectId()))
			return "Hero por donate";
		return null;
	}
	
	public static String checkGear(Player player)
	{
		List<ItemInstance> items = player.getInventory().getPaperdollItems();
		if (items == null)
			return null;
		
		for (ItemInstance item : items)
		{
			if (item == null)
				continue;
			int id = item.getItemId();
			if (Config.CUSTOM_OLY_DENY_ITEMS.contains(id))
				return "Item prohibido: " + item.getItemName();
			if (!Config.CUSTOM_OLY_ALLOW_ITEMS.isEmpty() && !Config.CUSTOM_OLY_ALLOW_ITEMS.contains(id))
				return "Item no permitido: " + item.getItemName();
			CrystalType grade = item.getItem().getCrystalType();
			if (grade != null && grade.getId() > CrystalType.S.getId())
				return "Maximo Grado S.";
			if (Config.CUSTOM_OLY_ENCHANT_CAP >= 0 && item.getEnchantLevel() > Config.CUSTOM_OLY_ENCHANT_CAP)
				return "Max enchant +" + Config.CUSTOM_OLY_ENCHANT_CAP + ".";
		}
		return null;
	}
	
	public static void prepareFighter(Player player)
	{
		player.stopAllEffects();
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		
		Map<Integer, Integer> buffs = player.isMageClass() ? Config.CUSTOM_OLY_MAGE_BUFFS : Config.CUSTOM_OLY_FIGHTER_BUFFS;
		if (buffs != null)
		{
			for (Map.Entry<Integer, Integer> e : buffs.entrySet())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(e.getKey(), e.getValue());
				if (skill != null)
					skill.getEffects(player, player);
			}
		}
		
		Summon summon = player.getSummon();
		if (summon != null)
		{
			summon.setCurrentHp(summon.getMaxHp());
			summon.setCurrentMp(summon.getMaxMp());
		}
		
		player.sendPacket(new ExShowScreenMessage("Buffs aplicados. Preparados.", 3000));
	}
	
	public static String className(int classId)
	{
		ClassId cid = ClassId.VALUES[classId];
		return cid != null ? cid.toString() : String.valueOf(classId);
	}
}
