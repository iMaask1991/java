package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.data.xml.BoxData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.BalanceClassManager;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * //reload boxes  -> recarga data/xml/custom/boxes.xml en caliente.
 * //reload alchemy -> recarga data/xml/custom/attributeAlchemy.xml (Sockets).
 * //reload elements -> recarga data/xml/custom/elements.xml (Elementos de items).
 * //reload balanceclass -> recarga el balance por clase de config/HadesProject/balance/ClassBalance.properties.
 * Cualquier otro //reload ... se pasa tal cual a AdminAdmin, asi que el resto de los reloads siguen igual.
 * Tiene que registrarse DESPUES de AdminAdmin en AdminCommandHandler.
 */
public class AdminReloadBoxes implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_reload"
	};

	private final IAdminCommandHandler _original = new AdminAdmin();

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final String[] args = command.trim().split("\\s+");
		if (args.length > 1 && (args[1].equalsIgnoreCase("boxes") || args[1].equalsIgnoreCase("box")))
		{
			BoxData.getInstance().reload();
			activeChar.sendMessage("Custom boxes recargadas.");
			return true;
		}

		if (args.length > 1 && args[1].equalsIgnoreCase("alchemy"))
		{
			net.sf.l2j.gameserver.alchemy.AttributeAlchemyData.getInstance().reload();
			activeChar.sendMessage("Attribute Alchemy (Sockets) recargado.");
			return true;
		}

		if (args.length > 1 && (args[1].equalsIgnoreCase("elements") || args[1].equalsIgnoreCase("element")))
		{
			net.sf.l2j.gameserver.element.ItemElementData.getInstance().reload();
			activeChar.sendMessage("Elementos de items recargados.");
			return true;
		}

		if (args.length > 1 && args[1].equalsIgnoreCase("balanceclass"))
		{
			BalanceClassManager.getInstance().reload();
			activeChar.sendMessage("BalanceClass recargado.");
			return true;
		}
		
		return _original.useAdminCommand(command, activeChar);
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
