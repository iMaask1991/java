package net.sf.l2j.gameserver.handler.itemhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * Coin VIP generico. Dias por itemId en Vip.properties
 * ({@code Vip_Coin1}/{@code Vip_1}, {@code Vip_Coin2}/{@code Vip_2}, etc.).
 * En el item XML: {@code handler="VipCoin"}.
 * {@link #apply(Player, int)} no stackea: si ya tiene VIP activo, no deja usar otro coin.<br>
 * {@link #addDays(Player, int)} / {@link #addHours(Player, int)} si acumulan sobre el vencimiento actual (usados por el COMBO y los eventos).
 */
public class VipCoin implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof Player)
			playable.sendMessage("VIP se compra desde el Community Board Donate.");
	}
	
	public static boolean apply(Player player, int days)
	{
		return applyDuration(player, days, 0);
	}
	
	public static boolean applyHours(Player player, int hours)
	{
		return applyDuration(player, 0, hours);
	}
	
	/**
	 * Acumula dias de VIP sin rechazar si ya tiene VIP activo. Si no tiene (o esta vencido) activa desde ahora; si ya tiene, suma los dias al vencimiento actual.
	 * @param player : the player to give VIP days.
	 * @param days : the amount of days to accumulate.
	 * @return {@link Boolean#TRUE} if applied, {@link Boolean#FALSE} otherwise.
	 */
	public static boolean addDays(Player player, int days)
	{
		return addDuration(player, days, 0);
	}
	
	/**
	 * Acumula horas de VIP sin rechazar si ya tiene VIP activo. Si no tiene (o esta vencido) activa desde ahora; si ya tiene, suma las horas al vencimiento actual.
	 * @param player : the player to give VIP hours.
	 * @param hours : the amount of hours to accumulate.
	 * @return {@link Boolean#TRUE} if applied, {@link Boolean#FALSE} otherwise.
	 */
	public static boolean addHours(Player player, int hours)
	{
		return addDuration(player, 0, hours);
	}
	
	private static boolean addDuration(Player player, int days, int hours)
	{
		if (player == null || (days <= 0 && hours <= 0))
			return false;
		
		final long now = System.currentTimeMillis();
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis((player.isVip() && player.getVipEndTime() > now) ? player.getVipEndTime() : now);
		
		if (days > 0)
			calendar.add(Calendar.DAY_OF_MONTH, days);
		if (hours > 0)
			calendar.add(Calendar.HOUR_OF_DAY, hours);
		
		player.setVip(true);
		player.setVipEndTime(calendar.getTimeInMillis());
		
		if (Config.ALLOW_VIP_TCOLOR)
			player.getAppearance().setTitleColor(Config.VIP_TCOLOR);
		
		player.broadcastUserInfo();
		storeVip(player);
		net.sf.l2j.gameserver.vip.VipBar.update(player); // barra VIP de la interface
		
		player.store();
		if (days > 0)
			player.sendMessage("VIP: se acumularon " + days + " dia(s).");
		else
			player.sendMessage("VIP: se acumularon " + hours + " hora(s).");
		return true;
	}
	
	private static void storeVip(Player player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET vip=1, vip_end=? WHERE obj_id=?"))
		{
			ps.setLong(1, player.getVipEndTime());
			ps.setInt(2, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			player.sendMessage("Ocurrio un error al guardar VIP, contacta al staff.");
		}
	}
	
	private static boolean applyDuration(Player player, int days, int hours)
	{
		if (player == null || (days <= 0 && hours <= 0))
			return false;
		
		if (player.isVip() && player.getVipEndTime() > System.currentTimeMillis())
		{
			player.sendMessage("Ya tenes VIP activo. Espera a que expire para usar otro.");
			return false;
		}
		
		final Calendar calendar = Calendar.getInstance();
		if (days > 0)
			calendar.add(Calendar.DAY_OF_MONTH, days);
		if (hours > 0)
			calendar.add(Calendar.HOUR_OF_DAY, hours);
		
		player.setVip(true);
		player.setVipEndTime(calendar.getTimeInMillis());
		
		if (Config.ALLOW_VIP_TCOLOR)
			player.getAppearance().setTitleColor(Config.VIP_TCOLOR);
		
		player.broadcastUserInfo();
		storeVip(player);
		net.sf.l2j.gameserver.vip.VipBar.update(player); // barra VIP de la interface
		
		player.store();
		if (hours > 0 && days <= 0)
			player.sendMessage("VIP activado por " + hours + " hora(s).");
		else
			player.sendMessage("VIP activado por " + days + " dia(s).");
		return true;
	}
}
