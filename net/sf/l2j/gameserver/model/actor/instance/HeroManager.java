package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class HeroManager extends Folk
{
    public HeroManager(int objectId, NpcTemplate template)
    {
        super(objectId, template);
    }
    
    @Override
    public void showChatWindow(Player playerInstance, int val)
    {
        if (playerInstance == null)
            return;
        
        String htmContent = HtmCache.getInstance().getHtm("data/html/mods/HadesProject/hero.htm");
        if (val > 0)
        {
            htmContent = HtmCache.getInstance().getHtm("data/html/mods/HadesProject/hero-" + val + ".htm");
        }
        if (htmContent != null)
        {
            NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
            
            npcHtmlMessage.setHtml(htmContent);
            npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
            playerInstance.sendPacket(npcHtmlMessage);
        }
        playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
    }
    
    @Override
    public void onBypassFeedback(Player player, String command)
    {
    	if (!(player.getInventory().getInventoryItemCount(57, 0) >= 10))
		{
    		final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/mods/HadesProject/noitems.htm");
			player.sendPacket(html);
		   return;
		} 
        if (command.startsWith("Hero"))
        {
    		if (!player.isHero())
    		{
    			player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 1000, 0));
    			player.sendPacket(new ExShowScreenMessage("Felicidades ahora eres heroe!", 8000)); 
    			player.setHero(true, true);
    			player.broadcastUserInfo();
    			player.destroyItemByItemId("Consume", 57, 10, player, true);
    			World.announceToOnlinePlayers("El Personaje " + player.getName() + " Ahora es Hero!");
    			final NpcHtmlMessage html = new NpcHtmlMessage(0);
    			html.setFile("data/html/mods/HadesProject/ok.htm");
    			player.sendPacket(html);
    		}
    		else
    		{
    			final NpcHtmlMessage html = new NpcHtmlMessage(0);
    			html.setFile("data/html/mods/HadesProject/already.htm");
    			player.sendPacket(html);
    		}
            return;
        }
        super.onBypassFeedback(player, command);
    }

}