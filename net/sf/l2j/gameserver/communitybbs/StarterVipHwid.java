package net.sf.l2j.gameserver.communitybbs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.handler.itemhandlers.VipCoin;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * VIP de bienvenida: una sola vez por HWID. Horas en DonationManager.properties.
 */
public final class StarterVipHwid
{
	private static final CLogger LOGGER = new CLogger(StarterVipHwid.class.getName());
	
	private static final String INSERT = "INSERT INTO starter_vip_hwid (hwid, char_id, char_name, claimed_at) VALUES (?,?,?,?)";
	private static final String DELETE = "DELETE FROM starter_vip_hwid WHERE hwid=?";
	
	private StarterVipHwid()
	{
	}
	
	public static void claim(Player player)
	{
		if (player == null)
			return;
		
		if (!Config.STARTER_VIP_ENABLED)
		{
			player.sendMessage("El VIP de bienvenida esta desactivado.");
			return;
		}
		
		final int hours = Config.STARTER_VIP_HOURS;
		if (hours <= 0)
		{
			player.sendMessage("El VIP de bienvenida no esta configurado.");
			return;
		}
		
		final String hwid = player.getHWID();
		if (hwid == null || hwid.isEmpty())
		{
			player.sendMessage("No se pudo leer tu HWID. Relogea e intenta de nuevo.");
			return;
		}
		
		if (player.isVip() && player.getVipEndTime() > System.currentTimeMillis())
		{
			player.sendMessage("Ya tenes VIP activo. Espera a que expire.");
			return;
		}
		
		if (!markClaimed(hwid, player))
		{
			player.sendMessage("Este PC ya reclamo el VIP de bienvenida.");
			return;
		}
		
		if (!VipCoin.applyHours(player, hours))
			unmarkClaimed(hwid);
	}
	
	private static boolean markClaimed(String hwid, Player player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT))
		{
			ps.setString(1, hwid);
			ps.setInt(2, player.getObjectId());
			ps.setString(3, player.getName());
			ps.setLong(4, System.currentTimeMillis());
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			return false;
		}
		catch (Exception e)
		{
			LOGGER.error("StarterVipHwid: insert failed for {}", e, hwid);
			return false;
		}
	}
	
	private static void unmarkClaimed(String hwid)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE))
		{
			ps.setString(1, hwid);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("StarterVipHwid: delete failed for {}", e, hwid);
		}
	}
}
