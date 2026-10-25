package net.sf.l2j.gameserver.model.zone.type;

import java.util.HashSet;
import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.instancefarm.InstanceFarmManager;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.zone.SpawnZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;

/**
 * Zona TIME del Instance Farm (publica, con PvP). Se define con &lt;time&gt; en data/xml/custom/InstanceFarmZones.xml
 * (la crea InstanceFarmManager al arrancar, NO va en la carpeta zones).<br>
 * Mismas opciones que TimeFarmZone (InviteParty, NoDropProtection, LoseBuff, BlockedClasses) + MinLevel.<br>
 * Los &lt;enter&gt; son los puntos donde el NPC teletransporta al jugador.<br>
 * <br>
 * No entra en la rotacion de TimeFarmZoneManager.
 */
public class InstanceTimeZone extends SpawnZoneType
{
	private String _zoneName = "Time Farm";
	private boolean _enabled = true;
	private boolean _inviteParty = false;
	private boolean _noDropProtection = false;
	private boolean _loseBuff = true;
	private int _minLevel = 0;
	private final Set<Integer> _blockedClasses = new HashSet<>();
	private int _farmId;
	
	public InstanceTimeZone(int id)
	{
		super(id);
	}
	
	/**
	 * Configuracion desde InstanceFarmZones.xml.
	 */
	public void configure(int farmId, String name, boolean enabled, int minLevel, boolean inviteParty, boolean noDropProtection, boolean loseBuff, Set<Integer> blockedClasses)
	{
		_farmId = farmId;
		_zoneName = name;
		_enabled = enabled;
		_minLevel = minLevel;
		_inviteParty = inviteParty;
		_noDropProtection = noDropProtection;
		_loseBuff = loseBuff;
		_blockedClasses.clear();
		_blockedClasses.addAll(blockedClasses);
	}
	
	/** Id de la zona en InstanceFarmZones.xml (el que usa el NPC). */
	public int getFarmId()
	{
		return _farmId;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("name"))
			_zoneName = value;
		else if (name.equalsIgnoreCase("EnabledZone"))
			_enabled = Boolean.parseBoolean(value.trim());
		else if (name.equalsIgnoreCase("InviteParty"))
			_inviteParty = Boolean.parseBoolean(value.trim());
		else if (name.equalsIgnoreCase("NoDropProtection"))
			_noDropProtection = Boolean.parseBoolean(value.trim());
		else if (name.equalsIgnoreCase("LoseBuff"))
			_loseBuff = Boolean.parseBoolean(value.trim());
		else if (name.equalsIgnoreCase("MinLevel"))
			_minLevel = Integer.parseInt(value.trim());
		else if (name.equalsIgnoreCase("BlockedClasses"))
		{
			for (String classId : value.split("[;,]"))
			{
				if (!classId.trim().isEmpty())
					_blockedClasses.add(Integer.parseInt(classId.trim()));
			}
		}
		else if (name.equalsIgnoreCase("BlockMultiplePlayers"))
		{
			// Siempre activo: el Time Farm permite 1 personaje por HWID (el tiempo es por HWID).
		}
		else
			super.setParameter(name, value);
	}
	
	@Override
	public String getZoneName()
	{
		return _zoneName;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public boolean isInvitePartyAllowed()
	{
		return _inviteParty;
	}
	
	public boolean isNoDropProtection()
	{
		return _noDropProtection;
	}
	
	public boolean canLoseBuff()
	{
		return _loseBuff;
	}
	
	public int getMinLevel()
	{
		return Math.max(_minLevel, Config.INSTANCE_FARM_TIME_MIN_LEVEL);
	}
	
	public boolean isClassBlocked(Player player)
	{
		return _blockedClasses.contains(player.getClassId().getId());
	}
	
	@Override
	protected void onEnter(Creature character)
	{
		if (!_enabled || !(character instanceof Player))
			return;
		
		final Player player = (Player) character;
		
		character.setInsideZone(ZoneId.TIME_FARM_ZONE, true);
		PvpFlagTaskManager.getInstance().remove(player);
		player.setPvpFlag(1);
		player.broadcastUserInfo();
		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
		
		if (!Config.INSTANCE_FARM_ENABLED)
			return;
		
		if (!player.isGM())
		{
			if (isClassBlocked(player) || player.getLevel() < getMinLevel())
			{
				player.sendMessage(isClassBlocked(player) ? "Tu clase no puede entrar a esta zona." : "Necesitas nivel " + getMinLevel() + " para esta zona.");
				net.sf.l2j.commons.concurrent.ThreadPool.schedule(() ->
				{
					if (player.isOnline() && isCharacterInZone(player))
						InstanceFarmManager.teleportOut(player);
				}, 1000);
				return;
			}
		}
		
		if (!InstanceFarmManager.getInstance().onTimeZoneEnter(player, this))
			return;
		
		if (!_inviteParty && player.getParty() != null)
			player.getParty().removePartyMember(player, MessageType.LEFT);
	}
	
	@Override
	protected void onExit(Creature character)
	{
		if (!_enabled || !(character instanceof Player))
			return;
		
		final Player player = (Player) character;
		
		character.setInsideZone(ZoneId.TIME_FARM_ZONE, false);
		player.updatePvPFlag(0);
		player.broadcastUserInfo();
		PvpFlagTaskManager.getInstance().add(player, Config.PVP_NORMAL_TIME);
		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
		
		if (Config.INSTANCE_FARM_ENABLED)
			InstanceFarmManager.getInstance().onTimeZoneExit(player, this);
	}
	
	/**
	 * @param object : el objeto.
	 * @return la InstanceTimeZone activa donde esta el objeto, o null.
	 */
	public static InstanceTimeZone getZoneOf(net.sf.l2j.gameserver.model.WorldObject object)
	{
		final InstanceTimeZone zone = net.sf.l2j.gameserver.data.manager.ZoneManager.getInstance().getZone(object, InstanceTimeZone.class);
		return (zone != null && zone.isEnabled()) ? zone : null;
	}
}
