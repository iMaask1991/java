package net.sf.l2j.gameserver.autofarm;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * One configurable AutoFarm skill slot (InertiaX-style).
 */
public class AutofarmSkillSlot
{
	private int _skillId;
	private double _reuseSec;
	private double _userHp = 100d;
	private double _targetHp = 100d;
	private long _lastUseMs;
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public void setSkillId(int skillId)
	{
		_skillId = Math.max(0, skillId);
		_lastUseMs = 0L;
	}
	
	public boolean isEmpty()
	{
		return _skillId <= 0;
	}
	
	public void clear()
	{
		_skillId = 0;
		_reuseSec = 0d;
		_userHp = 100d;
		_targetHp = 100d;
		_lastUseMs = 0L;
	}
	
	public double getReuseSec()
	{
		return _reuseSec;
	}
	
	public void setReuseSec(double reuseSec)
	{
		_reuseSec = Math.max(0d, Math.min(300d, reuseSec));
	}
	
	public double getUserHp()
	{
		return _userHp;
	}
	
	public void setUserHp(double userHp)
	{
		_userHp = Math.max(1d, Math.min(100d, userHp));
	}
	
	public double getTargetHp()
	{
		return _targetHp;
	}
	
	public void setTargetHp(double targetHp)
	{
		_targetHp = Math.max(1d, Math.min(100d, targetHp));
	}
	
	public boolean isOnReuse()
	{
		if (_reuseSec <= 0d)
			return false;
		return _lastUseMs + (long) (_reuseSec * 1000d) > System.currentTimeMillis();
	}
	
	public void markUsed()
	{
		_lastUseMs = System.currentTimeMillis();
	}
	
	public boolean matchesUserHp(Player player)
	{
		return hpPercent(player) <= _userHp + 0.001d;
	}
	
	public boolean matchesTargetHp(Creature target)
	{
		if (target == null)
			return true;
		return hpPercent(target) <= _targetHp + 0.001d;
	}
	
	private static double hpPercent(Creature creature)
	{
		final double max = creature.getMaxHp();
		if (max <= 0)
			return 100d;
		return creature.getCurrentHp() * 100d / max;
	}
	
	/** icon.skill#### client path (Interlude). */
	public static String iconOf(int skillId)
	{
		if (skillId <= 0)
			return "L2UI.SquareGray";
		return String.format("icon.skill%04d", skillId);
	}
	
	public String serialize()
	{
		return _skillId + "," + _reuseSec + "," + _userHp + "," + _targetHp;
	}
	
	public void deserialize(String raw)
	{
		clear();
		if (raw == null || raw.isEmpty())
			return;
		final String[] p = raw.split(",");
		if (p.length < 1)
			return;
		try
		{
			_skillId = Integer.parseInt(p[0].trim());
			if (p.length > 1)
				_reuseSec = Double.parseDouble(p[1].trim());
			if (p.length > 2)
				_userHp = Double.parseDouble(p[2].trim());
			if (p.length > 3)
				_targetHp = Double.parseDouble(p[3].trim());
			setReuseSec(_reuseSec);
			setUserHp(_userHp);
			setTargetHp(_targetHp);
		}
		catch (NumberFormatException ignored)
		{
			clear();
		}
	}
}
