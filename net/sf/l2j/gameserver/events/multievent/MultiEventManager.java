package net.sf.l2j.gameserver.events.multievent;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class MultiEventManager
{
	protected static final Logger _log = Logger.getLogger(MultiEventManager.class.getName());

	protected MultiEventManager()
	{
		if (MultiEventConfig.ENABLED)
		{
			spawnNpc();
			_log.info("Multi Event NPC: spawned (id " + MultiEventConfig.NPC_ID + ").");
		}
		else
			_log.info("Multi Event NPC: disabled.");
	}

	public static MultiEventManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private void spawnNpc()
	{
		final NpcTemplate tmpl = NpcData.getInstance().getTemplate(MultiEventConfig.NPC_ID);
		if (tmpl == null)
		{
			_log.warning("Multi Event NPC: invalid npc id " + MultiEventConfig.NPC_ID);
			return;
		}

		try
		{
			final L2Spawn npcSpawn = new L2Spawn(tmpl);
			npcSpawn.setLoc(MultiEventConfig.NPC_COORDINATES[0], MultiEventConfig.NPC_COORDINATES[1], MultiEventConfig.NPC_COORDINATES[2], MultiEventConfig.NPC_COORDINATES[3]);
			npcSpawn.setRespawnDelay(1);
			npcSpawn.setRespawnState(true);
			SpawnTable.getInstance().addSpawn(npcSpawn, false);
			npcSpawn.doSpawn(false);
			npcSpawn.getNpc().getStatus().setCurrentHp(9.99999999E8D);
			npcSpawn.getNpc().decayMe();
			npcSpawn.getNpc().spawnMe(npcSpawn.getNpc().getX(), npcSpawn.getNpc().getY(), npcSpawn.getNpc().getZ());
			npcSpawn.getNpc().broadcastPacket(new MagicSkillUse(npcSpawn.getNpc(), npcSpawn.getNpc(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Multi Event NPC: spawn failed.", e);
		}
	}

	private static class SingletonHolder
	{
		protected static final MultiEventManager _instance = new MultiEventManager();
	}
}
