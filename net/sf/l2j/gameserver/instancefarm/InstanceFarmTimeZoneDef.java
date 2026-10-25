package net.sf.l2j.gameserver.instancefarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.l2j.gameserver.model.location.Location;

/**
 * Definicion de una zona TIME leida de &lt;time&gt; en InstanceFarmZones.xml.
 */
public class InstanceFarmTimeZoneDef
{
	private final int _id;
	private final String _name;
	private final boolean _enabled;
	private final Location _center;
	private final int _radius;
	private final int _minZ;
	private final int _maxZ;
	private final List<int[]> _nodes;
	private final int _minLevel;
	private final boolean _inviteParty;
	private final boolean _noDropProtection;
	private final boolean _loseBuff;
	private final Set<Integer> _blockedClasses;
	private final List<Location> _enters;
	private final int _respawnSeconds;
	private final int _defaultMobId;
	private final List<InstanceFarmZone.KillSpawn> _spawns;
	
	public InstanceFarmTimeZoneDef(int id, String name, boolean enabled, Location center, int radius, int minZ, int maxZ, List<int[]> nodes, int minLevel, boolean inviteParty, boolean noDropProtection, boolean loseBuff, Set<Integer> blockedClasses, List<Location> enters, int respawnSeconds, int defaultMobId, List<InstanceFarmZone.KillSpawn> spawns)
	{
		_id = id;
		_name = name;
		_enabled = enabled;
		_center = center;
		_radius = radius;
		_minZ = minZ;
		_maxZ = maxZ;
		_nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
		_minLevel = minLevel;
		_inviteParty = inviteParty;
		_noDropProtection = noDropProtection;
		_loseBuff = loseBuff;
		_blockedClasses = Collections.unmodifiableSet(new HashSet<>(blockedClasses));
		_enters = Collections.unmodifiableList(new ArrayList<>(enters));
		_respawnSeconds = respawnSeconds;
		_defaultMobId = defaultMobId;
		_spawns = Collections.unmodifiableList(new ArrayList<>(spawns));
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public Location getCenter()
	{
		return _center;
	}
	
	public int getRadius()
	{
		return _radius;
	}
	
	public int getMinZ()
	{
		return _minZ;
	}
	
	public int getMaxZ()
	{
		return _maxZ;
	}
	
	/** Si hay 3 o mas &lt;node&gt; la zona es un poligono; si no, un circulo (center + radius). */
	public List<int[]> getNodes()
	{
		return _nodes;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public boolean isInviteParty()
	{
		return _inviteParty;
	}
	
	public boolean isNoDropProtection()
	{
		return _noDropProtection;
	}
	
	public boolean isLoseBuff()
	{
		return _loseBuff;
	}
	
	public Set<Integer> getBlockedClasses()
	{
		return _blockedClasses;
	}
	
	public List<Location> getEnters()
	{
		return _enters;
	}
	
	public int getRespawnSeconds()
	{
		return _respawnSeconds;
	}
	
	public int getDefaultMobId()
	{
		return _defaultMobId;
	}
	
	public List<InstanceFarmZone.KillSpawn> getSpawns()
	{
		return _spawns;
	}
}
