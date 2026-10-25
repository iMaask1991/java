package net.sf.l2j.gameserver.model;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * A datatype used to retain informations for announcements. It notably holds a {@link ScheduledFuture}.
 */
public class Announcement implements Runnable
{
	protected final String _message;
	
	protected boolean _critical;
	protected boolean _auto;
	protected boolean _unlimited;
	
	protected int _initialDelay;
	protected int _delay;
	protected int _limit;
	protected int _tempLimit; // Temporary limit, used by current timer.
	
	protected ScheduledFuture<?> _task;
	
	public Announcement(String message, boolean critical)
	{
		_message = message;
		_critical = critical;
	}
	
	public Announcement(String message, boolean critical, boolean auto, int initialDelay, int delay, int limit)
	{
		_message = message;
		_critical = critical;
		_auto = auto;
		_initialDelay = initialDelay;
		_delay = delay;
		_limit = limit;
		
		if (_auto)
		{
			switch (_limit)
			{
				case 0: // unlimited
					_task = ThreadPool.scheduleAtFixedRate(this, _initialDelay * 1000, _delay * 1000); // self schedule at fixed rate
					_unlimited = true;
					break;
				
				default:
					_task = ThreadPool.schedule(this, _initialDelay * 1000); // self schedule (initial)
					_tempLimit = _limit;
					break;
			}
		}
	}
	
	@Override
	public void run()
	{
		if (!_unlimited)
		{
			if (_tempLimit == 0)
				return;
			
			_task = ThreadPool.schedule(this, _delay * 1000); // self schedule (worker)
			_tempLimit--;
		}
		World.announceToOnlinePlayers(_message, _critical);
	}
	
	public String getMessage()
	{
		return _message;
	}
	
	public boolean isCritical()
	{
		return _critical;
	}
	
	public boolean isAuto()
	{
		return _auto;
	}
	
	public int getInitialDelay()
	{
		return _initialDelay;
	}
	
	public int getDelay()
	{
		return _delay;
	}
	
	public int getLimit()
	{
		return _limit;
	}
	
	public void stopTask()
	{
		if (_task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}

	public static void AnnounceEvents(String text)
	{
		// AnnounceIdEvents (ModsHadesProject.properties master). 18 = SuperNotif (10/18).
		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
				player.sendPacket(cs);
		}
	}
	
	public void reloadTask()
	{
		stopTask();
		
		if (_auto)
		{
			switch (_limit)
			{
				case 0: // unlimited
					_task = ThreadPool.scheduleAtFixedRate(this, _initialDelay * 1000, _delay * 1000); // self schedule at fixed rate
					_unlimited = true;
					break;
				
				default:
					_task = ThreadPool.schedule(this, _initialDelay * 1000); // self schedule (initial)
					_tempLimit = _limit;
					break;
			}
		}
	}

	public static boolean isSummoning = false;
	
	public static void ArenaAnnounce(String text)
	{
		isSummoning = true;
		
		// AnnounceIdEvents (ModsHadesProject.properties master). 18 = SuperNotif.
		CreatureSay cs = new CreatureSay(0, Config.ANNOUNCE_ID_EVENT, "", "" + text);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player != null && player.isOnline())
			{
				player.sendPacket(cs);
				
				if (Config.TOURNAMENT_EVENT_SUMMON)
				{
					if (!player.isAio() || !player.isAio() || !(player.isInsideZone(ZoneId.TOURNAMENT) || player.isDead() || player.isInArenaEvent() || player.isInsideZone(ZoneId.BOSS) || player.isInStoreMode() || player.isRooted() || player.getKarma() > 0 || player.isInOlympiadMode() || player.isOlympiadProtection() || player.isFestivalParticipant()))
					{
						
						ThreadPool.schedule(new Runnable()
						{
							@Override
							public void run()
							{
								isSummoning = false;
							}
						}, 31000);
						
						SpawnLocation _position = new SpawnLocation(Config.Tournament_locx + Rnd.get(-100, 100), Config.Tournament_locy + Rnd.get(-100, 100), Config.Tournament_locz, 0);
						
						ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.EVENT.getId());
						confirm.addString("Do you want to go to the Tournament?");
						confirm.addZoneName(_position);
						confirm.addTime(30000);
						confirm.addRequesterId(player.getObjectId());
						player.sendPacket(confirm);
					}
					
				}
			}
		}
		
		cs = null;
	}
}