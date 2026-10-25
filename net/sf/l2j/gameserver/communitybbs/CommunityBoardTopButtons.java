package net.sf.l2j.gameserver.communitybbs;

/**
 * Maps the 7 client Community Board tabs to custom HomeBoard pages (server-side).
 */
public class CommunityBoardTopButtons
{
	private static final String BYPASS_PREFIX = "bypass ";
	
	private CommunityBoardTopButtons()
	{
	}
	
	public static boolean isEnabled()
	{
		return CommunityBoardConfig.CUSTOM_CB_ENABLED && CommunityBoardConfig.CUSTOM_CB_TOP_BUTTONS;
	}
	
	public static String getTop()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbshome" : BYPASS_PREFIX + "_bbshome";
	}
	
	public static String getFavorite()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;buffer/main.html" : BYPASS_PREFIX + "_bbsgetfav";
	}
	
	public static String getRegion()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;gatekeeper/main.html" : BYPASS_PREFIX + "_bbsloc";
	}
	
	public static String getClan()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;merchant/main.html" : BYPASS_PREFIX + "_bbsclan";
	}
	
	public static String getMemo()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;buffer/main2.html" : BYPASS_PREFIX + "_bbsmemo";
	}
	
	public static String getMail()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;home.html" : BYPASS_PREFIX + "_maillist_0_1_0_";
	}
	
	public static String getFriends()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;home.html" : BYPASS_PREFIX + "_friendlist_0_";
	}
	
	public static String getAddFavorite()
	{
		return isEnabled() ? BYPASS_PREFIX + "_bbstop;home.html" : BYPASS_PREFIX + "bbs_add_fav";
	}
	
	/**
	 * Redirects original tab bypasses to custom pages when top-button remapping is enabled.
	 * @param command incoming CB command
	 * @return command to execute
	 */
	public static String resolveLegacyCommand(String command)
	{
		if (!isEnabled())
			return command;
		
		if (command.equals("_bbsgetfav") || command.startsWith("_bbsgetfav;"))
			return "_bbstop;buffer/main.html";
		
		if (command.equals("_bbsloc") || command.startsWith("_bbsloc"))
			return "_bbstop;gatekeeper/main.html";
		
		if (command.equals("_bbsclan") || command.startsWith("_bbsclan"))
			return "_bbstop;merchant/main.html";
		
		if (command.equals("_bbsmemo") || command.startsWith("_bbsmemo"))
			return "_bbstop;buffer/main2.html";
		
		if (command.equals("_maillist_0_1_0_") || command.startsWith("_bbsmail"))
			return "_bbstop;home.html";
		
		if (command.equals("_friendlist_0_") || command.startsWith("_friend") || command.startsWith("_block"))
			return "_bbstop;home.html";
		
		if (command.equals("bbs_add_fav") || command.startsWith("bbs_add_fav"))
			return "_bbstop;home.html";
		
		return command;
	}
}
