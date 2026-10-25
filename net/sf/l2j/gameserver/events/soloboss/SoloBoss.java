package net.sf.l2j.gameserver.events.soloboss;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

public class SoloBoss
{
	public static L2Spawn _monster;
	public static int _bossHeading = 0;
	public static String _eventName = "";
	public static boolean _started = false;
	public static boolean _aborted = false;
	public static boolean _finish = false;
	static SoloBoss _instance;
	public static boolean boss_teleporter = false;
	public static L2Spawn _npcSpawnActive;
	
	public static void bossSpawnMonster()
	{
		if (Config.SCREN_MSG_BOSS_SOLO)
			InviteForSpawnBoss();
		
		SpawnAllRaids();
		Broadcast.gameAnnounceToOnlinePlayers("Teleport Now!");
		Broadcast.gameAnnounceToOnlinePlayers("SoloBoss Duration " + Config.EVENT_SOLOBOSS_TIME + " minute(s)!");
		_aborted = false;
		_started = true;
		
		waiter(Config.EVENT_SOLOBOSS_TIME * 60 * 1000);
		if (!_aborted)
			Finish_Event();
	}
	
	public static void Finish_Event()
	{
		unspawnNpcNPCS();
		
		_started = false;
		_finish = true;
		_aborted = true;
		
		Broadcast.gameAnnounceToOnlinePlayers("SoloBoss Finished!");
		Broadcast.gameAnnounceToAll("Next SoloBoss Event " + InitialSoloBossEvent.getInstance().getNextTime() + " (GMT-3).");
		if (!AdminSoloBoss._bestfarm_manual)
			InitialSoloBossEvent.getInstance().StartCalculationOfNextEventTime();
		else
			AdminSoloBoss._bestfarm_manual = false;
	}
	
	private static void InviteForSpawnBoss()
	{
		boss_teleporter = true;
		ThreadPool.schedule(() -> boss_teleporter = false, 46000);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player == null || !player.isOnline() || !Config.SCREN_MSG_BOSS_SOLO)
				continue;
			
			if (player.isInObserverMode() || player.isDead() || DMEvent.isPlayerParticipant(player.getObjectId()) && DMEvent.isStarted() || KTBEvent.isPlayerParticipant(player.getObjectId()) && KTBEvent.isStarted() || LMEvent.isPlayerParticipant(player.getObjectId()) && LMEvent.isStarted() || player.isAlikeDead() || player.isInArenaEvent() || player.isArenaProtection() || player.isOlympiadProtection() || player.isInStoreMode() || player.isRooted() || player.getKarma() > 0 || player.isInOlympiadMode() || player.isOff() || player.isOffShop() || player.isFestivalParticipant() || player.isArenaAttack() || player.isInsideZone(ZoneId.BOSS) || player.isInsideZone(ZoneId.ARENA_EVENT) || player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE))
				continue;
			
			final SpawnLocation position = new SpawnLocation(Config.SOLO_BOSS_ID_ONE_LOC[0] + Rnd.get(-300, 300), Config.SOLO_BOSS_ID_ONE_LOC[1] + Rnd.get(-100, 100), Config.SOLO_BOSS_ID_ONE_LOC[2], 0);
			final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1.getId());
			confirm.addString("Let's go to Solo Boss?");
			confirm.addZoneName(new Location(position.getX(), position.getY(), position.getZ()));
			confirm.addTime(45000);
			confirm.addRequesterId(player.getObjectId());
			player.sendPacket(confirm);
		}
	}
	
	public static boolean is_started()
	{
		return _started;
	}
	
	public static boolean is_finish()
	{
		return _finish;
	}
	
	public static void SpawnAllRaids()
	{
		SpawnRaid(Config.SOLO_BOSS_ID_ONE, Config.SOLO_BOSS_ID_ONE_LOC);
	}
	
	public static void SpawnRaid(int bossId, int[] bossLoc)
	{
		final NpcTemplate tmpl = NpcData.getInstance().getTemplate(bossId);
		if (tmpl == null)
			return;
		
		try
		{
			final L2Spawn npcSpawn = new L2Spawn(tmpl);
			npcSpawn.setLoc(bossLoc[0], bossLoc[1], bossLoc[2], 0);
			npcSpawn.setRespawnDelay(10);
			_npcSpawnActive = npcSpawn;
			SpawnTable.getInstance().addSpawn(npcSpawn, false);
			npcSpawn.setRespawnState(false);
			npcSpawn.doSpawn(true);
			npcSpawn.getNpc().isAggressive();
			npcSpawn.getNpc().decayMe();
			npcSpawn.getNpc().spawnMe(npcSpawn.getNpc().getX(), npcSpawn.getNpc().getY(), npcSpawn.getNpc().getZ());
			npcSpawn.getNpc().broadcastPacket(new MagicSkillUse(npcSpawn.getNpc(), npcSpawn.getNpc(), 1034, 1, 1, 1));
			Broadcast.gameAnnounceToOnlinePlayers("SoloBoss " + NpcData.getInstance().getTemplate(bossId).getName() + " was spawned in the world!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void unspawnNpcNPCS()
	{
		if (_npcSpawnActive != null && _npcSpawnActive.getNpc() != null)
		{
			_npcSpawnActive.getNpc().deleteMe();
			_npcSpawnActive.setRespawnState(false);
			SpawnTable.getInstance().deleteSpawn(_npcSpawnActive, true);
			_npcSpawnActive = null;
		}
	}
	
	protected static void waiter(long interval)
	{
		final long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000L);
		
		while ((startWaiterTime + interval > System.currentTimeMillis()) && (!_aborted))
		{
			seconds--;
			
			if (_started)
			{
				if (seconds == 3600)
					Broadcast.gameAnnounceToOnlinePlayers("SoloBoss " + (seconds / 60 / 60) + " hour(s) till event finish!");
				else if (seconds == 60 || seconds == 120 || seconds == 180 || seconds == 240 || seconds == 300 || seconds == 600 || seconds == 900 || seconds == 1800)
					Broadcast.gameAnnounceToOnlinePlayers("SoloBoss " + (seconds / 60) + " minute(s) till event finish!");
				else if (seconds == 1 || seconds == 2 || seconds == 3 || seconds == 10 || seconds == 15 || seconds == 30)
					Broadcast.gameAnnounceToOnlinePlayers("SoloBoss " + seconds + " second(s) till event finish!");
			}
			
			try
			{
				Thread.sleep(1000L);
			}
			catch (InterruptedException ie)
			{
				if (Config.DEBUG_PATH)
					ie.printStackTrace();
			}
		}
	}
}
