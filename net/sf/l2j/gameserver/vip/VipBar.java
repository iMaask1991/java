package net.sf.l2j.gameserver.vip;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminVip;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * Barra VIP de la interface (StatusWnd.VIPBar).
 * Manda al cliente las horas de VIP que le quedan con un mensaje oculto "#VIP:<horas>"
 * (la interface lo intercepta en ChatWnd y no lo muestra en el chat).
 * 30 dias (720 horas) = barra llena. Ademas saca el VIP apenas vence, aunque este online.
 */
public final class VipBar
{
	private static final Logger LOG = Logger.getLogger(VipBar.class.getName());
	
	public static final String MARKER = "#VIP:";
	public static final int MAX_HOURS = 720; // 30 dias = 100%
	private static final long CHECK_INTERVAL = 60000L; // 1 minuto
	
	private static final Map<Integer, Integer> LAST_SENT = new ConcurrentHashMap<>();
	private static final AtomicBoolean STARTED = new AtomicBoolean(false);
	
	private VipBar()
	{
	}
	
	/** Horas de VIP restantes (0 si no es VIP), tope 720. */
	public static int getHoursLeft(Player player)
	{
		if (!player.isVip())
			return 0;
		final long left = player.getVipEndTime() - System.currentTimeMillis();
		if (left <= 0)
			return 0;
		final long hours = (left + 3599999L) / 3600000L; // redondeo para arriba
		return (int) Math.min(MAX_HOURS, hours);
	}
	
	/** Llamado al final de EnterWorld. */
	public static void onEnterWorld(Player player)
	{
		start();
		LAST_SENT.remove(player.getObjectId());
		update(player);
	}
	
	/** Manda el valor actual de la barra (se llama cuando cambia el VIP). */
	public static void update(Player player)
	{
		if (player == null || player.getClient() == null || player.isOfflineTrader())
			return;
		final int hours = getHoursLeft(player);
		LAST_SENT.put(player.getObjectId(), hours);
		player.sendPacket(new CreatureSay(0, 0, "", MARKER + hours));
	}
	
	private static void start()
	{
		if (STARTED.compareAndSet(false, true))
			ThreadPool.scheduleAtFixedRate(VipBar::tick, CHECK_INTERVAL, CHECK_INTERVAL);
	}
	
	private static void tick()
	{
		try
		{
			final long now = System.currentTimeMillis();
			for (Player player : World.getInstance().getPlayers())
			{
				if (player == null || player.getClient() == null || player.isOfflineTrader())
					continue;
				
				// Vencimiento estando online
				if (player.isVip() && player.getVipEndTime() > 0 && player.getVipEndTime() <= now)
				{
					new AdminVip().removeVip(null, player, player.getName());
					player.sendMessage("Tu VIP ha expirado.");
				}
				
				final int hours = getHoursLeft(player);
				final Integer last = LAST_SENT.get(player.getObjectId());
				if (last == null || last.intValue() != hours)
					update(player);
			}
			// limpiar los que ya no estan online
			LAST_SENT.keySet().removeIf(id -> World.getInstance().getPlayer(id) == null);
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, "VipBar: error en el chequeo.", e);
		}
	}
}
