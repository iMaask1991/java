package net.sf.l2j.gameserver.customoly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.events.instance.InstanceHolder;
import net.sf.l2j.gameserver.events.instance.InstanceManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public class CustomOlyArena implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(CustomOlyArena.class.getName());
	
	private final List<CustomOlyTeam> _q1 = new CopyOnWriteArrayList<>();
	private final List<CustomOlyTeam> _q2 = new CopyOnWriteArrayList<>();
	private final List<CustomOlyTeam> _q3 = new CopyOnWriteArrayList<>();
	private volatile boolean _running;
	
	public void start()
	{
		if (_running)
			return;
		_running = true;
		Thread t = new Thread(this, "CustomOlyArena");
		t.setDaemon(true);
		t.start();
	}
	
	@Override
	public void run()
	{
		while (_running)
		{
			try
			{
				if (CustomOlyEvent.getInstance().isSessionOpen())
				{
					tryMatch(_q1, 1);
					tryMatch(_q2, 2);
					tryMatch(_q3, 3);
				}
				Thread.sleep(Config.CUSTOM_OLY_CALL_SECONDS * 1000L);
			}
			catch (InterruptedException e)
			{
				break;
			}
			catch (Exception e)
			{
				LOGGER.warning("Custom Olympiad arena: " + e);
			}
		}
	}
	
	private void tryMatch(List<CustomOlyTeam> queue, int size)
	{
		while (queue.size() >= 2)
		{
			CustomOlyTeam a = queue.remove(0);
			if (!alive(a))
				continue;
			
			CustomOlyTeam b = null;
			for (CustomOlyTeam cand : queue)
			{
				if (!alive(cand))
				{
					queue.remove(cand);
					continue;
				}
				if (!Config.CUSTOM_OLY_ALLOW_SAME_HWID && sameHwid(a, cand))
					continue;
				b = cand;
				break;
			}
			if (b == null)
			{
				queue.add(0, a);
				return;
			}
			queue.remove(b);
			if (!alive(a) || !alive(b))
			{
				if (alive(a))
					queue.add(a);
				if (alive(b))
					queue.add(b);
				return;
			}
			new Thread(new Fight(a, b, size), "CustomOlyFight").start();
		}
	}
	
	private boolean alive(CustomOlyTeam team)
	{
		for (Player p : team.getMembers())
		{
			if (p == null || !p.isOnline())
				return false;
		}
		return true;
	}
	
	private boolean sameHwid(CustomOlyTeam a, CustomOlyTeam b)
	{
		for (Player pa : a.getMembers())
		{
			String ha = pa.getHWID();
			if (ha == null || ha.isEmpty())
				continue;
			for (Player pb : b.getMembers())
			{
				if (ha.equals(pb.getHWID()))
					return true;
			}
		}
		return false;
	}
	
	public boolean register1x1(Player player)
	{
		String err = CustomOlyRules.canJoin(player, 1);
		if (err != null)
		{
			player.sendMessage(err);
			return false;
		}
		if (isRegistered(player))
		{
			player.sendMessage("Ya estas registrado.");
			return false;
		}
		CustomOlyTeam team = new CustomOlyTeam(player);
		_q1.add(team);
		flagReg(team, true);
		player.sendMessage(Config.CUSTOM_OLY_NAME + ": registrado 1v1.");
		return true;
	}
	
	public boolean registerParty(Player leader, int size)
	{
		String err = CustomOlyRules.canJoin(leader, size);
		if (err != null)
		{
			leader.sendMessage(err);
			return false;
		}
		for (Player m : leader.getParty().getMembers())
		{
			if (isRegistered(m))
			{
				leader.sendMessage(m.getName() + " ya esta registrado.");
				return false;
			}
		}
		CustomOlyTeam team = new CustomOlyTeam(new ArrayList<>(leader.getParty().getMembers()));
		if (size == 2)
			_q2.add(team);
		else
			_q3.add(team);
		flagReg(team, true);
		leader.sendMessage(Config.CUSTOM_OLY_NAME + ": party registrada " + size + "v" + size + ".");
		return true;
	}
	
	public void unregister(Player player)
	{
		removeFrom(_q1, player);
		removeFrom(_q2, player);
		removeFrom(_q3, player);
		player.setArenaProtection(false);
		player.sendMessage(Config.CUSTOM_OLY_NAME + ": te desanotaste.");
	}
	
	private void removeFrom(List<CustomOlyTeam> queue, Player player)
	{
		for (CustomOlyTeam t : queue)
		{
			if (t.contains(player))
			{
				flagReg(t, false);
				queue.remove(t);
			}
		}
	}
	
	public boolean isRegistered(Player player)
	{
		for (CustomOlyTeam t : _q1)
		{
			if (t.contains(player))
				return true;
		}
		for (CustomOlyTeam t : _q2)
		{
			if (t.contains(player))
				return true;
		}
		for (CustomOlyTeam t : _q3)
		{
			if (t.contains(player))
				return true;
		}
		return false;
	}
	
	public int queueSize(int mode)
	{
		if (mode == 1)
			return _q1.size();
		if (mode == 2)
			return _q2.size();
		return _q3.size();
	}
	
	public boolean isHwidBusy(Player player)
	{
		String hwid = player.getHWID();
		if (hwid == null || hwid.isEmpty())
			return false;
		if (hwidInQueue(_q1, player, hwid) || hwidInQueue(_q2, player, hwid) || hwidInQueue(_q3, player, hwid))
			return true;
		for (Player plr : World.getInstance().getPlayers())
		{
			if (plr == null || plr == player || !plr.isOnline())
				continue;
			if (!plr.isInArenaEvent() && !plr.isArenaAttack())
				continue;
			if (hwid.equals(plr.getHWID()))
				return true;
		}
		return false;
	}
	
	private boolean hwidInQueue(List<CustomOlyTeam> queue, Player skip, String hwid)
	{
		for (CustomOlyTeam t : queue)
		{
			for (Player p : t.getMembers())
			{
				if (p != skip && hwid.equals(p.getHWID()))
					return true;
			}
		}
		return false;
	}
	
	public void clearQueues()
	{
		clearQ(_q1);
		clearQ(_q2);
		clearQ(_q3);
	}
	
	private void clearQ(List<CustomOlyTeam> queue)
	{
		for (CustomOlyTeam t : queue)
			flagReg(t, false);
		queue.clear();
	}
	
	private void flagReg(CustomOlyTeam team, boolean on)
	{
		for (Player p : team.getMembers())
		{
			if (p != null && p.isOnline())
				p.setArenaProtection(on);
		}
	}
	
	private class Fight implements Runnable
	{
		private final CustomOlyTeam _a;
		private final CustomOlyTeam _b;
		private final int[] _spawnA;
		private final int[] _spawnB;
		private InstanceHolder _instance;
		private final int[][] _returnA;
		private final int[][] _returnB;
		
		Fight(CustomOlyTeam a, CustomOlyTeam b, int size)
		{
			_a = a;
			_b = b;
			
			// Fixed team spawns (same coliseum map for every fight) when configured,
			// otherwise fall back to a random ArenasLoc arena with the legacy +-400 split.
			if (Config.CUSTOM_OLY_TEAM1_SPAWN != null && Config.CUSTOM_OLY_TEAM2_SPAWN != null)
			{
				_spawnA = Config.CUSTOM_OLY_TEAM1_SPAWN;
				_spawnB = Config.CUSTOM_OLY_TEAM2_SPAWN;
			}
			else
			{
				int[] arena = Config.CUSTOM_OLY_ARENAS[Rnd.get(Config.CUSTOM_OLY_ARENAS.length)];
				_spawnA = new int[]
				{
					arena[0] - 400,
					arena[1],
					arena[2]
				};
				_spawnB = new int[]
				{
					arena[0] + 400,
					arena[1],
					arena[2]
				};
			}
			
			_returnA = saveLoc(a);
			_returnB = saveLoc(b);
		}
		
		private int[][] saveLoc(CustomOlyTeam team)
		{
			int[][] loc = new int[team.size()][3];
			int i = 0;
			for (Player p : team.getMembers())
			{
				loc[i][0] = p.getX();
				loc[i][1] = p.getY();
				loc[i][2] = p.getZ();
				i++;
			}
			return loc;
		}
		
		@Override
		public void run()
		{
			announceCall();
			
			if (Config.CUSTOM_OLY_USE_INSTANCE)
				_instance = InstanceManager.getInstance().createInstance();
			
			port(_a, _spawnA, 1);
			try
			{
				Thread.sleep(250);
			}
			catch (InterruptedException e)
			{
			}
			port(_b, _spawnB, 2);
			freeze(true);
			buff();
			countdown();
			try
			{
				Thread.sleep(Config.CUSTOM_OLY_WAIT_SECONDS * 1000L);
			}
			catch (InterruptedException e)
			{
			}
			freeze(false);
			forEach(new Each()
			{
				@Override
				public void run(Player p)
				{
					p.setArenaAttack(true);
					p.setInArenaEvent(true);
					p.sendPacket(new ExShowScreenMessage("Fight!", 2000));
				}
			});
			
			long end = System.currentTimeMillis() + Config.CUSTOM_OLY_FIGHT_TIMEOUT * 1000L;
			int winner = 0;
			while (System.currentTimeMillis() < end)
			{
				boolean aLive = teamAlive(_a);
				boolean bLive = teamAlive(_b);
				if (aLive && !bLive)
				{
					winner = 1;
					break;
				}
				if (bLive && !aLive)
				{
					winner = 2;
					break;
				}
				if (!aLive && !bLive)
					break;
				try
				{
					Thread.sleep(Config.CUSTOM_OLY_CHECK_MS);
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
			
			if (winner == 1)
				reward(_a, _b);
			else if (winner == 2)
				reward(_b, _a);
			
			finish();
		}
		
		private void announceCall()
		{
			final int sec = Math.max(1, Config.CUSTOM_OLY_CALL_SECONDS);
			forEach(new Each()
			{
				@Override
				public void run(Player p)
				{
					p.sendMessage(Config.CUSTOM_OLY_NAME + ": te llamaron. Teleport en " + sec + "s.");
					p.sendPacket(new ExShowScreenMessage(Config.CUSTOM_OLY_NAME + ": te llamaron! Teleport en " + sec + "s", 4000, ExShowScreenMessage.SMPOS.TOP_CENTER, true));
				}
			});
			
			for (int left = sec; left > 0; left--)
			{
				final int time = left;
				if (time <= 5 || time == sec || time % 5 == 0)
				{
					forEach(new Each()
					{
						@Override
						public void run(Player p)
						{
							p.sendPacket(new ExShowScreenMessage("Teleport en " + time + "s", 1000));
						}
					});
				}
				try
				{
					Thread.sleep(1000L);
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
		}
		
		private void port(CustomOlyTeam team, int[] spawn, int aura)
		{
			final int size = team.size();
			int i = 0;
			for (Player p : team.getMembers())
			{
				if (p == null || !p.isOnline())
					continue;
				p.setArenaProtection(true);
				p.setInArenaEvent(true);
				if (Config.CUSTOM_OLY_USE_INSTANCE && _instance != null)
				{
					p.setInstance(_instance, true);
					Summon s = p.getSummon();
					if (s != null)
						s.setInstance(_instance, true);
				}
				// Scatter the team around its spawn point so members don't stack (centered on Y).
				int y = spawn[1] + ((2 * i - (size - 1)) * Config.CUSTOM_OLY_SPAWN_SPREAD) / 2;
				p.teleportTo(spawn[0], y, spawn[2], 0);
				p.setTeamTour(aura);
				i++;
			}
		}
		
		private void freeze(final boolean on)
		{
			forEach(new Each()
			{
				@Override
				public void run(Player p)
				{
					p.setIsInvul(on);
					p.setStopArena(on);
				}
			});
		}
		
		private void buff()
		{
			forEach(new Each()
			{
				@Override
				public void run(Player p)
				{
					CustomOlyRules.prepareFighter(p);
				}
			});
		}
		
		private void countdown()
		{
			forEach(new Each()
			{
				@Override
				public void run(Player p)
				{
					p.sendPacket(new ExShowScreenMessage("La pelea empieza en " + Config.CUSTOM_OLY_WAIT_SECONDS + "s", 4000));
				}
			});
		}
		
		private boolean teamAlive(CustomOlyTeam team)
		{
			for (Player p : team.getMembers())
			{
				if (p != null && p.isOnline() && !p.isDead() && p.isArenaAttack())
					return true;
			}
			return false;
		}
		
		private void reward(CustomOlyTeam win, CustomOlyTeam lose)
		{
			if (Config.CUSTOM_OLY_STEAL_PER_PLAYER)
			{
				int n = Math.min(win.size(), lose.size());
				for (int i = 0; i < n; i++)
					CustomOlyManager.getInstance().applyFightResult(win.getMembers().get(i), lose.getMembers().get(i));
				for (int i = n; i < win.size(); i++)
					CustomOlyManager.getInstance().applyFightResult(win.getMembers().get(i), lose.getMembers().get(0));
			}
			else
			{
				Player w = win.getLeader();
				Player l = lose.getLeader();
				if (w != null && l != null)
					CustomOlyManager.getInstance().applyFightResult(w, l);
			}
		}
		
		private void finish()
		{
			int ia = 0;
			for (Player p : _a.getMembers())
			{
				restore(p, _returnA[ia][0], _returnA[ia][1], _returnA[ia][2]);
				ia++;
			}
			int ib = 0;
			for (Player p : _b.getMembers())
			{
				restore(p, _returnB[ib][0], _returnB[ib][1], _returnB[ib][2]);
				ib++;
			}
			if (_instance != null && _instance.getId() != 0)
				InstanceManager.getInstance().deleteInstance(_instance.getId());
		}
		
		private void restore(Player p, int x, int y, int z)
		{
			if (p == null)
				return;
			if (p.isDead())
				p.doRevive();
			p.setArenaAttack(false);
			p.setInArenaEvent(false);
			p.setArenaProtection(false);
			p.setIsInvul(false);
			p.setStopArena(false);
			p.setTeamTour(0);
			if (Config.CUSTOM_OLY_USE_INSTANCE)
			{
				p.setInstance(InstanceManager.getInstance().getInstance(0), true);
				Summon s = p.getSummon();
				if (s != null)
					s.setInstance(InstanceManager.getInstance().getInstance(0), true);
			}
			if (p.isOnline())
			{
				p.setCurrentHp(p.getMaxHp());
				p.setCurrentCp(p.getMaxCp());
				p.setCurrentMp(p.getMaxMp());
				p.teleportTo(x, y, z, 0);
			}
		}
		
		private void forEach(Each each)
		{
			for (Player p : _a.getMembers())
			{
				if (p != null && p.isOnline())
					each.run(p);
			}
			for (Player p : _b.getMembers())
			{
				if (p != null && p.isOnline())
					each.run(p);
			}
		}
	}
	
	private interface Each
	{
		void run(Player p);
	}
	
	public static CustomOlyArena getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final CustomOlyArena INSTANCE = new CustomOlyArena();
	}
}
