package net.sf.l2j.gameserver.network.serverpackets;

import Base.Skin.DressMeData;
import Base.Skin.SkinPackage;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.enums.PolyType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public class UserInfo extends L2GameServerPacket
{
	private final Player _player;
	private int _relation;
	
	public UserInfo(Player player)
	{
		_player = player;
		
		_relation = _player.isClanLeader() ? 0x40 : 0;
		
		if (_player.getSiegeState() == 1)
			_relation |= 0x180;
		if (_player.getSiegeState() == 2)
			_relation |= 0x80;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x04);
		writeD(_player.getX());
		writeD(_player.getY());
		writeD(_player.getZ());
		writeD(_player.getHeading());
		writeD(_player.getObjectId());
		writeS((_player.getPolyTemplate() != null) ? _player.getPolyTemplate().getName() : _player.getName());
		if (_player.getCustomRaceSkin() == -1)
		writeD(_player.getRace().ordinal());
		else
		writeD(_player.getCustomRaceSkin());
		writeD(_player.getAppearance().getSex().ordinal());
		if ((_player.getCustomClassSkin() == -1) && (_player.getClassIndex() == 0))
		writeD(_player.getClassId().getId());
		else if (_player.getCustomClassSkin() > -1)
		writeD(_player.getCustomClassSkin());
		else
		writeD(_player.getBaseClass());
		writeD(_player.getLevel());
		writeQ(_player.getExp());
		writeD(_player.getSTR());
		writeD(_player.getDEX());
		writeD(_player.getCON());
		writeD(_player.getINT());
		writeD(_player.getWIT());
		writeD(_player.getMEN());
		writeD(_player.getMaxHp());
		writeD((int) _player.getCurrentHp());
		writeD(_player.getMaxMp());
		writeD((int) _player.getCurrentMp());
		writeD(_player.getSp());
		writeD(_player.getCurrentLoad());
		writeD(_player.getMaxLoad());
		writeD(_player.getActiveWeaponItem() != null ? 40 : 20);
		

		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIRALL));
		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
		writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
		
		
		if (Config.ALLOW_SKIN_SYSTEM)
		{
			if (_player.getWeaponSkinOption() > 0 && getWeaponOption(_player.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			}
			
			if (_player.getShieldSkinOption() > 0 && getShieldOption(_player.getShieldSkinOption()) != null)
			{
				writeD(getShieldOption(_player.getShieldSkinOption()).getShieldId() != 0 ? getShieldOption(_player.getShieldSkinOption()).getShieldId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			}
			
			if (_player.getArmorSkinOption() > 0 && getArmorOption(_player.getArmorSkinOption()) != null)
			{
				writeD(getArmorOption(_player.getArmorSkinOption()).getGlovesId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getGlovesId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
				writeD(getArmorOption(_player.getArmorSkinOption()).getChestId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getChestId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
				writeD(getArmorOption(_player.getArmorSkinOption()).getLegsId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getLegsId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
				writeD(getArmorOption(_player.getArmorSkinOption()).getFeetId() != 0 ? getArmorOption(_player.getArmorSkinOption()).getFeetId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			}
			
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
			
			if (_player.getWeaponSkinOption() > 0 && getWeaponOption(_player.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_player.getWeaponSkinOption()).getWeaponId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			}
			
			writeD(resolveSkinHairId(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR)));
			
			if (_player.getFaceSkinOption() > 0 && getFaceOption(_player.getFaceSkinOption()) != null)
			{
				writeD(getFaceOption(_player.getFaceSkinOption()).getFaceId() != 0 ? getFaceOption(_player.getFaceSkinOption()).getFaceId() : _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
			}
			else
			{
				writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
			}
			
		}
		else
		{
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			writeD(_player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
		}


		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_REAR));
		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_NECK));
		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
		writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
		
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
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(_player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));
		}

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
		writeH(0x00);
		writeH(0x00);
		writeD(_player.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
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
		writeD(_player.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		
		writeD(_player.getPAtk(null));
		writeD(_player.getPAtkSpd());
		writeD(_player.getPDef(null));
		writeD(_player.getEvasionRate(null));
		writeD(_player.getAccuracy());
		writeD(_player.getCriticalHit(null, null));
		writeD(_player.getMAtk(null, null));
		writeD(_player.getMAtkSpd());
		writeD(_player.getPAtkSpd());
		writeD(_player.getMDef(null, null));
		writeD(_player.getPvpFlag());
		writeD(_player.getKarma());
		
		final int runSpd = _player.getStat().getBaseRunSpeed();
		final int walkSpd = _player.getStat().getBaseWalkSpeed();
		final int swimSpd = _player.getStat().getBaseSwimSpeed();
		
		writeD(runSpd);
		writeD(walkSpd);
		writeD(swimSpd);
		writeD(swimSpd);
		writeD(0);
		writeD(0);
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
		writeD((_player.isGM()) ? 1 : 0);
		
		writeS((_player.getPolyType() != PolyType.DEFAULT) ? "Morphed" : _player.getTitle());
		
		writeD(_player.getClanId());
		writeD(_player.getClanCrestId());
		writeD(_player.getAllyId());
		writeD(_player.getAllyCrestId());
		writeD(_relation);
		writeC(_player.getMountType());
		writeC(_player.getStoreType().getId());
		writeC((_player.hasDwarvenCraft()) ? 1 : 0);
		writeD(_player.getPkKills());
		writeD(_player.getPvpKills());
		
		writeH(_player.getCubics().size());
		for (int id : _player.getCubics().keySet())
			writeH(id);
		
		writeC((_player.isInPartyMatchRoom()) ? 1 : 0);
		writeD((_player.getAppearance().getInvisible() && _player.isGM()) ? (_player.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) : _player.getAbnormalEffect());
		writeC(0x00);
		writeD(_player.getClanPrivileges());
		writeH(_player.getRecomLeft());
		writeH(_player.getRecomHave());
		writeD((_player.getMountNpcId() > 0) ? _player.getMountNpcId() + 1000000 : 0);
		writeH(_player.getInventoryLimit());
		writeD(_player.getClassId().getId());
		writeD(0x00);
		writeD(_player.getMaxCp());
		writeD((int) _player.getCurrentCp());
		writeC((_player.isMounted()) ? 0 : _player.getEnchantEffect());
		if (_player.getTeam() == 1)
			writeC(0x01); // team circle around feet 1= Blue, 2 = red
		else if (_player.getTeam() == 2)
			writeC(0x02); // team circle around feet 1= Blue, 2 = red
		else
			writeC(0x00); // team circle around feet 1= Blue, 2 = red
		writeD(_player.getClanCrestLargeId());
		writeC((_player.isNoble()) ? 1 : 0);
		writeC((_player.isHero() || (_player.isGM() && Config.GM_HERO_AURA)) ? 1 : 0);
		writeC((_player.isFishing()) ? 1 : 0);
		writeLoc(_player.getFishingStance().getLoc());
		
		writeD(_player.getAppearance().getNameColor());
		writeC((_player.isRunning()) ? 0x01 : 0x00);
		writeD(_player.getPledgeClass());
		writeD(_player.getPledgeType());

		writeD(_player.getAppearance().getTitleColor());
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

	public SkinPackage getShieldOption(int option)
	{
		return DressMeData.getInstance().getShieldSkinsPackage(option);
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

}