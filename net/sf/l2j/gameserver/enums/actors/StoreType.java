package net.sf.l2j.gameserver.enums.actors;

public enum StoreType
{
	NONE(0),
	SELL(1),
	SELL_MANAGE(2),
	BUY(3),
	BUY_MANAGE(4),
	MANUFACTURE(5),
	PACKAGE_SELL(8),
	SELL_BUFFS(9);
	
	private int _id;
	
	private StoreType(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
}