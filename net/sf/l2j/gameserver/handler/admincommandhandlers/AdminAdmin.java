package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.battlepass.BattlePassManager;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.CrestCache;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.AnnouncementData;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import net.sf.l2j.gameserver.data.xml.WalkerRouteData;
import net.sf.l2j.gameserver.events.bossevent.KTBManager;
import net.sf.l2j.gameserver.events.ctf.CTFManager;
import net.sf.l2j.gameserver.events.deathmatch.DMManager;
import net.sf.l2j.gameserver.events.lastman.LMManager;
import net.sf.l2j.gameserver.events.teamvsteam.TvTManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.rebirth.RebirthManager;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>admin/admin1/admin2/admin3/admin4 : the different admin menus.</li>
 * <li>gmlist : includes/excludes active character from /gmlist results.</li>
 * <li>kill : handles the kill command.</li>
 * <li>silence : toggles private messages acceptance mode.</li>
 * <li>tradeoff : toggles trade acceptance mode.</li>
 * <li>reload : reloads specified component.</li>
 * <li>script_load : loads following script. MUSTN'T be used instead of //reload quest !</li>
 * </ul>
 */
public class AdminAdmin implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminAdmin.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_admin",
		"admin_admin1",
		"admin_admin2",
		"admin_admin3",
		"admin_admin4",
		"admin_gmlist",
		"admin_kill",
		"admin_silence",
		"admin_tradeoff",
		"admin_reload"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_admin"))
			showMainPage(activeChar, command);
		else if (command.startsWith("admin_gmlist"))
			activeChar.sendMessage((AdminData.getInstance().showOrHideGm(activeChar)) ? "Removed from GMList." : "Registered into GMList.");
		else if (command.startsWith("admin_kill"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (!st.hasMoreTokens())
			{
				final WorldObject obj = activeChar.getTarget();
				if (!(obj instanceof Creature))
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else
					kill(activeChar, (Creature) obj);
				
				return true;
			}
			
			String firstParam = st.nextToken();
			Player player = World.getInstance().getPlayer(firstParam);
			if (player != null)
			{
				if (st.hasMoreTokens())
				{
					String secondParam = st.nextToken();
					if (StringUtil.isDigit(secondParam))
					{
						int radius = Integer.parseInt(secondParam);
						for (Creature knownChar : player.getKnownTypeInRadius(Creature.class, radius))
						{
							if (knownChar.equals(activeChar))
								continue;
							
							kill(activeChar, knownChar);
						}
						activeChar.sendMessage("Killed all characters within a " + radius + " unit radius around " + player.getName() + ".");
					}
					else
						activeChar.sendMessage("Invalid radius.");
				}
				else
					kill(activeChar, player);
			}
			else if (StringUtil.isDigit(firstParam))
			{
				int radius = Integer.parseInt(firstParam);
				for (Creature knownChar : activeChar.getKnownTypeInRadius(Creature.class, radius))
					kill(activeChar, knownChar);
				
				activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
			}
		}
		else if (command.startsWith("admin_silence"))
		{
			if (activeChar.isInRefusalMode()) // already in message refusal mode
			{
				activeChar.setInRefusalMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				activeChar.setInRefusalMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
		}
		else if (command.startsWith("admin_tradeoff"))
		{
			try
			{
				String mode = command.substring(15);
				if (mode.equalsIgnoreCase("on"))
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
				else if (mode.equalsIgnoreCase("off"))
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
			}
			catch (Exception e)
			{
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
				else
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
			}
		}
		else if (command.startsWith("admin_reload"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				sendReloadUsage(activeChar);
				return true;
			}
			
			final List<String> types = new ArrayList<>();
			while (st.hasMoreTokens())
				types.add(st.nextToken());
			
			activeChar.sendMessage("Reload started (" + String.join(", ", types) + "). Server stays online.");
			ThreadPool.execute(() ->
			{
				for (String type : types)
				{
					try
					{
						reloadType(type, activeChar);
					}
					catch (Throwable t)
					{
						LOGGER.log(Level.SEVERE, "Reload failed: " + type, t);
						activeChar.sendMessage("Reload FAILED (" + type + "): " + t.getClass().getSimpleName() + " - check console. Old data kept when possible.");
					}
				}
				activeChar.sendMessage("Reload finished.");
			});
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void sendReloadUsage(Player activeChar)
	{
		activeChar.sendMessage("Usage : //reload <admin|announcement|config|crest|cw>");
		activeChar.sendMessage("Usage : //reload <door|event|htm|item|multisell|npc>");
		activeChar.sendMessage("Usage : //reload <npcwalker|skill|teleport|xml|zone|bp|battlepass>");
		activeChar.sendMessage("Safe online: htm, multisell, announcement, teleport, event.");
		activeChar.sendMessage("Avoid online: skill, item, zone, door, npc, config.");
	}
	
	private static void reloadType(String type, Player activeChar)
	{
		if (type.startsWith("admin"))
		{
			AdminData.getInstance().reload();
			activeChar.sendMessage("Admin data has been reloaded.");
		}
		else if (type.startsWith("announcement"))
		{
			AnnouncementData.getInstance().reload();
			activeChar.sendMessage("The content of announcements.xml has been reloaded.");
		}
		else if (type.startsWith("config"))
		{
			Config.reloadGameServer();
			reloadEventSchedules();
			// RebirthManager keeps its OWN copy of Rebirth.properties (Config's
			// ENABLE_REBIRTH_SYSTEM/REBIRTH_* fields are the old, now-unused system - see the
			// "NO SE USA MAS" block at the bottom of Rebirth.properties). Config.reloadGameServer()
			// never touched these, so //reload config silently never picked up Rebirth.properties
			// changes (Range1..N, MaxRebirths, Delevel, MinLevel, etc.). load() clears
			// its internal state before repopulating, so it's safe to call repeatedly.
			RebirthManager.load();
			net.sf.l2j.gameserver.customdrop.CustomDropManager.load();
			net.sf.l2j.gameserver.antiinterface.AntiInterfaceManager.load();
			activeChar.sendMessage("Configs files have been reloaded.");
		}
		else if (type.startsWith("event"))
		{
			reloadEventSchedules();
			activeChar.sendMessage("Event schedules have been reloaded (TvT/KTB/CTF/DM/LM).");
		}
		else if (type.startsWith("crest"))
		{
			CrestCache.getInstance().reload();
			activeChar.sendMessage("Crests have been reloaded.");
		}
		else if (type.startsWith("cw"))
		{
			CursedWeaponManager.getInstance().reload();
			activeChar.sendMessage("Cursed weapons have been reloaded.");
		}
		else if (type.startsWith("door"))
		{
			DoorData.getInstance().reload();
			activeChar.sendMessage("Doors instance has been reloaded.");
		}
		else if (type.startsWith("htm"))
		{
			HtmCache.getInstance().reload();
			activeChar.sendMessage("The HTM cache has been reloaded.");
		}
		else if (type.startsWith("item"))
		{
			ItemTable.getInstance().reload();
			activeChar.sendMessage("Items' templates have been reloaded.");
		}
		else if (type.equals("multisell"))
		{
			MultisellData.getInstance().reload();
			activeChar.sendMessage("The multisell instance has been reloaded.");
		}
		else if (type.equals("npc"))
		{
			NpcData.getInstance().reload();
			net.sf.l2j.gameserver.customdrop.CustomDropManager.load();
			activeChar.sendMessage("NPCs templates have been reloaded.");
		}
		else if (type.startsWith("npcwalker"))
		{
			WalkerRouteData.getInstance().reload();
			activeChar.sendMessage("Walker routes have been reloaded.");
		}
		else if (type.startsWith("skill"))
		{
			SkillTable.getInstance().reload();
			activeChar.sendMessage("Skills' XMLs have been reloaded.");
		}
		else if (type.startsWith("teleport"))
		{
			TeleportLocationData.getInstance().reload();
			activeChar.sendMessage("Teleport locations have been reloaded.");
		}
		else if (type.startsWith("zone"))
		{
			ZoneManager.getInstance().reload();
			activeChar.sendMessage("Zones have been reloaded.");
		}
		else if (type.equals("bp") || type.startsWith("battlepass") || type.startsWith("xml"))
		{
			BattlePassManager.getInstance().load();
			BattlePassManager.getVipInstance().load();
			HtmCache.getInstance().reload();
			activeChar.sendMessage("Battle Pass XML and HTM cache reloaded.");
		}
		else
			sendReloadUsage(activeChar);
	}
	
	private static void reloadEventSchedules()
	{
		TvTManager.getInstance().reloadSchedule();
		KTBManager.getInstance().reloadSchedule();
		CTFManager.getInstance().reloadSchedule();
		DMManager.getInstance().reloadSchedule();
		LMManager.getInstance().reloadSchedule();
	}
	
	private static void kill(Player activeChar, Creature target)
	{
		if (target instanceof Player)
		{
			if (!((Player) target).isGM())
				target.stopAllEffects(); // e.g. invincibility effect
			target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
		}
		else if (target.isChampion())
			target.reduceCurrentHp(target.getMaxHp() * Config.CHAMPION_HP + 1, activeChar, null);
		else
			target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
	}
	
	private static void showMainPage(Player activeChar, String command)
	{
		int mode = 0;
		String filename = null;
		try
		{
			mode = Integer.parseInt(command.substring(11));
		}
		catch (Exception e)
		{
		}
		
		switch (mode)
		{
			case 1:
				filename = "main";
				break;
			case 2:
				filename = "game";
				break;
			case 3:
				filename = "effects";
				break;
			case 4:
				filename = "server";
				break;
			default:
				filename = "main";
				break;
		}
		AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
	}
}