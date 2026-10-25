package net.sf.l2j.gameserver.instancefarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.l2j.gameserver.model.location.Location;

/**
 * Zona KILL (instancia privada). Los mobs salen EXACTAMENTE en los &lt;spawn&gt; guardados con //loc_kill.
 */
public class InstanceFarmZone
{
	public static class KillSpawn
	{
		private final int _mobId;
		private final int _x;
		private final int _y;
		private final int _z;
		private final int _heading;
		
		public KillSpawn(int mobId, int x, int y, int z, int heading)
		{
			_mobId = mobId;
			_x = x;
			_y = y;
			_z = z;
			_heading = heading;
		}
		
		public int getMobId()
		{
			return _mobId;
		}
		
		public int getX()
		{
			return _x;
		}
		
		public int getY()
		{
			return _y;
		}
		
		public int getZ()
		{
			return _z;
		}
		
		public int getHeading()
		{
			return _heading;
		}
	}
	
	private final int _id;
	private final String _name;
	private final boolean _enabled;
	private final Location _enterLoc;
	private final int _respawnSeconds;
	private final int _defaultMobId;
	private final int _minLevel;
	private final List<KillSpawn> _spawns;
	
	public InstanceFarmZone(int id, String name, boolean enabled, Location enterLoc, int respawnSeconds, int defaultMobId, int minLevel, List<KillSpawn> spawns)
	{
		_id = id;
		_name = name;
		_enabled = enabled;
		_enterLoc = enterLoc;
		_respawnSeconds = respawnSeconds;
		_defaultMobId = defaultMobId;
		_minLevel = minLevel;
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
	
	public InstanceFarmZoneType getType()
	{
		return InstanceFarmZoneType.KILL;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	/** Punto donde aparece el jugador al entrar. */
	public Location getEnterLoc()
	{
		return _enterLoc;
	}
	
	public int getRespawnSeconds()
	{
		return _respawnSeconds;
	}
	
	public int getDefaultMobId()
	{
		return _defaultMobId;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public List<KillSpawn> getSpawns()
	{
		return _spawns;
	}
}
