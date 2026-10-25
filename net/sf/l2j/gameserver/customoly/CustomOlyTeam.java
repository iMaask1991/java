package net.sf.l2j.gameserver.customoly;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.Player;

public class CustomOlyTeam
{
	private final List<Player> _members = new ArrayList<>();
	
	public CustomOlyTeam(Player leader)
	{
		_members.add(leader);
	}
	
	public CustomOlyTeam(List<Player> members)
	{
		_members.addAll(members);
	}
	
	public List<Player> getMembers()
	{
		return _members;
	}
	
	public Player getLeader()
	{
		return _members.isEmpty() ? null : _members.get(0);
	}
	
	public boolean contains(Player player)
	{
		return _members.contains(player);
	}
	
	public int size()
	{
		return _members.size();
	}
}
