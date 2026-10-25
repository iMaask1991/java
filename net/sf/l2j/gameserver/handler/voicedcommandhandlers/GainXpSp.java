package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

public class GainXpSp implements IVoicedCommandHandler
{
   private static final String[] VOICED_COMMANDS =
   {
       "expon",
       "expoff"
  };
   
   @Override
   public boolean useVoicedCommand(final String command, final Player activeChar, final String target)
   {
       if (command.startsWith("expon"))
       {
           activeChar.setGainXpSpEnable(true);
           activeChar.sendMessage("[Gain XP SP]: Enable!");
       }
       else if (command.startsWith("expoff"))
       {
           activeChar.setGainXpSpEnable(false);
           activeChar.sendMessage("[Gain XP SP]: Disable!");
       }  
       return true;
   }
   
   @Override
  public String[] getVoicedCommandList()
   {
       return VOICED_COMMANDS;
   }
}
