package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class BufferBBSManager extends BaseBBSManager
{
   protected BufferBBSManager()
   {
   }
   
   @Override
   public void parseCmd(String command, Player player)
   {
	   	StringTokenizer st = new StringTokenizer(command, " ");
	   	String actualCommand = st.nextToken();
	   	
	   	int buffid = 0, bufflevel = 1;
	   	if (st.countTokens() == 2)
	   	{
	   		buffid = Integer.valueOf(st.nextToken());
	   		bufflevel = Integer.valueOf(st.nextToken());
	   	}
	   	else if (st.countTokens() == 1)
	   		buffid = Integer.valueOf(st.nextToken());
		
       if (actualCommand.equalsIgnoreCase("_bbsbuff"))
   	   {
	   		if (player.getPvpFlag() > 0)
	   		{
	   			player.sendMessage("You can't use buffer when you are pvp flagged.");
	   			return;
	   		}
	
	   		if (player.isInCombat())
	   		{
	   			player.sendMessage("You can't use buffer when you are in combat.");
	   			return;
	   		}
	
	   		if (player.isDead())
	   			return;
	   		
			L2Skill skill = SkillTable.getInstance().getInfo(buffid, bufflevel);
			if (skill != null)
				skill.getEffects(player, player);
	   		player.broadcastPacket(new MagicSkillUse(player, player, buffid, bufflevel, 500, 0));

	   		loadStaticHtm("buffer.htm", player);
   	   }
       else if (actualCommand.equalsIgnoreCase("_bbsbuff_heal"))
   	   {
	   		if (player.getPvpFlag() > 0)
	   		{
	   			player.sendMessage("You can't use buffer when you are pvp flagged.");
	   			return;
	   		}
	
	   		if (player.isInCombat())
	   		{
	   			player.sendMessage("You can't use buffer when you are in combat.");
	   			return;
	   		}
	
	   		if (player.isDead())
	   			return;
	   		
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			player.broadcastPacket(new MagicSkillUse(player, player, 1258, 4, 500, 0));

	   		loadStaticHtm("buffer.htm", player);
   	   }
       else if (actualCommand.equalsIgnoreCase("_bbsbuff_cancel"))
   	   {
	   		if (player.getPvpFlag() > 0)
	   		{
	   			player.sendMessage("You can't use buffer when you are pvp flagged.");
	   			return;
	   		}
	
	   		if (player.isInCombat())
	   		{
	   			player.sendMessage("You can't use buffer when you are in combat.");
	   			return;
	   		}
	
	   		if (player.isDead())
	   			return;
	   		
	   		player.stopAllEffects();
	   		player.broadcastPacket(new MagicSkillUse(player, player, 1056, 12, 500, 0));
	
		   	loadStaticHtm("buffer.htm", player);
   	   }       
		else
			super.parseCmd(command, player); 		
   }
   
   @Override
   protected String getFolder()
   {
       return "top/";
   }
   
   public static BufferBBSManager getInstance()
   {
       return SingletonHolder.INSTANCE;
   }
   
   private static class SingletonHolder
   {
       protected static final BufferBBSManager INSTANCE = new BufferBBSManager();
   }
}