package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;

public class BankingCommand implements IVoicedCommandHandler
{
	private static String[] _voicedCommands =
	{
		"withdraw",
		"deposit"
	};

	@Override
	public boolean useVoicedCommand(final String command, final Player activeChar, final String target)
	{
		if (command.equalsIgnoreCase("deposit") && !(Config.BANKING_SYSTEM_GOLDBARS <= 0))
		{
			if (activeChar.getInventory().getInventoryItemCount(57, 0) >= Config.BANKING_SYSTEM_ADENA)
			{
				activeChar.getInventory().reduceAdena("Goldbar", Config.BANKING_SYSTEM_ADENA, activeChar, null);
				activeChar.getInventory().addItem("Goldbar", Config.BANKING_SYSTEM_GOLDBAR_ID, Config.BANKING_SYSTEM_GOLDBARS, activeChar, null);
				activeChar.getInventory().updateDatabase();
				activeChar.sendPacket(new ItemList(activeChar, true));
				activeChar.sendMessage("Thank you, now you have Adena Coin(s), and " + Config.BANKING_SYSTEM_ADENA + " less adena.");
			}
			else
				activeChar.sendMessage("You do not have enough Adena to convert to Adena Coin(s), you need " + Config.BANKING_SYSTEM_ADENA + " Adena.");
		}
		else if (command.equalsIgnoreCase("withdraw") && !(Config.BANKING_SYSTEM_GOLDBARS <= 0))
		{
			// If player hasn't enough space for adena
			final long a = activeChar.getInventory().getInventoryItemCount(57, 0);
			final long b = Config.BANKING_SYSTEM_ADENA;
			if (a + b > Integer.MAX_VALUE)
			{
				activeChar.sendMessage("You do not have enough space for all the adena in inventory!");
				return false;
			}

			if (activeChar.getInventory().getInventoryItemCount(Config.BANKING_SYSTEM_GOLDBAR_ID, 0) >= Config.BANKING_SYSTEM_GOLDBARS)
			{
				activeChar.getInventory().destroyItemByItemId("Adena", Config.BANKING_SYSTEM_GOLDBAR_ID, Config.BANKING_SYSTEM_GOLDBARS, activeChar, null);
				activeChar.getInventory().addAdena("Adena", Config.BANKING_SYSTEM_ADENA, activeChar, null);
				activeChar.getInventory().updateDatabase();
				activeChar.sendPacket(new ItemList(activeChar, true));
				activeChar.sendMessage("Thank you, now you have " + Config.BANKING_SYSTEM_ADENA + " Adena, and " + Config.BANKING_SYSTEM_GOLDBARS + " less Adena Coin(s).");
			}
			else
				activeChar.sendMessage("You do not have any Adena Coin(s) to turn into " + Config.BANKING_SYSTEM_ADENA + " Adena.");
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}