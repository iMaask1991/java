package net.sf.l2j.gameserver.network.serverpackets;

/**
 * Elemento de un item para la interface (InventoryWnd, evento 1910).<br>
 * Usa el mismo formato que ExPCCafePointInfo (FE:31), pero con PointType = -2 la interface lo toma como elemento:
 * <ul>
 * <li>TotalPoint = id del elemento (0 = sacar el elemento)</li>
 * <li>AddPoint = objectId del item</li>
 * <li>RemainTime = valor del elemento</li>
 * </ul>
 */
public class ExItemAttribute extends L2GameServerPacket
{
	private static final int POINT_TYPE_ELEMENT = -2;
	
	private final int _objectId;
	private final int _attributeId;
	private final int _value;
	
	public ExItemAttribute(int objectId, int attributeId, int value)
	{
		_objectId = objectId;
		_attributeId = attributeId;
		_value = value;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_attributeId); // TotalPoint
		writeD(_objectId); // AddPoint
		writeC(1); // PeriodType
		writeD(_value); // RemainTime
		writeC(POINT_TYPE_ELEMENT); // PointType
	}
}
