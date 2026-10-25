package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Turns off afk status of {@link Character} after PERIOD Ms.
 * @author SweeTs
 */
public final class AfkTaskManager implements Runnable
{
	private static final long ALLOWED_AFK_PERIOD = TimeUnit.HOURS.toMillis(48); // 48h

	private final Map<Player, Long> _players = new ConcurrentHashMap<>();

	public static final AfkTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected AfkTaskManager()
	{
		// Run task each second.
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}

	/**
	 * Adds {@link Player} to the AfkTask.
	 * @param player : Player to be added and checked.
	 */
	public final void add(Player player)
	{
		_players.put(player, System.currentTimeMillis() + ALLOWED_AFK_PERIOD);
	}

	/**
	 * Removes {@link Character} from the AfkTask.
	 * @param character : {@link Character} to be removed.
	 */
	public final void remove(Creature character)
	{
		_players.remove(character);
	}

	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_players.isEmpty())
			return;

		// Get current time.
		final long time = System.currentTimeMillis();

		// Loop all characters.
		for (Map.Entry<Player, Long> entry : _players.entrySet())
		{
			// Time hasn't passed yet, skip.
			if (time < entry.getValue())
				continue;

			// Get character.
			final Player player = entry.getKey();

			player.setAfking(true);

			_players.remove(player);
		}
	}

	private static class SingletonHolder
	{
		protected static final AfkTaskManager _instance = new AfkTaskManager();
	}
}