package net.sf.l2j.gameserver.instancefarm;

/**
 * Tipos de Instance Farm.<br>
 * TIME = zona publica PvP (se define en data/xml/zones/InstanceTimeZone.xml).<br>
 * KILL = instancia privada (se define en InstanceFarmZones.xml).<br>
 * El modo WAVE (raid por oleadas) fue eliminado.
 */
public enum InstanceFarmZoneType
{
	TIME,
	KILL
}
