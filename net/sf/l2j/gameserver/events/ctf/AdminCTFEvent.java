package net.sf.l2j.gameserver.events.ctf;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminCTFEvent implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS =
    {
        "admin_ctf_add",
        "admin_ctf_remove",
        "admin_ctf_advance"
    };
   
    @Override
	public boolean useAdminCommand(String command, Player activeChar)
    {
        if (command.startsWith("admin_ctf_add"))
        {
            WorldObject target = activeChar.getTarget();
           
            if (!(target instanceof Player))
            {
                activeChar.sendMessage("You should select a player!");
                return true;
            }
           
            add(activeChar, (Player) target);
        }
        else if (command.startsWith("admin_ctf_remove"))
        {
            WorldObject target = activeChar.getTarget();
           
            if (!(target instanceof Player))
            {
                activeChar.sendMessage("You should select a player!");
                return true;
            }
           
            remove(activeChar, (Player) target);
        }
        else if (command.startsWith( "admin_ctf_advance" ))
        {
            CTFManager.getInstance().skipDelay();
            activeChar.sendMessage("CTF: fase avanzada manualmente.");
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
        if (CTFEvent.isPlayerParticipant(playerInstance.getObjectId()))
        {
            activeChar.sendMessage("Player already participated in the event!");
            return;
        }
       
        if (!CTFEvent.addParticipant(playerInstance))
        {
            activeChar.sendMessage("Player instance could not be added, it seems to be null!");
            return;
        }
       
        if (CTFEvent.isStarted())
        {
            new CTFEventTeleporter(playerInstance, CTFEvent.getParticipantTeamCoordinates(playerInstance.getObjectId()), true, false);
        }
    }
   
    private static void remove(Player activeChar, Player playerInstance)
    {
        if (!CTFEvent.removeParticipant(playerInstance.getObjectId()))
        {
            activeChar.sendMessage("Player is not part of the event!");
            return;
        }
       
        new CTFEventTeleporter(playerInstance, CTFConfig.CTF_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
    }
}