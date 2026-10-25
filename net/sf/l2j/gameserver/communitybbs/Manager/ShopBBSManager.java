package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.model.actor.Player;

public class ShopBBSManager extends BaseBBSManager
{
   protected ShopBBSManager()
   {
   }
   
   @Override
   public void parseCmd(String command, Player player)
   {
	   StringTokenizer st = new StringTokenizer(command, " ");
	   String actualCommand = st.nextToken(); // Get actual command
		
		if (st.countTokens() < 1)
			return;
		
       if (actualCommand.equalsIgnoreCase("_bbsshop"))
   	   {
   		player.setIsUsingCMultisell(true);
   			MultisellData.getInstance().separateAndSend(st.nextToken(), player, null, false);
   			loadStaticHtm("shop.htm", player);
   	   }
       else if (actualCommand.equalsIgnoreCase("_bbsshop_weapons"))
   	   {
   		player.setIsUsingCMultisell(true);
   			MultisellData.getInstance().separateAndSend(st.nextToken(), player, null, false);
   			loadStaticHtm("weapons.htm", player);
   	   }
       else if (actualCommand.equalsIgnoreCase("_bbsshop_armors"))
   	   {
   		player.setIsUsingCMultisell(true);
   			MultisellData.getInstance().separateAndSend(st.nextToken(), player, null, false);
   			loadStaticHtm("armors.htm", player);
   	   }
       else if (actualCommand.equalsIgnoreCase("_bbsshop_jewels"))
   	   {
   		player.setIsUsingCMultisell(true);
   			MultisellData.getInstance().separateAndSend(st.nextToken(), player, null, false);
   			loadStaticHtm("jewels.htm", player);
   	   }
		else
			super.parseCmd(command, player);		
   }
   
   @Override
   protected String getFolder()
   {
       return "top/";
   }
   
   public static ShopBBSManager getInstance()
   {
       return SingletonHolder.INSTANCE;
   }
   
   private static class SingletonHolder
   {
       protected static final ShopBBSManager INSTANCE = new ShopBBSManager();
   }
}