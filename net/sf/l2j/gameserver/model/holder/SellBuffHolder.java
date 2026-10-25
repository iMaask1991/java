package net.sf.l2j.gameserver.model.holder;

/**
 * Stores skill id and price for the Sell Buffs system.
 */
public class SellBuffHolder
{
	private final int _skillId;
	private int _price;
	
	public SellBuffHolder(int skillId, int price)
	{
		_skillId = skillId;
		_price = price;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public void setPrice(int price)
	{
		_price = price;
	}
}
