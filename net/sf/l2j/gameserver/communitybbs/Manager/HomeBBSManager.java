package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.battlepass.BattlePassManager;
import net.sf.l2j.gameserver.communitybbs.CommunityBoardConfig;
import net.sf.l2j.gameserver.communitybbs.StarterVipHwid;
import net.sf.l2j.gameserver.communitybbs.HomeBoardBossInfo;
import net.sf.l2j.gameserver.communitybbs.HomeBoardEventInfo;
import net.sf.l2j.gameserver.communitybbs.HomeBoardRanking;
import net.sf.l2j.gameserver.communitybbs.HomeBoardStatusInfo;
import net.sf.l2j.gameserver.data.BufferTable;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.events.bossevent.KTBEvent;
import net.sf.l2j.gameserver.events.ctf.CTFEvent;
import net.sf.l2j.gameserver.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.events.lastman.LMEvent;
import net.sf.l2j.gameserver.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.PlayerStats;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.rebirth.RebirthManager;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.handler.itemhandlers.AioCoin;
import net.sf.l2j.gameserver.handler.itemhandlers.HeroCoin;
import net.sf.l2j.gameserver.handler.itemhandlers.VipCoin;
import net.sf.l2j.gameserver.model.actor.instance.MultiManager;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.TeleportLocation;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Home board custom services (status pages + L2Interlude-style dynamic home).
 */
public class HomeBBSManager extends BaseBBSManager
{
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String CUSTOM_CB_PATH = "data/html/CommunityBoard/Custom/";
	
	protected HomeBBSManager()
	{
	}
	
	@Override
	public void parseCmd(String command, Player player)
	{
		if (CommunityBoardConfig.COMMUNITYBOARD_COMBAT_DISABLED && isCombatRestricted(command, player))
		{
			player.sendMessage("You can't use the Community Board right now.");
			refreshBoardAfterDeniedAction(command, player);
			return;
		}
		
		if (CommunityBoardConfig.COMMUNITYBOARD_KARMA_DISABLED && player.getKarma() > 0)
		{
			player.sendMessage("Players with Karma cannot use the Community Board.");
			return;
		}
		
		if (CommunityBoardConfig.COMMUNITYBOARD_PEACE_ONLY && !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("Community Board cannot be used out of peace zone.");
			return;
		}
		
		String returnHtml = null;
		String navigation = null;
		Player statsTarget = player;
		String statsMessage = "Tus stats";
		
		if (CommunityBoardConfig.CUSTOM_CB_ENABLED)
			navigation = HtmCache.getInstance().getHtm(NAVIGATION_PATH);
		
		if (command.startsWith("_bbspassvip"))
		{
			BattlePassManager.getVipInstance().parseCmd(command, player);
			return;
		}
		if (command.startsWith("_bbspass"))
		{
			BattlePassManager.getInstance().parseCmd(command, player);
			return;
		}
		
		if (command.startsWith("_bbsboss"))
		{
			returnHtml = HomeBoardBossInfo.resolveHtml(command);
		}
		else if (command.startsWith("_bbsrank"))
		{
			returnHtml = HomeBoardRanking.resolveHtml(command);
		}
		else if (command.startsWith("_bbsrebirth"))
		{
			returnHtml = handleRebirthCommand(command, player);
			if (returnHtml == null)
				return;
		}
		else if (command.startsWith("_bbsdonate"))
		{
			returnHtml = handleDonateCommand(command, player);
			if (returnHtml == null)
				return;
		}
		else if (command.equals("_bbshome") || command.equals("_bbstop"))
		{
			returnHtml = loadCustomHtml("home.html");
		}
		else if (command.equals("_bbsstats_ask"))
		{
			// CB edit/$var no sustituye bien en este cliente; usamos ventana NPC (sí reemplaza $name).
			showStatsSearchDialog(player);
			return;
		}
		else if (command.startsWith("_bbsstats_find"))
		{
			String name = command.substring("_bbsstats_find".length()).trim();
			if (name.startsWith(";") || name.startsWith(" "))
				name = name.substring(1).trim();
			if (name.startsWith("$"))
				name = "";
			
			final Object[] resolved = resolveStatsTarget(player, name);
			statsTarget = (Player) resolved[0];
			statsMessage = (String) resolved[1];
			returnHtml = loadCustomHtml("stats/main.html");
		}
		else if (command.equals("_bbsstats") || command.startsWith("_bbsstats;"))
		{
			final String raw = command.length() > 9 ? command.substring(9) : "";
			String name = raw.startsWith(";") ? raw.substring(1).trim() : raw.trim();
			if (name.startsWith("$"))
				name = "";
			
			final Object[] resolved = resolveStatsTarget(player, name);
			statsTarget = (Player) resolved[0];
			statsMessage = (String) resolved[1];
			returnHtml = loadCustomHtml("stats/main.html");
		}
		else if (command.startsWith("_bbstop;"))
		{
			final String page = command.substring("_bbstop;".length());
			if (page.startsWith("rebirth/"))
				returnHtml = handleRebirthPage(page, player);
			else
				returnHtml = loadCustomHtml(page);
		}
		else if (command.startsWith("_bbsmultisell") || command.startsWith("_bbsexcmultisell"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			final boolean exc = command.startsWith("_bbsexcmultisell");
			final String fullBypass = command.substring(exc ? "_bbsexcmultisell;".length() : "_bbsmultisell;".length());
			final String[] bypassOptions = fullBypass.split(",");
			if (bypassOptions.length < 2)
			{
				player.sendMessage("Invalid shop request.");
				return;
			}
			
			final String listName = bypassOptions[0].trim();
			if (MultisellData.getInstance().getList(listName) == null)
			{
				player.sendMessage("Shop list not found.");
				return;
			}
			
			returnHtml = loadCustomHtml(bypassOptions[1]);
			player.setIsUsingCMultisell(true);
			MultisellData.getInstance().separateAndSend(listName, player, null, exc);
		}
		else if (command.startsWith("_bbssell"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			returnHtml = loadCustomHtml(command.substring("_bbssell;".length()));
			final List<ItemInstance> items = player.getInventory().getSellableItems();
			player.sendPacket(new SellList(player.getAdena(), items));
		}
		else if (command.startsWith("_bbsteleport"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_TELEPORTS)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			if (player.isInOlympiadMode() || player.isInJail() || player.isAlikeDead() || player.isOutOfControl() || isOnEvent(player))
			{
				player.sendMessage("You can't teleport right now.");
				return;
			}
			
			final String teleBypass = command.length() > 13 ? command.substring(13) : "";
			if (teleBypass.toLowerCase().startsWith("goto"))
			{
				handleRetailGoto(player, teleBypass);
				return;
			}
			
			final Location loc;
			if (teleBypass.toLowerCase().startsWith("farmzone"))
			{
				String zone = "";
				final int sep = teleBypass.indexOf(';');
				if (sep >= 0)
					zone = teleBypass.substring(sep + 1).trim();
				loc = Gatekeeper.getRandomFarmLocation(player, zone);
			}
			else
				loc = CommunityBoardConfig.getTeleportLocation(teleBypass);
			
			if (loc == null)
			{
				if (!teleBypass.toLowerCase().startsWith("farmzone"))
					player.sendMessage("Invalid teleport.");
				return;
			}
			
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < CommunityBoardConfig.COMMUNITYBOARD_TELEPORT_PRICE)
			{
				player.sendMessage("Not enough currency!");
				return;
			}
			
			player.sendPacket(ShowBoard.STATIC_CLOSE);
			player.destroyItemByItemId("CommunityBoard", CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_TELEPORT_PRICE, player, true);
			player.setIsIn7sDungeon(false);
			player.teleToLocation(loc);
			if (player.isAutoFarm())
				VoicedAutoFarm.stopAutoFarm(player, "teleport");
		}
		else if (command.startsWith("_bbsresist"))
		{
			returnHtml = handleResistCommand(command, player);
			if (returnHtml == null)
				return;
		}
		else if (command.startsWith("_bbsbuff"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_BUFFS)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			final String fullBypass = command.substring("_bbsbuff;".length());
			final String[] bypassOptions = fullBypass.split(";");
			if (bypassOptions.length < 2)
			{
				player.sendMessage("Invalid buff request.");
				return;
			}
			
			final int buffCount = bypassOptions.length - 1;
			final String page = bypassOptions[buffCount];
			
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < (CommunityBoardConfig.COMMUNITYBOARD_BUFF_PRICE * buffCount))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId("CommunityBoard", CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_BUFF_PRICE * buffCount, player, true);
				
				final Summon pet = player.getSummon();
				final List<Creature> targets = new ArrayList<>(4);
				targets.add(player);
				if (pet != null)
					targets.add(pet);
				
				for (int i = 0; i < buffCount; i++)
				{
					final String[] skillInfo = bypassOptions[i].split(",");
					if (skillInfo.length < 2)
						continue;
					
					final int skillId;
					final int skillLvl;
					try
					{
						skillId = Integer.parseInt(skillInfo[0]);
						skillLvl = Integer.parseInt(skillInfo[1]);
					}
					catch (NumberFormatException e)
					{
						continue;
					}
					
					final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
					if (skill == null || !CommunityBoardConfig.COMMUNITY_AVAILABLE_BUFFS.contains(skill.getId()) || CommunityBoardConfig.COMMUNITY_RESIST_BUFFS.contains(skill.getId()))
						continue;
					
					for (Creature target : targets)
					{
						skill.getEffects(player, target);
						if (CommunityBoardConfig.COMMUNITYBOARD_CAST_ANIMATIONS)
							player.sendPacket(new MagicSkillUse(player, target, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
					}
				}
			}
			
			returnHtml = loadCustomHtml(page);
		}
		else if (command.startsWith("_bbsheal"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_HEAL)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			final String page = command.substring("_bbsheal;".length());
			if (player.getInventory().getInventoryItemCount(CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, -1) < CommunityBoardConfig.COMMUNITYBOARD_HEAL_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId("CommunityBoard", CommunityBoardConfig.COMMUNITYBOARD_CURRENCY, CommunityBoardConfig.COMMUNITYBOARD_HEAL_PRICE, player, true);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				
				final Summon summon = player.getSummon();
				if (summon != null)
				{
					summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp());
					summon.setCurrentCp(summon.getMaxCp());
				}
				
				player.broadcastUserInfo();
				player.sendMessage("You used heal!");
			}
			
			returnHtml = loadCustomHtml(page);
		}
		else if (command.startsWith("_bbscancel"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_BUFFS)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			final String page = command.length() > 11 && command.charAt(10) == ';' ? command.substring(11) : "buffer/main.html";
			player.stopAllEffects();
			final Summon summon = player.getSummon();
			if (summon != null)
				summon.stopAllEffects();
			player.sendMessage("Your buffs have been canceled.");
			
			returnHtml = loadCustomHtml(page);
		}
		else if (command.startsWith("_bbspreset"))
		{
			if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_BUFFS)
			{
				player.sendMessage("This Community Board service is disabled.");
				return;
			}
			
			final String[] parts = command.split(";");
			if (parts.length < 2)
			{
				player.sendMessage("Invalid buff request.");
				return;
			}
			
			final String type = parts[1].toLowerCase();
			final String page = parts.length > 2 ? parts[2] : "buffer/main.html";
			
			if ("fighter".equals(type))
				applyCommunityPreset(player, player, CommunityBoardConfig.COMMUNITY_FIGHTER_BUFFS);
			else if ("mage".equals(type))
				applyCommunityPreset(player, player, CommunityBoardConfig.COMMUNITY_MAGE_BUFFS);
			else if ("summoner".equals(type) || "summon".equals(type))
			{
				final Summon summon = player.getSummon();
				if (summon == null || summon.isDead())
					player.sendMessage("You don't have a servitor summoned.");
				else
					applyCommunityPreset(player, summon, CommunityBoardConfig.COMMUNITY_SUMMON_BUFFS);
			}
			
			returnHtml = loadCustomHtml(page);
		}
		else
		{
			super.parseCmd(command, player);
			return;
		}
		
		if (returnHtml != null)
			sendBoardHtml(returnHtml, navigation, player, command, statsTarget, statsMessage);
	}
	
	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		// Fallback Write: toma el primer arg de edit con texto real.
		String name = firstFilled(ar3, ar4, ar5);
		if ("search".equalsIgnoreCase(ar1) || "Search".equals(ar1))
		{
			// ok
		}
		final Object[] resolved = resolveStatsTarget(player, name);
		final String navigation = CommunityBoardConfig.CUSTOM_CB_ENABLED ? HtmCache.getInstance().getHtm(NAVIGATION_PATH) : null;
		final String returnHtml = loadCustomHtml("stats/main.html");
		sendBoardHtml(returnHtml, navigation, player, "_bbsstats", (Player) resolved[0], (String) resolved[1]);
	}
	
	private static void showStatsSearchDialog(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml("<html><title>Stats Search</title><body><center><br><font color=LEVEL>Player Stats</font><br1><img src=\"L2UI.SquareGray\" width=280 height=1><br>Escribí el nick online:<br1><edit var=\"name\" width=150 height=15 length=\"16\"><br><br><button value=\"Search\" action=\"bypass _bbsstats_find $name\" width=80 height=24 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f><br1><button value=\"Back\" action=\"bypass _bbsstats\" width=80 height=24 back=L2UI_NOVA.btn_f_down fore=L2UI_NOVA.btn_f></center></body></html>");
		player.sendPacket(html);
	}
	
	private static String firstFilled(String... values)
	{
		if (values == null)
			return "";
		
		for (String value : values)
		{
			if (value == null)
				continue;
			
			final String trimmed = value.trim();
			if (!trimmed.isEmpty() && !"_".equals(trimmed) && !trimmed.startsWith("$"))
				return trimmed;
		}
		return "";
	}
	
	private static Object[] resolveStatsTarget(Player viewer, String name)
	{
		if (name == null || name.isEmpty() || name.startsWith("$") || "_".equals(name))
			return new Object[]
			{
				viewer,
				"Tus stats"
			};
		
		final Player found = World.getInstance().getPlayer(name);
		if (found == null)
		{
			viewer.sendMessage("Jugador offline o no encontrado: " + name);
			return new Object[]
			{
				viewer,
				"Jugador offline o no encontrado: " + name
			};
		}
		
		return new Object[]
		{
			found,
			"Stats de " + found.getName()
		};
	}
	
	/**
	 * Rebirth: _bbsrebirth (just shows the shrine) and _bbsrebirth;confirm (does the rebirth right away,
	 * then re-shows the shrine). No separate ask/result page - success or failure feedback comes as a
	 * chat message from RebirthManager.doRebirth() itself (see Rebirth.properties: Announce/MessageSelf).
	 */
	private static String handleRebirthCommand(String command, Player player)
	{
		if (!CommunityBoardConfig.CUSTOM_CB_ENABLED)
			return null;

		if (command.startsWith("_bbsrebirth;confirm"))
			RebirthManager.doRebirth(player);

		return RebirthManager.buildShrineHtml(player);
	}
	
	/**
	 * Renders the Rebirth pages requested through {@code _bbstop;rebirth/<page>}, filling their live placeholders.
	 * @param page The relative page (main.html, ask.html, result.html, powers.html).
	 * @param player The viewer.
	 * @return The filled HTML, or null when the page is unknown.
	 */
	private static String handleRebirthPage(String page, Player player)
	{
		final String normalized = page.toLowerCase().replace('\\', '/');

		// El sistema de "Poderes del Renacido" (stats por skills power) fue eliminado, y ask.html /
		// result.html ya no existen (el boton RENACER hace el rebirth directo) - cualquier link viejo
		// que apunte a esas paginas vuelve al shrine.
		if (normalized.endsWith("main.html") || normalized.endsWith("main.htm") || normalized.endsWith("ask.html") || normalized.endsWith("ask.htm") || normalized.endsWith("result.html") || normalized.endsWith("result.htm") || normalized.endsWith("powers.html") || normalized.endsWith("powers.htm"))
			return RebirthManager.buildShrineHtml(player);

		return loadCustomHtml(page);
	}
	
	private static String handleDonateCommand(String command, Player player)
	{
		if (command.equals("_bbsdonate") || command.equals("_bbsdonate;main"))
			return loadCustomHtml("donate/main.html");
		
		if (command.equals("_bbsdonate;skins"))
			return loadCustomHtml("donate/skins.html");
		
		if (command.equals("_bbsdonate;augments"))
			return loadCustomHtml("donate/augments.html");
		
		if (command.equals("_bbsdonate;status") || command.equals("_bbsdonate;vip"))
			return loadCustomHtml("donate/vip.html");
		
		if (command.equals("_bbsdonate;hero"))
			return loadCustomHtml("donate/hero.html");
		
		if (command.equals("_bbsdonate;aio"))
			return loadCustomHtml("donate/aio.html");
		
		if (command.equals("_bbsdonate;startervip"))
		{
			StarterVipHwid.claim(player);
			return loadCustomHtml("donate/main.html");
		}
		
		// _bbsdonate;gbhero eliminado: los status ahora se pagan solo con Donation Coin (10001).
		
		// _bbsdonate;gbaio eliminado: los status ahora se pagan solo con Donation Coin (10001).
		
		if (command.startsWith("_bbsdonate;buy;"))
		{
			final String params = command.substring("_bbsdonate;buy;".length()).trim();
			handleDonateStatusBuy(player, params);
			if (params.startsWith("hero;"))
				return loadCustomHtml("donate/hero.html");
			if (params.startsWith("aio;"))
				return loadCustomHtml("donate/aio.html");
			return loadCustomHtml("donate/vip.html");
		}
		
		if (command.equals("_bbsdonate;colors;name"))
			return loadCustomHtml("donate/namecolor.html");
		
		if (command.equals("_bbsdonate;colors;title"))
			return loadCustomHtml("donate/titlecolor.html");
		
		if (command.startsWith("_bbsdonate;namecolor;"))
		{
			MultiManager.getInstance().trySetNameColor(player, command.substring("_bbsdonate;namecolor;".length()).trim());
			return loadCustomHtml("donate/namecolor.html");
		}
		
		if (command.startsWith("_bbsdonate;titlecolor;"))
		{
			MultiManager.getInstance().trySetTitleColor(player, command.substring("_bbsdonate;titlecolor;".length()).trim());
			return loadCustomHtml("donate/titlecolor.html");
		}
		
		if (command.startsWith("_bbsdonate;skin;"))
		{
			MultiManager.getInstance().Skin("skin " + command.substring("_bbsdonate;skin;".length()).trim(), player);
			return loadCustomHtml("donate/skins.html");
		}
		
		if (command.equals("_bbsdonate;sex"))
		{
			MultiManager.getInstance().Skin("skin ChangeSex", player);
			return loadCustomHtml("donate/skins.html");
		}
		
		if (command.startsWith("_bbsdonate;active;"))
		{
			MultiManager.getInstance().ActiveSkill("active " + command.substring("_bbsdonate;active;".length()).trim(), player);
			return loadCustomHtml("donate/augments.html");
		}
		
		if (command.startsWith("_bbsdonate;passive;"))
		{
			MultiManager.getInstance().PassiveSkill("passive " + command.substring("_bbsdonate;passive;".length()).trim(), player);
			return loadCustomHtml("donate/augments.html");
		}
		
		return loadCustomHtml("donate/main.html");
	}
	
	private static String applyDonatePrices(String html)
	{
		if (html == null || !html.contains("%donate_"))
			return html;
		
		html = html.replace("%donate_skin%", String.valueOf(Config.CANTIDADSKIN));
		html = html.replace("%donate_sexo%", String.valueOf(Config.CANTIDADSEXO));
		html = html.replace("%donate_argu%", String.valueOf(Config.CANTIDADARGU));
		html = html.replace("%donate_namecolor%", String.valueOf(Config.CANTIDADNAMECOLOR));
		html = html.replace("%donate_titlecolor%", String.valueOf(Config.CANTIDADTITLECOLOR));
		html = html.replace("%donate_vip_1d%", String.valueOf(Config.CANTIDAD_VIP_1D));
		html = html.replace("%donate_vip_30d%", String.valueOf(Config.CANTIDAD_VIP_30D));
		html = html.replace("%donate_vip_90d%", String.valueOf(Config.CANTIDAD_VIP_90D));
		html = html.replace("%donate_hero_1d%", String.valueOf(Config.CANTIDAD_HERO_1D));
		html = html.replace("%donate_hero_30d%", String.valueOf(Config.CANTIDAD_HERO_30D));
		html = html.replace("%donate_hero_90d%", String.valueOf(Config.CANTIDAD_HERO_90D));
		html = html.replace("%donate_aio_1d%", String.valueOf(Config.CANTIDAD_AIO_1D));
		html = html.replace("%donate_aio_30d%", String.valueOf(Config.CANTIDAD_AIO_30D));
		html = html.replace("%donate_aio_90d%", String.valueOf(Config.CANTIDAD_AIO_90D));
		html = html.replace("%donate_vip_dias_1d%", String.valueOf(Config.DIAS_VIP_1D));
		html = html.replace("%donate_vip_dias_30d%", String.valueOf(Config.DIAS_VIP_30D));
		html = html.replace("%donate_vip_dias_90d%", String.valueOf(Config.DIAS_VIP_90D));
		html = html.replace("%donate_hero_dias_1d%", String.valueOf(Config.DIAS_HERO_1D));
		html = html.replace("%donate_hero_dias_30d%", String.valueOf(Config.DIAS_HERO_30D));
		html = html.replace("%donate_hero_dias_90d%", String.valueOf(Config.DIAS_HERO_90D));
		html = html.replace("%donate_aio_dias_1d%", String.valueOf(Config.DIAS_AIO_1D));
		html = html.replace("%donate_aio_dias_30d%", String.valueOf(Config.DIAS_AIO_30D));
		html = html.replace("%donate_aio_dias_90d%", String.valueOf(Config.DIAS_AIO_90D));
		html = html.replace("%donate_starter_vip_hours%", String.valueOf(Config.STARTER_VIP_HOURS));
		html = html.replace("%donate_combo_price%", String.valueOf(Config.CANTIDAD_COMBO_30D));
		html = html.replace("%donate_combo_dias%", String.valueOf(Config.DIAS_COMBO_30D));
		return html;
	}
	
	private static void handleDonateStatusBuy(Player player, String params)
	{
		final String[] parts = params.split(";");
		if (parts.length != 2)
			return;
		
		final String type = parts[0].toLowerCase();
		final int pack;
		try
		{
			pack = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException e)
		{
			return;
		}
		
		// Solo se vende el pack de 30 dias (precio/duracion salen de DonationManager.properties).
		if (pack != 30)
			return;
		
		final int price;
		final int days;
		switch (type)
		{
			case "vip":
				if (player.isVip() && player.getVipEndTime() > System.currentTimeMillis())
				{
					player.sendMessage("Ya tenes VIP activo. Espera a que expire para comprar otro.");
					return;
				}
				price = Config.CANTIDAD_VIP_30D;
				days = Config.DIAS_VIP_30D;
				break;
			case "hero":
				if (player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > System.currentTimeMillis()))
				{
					player.sendMessage("Ya tenes Hero activo. Espera a que expire para comprar otro.");
					return;
				}
				price = Config.CANTIDAD_HERO_30D;
				days = Config.DIAS_HERO_30D;
				break;
			case "aio":
				if (!Config.ENABLE_AIO_SYSTEM)
				{
					player.sendMessage("El sistema AIO esta desactivado.");
					return;
				}
				if (player.isAio() && player.getAioEndTime() > System.currentTimeMillis())
				{
					player.sendMessage("Ya tenes AIO activo. Espera a que expire para comprar otro.");
					return;
				}
				price = Config.CANTIDAD_AIO_30D;
				days = Config.DIAS_AIO_30D;
				break;
			case "combo":
				if (!Config.COMBO_ENABLED)
				{
					player.sendMessage("El Combo VIP + Hero + Nobless esta desactivado.");
					return;
				}
				// El combo NO bloquea si ya tenes VIP/Hero: los SUMA al tiempo que te queda.
				price = Config.CANTIDAD_COMBO_30D;
				days = Config.DIAS_COMBO_30D;
				break;
			default:
				return;
		}
		
		if (price <= 0 || days <= 0)
		{
			player.sendMessage("Este pack no esta configurado.");
			return;
		}
		
		final int payId = Config.ITEMSKIN > 0 ? Config.ITEMSKIN : 10001;
		if (!player.destroyItemByItemId("DonateStatus", payId, price, player, true))
		{
			player.sendMessage("No tenes suficientes Donation Coin.");
			return;
		}
		
		final boolean applied;
		switch (type)
		{
			case "vip":
				applied = VipCoin.apply(player, days);
				break;
			case "hero":
				applied = HeroCoin.apply(player, days);
				break;
			case "aio":
				applied = AioCoin.apply(player, days);
				break;
			case "combo":
				applied = applyDonateCombo(player, days);
				break;
			default:
				applied = false;
				break;
		}
		
		if (!applied)
			player.addItem("DonateStatusRefund", payId, price, player, true);
	}
	
	/**
	 * COMBO: VIP + Hero + Nobless en una sola compra.<br>
	 * VIP y Hero se SUMAN al tiempo que ya tenga (addDays nunca rechaza) | el Noblesse queda permanente.
	 * @return true si se aplico al menos un status (si no, se devuelve el pago).
	 */
	private static boolean applyDonateCombo(Player player, int days)
	{
		boolean applied = false;
		
		if (Config.COMBO_GIVE_VIP && VipCoin.addDays(player, days))
			applied = true;
		
		if (Config.COMBO_GIVE_HERO && HeroCoin.addDays(player, days))
			applied = true;
		
		if (Config.COMBO_GIVE_NOBLESS)
		{
			if (!player.isNoble())
			{
				player.setNoble(true, true);
				player.sendMessage("Noblesse activado.");
			}
			// Ya era noble: no hay nada que dar, pero el combo se cobra igual (no se devuelve).
			applied = true;
		}
		
		if (applied)
			player.sendMessage("COMBO activado: VIP " + days + "d + Hero " + days + "d + Nobless.");
		
		return applied;
	}
	
	private static void handleGbStatusBuy(Player player, String type)
	{
		if (!Config.GB_STATUS_ENABLED)
		{
			player.sendMessage("La compra por Gold Bars esta desactivada.");
			return;
		}
		
		final int itemId;
		final int price;
		final int days;
		switch (type)
		{
			case "hero":
				if (player.isHero() && (player.getHeroEndTime() == 0 || player.getHeroEndTime() > System.currentTimeMillis()))
				{
					player.sendMessage("Ya tenes Hero activo. Espera a que expire para comprar otro.");
					return;
				}
				itemId = Config.ITEM_HERO_GB;
				price = Config.CANTIDAD_HERO_GB;
				days = Config.DIAS_HERO_GB;
				break;
			case "aio":
				if (!Config.ENABLE_AIO_SYSTEM)
				{
					player.sendMessage("El sistema AIO esta desactivado.");
					return;
				}
				if (player.isAio() && player.getAioEndTime() > System.currentTimeMillis())
				{
					player.sendMessage("Ya tenes AIO activo. Espera a que expire para comprar otro.");
					return;
				}
				itemId = Config.ITEM_AIO_GB;
				price = Config.CANTIDAD_AIO_GB;
				days = Config.DIAS_AIO_GB;
				break;
			default:
				return;
		}
		
		if (itemId <= 0 || price <= 0 || days <= 0)
		{
			player.sendMessage("Este pack no esta configurado.");
			return;
		}
		
		if (!player.destroyItemByItemId("DonateGbStatus", itemId, price, player, true))
		{
			player.sendMessage("No tenes suficientes Gold Bars.");
			return;
		}
		
		final boolean applied = "hero".equals(type) ? HeroCoin.apply(player, days) : AioCoin.apply(player, days);
		if (!applied)
			player.addItem("DonateGbStatusRefund", itemId, price, player, true);
	}
	
	private static String loadCustomHtml(String relativePath)
	{
		final String safe = sanitizeHtmlPath(relativePath);
		if (safe == null)
			return null;
		
		return HtmCache.getInstance().getHtm(CUSTOM_CB_PATH + safe);
	}
	
	private static String sanitizeHtmlPath(String path)
	{
		if (path == null)
			return null;
		
		path = path.replace('\\', '/').trim();
		if (path.isEmpty() || path.contains("..") || path.startsWith("/") || path.contains(":"))
			return null;
		
		if (!path.endsWith(".html") && !path.endsWith(".htm"))
			path = path + ".html";
		
		if (!path.matches("[a-zA-Z0-9_./-]+"))
			return null;
		
		return path;
	}
	
	/**
	 * Retail gatekeeper teleport: coords, price and noblesse flag come from teleportLocations.xml.
	 * Bypass: _bbsteleport;goto;135
	 */
	private static void handleRetailGoto(Player player, String teleBypass)
	{
		String idPart = teleBypass.substring(4).trim();
		if (idPart.startsWith(";") || idPart.startsWith(" "))
			idPart = idPart.substring(1).trim();
		
		final int teleportId;
		try
		{
			teleportId = Integer.parseInt(idPart);
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("Invalid teleport.");
			return;
		}
		
		final TeleportLocation list = TeleportLocationData.getInstance().getTeleportLocation(teleportId);
		if (list == null)
		{
			player.sendMessage("Invalid teleport.");
			return;
		}
		
		if (CastleManager.getInstance().getActiveSiege(list.getX(), list.getY(), list.getZ()) != null)
		{
			player.sendPacket(SystemMessageId.CANNOT_PORT_VILLAGE_IN_SIEGE);
			return;
		}
		
		if (list.isNoble() && !player.isNoble())
		{
			player.sendMessage("Only a Noblesse can use the Noblesse Only Teleport.");
			return;
		}
		
		int price = list.getPrice();
		if (!list.isNoble())
		{
			final Calendar cal = Calendar.getInstance();
			if (cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 && (cal.get(Calendar.DAY_OF_WEEK) == 1 || cal.get(Calendar.DAY_OF_WEEK) == 7))
				price /= 2;
		}
		
		final int itemId = list.isNoble() ? 6651 : 57;
		if (!player.destroyItemByItemId("CommunityBoard", itemId, price, player, true))
			return;
		
		player.sendPacket(ShowBoard.STATIC_CLOSE);
		player.setIsIn7sDungeon(false);
		player.teleportTo(list, 20);
		player.sendPacket(new SystemMessage(SystemMessageId.WILL_BE_MOVED_INTERFACE));
		if (player.isAutoFarm())
			VoicedAutoFarm.stopAutoFarm(player, "teleport");
	}
	
	private static void refreshBoardAfterDeniedAction(String command, Player player)
	{
		if (!CommunityBoardConfig.CUSTOM_CB_ENABLED)
			return;
		
		final String html = loadCustomHtml(resolveDeniedFallbackPage(command));
		if (html == null)
			return;
		
		sendBoardHtml(html, HtmCache.getInstance().getHtm(NAVIGATION_PATH), player, command, player, "Tus stats");
	}
	
	private static String resolveDeniedFallbackPage(String command)
	{
		if (command.startsWith("_bbsteleport;farmzone"))
			return "gatekeeper/farm.html";
		if (command.startsWith("_bbsteleport;goto") || command.startsWith("_bbsteleport"))
			return "gatekeeper/towns.html";
		if (command.startsWith("_bbsmultisell") || command.startsWith("_bbsexcmultisell") || command.startsWith("_bbssell"))
			return "merchant/main.html";
		if (command.startsWith("_bbsresist;vip"))
			return "buffer/premium.html";
		if (command.startsWith("_bbsresist;coin"))
			return "buffer/coin.html";
		
		final int last = command.lastIndexOf(';');
		if (last >= 0)
		{
			final String tail = command.substring(last + 1).trim();
			if (tail.endsWith(".html") || tail.endsWith(".htm"))
				return tail;
		}
		
		return "home.html";
	}
	
	private static void sendBoardHtml(String returnHtml, String navigation, Player player, String command, Player statsTarget, String statsMessage)
	{
		if (returnHtml == null)
		{
			player.sendMessage("Community Board page not found.");
			return;
		}
		
		if (CommunityBoardConfig.CUSTOM_CB_ENABLED && navigation != null)
			returnHtml = returnHtml.replace("%navigation%", navigation);
		
		returnHtml = applyPlayerStats(returnHtml, player);
		returnHtml = applyDonatePrices(returnHtml);
		returnHtml = applyStatusPages(returnHtml, player, command, statsTarget, statsMessage);
		returnHtml = HomeBoardEventInfo.applyPlaceholders(returnHtml);
		separateAndSend(returnHtml, player);
	}
	
	private static String applyStatusPages(String html, Player player, String command, Player statsTarget, String statsMessage)
	{
		final boolean isStatsPage = command.startsWith("_bbsstats") || command.contains("stats/main.html");
		final boolean isVip = command.contains("vip/main.html");
		final boolean isHero = command.contains("hero/main.html");
		final boolean isAio = command.contains("aio/main.html");
		
		if (isStatsPage)
		{
			html = html.replace("%stats_message%", statsMessage);
			html = PlayerStats.fillPlaceholders(html, statsTarget != null ? statsTarget : player);
		}
		if (isVip)
			html = HomeBoardStatusInfo.applyVip(html, player);
		if (isHero)
			html = HomeBoardStatusInfo.applyHero(html, player);
		if (isAio)
			html = HomeBoardStatusInfo.applyAio(html, player);
		if (command.equals("_bbshome") || command.equals("_bbstop") || command.contains("home.html") || html.contains("%vip_remaining%") || html.contains("%hero_remaining%"))
			html = HomeBoardStatusInfo.applyHomeDashboard(html, player);
		if (command.contains("raidboss/main.html"))
			html = HomeBoardBossInfo.applyRaidBossPage(html, 1);
		if (command.contains("grandboss/main.html"))
			html = HomeBoardBossInfo.applyGrandBossPage(html, 1);
		
		return html;
	}
	
	private static String handleResistCommand(String command, Player player)
	{
		if (!CommunityBoardConfig.COMMUNITYBOARD_ENABLE_BUFFS)
		{
			player.sendMessage("This Community Board service is disabled.");
			return null;
		}
		
		final String[] parts = command.split(";");
		if (parts.length < 3)
		{
			player.sendMessage("Invalid buff request.");
			return loadCustomHtml("buffer/premium.html");
		}
		
		final String mode = parts[1].toLowerCase();
		final boolean vipMode = "vip".equals(mode);
		final boolean coinMode = "coin".equals(mode);
		final String page = vipMode ? "buffer/premium.html" : "buffer/coin.html";
		if (!vipMode && !coinMode)
			return loadCustomHtml(page);
		
		final String[] skillInfo = parts[2].split(",");
		if (skillInfo.length < 2)
			return loadCustomHtml(page);
		
		final int skillId;
		final int skillLvl;
		try
		{
			skillId = Integer.parseInt(skillInfo[0]);
			skillLvl = Integer.parseInt(skillInfo[1]);
		}
		catch (NumberFormatException e)
		{
			return loadCustomHtml(page);
		}
		
		if (!CommunityBoardConfig.COMMUNITY_RESIST_BUFFS.contains(skillId))
		{
			player.sendMessage("This buff is not available.");
			return loadCustomHtml(page);
		}
		
		if (vipMode)
		{
			if (!player.isVip() || player.getVipEndTime() <= System.currentTimeMillis())
			{
				player.sendMessage("Solo VIP puede usar Premium Buffs.");
				return loadCustomHtml(page);
			}
		}
		else
		{
			final int coinId = CommunityBoardConfig.RESIST_COIN_ID;
			final int coinPrice = CommunityBoardConfig.RESIST_COIN_PRICE;
			if (coinPrice > 0 && player.getInventory().getInventoryItemCount(coinId, -1) < coinPrice)
			{
				player.sendMessage("No tienes suficientes coins.");
				return loadCustomHtml(page);
			}
			if (coinPrice > 0)
				player.destroyItemByItemId("CommunityResist", coinId, coinPrice, player, true);
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
		if (skill == null)
		{
			player.sendMessage("This buff is not available.");
			return loadCustomHtml(page);
		}
		
		skill.getEffects(player, player);
		final Summon pet = player.getSummon();
		if (pet != null)
			skill.getEffects(player, pet);
		if (CommunityBoardConfig.COMMUNITYBOARD_CAST_ANIMATIONS)
			player.sendPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
		
		return loadCustomHtml(page);
	}
	
	private static void applyCommunityPreset(Player caster, Creature target, List<Integer> skillIds)
	{
		if (caster == null || target == null || skillIds == null || skillIds.isEmpty())
			return;
		
		target.setBatchEffectUpdate(true);
		try
		{
			for (int skillId : skillIds)
			{
				if (!CommunityBoardConfig.COMMUNITY_AVAILABLE_BUFFS.contains(skillId) || CommunityBoardConfig.COMMUNITY_RESIST_BUFFS.contains(skillId))
					continue;
				
				final int level = getCommunityBuffLevel(skillId);
				final L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
				if (skill != null)
					skill.getEffects(caster, target);
			}
		}
		finally
		{
			target.setBatchEffectUpdate(false);
		}
		
		target.updateEffectIcons();
		if (target instanceof Player)
			((Player) target).updateAndBroadcastStatus(2);
	}
	
	/**
	 * CB schemes use retail max. Extra XML levels are AIO-only.
	 */
	private static int getCommunityBuffLevel(int skillId)
	{
		return BufferTable.getRetailBuffLevel(skillId);
	}
	
	/**
	 * Injects live player/server placeholders (L2Interlude-style).
	 */
	public static String applyPlayerStats(String content, Player player)
	{
		if (content == null || player == null)
			return content;
		
		content = content.replace("%playerName%", player.getName());
		content = content.replace("%LV%", String.valueOf(player.getLevel()));
		content = content.replace("%className%", player.getTemplate().getClassName());
		content = content.replace("%clanName%", player.getClan() != null ? player.getClan().getName() : "No Clan");
		content = content.replace("%Adena%", StringUtil.formatNumber(player.getAdena()));
		content = content.replace("%pvpPk%", player.getPvpKills() + " / " + player.getPkKills());
		content = content.replace("%playersOnline%", String.valueOf(World.getInstance().getPlayers().size()));
		content = content.replace("%HP%", (int) player.getCurrentHp() + " / " + player.getMaxHp());
		content = content.replace("%MP%", (int) player.getCurrentMp() + " / " + player.getMaxMp());
		content = content.replace("%CP%", (int) player.getCurrentCp() + " / " + player.getMaxCp());
		return content;
	}
	
	private static boolean isCombatRestricted(String command, Player player)
	{
		if (!isRestrictedCustomCommand(command))
			return false;
		
		return player.isCastingNow() || player.isCastingSimultaneouslyNow() || player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || player.getPvpFlag() > 0 || player.isAlikeDead() || isOnEvent(player) || player.isInStoreMode();
	}
	
	private static boolean isRestrictedCustomCommand(String command)
	{
		if (CommunityBoardConfig.COMMUNITYBOARD_ENABLE_MULTISELLS && (command.startsWith("_bbsexcmultisell") || command.startsWith("_bbsmultisell") || command.startsWith("_bbssell")))
			return true;
		
		if (CommunityBoardConfig.COMMUNITYBOARD_ENABLE_TELEPORTS && command.startsWith("_bbsteleport"))
			return true;
		
		if (CommunityBoardConfig.COMMUNITYBOARD_ENABLE_BUFFS && (command.startsWith("_bbsbuff") || command.startsWith("_bbsresist") || command.startsWith("_bbscancel") || command.startsWith("_bbspreset")))
			return true;
		
		if (command.startsWith("_bbsdonate;skin") || command.startsWith("_bbsdonate;active") || command.startsWith("_bbsdonate;passive") || command.startsWith("_bbsdonate;sex") || command.startsWith("_bbsdonate;namecolor;") || command.startsWith("_bbsdonate;titlecolor;") || command.startsWith("_bbsdonate;buy") || command.equals("_bbsdonate;gbhero") || command.equals("_bbsdonate;gbaio"))
			return true;
		
		// Rebirth: purchases can't be done while fighting.
		if (command.startsWith("_bbsrebirth;confirm"))
			return true;
		
		return CommunityBoardConfig.COMMUNITYBOARD_ENABLE_HEAL && command.startsWith("_bbsheal");
	}
	
	private static boolean isOnEvent(Player player)
	{
		final int objectId = player.getObjectId();
		return (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(objectId)) || (CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(objectId)) || (DMEvent.isStarted() && DMEvent.isPlayerParticipant(objectId)) || (LMEvent.isStarted() && LMEvent.isPlayerParticipant(objectId)) || KTBEvent.isPlayerParticipant(player);
	}
	
	public static boolean handlesCommand(String command)
	{
		if (!CommunityBoardConfig.CUSTOM_CB_ENABLED)
			return isRestrictedCustomCommand(command);
		
		return command.equals("_bbshome") || command.equals("_bbstop") || command.startsWith("_bbstop;") || command.startsWith("_bbsstats") || command.startsWith("_bbspass") || command.startsWith("_bbsboss") || command.startsWith("_bbsrank") || command.startsWith("_bbsrebirth") || command.startsWith("_bbsdonate") || command.startsWith("_bbsmultisell") || command.startsWith("_bbsexcmultisell") || command.startsWith("_bbssell") || command.startsWith("_bbsteleport") || command.startsWith("_bbsbuff") || command.startsWith("_bbsresist") || command.startsWith("_bbsheal") || command.startsWith("_bbscancel") || command.startsWith("_bbspreset");
	}
	
	public static HomeBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HomeBBSManager INSTANCE = new HomeBBSManager();
	}
}
