package net.sf.l2j.gameserver.events.instance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.gameserver.model.actor.instance.Door;

public class InstanceManager
{
	private Map<Integer, InstanceHolder> instances;
	
	protected InstanceManager()
	{
		instances = new ConcurrentHashMap<>();
		instances.put(0, new InstanceHolder(0));
	}
	
	public void addDoor(int id, Door door)
	{
		if (!instances.containsKey(id) || id == 0)
			return;
		
		instances.get(id).addDoor(door);
	}
	
	public void deleteInstance(int id)
	{
		if (id == 0)
		{
			System.out.println("Attempt to delete instance with id 0.");
			return;
		}
		
		// delete doors
	}
	
	public synchronized InstanceHolder createInstance()
	{
		InstanceHolder instance = new InstanceHolder(InstanceIdFactory.getNextAvailable());
		instances.put(instance.getId(), instance);
		return instance;
	}
	
	public InstanceHolder getInstance(int id)
	{
		return instances.get(id);
	}
	
	public static InstanceManager getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static final class SingletonHolder
	{
		protected static final InstanceManager instance = new InstanceManager();
	}
}
