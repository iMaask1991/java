package net.sf.l2j.gameserver.events.bossevent;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminKTBEvent implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS =
    {
        "admin_ktb_add",
        "admin_ktb_remove",
        "admin_ktb_advance"
    };
   
    @Override
	public boolean useAdminCommand(String command, Player activeChar)
    {
        if (command.startsWith("admin_ktb_add"))
        {
            WorldObject target = activeChar.getTarget();
           
            if (!(target instanceof Player))
            {
                activeChar.sendMessage("You should select a player!");
                return true;
            }
           
            add(activeChar, (Player) target);
        }
        else if (command.startsWith("admin_ktb_remove"))
        {
        	WorldObject target = activeChar.getTarget();
           
            if (!(target instanceof Player))
            {
                activeChar.sendMessage("You should select a player!");
                return true;
            }
           
            remove(activeChar, (Player) target);
        }
        else if (command.startsWith( "admin_ktb_advance" ))
        {
            KTBManager.getInstance().skipDelay();
            activeChar.sendMessage("Kill The Boss: fase avanzada manualmente.");
        }
       
        return true;
    }
   
    @Override
	public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }
   
    private static void add(Player activeChar, Player playerInstance)
    {
        if (KTBEvent.isPlayerParticipant(playerInstance.getObjectId()))
        {
            activeChar.sendMessage("Player already participated in the event!");
            return;
        }
       
        if (!KTBEvent.addParticipant(playerInstance))
        {
            activeChar.sendMessage("Player instance could not be added, it seems to be null!");
            return;
        }
       
        if (KTBEvent.isStarted())
        {
        	new KTBEventTeleporter(playerInstance, true, false);
        }
    }
   
    private static void remove(Player activeChar, Player playerInstance)
    {
        if (!KTBEvent.removeParticipant(playerInstance))
        {
            activeChar.sendMessage("Player is not part of the event!");
            return;
        }
       
        new KTBEventTeleporter(playerInstance, KTBConfig.KTB_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
    }
}