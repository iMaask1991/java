package net.sf.l2j.gameserver.element;

/**
 * Elementos de items (estilo High Five). El id es el que viaja a la interface (ToolTip / InventoryWnd).
 */
public enum ElementType
{
	FIRE(1, "Fire"),
	WATER(2, "Water"),
	WIND(3, "Wind"),
	EARTH(4, "Earth"),
	HOLY(5, "Holy"),
	DARK(6, "Dark");
	
	private final int _id;
	private final String _name;
	
	private ElementType(int id, String name)
	{
		_id = id;
		_name = name;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return el elemento opuesto (Fire/Water, Wind/Earth, Holy/Dark). En armadura la piedra da defensa contra el opuesto.
	 */
	public ElementType getOpposite()
	{
		switch (this)
		{
			case FIRE:
				return WATER;
			case WATER:
				return FIRE;
			case WIND:
				return EARTH;
			case EARTH:
				return WIND;
			case HOLY:
				return DARK;
			default:
				return HOLY;
		}
	}
	
	public static ElementType getById(int id)
	{
		for (ElementType e : values())
			if (e._id == id)
				return e;
		return null;
	}
	
	/**
	 * @param text : nombre (FIRE, Fire, fuego...) o id (1-6).
	 * @return el elemento, o null.
	 */
	public static ElementType parse(String text)
	{
		if (text == null)
			return null;
		
		try
		{
			return getById(Integer.parseInt(text.trim()));
		}
		catch (NumberFormatException e)
		{
			// no es numero
		}
		
		final String t = text.trim().toUpperCase();
		switch (t)
		{
			case "FUEGO":
				return FIRE;
			case "AGUA":
				return WATER;
			case "VIENTO":
				return WIND;
			case "TIERRA":
				return EARTH;
			case "DIVINE":
			case "SAGRADO":
				return HOLY;
			case "UNHOLY":
			case "OSCURO":
				return DARK;
		}
		
		for (ElementType e : values())
			if (e.name().equals(t))
				return e;
		return null;
	}
}
