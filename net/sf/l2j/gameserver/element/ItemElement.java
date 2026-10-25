package net.sf.l2j.gameserver.element;

/**
 * Elemento de un item: que piedra se uso y cuanto vale.<br>
 * En armas es ataque de ese elemento. En armaduras es defensa contra el elemento opuesto.
 */
public class ItemElement
{
	private final ElementType _element;
	private final int _value;
	
	public ItemElement(ElementType element, int value)
	{
		_element = element;
		_value = value;
	}
	
	public ElementType getElement()
	{
		return _element;
	}
	
	public int getValue()
	{
		return _value;
	}
}
