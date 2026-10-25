package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.farmhwid.FarmHwidService;
import net.sf.l2j.gameserver.farmhwid.FarmHwidService.Owner;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

public class AdminFarmHwid implements IAdminCommandHandler
{
	private static final String[] COMMANDS =
	{
		"admin_farmhwid",
		"admin_farmunbind"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		
		Player target = null;
		String extra = null;
		if (st.hasMoreTokens())
		{
			final String token = st.nextToken();
			if ("force".equalsIgnoreCase(token))
				extra = "force";
			else
			{
				target = World.getInstance().getPlayer(token);
				if (target == null)
					extra = token;
			}
			if (st.hasMoreTokens() && extra == null)
				extra = st.nextToken();
		}
		
		if (target == null && extra == null && activeChar.getTarget() instanceof Player)
			target = (Player) activeChar.getTarget();
		if (target == null)
			target = activeChar;
		
		final String hwid = FarmHwidService.normalizeHwid(target);
		final Owner owner = hwid == null ? null : FarmHwidService.getOwner(hwid);
		
		if (cmd.equals("admin_farmhwid"))
		{
			activeChar.sendMessage("Char: " + target.getName() + " cuenta " + target.getAccountName());
			activeChar.sendMessage("HWID: " + (hwid == null ? "n/a" : hwid));
			if (owner == null)
				activeChar.sendMessage("Sin dueño de farm en este HWID.");
			else
				activeChar.sendMessage("Dueño HWID: cuenta " + owner.accountName);
			
			final long remain = FarmHwidService.remainingCooldown(hwid);
			if (remain > 0)
				activeChar.sendMessage("Cooldown: " + FarmHwidService.formatRemain(remain));
			return true;
		}
		
		final boolean force = extra != null && extra.equalsIgnoreCase("force");
		if (!FarmHwidService.unbindPlayer(target, !force))
		{
			activeChar.sendMessage("No se pudo liberar farm de " + target.getName() + ". Proba target o //farmunbind nick force");
			return false;
		}
		
		if (force)
			activeChar.sendMessage("Farm PC liberado sin cooldown: " + target.getName());
		else
			activeChar.sendMessage("Farm PC liberado: " + target.getName() + " (usa force para sin cooldown)");
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
}
