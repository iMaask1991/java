package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.tournament.Arena1x1;

public class Tournament extends Folk
{
	public Tournament(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/mods/tournament/index.htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%1x1%", getQueueIndicator(Arena1x1.registered.size()));
		player.sendPacket(html);
	}
	
	private static String getQueueIndicator(int size)
	{
		final int level = Math.min(9, Math.max(0, size));
		return "<button value=\"\" action=\"\" width=32 height=32 back=\"L2UI_CH3.calculate1_" + level + "_over\" fore=\"L2UI_CH3.calculate1_" + level + "\">";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("1x1"))
		{
			if (!Config.ALLOW_1X1_REGISTER)
			{
				player.sendMessage("[WARN]: The 1x1 is temporarily unavailable.");
				return;
			}
			
			if (!Config.TOUR_GRADE_A_1X1)
			{
				for (int i = 1; i < 15; i++)
				{
					final ItemInstance item = player.getInventory().getPaperdollItem(i);
					if (item == null)
						continue;
					
					if (item.getItem().getCrystalType() == CrystalType.S)
					{
						player.sendMessage("Tournament: Grade S items are not allowed.");
						player.sendPacket(new ExShowScreenMessage("Tournament: Grade S items are not allowed.", 5000));
						return;
					}
					
					if (item.getEnchantLevel() > Config.ALT_TOUR_ENCHANT_LIMIT)
					{
						player.sendMessage("Max enchant allowed in tournament +" + Config.ALT_TOUR_ENCHANT_LIMIT + ".");
						player.sendPacket(new ExShowScreenMessage("Max enchant allowed in tournament +" + Config.ALT_TOUR_ENCHANT_LIMIT + ".", 5000));
						return;
					}
				}
			}
			
			if (!Config.Allow_Same_HWID_On_Tournament && Arena1x1.getInstance().isHwidAlreadyInTournament(player))
			{
				player.sendMessage("Tournament: Only one character per PC is allowed.");
				player.sendPacket(new ExShowScreenMessage("Tournament: Only one character per PC is allowed.", 5000));
				return;
			}
			
			if (player.isArena1x1() || player.isArenaProtection())
			{
				player.sendMessage("Tournament: You are already registered.");
				return;
			}
			
			if (player.getClassId() == ClassId.SHILLIEN_ELDER || player.getClassId() == ClassId.SHILLIEN_SAINT || player.getClassId() == ClassId.BISHOP || player.getClassId() == ClassId.CARDINAL || player.getClassId() == ClassId.ELVEN_ELDER || player.getClassId() == ClassId.EVAS_SAINT)
			{
				player.sendMessage("Tournament: Bishop is not allowed in 1v1.");
				return;
			}
			
			if (player.isCursedWeaponEquipped() || player.isInStoreMode() || player.getKarma() > 0)
			{
				player.sendMessage("Tournament: You do not meet the requirements.");
				return;
			}
			
			if (player.isInParty())
			{
				player.sendMessage("Tournament: Leave your party.");
				player.sendPacket(new ExShowScreenMessage("Tournament: Leave your party.", 6000));
				return;
			}
			
			if (OlympiadManager.getInstance().isRegistered(player))
			{
				player.sendMessage("Tournament: You are registered in the Olympiad.");
				return;
			}
			
			if (player.isAio())
			{
				player.sendMessage("Aio Buffer cannot participate in the event.");
				return;
			}
			
			if (Arena1x1.getInstance().register(player))
			{
				player.sendMessage("Tournament: Your participation has been approved.");
				player.sendPacket(new ExShowScreenMessage("Successfully registered - 1v1", 4000, ExShowScreenMessage.SMPOS.MIDDLE_RIGHT, false));
				player.setArenaProtection(true);
				player.setArena1x1(true);
				showChatWindow(player);
			}
			else
				player.sendMessage("Tournament: You do not meet the requirements.");
		}
		else if (command.startsWith("remove"))
		{
			if (player.isArena1x1())
				Arena1x1.getInstance().remove(player);
			else
				player.sendMessage("Tournament: You are not registered.");
		}
		else if (command.startsWith("observe_list"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/mods/tournament/observer.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else if (command.startsWith("back"))
			showChatWindow(player);
		else if (command.startsWith("tournament_observe"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			final int x = Integer.parseInt(st.nextToken());
			final int y = Integer.parseInt(st.nextToken());
			final int z = Integer.parseInt(st.nextToken());
			player.setArenaObserv(true);
			player.enterTvTObserverMode(x, y, z);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
