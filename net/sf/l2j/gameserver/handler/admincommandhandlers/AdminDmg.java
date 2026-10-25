package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * //dmg          -> desglose del ultimo golpe que le pegaste a un NPC.
 * //dmg Nombre   -> desglose del ultimo golpe de ESE personaje (tiene que estar online).
 * Necesita PveDamageDebug = true en Balance.properties (//reload config).
 * Sirve para pjs comunes: no hace falta que el que pega sea GM.
 */
public class AdminDmg implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_dmg"
	};

	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (!Config.PVE_DAMAGE_DEBUG)
		{
			activeChar.sendMessage("PveDamageDebug esta en false: ponelo en true en Balance.properties y //reload config.");
			return true;
		}

		final String[] args = command.trim().split("\\s+");
		Player target = activeChar;
		if (args.length > 1)
		{
			target = World.getInstance().getPlayer(args[1]);
			if (target == null)
			{
				activeChar.sendMessage("No encontre al personaje '" + args[1] + "' online.");
				return true;
			}
		}

		activeChar.sendMessage("Ultimo golpe de " + target.getName() + ": " + Formulas.getLastPveDebug(target.getObjectId()));
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
