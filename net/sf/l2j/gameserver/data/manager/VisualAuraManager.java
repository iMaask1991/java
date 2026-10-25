package net.sf.l2j.gameserver.data.manager;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Npc;

public final class VisualAuraManager
{
	private VisualAuraManager()
	{
	}
	
	public static void applyNpcAura(Npc npc)
	{
		if (!(npc instanceof Attackable) && Config.ENABLE_NPC_VISUAL_AURA)
			npc.setTeamAura(resolveNpcAura(npc));
	}
	
	public static void applyMobAura(Npc npc)
	{
		if (npc instanceof Attackable && Config.ENABLE_MOB_VISUAL_AURA)
			npc.setTeamAura(resolveMobAura(npc));
	}
	
	private static int resolveNpcAura(Npc npc)
	{
		if (Config.NPC_VISUAL_AURA_IDS.contains(npc.getNpcId()))
			return Config.NPC_VISUAL_AURA_COLOR;
		
		return 0;
	}
	
	private static int resolveMobAura(Npc npc)
	{
		final int npcId = npc.getNpcId();
		
		if (Config.MOB_VISUAL_AURA_RED_IDS.contains(npcId))
			return 2;
		
		if (Config.MOB_VISUAL_AURA_BLUE_IDS.contains(npcId))
			return 1;
		
		return 0;
	}
}
