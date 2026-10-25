package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.Calendar;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class TeleportBBSManager extends BaseBBSManager
{
   protected TeleportBBSManager()
   {
   }
   
   @Override
   public void parseCmd(String command, Player player)
   {
	   StringTokenizer st = new StringTokenizer(command, " ");
	   String actualCommand = st.nextToken(); // Get actual command
		
		if (st.countTokens() < 1)
			return;
		
       if (actualCommand.equalsIgnoreCase("_bbsteleport"))
   	   {

			// Retrieve the list.
			final TeleportLocation list = TeleportLocationData.getInstance().getTeleportLocation(Integer.parseInt(st.nextToken()));
			if (list == null)
				return;
			
			// Siege is currently in progress in this location.
			if (CastleManager.getInstance().getActiveSiege(list.getX(), list.getY(), list.getZ()) != null)
			{
				player.sendPacket(SystemMessageId.CANNOT_PORT_VILLAGE_IN_SIEGE);
				return;
			}
			
			// The list is for noble, but player isn't noble.
			if (list.isNoble() && !player.isNoble())
			{
				player.sendMessage("you need to be noble");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Retrieve price list. Potentially cut it by 2 depending of current date.
			int price = list.getPrice();
			
			if (!list.isNoble())
			{
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
					price /= 2;
			}
			
			// Delete related items, and if successful teleport the player to the location.
			if (player.destroyItemByItemId("Teleport ", (list.isNoble()) ? 6651 : 57, price, player, true))
			{
				loadStaticHtm("teleport.htm", player);
				player.teleportTo(list, 20);
				player.sendPacket(new SystemMessage(SystemMessageId.WILL_BE_MOVED_INTERFACE));
				
				if (player.isAutoFarm())
					VoicedAutoFarm.stopAutoFarm(player, "teleport");
			}
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
   	   }
       
		else
			super.parseCmd(command, player); 		
   }
   
   @Override
   protected String getFolder()
   {
       return "top/";
   }
   
   public static TeleportBBSManager getInstance()
   {
       return SingletonHolder.INSTANCE;
   }
   
   private static class SingletonHolder
   {
       protected static final TeleportBBSManager INSTANCE = new TeleportBBSManager();
   }
}