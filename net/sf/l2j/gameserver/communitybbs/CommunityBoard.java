package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.BufferBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.ClanBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.FriendsBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.HomeBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.MailBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PostBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.ShopBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TeleportBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopicBBSManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class CommunityBoard
{
	protected CommunityBoard()
	{
	}
	
	/**
	 * Opens the custom Community Board page for configured shop NPCs.
	 * IDs: CommunityBoard.ini CommunityNpcMerchant / Buffer / Gatekeeper / Donate.
	 */
	public static boolean openCustomNpcBoard(Player player, int npcId)
	{
		if (player == null || !Config.ENABLE_COMMUNITY_BOARD || !CommunityBoardConfig.CUSTOM_CB_ENABLED)
			return false;
		
		final String command = CommunityBoardConfig.getNpcBoardCommand(npcId);
		if (command == null)
			return false;
		
		getInstance().handleCommands(player.getClient(), command);
		return true;
	}
	
	public void handleCommands(GameClient client, String command)
	{
		final Player player = client.getPlayer();
		if (player == null)
			return;
		
		if (!Config.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}
		
		command = CommunityBoardTopButtons.resolveLegacyCommand(command);
		
		if (HomeBBSManager.handlesCommand(command))
			HomeBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbshome"))
			TopBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsloc"))
			RegionBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsclan"))
			ClanBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsmemo"))
			TopicBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsmail") || command.equals("_maillist_0_1_0_"))
			MailBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_friend") || command.startsWith("_block"))
			FriendsBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbstopics"))
			TopicBBSManager.getInstance().parseCmd(command, player);
		else if (command.startsWith("_bbsposts"))
			PostBBSManager.getInstance().parseCmd(command, player);
		else if (!CommunityBoardConfig.CUSTOM_CB_ENABLED && command.startsWith("_bbsshop"))
			ShopBBSManager.getInstance().parseCmd(command, player);
		else if (!CommunityBoardConfig.CUSTOM_CB_ENABLED && command.startsWith("_bbsbuff"))
			BufferBBSManager.getInstance().parseCmd(command, player);
		else if (!CommunityBoardConfig.CUSTOM_CB_ENABLED && command.startsWith("_bbsteleport"))
			TeleportBBSManager.getInstance().parseCmd(command, player);
		else
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", player);
	}
	
	public void handleWriteCommands(GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		final Player player = client.getPlayer();
		if (player == null)
			return;
		
		if (!Config.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.CB_OFFLINE);
			return;
		}
		
		if (url.equals("Topic"))
			TopicBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else if (url.equals("Post"))
			PostBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else if (url.equals("_bbsloc"))
			RegionBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else if (url.equals("_bbsclan"))
			ClanBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else if (url.equals("Mail"))
			MailBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else if (url.equals("_friend"))
			FriendsBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else if (url.equals("_bbsstats"))
			HomeBBSManager.getInstance().parseWrite(arg1, arg2, arg3, arg4, arg5, player);
		else
			BaseBBSManager.separateAndSend("<html><body><br><br><center>The command: " + url + " isn't implemented.</center></body></html>", player);
	}
	
	public static CommunityBoard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBoard INSTANCE = new CommunityBoard();
	}
}