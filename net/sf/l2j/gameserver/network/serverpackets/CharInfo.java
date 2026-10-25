package net.sf.l2j.gameserver.network.serverpackets;

import Base.Skin.DressMeData;
import Base.Skin.SkinPackage;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public class CharInfo extends L2GameServerPacket
{
	private final Player _player;
	private final Inventory _inv;
	
	public CharInfo(Player player)
	{
		_player = player;
		_inv = _player.getInventory();
	}
	
	@Override
	protected final void writeImpl()
	{
		boolean gmSeeInvis = false;
		boolean tournament_observer = false;
		boolean tournament_pt = false;
		boolean tournament_start = false;
		boolean tournament_zone = false;
		boolean Olympiad_start = false;
		boolean Olympiad_observer = false;
		
		final Party party = _player.getParty();
		
		// Event Gear: los jugadores ven "SanSara", sin clan; el GM ve el nick real.
		final Player egViewer = (getClient() != null) ? getClient().getPlayer() : null;
		final boolean egMask = net.sf.l2j.gameserver.events.eventgear.EventGearManager.hideFor(_player, egViewer);
		final boolean egClan = egMask && net.sf.l2j.gameserver.events.eventgear.EventGearConfig.HIDE_CLAN;
		
		if (_player.getAppearance().getInvisible())
		{
			Player tmp = getClient().getPlayer();
			if (tmp != null)
			{
				if (_player.isArenaProtection() && tmp.isArenaObserv())
					tournament_observer = true;
				else if (_player.isArenaProtection() && party != null && !tmp.isArenaAttack() && tmp.isInArenaEvent() && party.getMembers().contains(tmp))
					tournament_pt = true;
				else if (_player.isArenaProtection() && tmp.isArenaAttack() && !tmp.isInObserverMode())
					tournament_start = true;
				else if ((_player.isInsideZone(ZoneId.TOURNAMENT) && !_player.isGM() && !_player.isOlympiadProtection()) && tmp.isInsideZone(ZoneId.TOURNAMENT) && !tmp.isInObserverMode() && !tmp.isOlympiadProtection())
					tournament_zone = true;
				else if (tmp.isGM())
					gmSeeInvis = true;
			}
		}

		if (_player.isOlympiadProtection())
		{
			Player tmp = getClient().getPlayer();
			if (tmp != null)
			{
				if (tmp.isOlympiadProtection() && !tmp.isInObserverMode())
					Olympiad_start = true;
				else if (tmp.isInObserverMode() && !tmp.isArenaObserv())
					Olympiad_observer = true;
			}
		}
		
		writeC(0x03);
		writeD(_player.getX());
		writeD(_player.getY());
		writeD(_player.getZ());
		writeD((_player.getBoat() == null) ? 0 : _player.getBoat().getObjectId());
		writeD(_player.getObjectId());
		writeS(egMask ? net.sf.l2j.gameserver.events.eventgear.EventGearConfig.FAKE_NAME : _player.getName());
		if (_player.getCustomRaceSkin() == -1)
		writeD(_player.getRace().ordinal());
		else
		writeD(_player.getCustomRaceSkin());
		writeD(_player.getAppearance().getSex().ordinal());
		if ((_player.getCustomClassSkin() == -1) && (_player.getClassIndex() == 0))
		{
		writeD(_player.getClassId().getId());
		}
		else
		{
		if (_player.getCustomClassSkin() > -1)
		{
		writeD(_player.getCustomClassSkin());
		}
		else
		{
		writeD(_player.getBaseClass());
		}
	    }
		
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
		writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));

		if (Config.ALLOW_SKIN_SYSTEM)
		{
			if (_player.getWeaponSkinOption() > 0 && getWeaponOption(_player.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			
			if (_player.getShieldSkinOption() > 0 && getShieldOption(_player.getShieldSkinOption()) != null)
			{
				writeD(getShieldOption(_player.getShieldSkinOption()).getShieldId() != 0 ? getShieldOption(_player.getShieldSkinOption()).getShieldId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			}
			
			if (_player.getArmorSkinOption() > 0 && getArmorOption(_player.getArmorSkinOption()) != null)
			{
				writeD(getArmorOption(_player.getArmorSkinOption()).getGlovesId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getGlovesId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(getArmorOption(_player.getArmorSkinOption()).getChestId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getChestId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(getArmorOption(_player.getArmorSkinOption()).getLegsId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getLegsId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(getArmorOption(_player.getArmorSkinOption()).getFeetId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getFeetId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			}
			
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			
			if (_player.getWeaponSkinOption() > 0 && getWeaponOption(_player.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			
			writeD(resolveSkinHairId(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR)));
			
			if (_player.getFaceSkinOption() > 0 && getFaceOption(_player.getFaceSkinOption()) != null)
			{
				writeD(getFaceOption(_player.getFaceSkinOption()).getFaceId() != 0 ? getFaceOption(_player.getFaceSkinOption()).getFaceId() : _player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			}
			
		}
		else
		{
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
		}

		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		
		writeD(_player.getPvpFlag());
		writeD(_player.getKarma());
		writeD(_player.getMAtkSpd());
		writeD(_player.getPAtkSpd());
		writeD(_player.getPvpFlag());
		writeD(_player.getKarma());
		
		final int runSpd = _player.getStat().getBaseRunSpeed();
		final int walkSpd = _player.getStat().getBaseWalkSpeed();
		final int swimSpd = _player.getStat().getBaseSwimSpeed();
		
		writeD(runSpd);
		writeD(walkSpd);
		writeD(swimSpd);
		writeD(swimSpd);
		writeD(runSpd);
		writeD(walkSpd);
		writeD((_player.isFlying()) ? runSpd : 0);
		writeD((_player.isFlying()) ? walkSpd : 0);
		
		writeF(_player.getStat().getMovementSpeedMultiplier());
		writeF(_player.getStat().getAttackSpeedMultiplier());
		
		final Summon summon = _player.getSummon();
		if (_player.getMountType() != 0 && summon != null)
		{
			writeF(summon.getCollisionRadius());
			writeF(summon.getCollisionHeight());
		}
		else
		{
			if (_player.getCustomRaceSkin() == -1)
			{	
			writeF(_player.getCollisionRadius());
			writeF(_player.getCollisionHeight());
			}
			else
			{
				writeF(_player.getCustomSkinTemplate().getCollisionRadius());
				writeF(_player.getCustomSkinTemplate().getCollisionHeight());
			}
		}
		
		writeD(_player.getAppearance().getHairStyle());
		writeD(_player.getAppearance().getHairColor());
		writeD(_player.getAppearance().getFace());
		
		writeS((gmSeeInvis) ? "Invisible" : _player.getTitle());
		
		writeD(egClan ? 0 : _player.getClanId());
		writeD(egClan ? 0 : _player.getClanCrestId());
		writeD(egClan ? 0 : _player.getAllyId());
		writeD(egClan ? 0 : _player.getAllyCrestId());
		
		writeD(0);
		
		writeC((_player.isSitting()) ? 0 : 1);
		writeC((_player.isRunning()) ? 1 : 0);
		writeC((_player.isInCombat()) ? 1 : 0);
		writeC((_player.isAlikeDead()) ? 1 : 0);
		
		if (gmSeeInvis || tournament_pt || tournament_start || tournament_observer || Olympiad_start || Olympiad_observer || tournament_zone)
			writeC(0);
		else
		writeC((!gmSeeInvis && _player.getAppearance().getInvisible()) ? 1 : 0);
		
		writeC(_player.getMountType());
		writeC(_player.getStoreType().getId());
		
		writeH(_player.getCubics().size());
		for (int id : _player.getCubics().keySet())
			writeH(id);
		
		writeC((_player.isInPartyMatchRoom()) ? 1 : 0);
		writeD((gmSeeInvis) ? (_player.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) : _player.getAbnormalEffect());
		writeC(_player.getRecomLeft());
		writeH(_player.getRecomHave());
		writeD(_player.getClassId().getId());
		writeD(_player.getMaxCp());
		writeD((int) _player.getCurrentCp());
		writeC((_player.isMounted()) ? 0 : _player.getEnchantEffect());
		if (_player.getTeam() == 1)
			writeC(0x01); // team circle around feet 1= Blue, 2 = red
		else if (_player.getTeam() == 2)
			writeC(0x02); // team circle around feet 1= Blue, 2 = red
		else
			writeC(0x00); // team circle around feet 1= Blue, 2 = red
		writeD(egClan ? 0 : _player.getClanCrestLargeId());
		writeC((_player.isNoble()) ? 1 : 0);
		writeC((egMask && net.sf.l2j.gameserver.events.eventgear.EventGearConfig.HIDE_HERO_AURA) ? 0 : ((_player.isHero() || (_player.isGM() && Config.GM_HERO_AURA)) ? 1 : 0));
		writeC((_player.isFishing()) ? 1 : 0);
		writeLoc(_player.getFishingStance().getLoc());
		writeD((egMask && net.sf.l2j.gameserver.events.eventgear.EventGearConfig.FORCE_COLORS) ? net.sf.l2j.gameserver.events.eventgear.EventGearConfig.NAME_COLOR : _player.getAppearance().getNameColor());
		writeD(_player.getHeading());
		writeD(_player.getPledgeClass());
		writeD(_player.getPledgeType());
		writeD((egMask && net.sf.l2j.gameserver.events.eventgear.EventGearConfig.FORCE_COLORS) ? net.sf.l2j.gameserver.events.eventgear.EventGearConfig.TITLE_COLOR : _player.getAppearance().getTitleColor());
		writeD(CursedWeaponManager.getInstance().getCurrentStage(_player.getCursedWeaponEquippedId()));
	}
	
public SkinPackage getArmorOption(int option)
{
	return (DressMeData.getInstance().getArmorSkinsPackage(option));
}

public SkinPackage getWeaponOption(int option)
{
	return DressMeData.getInstance().getWeaponSkinsPackage(option);
}

public SkinPackage getHairOption(int option)
{
	return DressMeData.getInstance().getHairSkinsPackage(option);
}

public SkinPackage getFaceOption(int option)
{
	return DressMeData.getInstance().getFaceSkinsPackage(option);
}

/** Hair skin explícita, o hairId del set de armor, o el item real. */
private int resolveSkinHairId(int fallback)
{
	if (_player.getHairSkinOption() > 0 && getHairOption(_player.getHairSkinOption()) != null)
	{
		final int hairId = getHairOption(_player.getHairSkinOption()).getHairId();
		if (hairId != 0)
			return hairId;
	}
	
	if (_player.getArmorSkinOption() > 0 && getArmorOption(_player.getArmorSkinOption()) != null)
	{
		final int hairId = getArmorOption(_player.getArmorSkinOption()).getHairId();
		if (hairId != 0)
			return hairId;
	}
	
	return fallback;
}

public SkinPackage getShieldOption(int option)
{
	return DressMeData.getInstance().getShieldSkinsPackage(option);
}		

}