package net.sf.l2j.gameserver.model.actor.player;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.ClassId;

/**
 * Used to store information about a Player SubClass.
 */
public final class SubClass
{
	private ClassId _class;
	private final int _classIndex;
	private long _exp;
	private int _sp;
	private byte _level;
	
	/**
	 * Implicit constructor with all parameters to be set.
	 * @param classId : Class ID of the subclass.
	 * @param classIndex : Class index of the subclass.
	 * @param exp : Exp of the subclass.
	 * @param sp : Sp of the subclass.
	 * @param level : Level of the subclass.
	 */
	public SubClass(int classId, int classIndex, long exp, int sp, byte level)
	{
		_class = ClassId.VALUES[classId];
		_classIndex = classIndex;
		_sp = sp;
		setExp(exp);
		setLevel(level);
		syncExpAndLevel();
	}
	
	/**
	 * Implicit constructor with default EXP, SP and level parameters.
	 * @param classId : Class ID of the subclass.
	 * @param classIndex : Class index of the subclass.
	 */
	public SubClass(int classId, int classIndex)
	{
		_class = ClassId.VALUES[classId];
		_classIndex = classIndex;
		_exp = Experience.LEVEL[Config.BASE_SUBCLASS_LEVEL];
		_sp = 0;
		_level = (byte) Config.BASE_SUBCLASS_LEVEL;
	}
	
	public ClassId getClassDefinition()
	{
		return _class;
	}
	
	public int getClassId()
	{
		return _class.getId();
	}
	
	public void setClassId(int classId)
	{
		_class = ClassId.VALUES[classId];
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public void setExp(long exp)
	{
		final int maxLvl = Math.min(Config.MAX_SUBCLASS_LEVEL, Experience.LEVEL.length - 1);
		final long maxExp = Experience.LEVEL[maxLvl];
		if (exp > maxExp)
			exp = maxExp;
		
		_exp = exp;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public void setSp(int sp)
	{
		_sp = sp;
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public void setLevel(byte level)
	{
		final int maxReachable = Config.MAX_SUBCLASS_LEVEL - 1;
		if (level > maxReachable)
			level = (byte) maxReachable;
		else if (level < Config.BASE_SUBCLASS_LEVEL)
			level = (byte) Config.BASE_SUBCLASS_LEVEL;
		
		_level = level;
		
		// La exp NUNCA puede quedar por debajo del piso de su propio nivel, si no el cliente
		// calcula la barra en NEGATIVO (ej: -52%) y el jugador tiene que matar bichos para
		// recuperar el % perdido. La exp solo SUBE, jamas se pierde.
		if (_exp < Experience.LEVEL[_level])
			_exp = Experience.LEVEL[_level];
	}
	
	/**
	 * Normaliza el par nivel / exp sin perder exp.<br>
	 * Ajusta el nivel al que le corresponde segun la exp acumulada (respetando {@code BaseSubclassLevel}
	 * y {@code MaxSubclassLevel}) y garantiza {@code exp >= Experience.LEVEL[level]}.<br>
	 * Se usa al cargar la sub desde la DB, que es cuando el par puede venir desfasado si el admin
	 * cambio los limites de nivel en {@code Subclass.properties}.
	 */
	private void syncExpAndLevel()
	{
		final int maxReachable = Math.max(Config.BASE_SUBCLASS_LEVEL, Math.min(Config.MAX_SUBCLASS_LEVEL - 1, Experience.LEVEL.length - 2));
		
		int level = _level;
		if (level < Config.BASE_SUBCLASS_LEVEL)
			level = Config.BASE_SUBCLASS_LEVEL;
		
		while (level < maxReachable && _exp >= Experience.LEVEL[level + 1])
			level++;
		
		while (level > Config.BASE_SUBCLASS_LEVEL && _exp < Experience.LEVEL[level])
			level--;
		
		_level = (byte) level;
		
		if (_exp < Experience.LEVEL[_level])
			_exp = Experience.LEVEL[_level];
	}
}
