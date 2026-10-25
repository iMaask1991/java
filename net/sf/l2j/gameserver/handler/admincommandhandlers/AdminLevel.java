package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class AdminLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_addlevel",
		"admin_setlevel"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (activeChar == null)
			return false;
		
		WorldObject targetChar = activeChar.getTarget();
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		String val = "";
		if (st.countTokens() >= 1)
			val = st.nextToken();
		
		if (actualCommand.equalsIgnoreCase("admin_addlevel"))
		{
			try
			{
				if (targetChar instanceof Playable)
					((Playable) targetChar).getStat().addLevel(Byte.parseByte(val));
			}
			catch (NumberFormatException e)
			{
				activeChar.sendMessage("Wrong number format.");
				return false;
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_setlevel"))
		{
			try
			{
				if (targetChar == null || !(targetChar instanceof Player))
				{
					activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT); // incorrect target!
					return false;
				}
				Player targetPlayer = (Player) targetChar;
				
				byte lvl = Byte.parseByte(val);
				int minLevel = 1;
				// Primer nivel inalcanzable (81 = 80 @ 100%)
				int maxLevel = Config.MAX_PLAYER_LEVEL;
				
				if (targetPlayer.isSubClassActive())
				{
					minLevel = Config.BASE_SUBCLASS_LEVEL;
					maxLevel = Config.MAX_SUBCLASS_LEVEL;
					if (lvl < minLevel)
						lvl = (byte) minLevel;
				}
				
				if (lvl < minLevel || lvl > maxLevel)
				{
					activeChar.sendMessage("You must specify level between " + minLevel + " and " + maxLevel + ".");
					return false;
				}
				
				// lvl == maxLevel      → nivel max INALCANZABLE (ej: 79 con tope 79)
				// lvl == maxLevel - 1  → nivel max JUGABLE     (ej: 78 con tope 79)
				// en AMBOS casos la exp va justo al tope del tier = barra 100%
				// OJO: sin "- 1", si no la barra queda en 99% y nunca llega a 100%.
				long pXp = targetPlayer.getExp();
				long tXp = (lvl >= maxLevel - 1) ? Experience.LEVEL[maxLevel] : Experience.LEVEL[lvl];
				
				if (pXp > tXp)
					targetPlayer.removeExpAndSp(pXp - tXp, 0);
				else if (pXp < tXp)
					targetPlayer.addExpAndSp(tXp - pXp, 0);
			}
			catch (NumberFormatException e)
			{
				activeChar.sendMessage("You must specify a valid level.");
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
