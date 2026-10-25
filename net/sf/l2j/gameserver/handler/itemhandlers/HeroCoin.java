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
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

/**
 * Coin Hero generico. Dias por itemId en Hero.properties
 * ({@code Hero_Coin1}/{@code Hero_1}, {@code Hero_Coin2}/{@code Hero_2}, etc.).
 * En el item XML: {@code handler="HeroCoin"}.
 * {@link #apply(Player, int)} no stackea: si ya tiene Hero activo (timed o permanente), no deja usar otro coin.<br>
 * {@link #addDays(Player, int)} si acumula sobre el vencimiento actual (usado por el COMBO).
 */
public class HeroCoin implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof Player)
			playable.sendMessage("Hero se compra desde el Community Board Donate.");
	}
	
	public static boolean apply(Player player, int days)
	{
		if (player == null || days <= 0)
			return false;
		
		final long now = System.currentTimeMillis();
		if (player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > now))
		{
			player.sendMessage("Ya tenes Hero activo. Espera a que expire para usar otro.");
			return false;
		}
		
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, days);
		
		player.setHero(true, true);
		player.setHeroEndTime(calendar.getTimeInMillis());
		player.broadcastPacket(new SocialAction(player, 16));
		player.broadcastUserInfo();
		storeHero(player, true);
		
		player.store();
		player.sendMessage("Hero activado por " + days + " dia(s). El tiempo restante se ve en el Community Board.");
		return true;
	}
	
	/**
	 * Acumula dias de Hero sin rechazar si ya tiene Hero activo. Si no tiene (o esta vencido) activa desde ahora; si ya tiene, suma los dias al vencimiento actual. Si el Hero es permanente (sin fecha) no se toca, pero se considera aplicado.
	 * @param player : the player to give Hero days.
	 * @param days : the amount of days to accumulate.
	 * @return {@link Boolean#TRUE} if applied, {@link Boolean#FALSE} otherwise.
	 */
	public static boolean addDays(Player player, int days)
	{
		if (player == null || days <= 0)
			return false;
		
		final long now = System.currentTimeMillis();
		final boolean active = player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > now);
		
		// Hero permanente (hero_end = 0): no hay fecha que sumar, se respeta.
		if (active && player.getHeroEndTime() == 0)
		{
			player.sendMessage("Hero: ya lo tenes permanente.");
			return true;
		}
		
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(active ? player.getHeroEndTime() : now);
		calendar.add(Calendar.DAY_OF_MONTH, days);
		
		if (!player.isHero())
			player.setHero(true, true);
		
		player.setHeroEndTime(calendar.getTimeInMillis());
		player.broadcastPacket(new SocialAction(player, 16));
		player.broadcastUserInfo();
		storeHero(player, false);
		
		player.store();
		player.sendMessage("Hero: se acumularon " + days + " dia(s).");
		return true;
	}
	
	/**
	 * @param player : the player to save.
	 * @param storeFlag : also store the custom_hero flag (only needed when the Hero status is first granted).
	 */
	private static void storeHero(Player player, boolean storeFlag)
	{
		final String query = storeFlag ? "UPDATE characters SET custom_hero=1, hero_end=? WHERE obj_id=?" : "UPDATE characters SET hero_end=? WHERE obj_id=?";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setLong(1, player.getHeroEndTime());
			ps.setInt(2, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			player.sendMessage("Ocurrio un error al guardar Hero, contacta al staff.");
		}
	}
}
