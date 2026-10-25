package net.sf.l2j.gameserver.alchemy;

/**
 * Attribute Alchemy - lo que tiene un socket de un item (atributo + tier + valor).
 */
public class AlchemySlot
{
	private final int _slot;
	private final String _attributeId;
	private final int _tier;
	private final double _value;
	
	public AlchemySlot(int slot, String attributeId, int tier, double value)
	{
		_slot = slot;
		_attributeId = attributeId;
		_tier = tier;
		_value = value;
	}
	
	public int getSlot()
	{
		return _slot;
	}
	
	public String getAttributeId()
	{
		return _attributeId;
	}
	
	public int getTier()
	{
		return _tier;
	}
	
	public double getValue()
	{
		return _value;
	}
	
	public AlchemyAttribute getAttribute()
	{
		return AttributeAlchemyData.getInstance().getAttribute(_attributeId);
	}
	
	/**
	 * @return texto tipo "P.Atk +7.3%"
	 */
	public String getDisplay()
	{
		final AlchemyAttribute attr = getAttribute();
		if (attr == null)
			return _attributeId + " +" + _value;
		
		return attr.getName() + " +" + String.format(java.util.Locale.US, "%.1f", _value) + attr.getUnit();
	}
}
