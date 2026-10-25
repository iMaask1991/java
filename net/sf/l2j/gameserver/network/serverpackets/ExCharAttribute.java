package net.sf.l2j.gameserver.network.serverpackets;

/**
 * Datos de la ventana de atributos del ALT+T (AttributeStatusWnd, evento 1910).<br>
 * Usa el mismo formato que ExPCCafePointInfo (FE:31), con PointType = -3:
 * <ul>
 * <li>TotalPoint = codigo del dato</li>
 * <li>AddPoint = valor</li>
 * <li>RemainTime = valor extra</li>
 * </ul>
 */
public class ExCharAttribute extends L2GameServerPacket
{
	private static final int POINT_TYPE_STATUS = -3;
	
	private final int _code;
	private final int _value;
	private final int _extra;
	
	public ExCharAttribute(int code, int value, int extra)
	{
		_code = code;
		_value = value;
		_extra = extra;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_code); // TotalPoint
		writeD(_value); // AddPoint
		writeC(1); // PeriodType
		writeD(_extra); // RemainTime
		writeC(POINT_TYPE_STATUS); // PointType
	}
}
