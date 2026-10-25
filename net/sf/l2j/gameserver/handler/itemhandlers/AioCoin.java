package net.sf.l2j.gameserver.handler.itemhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAio;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Coin AIO generico. Dias por itemId en Aio.properties
 * ({@code Aio_Coin1}/{@code Aio_1}, {@code Aio_Coin2}/{@code Aio_2}, etc.).
 * En el item XML: {@code handler="AioCoin"}.
 * No stackea: si ya tiene AIO activo, no deja usar otro coin.
 */
public class AioCoin implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof Player)
			playable.sendMessage("AIO se compra desde el Community Board Donate.");
	}
	
	public static boolean apply(Player player, int days)
	{
		if (player == null || days <= 0)
			return false;
		
		if (!Config.ENABLE_AIO_SYSTEM)
		{
			player.sendMessage("El sistema AIO esta desactivado.");
			return false;
		}
		
		if (player.isAio() && player.getAioEndTime() > System.currentTimeMillis())
		{
			player.sendMessage("Ya tenes AIO activo. Espera a que expire para usar otro.");
			return false;
		}
		
		player.getStat().addExp(player.getStat().getExpForLevel(81));
		player.setClassId(player.getClassId().getFirstClass().getId());
		player.lostAioSkills();
		player.setAio(true);
		AdminAio.applyAioName(player);
		
		if (Config.ALLOW_AIO_EQUIP_ITEM)
			AdminAio.giveAioItems(player);
		
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, days);
		
		player.setAioEndTime(calendar.getTimeInMillis());
		
		if (Config.ALLOW_AIO_NCOLOR)
			player.getAppearance().setNameColor(Config.AIO_NCOLOR);
		
		if (Config.ALLOW_AIO_TCOLOR)
			player.getAppearance().setTitleColor(Config.AIO_TCOLOR);
		
		player.rewardAioSkills();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET aio=1, aio_end=? WHERE obj_id=?"))
		{
			ps.setLong(1, player.getAioEndTime());
			ps.setInt(2, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			player.sendMessage("Ocurrio un error al guardar AIO, contacta al staff.");
		}
		
		updateChar(player);
		player.sendMessage("AIO activado por " + days + " dia(s).");
		return true;
	}
	
	private static void updateChar(Player player)
	{
		player.sendPacket(new InventoryUpdate());
		player.broadcastUserInfo();
		player.broadcastCharInfo();
		player.sendSkillList();
		player.store();
	}
}
