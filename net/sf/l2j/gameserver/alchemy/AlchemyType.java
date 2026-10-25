package net.sf.l2j.gameserver.alchemy;

/**
 * Attribute Alchemy (Sockets) - tipo de item / orbe.
 * <ul>
 * <li>WEAPON : Fury Orb (armas)</li>
 * <li>ARMOR : Guardian Orb (casco, pechera, pantalon, guantes, botas, escudo)</li>
 * <li>JEWEL : Fortune Orb (aros, collar, anillos)</li>
 * </ul>
 */
public enum AlchemyType
{
	WEAPON("armas"),
	ARMOR("armaduras"),
	JEWEL("joyas");
	
	private final String _desc;
	
	private AlchemyType(String desc)
	{
		_desc = desc;
	}
	
	public String getDesc()
	{
		return _desc;
	}
	
	public static AlchemyType fromString(String name)
	{
		for (AlchemyType t : values())
			if (t.name().equalsIgnoreCase(name))
				return t;
		return WEAPON;
	}
}
