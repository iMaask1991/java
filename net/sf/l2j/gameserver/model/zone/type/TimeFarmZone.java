package net.sf.l2j.gameserver.model.zone.type;

import java.util.HashSet;
import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.instancemanager.TimeZoneManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.SpawnZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;
import net.sf.l2j.gameserver.timezone.TimeFarmZoneManager;

public class TimeFarmZone extends SpawnZoneType
{
	private String _zoneName = "Unknown";
	private boolean _inviteParty = true;
	private final Set<String> _classes = new HashSet<>();
	private boolean _noDropProtection = false;
	private boolean _blockMultiplePlayers = false;
	private int _centerX;
	private int _centerY;
	private int _centerZ;
	private boolean _loseBuff = true;
	private boolean _enabled = true;
	private boolean _registered = false;
	
	public TimeFarmZone(int id)
	{
		super(id);
		_inviteParty = false;
	}
	
	public void setZoneCenter(int x, int y, int z)
	{
		_centerX = x;
		_centerY = y;
		_centerZ = z;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("name"))
		{
			setZoneName(value);
			if (!_registered && Config.ENABLE_TIME_ZONE_SYSTEM)
			{
				TimeFarmZoneManager.registerZone(this);
				_registered = true;
			}
		}
		else if (name.equalsIgnoreCase("EnabledZone"))
			_enabled = Boolean.parseBoolean(value);
		else if (name.equalsIgnoreCase("LoseBuff"))
			_loseBuff = Boolean.parseBoolean(value);
		else if (name.equalsIgnoreCase("InviteParty"))
			_inviteParty = Boolean.parseBoolean(value);
		else if (name.equalsIgnoreCase("BlockedClasses"))
		{
			for (String classId : value.split(";"))
				_classes.add(classId.trim());
		}
		else if (name.equalsIgnoreCase("NoDropProtection"))
			_noDropProtection = Boolean.parseBoolean(value);
		else if (name.equalsIgnoreCase("BlockMultiplePlayers"))
			_blockMultiplePlayers = Boolean.parseBoolean(value);
		else
			super.setParameter(name, value);
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public boolean isBlockMultiplePlayers()
	{
		return _blockMultiplePlayers;
	}
	
	public boolean canLoseBuff()
	{
		return _loseBuff;
	}
	
	public boolean isInvitePartyAllowed()
	{
		return _inviteParty;
	}
	
	public boolean isNoDropProtection()
	{
		return _noDropProtection;
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (!isEnabled())
			return;
		
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			
			character.setInsideZone(ZoneId.TIME_FARM_ZONE, true);
			// Cancel pending unflag from previous TimeFarmZone exit (zone rotate/teleport).
			PvpFlagTaskManager.getInstance().remove(player);
			player.setPvpFlag(1);
			player.broadcastUserInfo();
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
			
			final String classIdStr = String.valueOf(player.getClassId().getId());
			if (!player.isGM() && _classes.contains(classIdStr))
			{
				player.sendMessage("Your class is not allowed in this zone.");
				ThreadPool.schedule(() -> character.teleportTo(83597, 147888, -3405, 0), 1000);
				return;
			}
			
			if (_blockMultiplePlayers)
				maxPlayersOnArea(player);
			
			if (!_inviteParty && player.getParty() != null)
				player.getParty().removePartyMember(player, MessageType.LEFT);
		}
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (!isEnabled())
			return;
		
		if (character instanceof Player)
		{
			final Player player = (Player) character;
			character.setInsideZone(ZoneId.TIME_FARM_ZONE, false);
			player.updatePvPFlag(0);
			player.broadcastUserInfo();
			PvpFlagTaskManager.getInstance().add(player, Config.PVP_NORMAL_TIME);
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
		}
	}
	
	public boolean maxPlayersOnArea(Player activeChar)
	{
		return TimeZoneManager.getInstance().checkPlayersArea(activeChar, 1, this);
	}
	
	public void setZoneName(String name)
	{
		_zoneName = name;
	}
	
	public String getZoneName()
	{
		return _zoneName;
	}
	
	public Location getZoneCenter()
	{
		return new Location(_centerX, _centerY, _centerZ);
	}
	
	public void onDieInside(Creature character)
	{
	}
	
	public void onReviveInside(Creature character)
	{
	}
}
