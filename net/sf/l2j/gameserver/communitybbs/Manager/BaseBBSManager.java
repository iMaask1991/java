package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public abstract class BaseBBSManager
{
	protected static final CLogger LOGGER = new CLogger(BaseBBSManager.class.getName());
	
	protected static final String CB_PATH = "data/html/CommunityBoard/";
	
	private static final long OLYS_CACHE_MS = 60000L;
	private static volatile String _cachedOlys = "-";
	private static volatile long _cachedOlysAt;
	
	public void parseCmd(String command, Player player)
	{
		separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", player);
	}
	
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		separateAndSend("<html><body><br><br><center>The command: " + ar1 + " isn't implemented.</center></body></html>", player);
	}
	
	public static void separateAndSend(String html, Player player)
	{
		if (html == null || player == null)
			return;
		
		if (html.length() < 4090)
		{
			player.sendPacket(new ShowBoard(html, "101"));
			player.sendPacket(ShowBoard.STATIC_SHOWBOARD_102);
			player.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
		}
		else if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090, html.length()), "102"));
			player.sendPacket(ShowBoard.STATIC_SHOWBOARD_103);
		}
		else if (html.length() < 12270)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
			player.sendPacket(new ShowBoard(html.substring(8180, html.length()), "103"));
		}
		else
		{
			LOGGER.warn("Community Board HTML truncated ({} chars, max 12269).", html.length());
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
			player.sendPacket(new ShowBoard(html.substring(8180, 12269), "103"));
		}
	}
	
	protected static void send1001(String html, Player player)
	{
		if (html.length() < 8180)
			player.sendPacket(new ShowBoard(html, "1001"));
	}
	
	protected static void send1002(Player player)
	{
		send1002(player, " ", " ", "0");
	}
	
	protected static void send1002(Player player, String string, String string2, String string3)
	{
		final List<String> params = new ArrayList<>();
		params.add("0");
		params.add("0");
		params.add("0");
		params.add("0");
		params.add("0");
		params.add("0");
		params.add(player.getName());
		params.add(Integer.toString(player.getObjectId()));
		params.add(player.getAccountName());
		params.add("9");
		params.add(string2);
		params.add(string2);
		params.add(string);
		params.add(string3);
		params.add(string3);
		params.add("0");
		params.add("0");
		
		player.sendPacket(new ShowBoard(params));
	}
	
	/**
	 * Loads an HTM located in the default CB path.
	 * @param file : the file to load.
	 * @param player : the requester.
	 */
	protected void loadStaticHtm(String file, Player player)
	{
		String htmlContent = HtmCache.getInstance().getHtm(CB_PATH + getFolder() + file);
		if (htmlContent == null)
		{
			separateAndSend("<html><body><br><br><center>File not found.</center></body></html>", player);
			return;
		}
		
		htmlContent = htmlContent.replace("%playername%", player.getName());
		htmlContent = htmlContent.replace("%olys%", getOlympiadEndDate());
		separateAndSend(htmlContent, player);
	}
	
	private static String getOlympiadEndDate()
	{
		final long now = System.currentTimeMillis();
		if (now - _cachedOlysAt < OLYS_CACHE_MS)
			return _cachedOlys;
		
		String formatted = "-";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT olympiad_end FROM olympiad_data");
			ResultSet rs = statement.executeQuery())
		{
			if (rs.next())
			{
				final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				sdf.setTimeZone(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
				formatted = sdf.format(new Date(rs.getLong("olympiad_end")));
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Exception: Select time olys: {}", e.getMessage());
		}
		
		_cachedOlys = formatted;
		_cachedOlysAt = now;
		return formatted;
	}
	
	/**
	 * That method is overidden in every board type. It allows to switch of folders following the board.
	 * @return the folder.
	 */
	protected String getFolder()
	{
		return "";
	}
}