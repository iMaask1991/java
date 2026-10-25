package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.taskmanager.AfkTaskManager;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.tournament.ArenaTask;

public final class Logout extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;

		if (player.getActiveEnchantItem() != null || player.isLocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if ((player.isInArenaEvent() || player.isArenaProtection()) && ArenaTask.is_started())
		{
			player.sendMessage("You cannot logout while in Tournament Event!");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInsideZone(ZoneId.NO_RESTART))
		{
			player.sendPacket(SystemMessageId.NO_LOGOUT_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().isInAttackStance(player))
		{
			player.sendPacket(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isFestivalParticipant() && FestivalOfDarknessManager.getInstance().isFestivalInitialized())
		{
			player.sendPacket(SystemMessageId.NO_LOGOUT_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.removeFromBossZone();
		AfkTaskManager.getInstance().remove(player);
		TvTEvent.onLogout(player);
		player.logout(true);
	}
}