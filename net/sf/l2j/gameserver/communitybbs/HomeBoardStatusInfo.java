package net.sf.l2j.gameserver.communitybbs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Community Board placeholders for .vip / .hero / .aio status pages.
 */
public final class HomeBoardStatusInfo
{
	private HomeBoardStatusInfo()
	{
	}
	
	public static String applyHomeDashboard(String html, Player player)
	{
		if (html == null || player == null)
			return html;
		
		html = applyVip(html, player);
		html = applyHero(html, player);
		html = html.replace("%noble%", player.isNoble() ? "Si" : "No");
		html = html.replace("%play_time%", formatPlayTime(player.getOnlineTime()));
		html = html.replace("%player_ip%", resolvePlayerIp(player));
		return html;
	}
	
	private static String formatPlayTime(long totalSeconds)
	{
		final long days = Math.max(0, totalSeconds / 86400);
		final long hours = Math.max(0, (totalSeconds % 86400) / 3600);
		final long minutes = Math.max(0, (totalSeconds % 3600) / 60);
		if (days > 0)
			return days + "d " + hours + "h";
		if (hours > 0)
			return hours + "h " + minutes + "m";
		return minutes + "m";
	}
	
	private static String resolvePlayerIp(Player player)
	{
		if (player.getClient() == null || player.getClient().getConnection() == null || player.getClient().getConnection().getInetAddress() == null)
			return "-";
		
		return player.getClient().getConnection().getInetAddress().getHostAddress();
	}
	
	public static String applyVip(String html, Player player)
	{
		if (html == null || player == null)
			return html;
		
		html = html.replace("%name%", player.getName());
		html = html.replace("%vip_xp%", formatBonusPercent(Config.VIP_RATE_XP));
		html = html.replace("%vip_sp%", formatBonusPercent(Config.VIP_RATE_SP));
		html = html.replace("%vip_adena%", formatBonusPercent(Config.VIP_ADENA_RATES));
		html = html.replace("%vip_spoil%", formatBonusPercent(Config.VIP_SPOIL_RATES));
		html = html.replace("%vip_drop%", formatBonusPercent(Config.VIP_DROP_RATES));
		html = html.replace("%vip_raid%", formatBonusPercent(Config.VIP_RATE_DROP_ITEMS_BY_RAID));
		
		if (player.isVip() && player.getVipEndTime() > System.currentTimeMillis())
		{
			final long remainingMs = player.getVipEndTime() - System.currentTimeMillis();
			final long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(remainingMs));
			final long hours = Math.max(0, TimeUnit.MILLISECONDS.toHours(remainingMs) % 24);
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60);
			
			html = html.replace("%vip_status%", "Activo");
			html = html.replace("%vip_end%", new SimpleDateFormat(Config.VIP_DATE_FORMAT).format(new Date(player.getVipEndTime())));
			html = html.replace("%vip_days%", String.valueOf(days));
			html = html.replace("%vip_hours%", String.valueOf(hours));
			html = html.replace("%vip_minutes%", String.valueOf(minutes));
			html = html.replace("%vip_remaining%", formatRemaining(days, hours, minutes));
		}
		else
		{
			html = html.replace("%vip_status%", "Inactivo");
			html = html.replace("%vip_end%", "-");
			html = html.replace("%vip_days%", "0");
			html = html.replace("%vip_hours%", "0");
			html = html.replace("%vip_minutes%", "0");
			html = html.replace("%vip_remaining%", "Sin VIP");
		}
		
		return html;
	}
	
	public static String applyHero(String html, Player player)
	{
		if (html == null || player == null)
			return html;
		
		html = html.replace("%name%", player.getName());
		
		final boolean olyHero = HeroManager.getInstance().isActiveHero(player.getObjectId());
		final long end = player.getHeroEndTime();
		final long now = System.currentTimeMillis();
		
		if (player.isHero() && end > now)
		{
			final long remainingMs = end - now;
			final long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(remainingMs));
			final long hours = Math.max(0, TimeUnit.MILLISECONDS.toHours(remainingMs) % 24);
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60);
			
			html = html.replace("%hero_status%", olyHero ? "Activo (Olympiad + Custom)" : "Activo");
			html = html.replace("%hero_end%", new SimpleDateFormat(Config.HERO_DATE_FORMAT).format(new Date(end)));
			html = html.replace("%hero_days%", String.valueOf(days));
			html = html.replace("%hero_hours%", String.valueOf(hours));
			html = html.replace("%hero_minutes%", String.valueOf(minutes));
			html = html.replace("%hero_remaining%", formatRemaining(days, hours, minutes));
		}
		else if (player.isHero() && end == 0)
		{
			html = html.replace("%hero_status%", olyHero ? "Activo (Olympiad)" : "Activo (Permanente)");
			html = html.replace("%hero_end%", olyHero ? "Periodo Olympiad" : "Permanente");
			html = html.replace("%hero_days%", "-");
			html = html.replace("%hero_hours%", "-");
			html = html.replace("%hero_minutes%", "-");
			html = html.replace("%hero_remaining%", olyHero ? "Hero de Olympiad" : "Sin vencimiento");
		}
		else
		{
			html = html.replace("%hero_status%", "Inactivo");
			html = html.replace("%hero_end%", "-");
			html = html.replace("%hero_days%", "0");
			html = html.replace("%hero_hours%", "0");
			html = html.replace("%hero_minutes%", "0");
			html = html.replace("%hero_remaining%", "Sin Hero");
		}
		
		return html;
	}
	
	public static String applyAio(String html, Player player)
	{
		if (html == null || player == null)
			return html;
		
		html = html.replace("%name%", player.getName());
		
		if (player.isAio() && player.getAioEndTime() > System.currentTimeMillis())
		{
			final long remainingMs = player.getAioEndTime() - System.currentTimeMillis();
			final long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(remainingMs));
			final long hours = Math.max(0, TimeUnit.MILLISECONDS.toHours(remainingMs) % 24);
			final long minutes = Math.max(0, TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60);
			
			html = html.replace("%aio_status%", "Activo");
			html = html.replace("%aio_end%", new SimpleDateFormat(Config.AIO_DATE_FORMAT).format(new Date(player.getAioEndTime())));
			html = html.replace("%aio_days%", String.valueOf(days));
			html = html.replace("%aio_hours%", String.valueOf(hours));
			html = html.replace("%aio_minutes%", String.valueOf(minutes));
			html = html.replace("%aio_remaining%", formatRemaining(days, hours, minutes));
		}
		else
		{
			html = html.replace("%aio_status%", "Inactivo");
			html = html.replace("%aio_end%", "-");
			html = html.replace("%aio_days%", "0");
			html = html.replace("%aio_hours%", "0");
			html = html.replace("%aio_minutes%", "0");
			html = html.replace("%aio_remaining%", "Sin AIO");
		}
		
		return html;
	}
	
	private static String formatRemaining(long days, long hours, long minutes)
	{
		if (days > 0)
			return days + "d " + hours + "h";
		if (hours > 0)
			return hours + "h " + minutes + "m";
		return minutes + "m";
	}
	
	private static String formatBonusPercent(double multiplier)
	{
		final int bonus = (int) Math.round((multiplier - 1.0) * 100.0);
		if (bonus > 0)
			return "+" + bonus + "%";
		if (bonus < 0)
			return bonus + "%";
		return "+0%";
	}
}
