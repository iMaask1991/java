package net.sf.l2j.gameserver.model.extender;

import net.sf.l2j.gameserver.model.WorldObject;

public class BaseExtender
{
	protected WorldObject _owner;
	
	public static enum EventType
	{
		LOAD("load"),
		STORE("store"),
		CAST("cast"),
		ATTACK("attack"),
		CRAFT("craft"),
		ENCHANT("enchant"),
		SPAWN("spawn"),
		DELETE("delete"),
		SETOWNER("setwoner"),
		DROP("drop"),
		DIE("die"),
		REVIVE("revive"),
		SETINTENTION("setintention");
		
		public final String name;
		
		private EventType(String name)
		{
			this.name = name;
		}
	}
	
	public static boolean canCreateFor(WorldObject object)
	{
		return true;
	}
	
	private BaseExtender _next = null;
	
	public BaseExtender(WorldObject owner)
	{
		_owner = owner;
	}
	
	public WorldObject getOwner()
	{
		return _owner;
	}
	
	public Object onEvent(String event, Object... params)
	{
		if (_next == null)
		{
			return null;
		}
		return _next.onEvent(event, params);
	}
	
	public BaseExtender getExtender(String simpleClassName)
	{
		if (getClass().getSimpleName().compareTo(simpleClassName) == 0)
		{
			return this;
		}
		if (_next != null)
		{
			return _next.getExtender(simpleClassName);
		}
		return null;
	}
	
	public void removeExtender(BaseExtender ext)
	{
		if (_next != null)
		{
			if (_next == ext)
			{
				_next = _next._next;
			}
			else
			{
				_next.removeExtender(ext);
			}
		}
	}
	
	public BaseExtender getNextExtender()
	{
		return _next;
	}
	
	public void addExtender(BaseExtender newExtender)
	{
		if (_next == null)
		{
			_next = newExtender;
		}
		else
		{
			_next.addExtender(newExtender);
		}
	}
}
