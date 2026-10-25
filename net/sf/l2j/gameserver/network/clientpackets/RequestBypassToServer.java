package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Base.Skin.DressMeData;
import Base.Skin.SkinPackage;
import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.autofarm.AutofarmPlayerRoutine;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.CustomBypassHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.RaidInfoBypassHandler;
import net.sf.l2j.gameserver.handler.SellBuffBypassHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.DonationManager;
import net.sf.l2j.gameserver.model.actor.instance.Gatekeeper;
import net.sf.l2j.gameserver.model.actor.instance.MultiManager;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.franchus.Franchus;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.QuestState;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_command.isEmpty())
			return;
		
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		// Anti interface: respuesta de la interface del server al desafio.
		if (_command.startsWith("_ai "))
		{
			net.sf.l2j.gameserver.antiinterface.AntiInterfaceManager.onBypass(player, _command);
			return;
		}
		
		// SanSara: comandos de la interface ACDC (Essence)
		if (net.sf.l2j.gameserver.handler.InterfaceBypassHandler.isInterfaceCommand(_command))
		{
			net.sf.l2j.gameserver.handler.InterfaceBypassHandler.handle(player, _command, getClient());
			return;
		}
		
		final AutofarmPlayerRoutine bot = player.getBot();
		
		if (_command.startsWith("admin_"))
		{
			String command = _command.split(" ")[0];
			
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
			if (ach == null)
			{
				if (player.isGM())
					player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
				
				LOGGER.warn("No handler registered for admin command '{}'.", command);
				return;
			}
			
			if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command.");
				LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
				return;
			}
			
			if (Config.GMAUDIT)
				GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
			
			ach.useAdminCommand(_command, player);
		}
		else if (_command.startsWith("custom_"))
		{
			CustomBypassHandler.getInstance().handleBypass(player, _command);
		}
		else if (_command.startsWith("dressme"))
		{
			// Event Gear: sin skins personales durante los eventos.
			if (net.sf.l2j.gameserver.events.eventgear.EventGearManager.blockSkin(player))
				return;
			
			if (!Config.ALLOW_DRESS_ME_IN_OLY && player.isInOlympiadMode())
			{
				player.sendMessage("DressMe can't be used on The Olympiad game.");
				return;
			}
			
			StringTokenizer st = new StringTokenizer(_command, " ");
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				showDressMeMainPage(player);
				return;
			}
			int page = Integer.parseInt(st.nextToken());
			
			if (!st.hasMoreTokens())
			{
				showDressMeMainPage(player);
				return;
			}
			String next = st.nextToken();
			if (next.startsWith("skinlist"))
			{
				String type = st.nextToken();
				showSkinList(player, type, page);
			}
			else if (next.startsWith("myskinlist"))
			{
				
				showMySkinList(player, page);
			}
			if (next.equals("clean"))
			{
				String type = st.nextToken();
				
				if (player.isTryingSkin())
				{
					player.sendMessage("You can't do this while trying a skin.");
					player.sendPacket(new ExShowScreenMessage("You can't do this while trying a skin.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showDressMeMainPage(player);
					return;
				}
				
				switch (type.toLowerCase())
				{
					case "armor":
						player.setArmorSkinOption(0);
						break;
					case "weapon":
						player.setWeaponSkinOption(0);
						break;
					case "hair":
						player.setHairSkinOption(0);
						break;
					case "face":
						player.setFaceSkinOption(0);
						break;
					case "shield":
						player.setShieldSkinOption(0);
						break;
				}
				
				player.broadcastUserInfo();
				showMySkinList(player, page);
			}
			else if (next.startsWith("buyskin"))
			{
				if (!st.hasMoreTokens())
				{
					showDressMeMainPage(player);
					return;
				}
				
				int skinId = Integer.parseInt(st.nextToken());
				String type = st.nextToken();
				int itemId = Integer.parseInt(st.nextToken());
				
				SkinPackage sp = null;
				
				switch (type.toLowerCase())
				{
					case "armor":
						sp = DressMeData.getInstance().getArmorSkinsPackage(skinId);
						break;
					case "weapon":
						sp = DressMeData.getInstance().getWeaponSkinsPackage(skinId);
						
						if (player.getActiveWeaponItem() == null)
						{
							player.sendMessage("You can't buy this skin without a weapon.");
							player.sendPacket(new ExShowScreenMessage("You can't buy this skin without a weapon.", 2000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							showSkinList(player, type, page);
							return;
						}
						
						ItemInstance skinWeapon = null;
						if (ItemTable.getInstance().getTemplate(itemId) != null)
						{
							skinWeapon = ItemTable.getInstance().createDummyItem(itemId);
							
							if (!checkWeapons(player, skinWeapon, WeaponType.BOW, WeaponType.BOW) //
								|| !checkWeapons(player, skinWeapon, WeaponType.SWORD, WeaponType.SWORD) //
								|| !checkWeapons(player, skinWeapon, WeaponType.BLUNT, WeaponType.BLUNT) //
								|| !checkWeapons(player, skinWeapon, WeaponType.DAGGER, WeaponType.DAGGER) //
								|| !checkWeapons(player, skinWeapon, WeaponType.POLE, WeaponType.POLE) //
								|| !checkWeapons(player, skinWeapon, WeaponType.DUAL, WeaponType.DUAL) //
								|| !checkWeapons(player, skinWeapon, WeaponType.DUALFIST, WeaponType.DUALFIST) //
								|| !checkWeapons(player, skinWeapon, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
								|| !checkWeapons(player, skinWeapon, WeaponType.FIST, WeaponType.FIST) //
								|| !checkWeapons(player, skinWeapon, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
							{
								player.sendMessage("This skin is not suitable for your weapon type.");
								player.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showSkinList(player, type, page);
								return;
							}
						}
						break;
					case "hair":
						sp = DressMeData.getInstance().getHairSkinsPackage(skinId);
						break;
					case "face":
						sp = DressMeData.getInstance().getFaceSkinsPackage(skinId);
						break;
					case "shield":
						sp = DressMeData.getInstance().getShieldSkinsPackage(skinId);
						if (player.getActiveWeaponItem() == null)
						{
							player.sendMessage("You can't buy this skin without a weapon.");
							player.sendPacket(new ExShowScreenMessage("You can't buy this skin without a weapon.", 2000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							showSkinList(player, type, page);
							return;
						}
						
						ItemInstance skinShield = null;
						if (ItemTable.getInstance().getTemplate(itemId) != null)
						{
							skinShield = ItemTable.getInstance().createDummyItem(itemId);
							
							if (!checkWeapons(player, skinShield, WeaponType.BOW, WeaponType.BOW) //
								|| !checkWeapons(player, skinShield, WeaponType.POLE, WeaponType.POLE) //
								|| !checkWeapons(player, skinShield, WeaponType.DUAL, WeaponType.DUAL) //
								|| !checkWeapons(player, skinShield, WeaponType.DUALFIST, WeaponType.DUALFIST) //
								|| !checkWeapons(player, skinShield, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
								|| !checkWeapons(player, skinShield, WeaponType.FIST, WeaponType.FIST) //
								|| !checkWeapons(player, skinShield, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
							{
								player.sendMessage("This skin is not suitable for your weapon type.");
								player.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showSkinList(player, type, page);
								return;
							}
						}
						break;
				}
				
				if (sp == null)
				{
					player.sendMessage("There is no such skin.");
					player.sendPacket(new ExShowScreenMessage("There is no such skin.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showSkinList(player, type, page);
					return;
				}
				
				
				if (player.destroyItemByItemId("dressme", sp.getPriceId(), sp.getPriceCount(), player, true))
				{
					player.sendMessage("You have successfully purchased " + sp.getName() + " skin.");
					player.sendPacket(new ExShowScreenMessage("You have successfully purchased " + sp.getName() + " skin.", 2000, 2, false));
					
					switch (type.toLowerCase())
					{
						case "armor":
							player.buyArmorSkin(skinId);
							player.setArmorSkinOption(skinId);
							player.storeDressMeData();
							break;
						case "weapon":
							player.buyWeaponSkin(skinId);
							player.setWeaponSkinOption(skinId);
							player.storeDressMeData();
							break;
						case "hair":
							player.buyHairSkin(skinId);
							player.setHairSkinOption(skinId);
							player.storeDressMeData();
							break;
						case "face":
							player.buyFaceSkin(skinId);
							player.setFaceSkinOption(skinId);
							break;
						case "shield":
							player.buyShieldSkin(skinId);
							player.setShieldSkinOption(skinId);
							player.storeDressMeData();
							break;
					}
					
					player.broadcastUserInfo();
				}
				showSkinList(player, type, page);
			}
			else if (next.startsWith("tryskin"))
			{
				
				int skinId = Integer.parseInt(st.nextToken());
				
				String type = st.nextToken();
				
				if (player.isTryingSkin())
				{
					player.sendMessage("You are already trying a skin.");
					player.sendPacket(new ExShowScreenMessage("You are already trying a skin.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showSkinList(player, type, page);
					return;
				}
				
				player.setIsTryingSkin(true);
				
				int oldArmorSkinId = player.getArmorSkinOption();
				int oldWeaponSkinId = player.getWeaponSkinOption();
				int oldHairSkinId = player.getHairSkinOption();
				int oldFaceSkinId = player.getFaceSkinOption();
				int oldShieldSkinId = player.getShieldSkinOption();
				
				switch (type.toLowerCase())
				{
					case "armor":
						player.setArmorSkinOption(skinId);
						break;
					case "weapon":
						player.setWeaponSkinOption(skinId);
						break;
					case "hair":
						player.setHairSkinOption(skinId);
						break;
					case "face":
						player.setFaceSkinOption(skinId);
						break;
					case "shield":
						
						player.setShieldSkinOption(skinId);
						
						break;
				}
				
				player.broadcastUserInfo();
				showSkinList(player, type, page);
				
				ThreadPool.schedule(() ->
				{
					switch (type.toLowerCase())
					{
						case "armor":
							player.setArmorSkinOption(oldArmorSkinId);
							break;
						case "weapon":
							player.setWeaponSkinOption(oldWeaponSkinId);
							break;
						case "hair":
							player.setHairSkinOption(oldHairSkinId);
							break;
						case "face":
							player.setFaceSkinOption(oldFaceSkinId);
							break;
						case "shield":
							player.setShieldSkinOption(oldShieldSkinId);
							break;
					}
					
					player.broadcastUserInfo();
					player.setIsTryingSkin(false);
				}, 5000);
			}
			else if (next.startsWith("setskin"))
			{
				int id = Integer.parseInt(st.nextToken());
				String type = st.nextToken();
				int itemId = Integer.parseInt(st.nextToken());
				
				if (player.isTryingSkin())
				{
					player.sendMessage("You can't do this while trying skins.");
					player.sendPacket(new ExShowScreenMessage("You can't do this while trying skins.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showMySkinList(player, page);
					return;
				}
				
				if (type.toLowerCase().contains("armor") && player.hasEquippedArmorSkin(String.valueOf(id)) || type.toLowerCase().contains("weapon") && player.hasEquippedWeaponSkin(String.valueOf(id))
					|| type.toLowerCase().contains("hair") && player.hasEquippedHairSkin(String.valueOf(id)) || type.toLowerCase().contains("face") && player.hasEquippedFaceSkin(String.valueOf(id)))
				{
					player.sendMessage("You are already equipped this skin.");
					player.sendPacket(new ExShowScreenMessage("You are already equipped this skin.", 2000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					showMySkinList(player, page);
					return;
				}
				
				switch (type.toLowerCase())
				{
					case "armor":
						player.setArmorSkinOption(id);
						break;
					case "weapon":
						if (player.getActiveWeaponItem() == null)
						{
							player.sendMessage("You can't use this skin without a weapon.");
							player.sendPacket(new ExShowScreenMessage("You can't use this skin without a weapon.", 2000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							showMySkinList(player, page);
							return;
						}
						
						ItemInstance skinWeapon = null;
						if (ItemTable.getInstance().getTemplate(itemId) != null)
						{
							skinWeapon = ItemTable.getInstance().createDummyItem(itemId);
							
							if (!checkWeapons(player, skinWeapon, WeaponType.BOW, WeaponType.BOW) //
								|| !checkWeapons(player, skinWeapon, WeaponType.SWORD, WeaponType.SWORD) //
								|| !checkWeapons(player, skinWeapon, WeaponType.BLUNT, WeaponType.BLUNT) //
								|| !checkWeapons(player, skinWeapon, WeaponType.DAGGER, WeaponType.DAGGER) //
								|| !checkWeapons(player, skinWeapon, WeaponType.POLE, WeaponType.POLE) //
								|| !checkWeapons(player, skinWeapon, WeaponType.DUAL, WeaponType.DUAL) //
								|| !checkWeapons(player, skinWeapon, WeaponType.DUALFIST, WeaponType.DUALFIST) //
								|| !checkWeapons(player, skinWeapon, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
								|| !checkWeapons(player, skinWeapon, WeaponType.FIST, WeaponType.FIST) //
								|| !checkWeapons(player, skinWeapon, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
							{
								player.sendMessage("This skin is not suitable for your weapon type.");
								player.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showMySkinList(player, page);
								return;
							}
							
							player.setWeaponSkinOption(id);
						}
						break;
					case "hair":
						player.setHairSkinOption(id);
						break;
					case "face":
						player.setFaceSkinOption(id);
						break;
					case "shield":
						if (player.getActiveWeaponItem() == null)
						{
							player.sendMessage("You can't use this skin without a weapon.");
							player.sendPacket(new ExShowScreenMessage("You can't use this skin without a weapon.", 2000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							showMySkinList(player, page);
							return;
						}
						
						ItemInstance skinShield = null;
						if (ItemTable.getInstance().getTemplate(itemId) != null)
						{
							skinShield = ItemTable.getInstance().createDummyItem(itemId);
							
							if (!checkWeapons(player, skinShield, WeaponType.BOW, WeaponType.BOW) //
								|| !checkWeapons(player, skinShield, WeaponType.POLE, WeaponType.POLE) //
								|| !checkWeapons(player, skinShield, WeaponType.DUAL, WeaponType.DUAL) //
								|| !checkWeapons(player, skinShield, WeaponType.DUALFIST, WeaponType.DUALFIST) //
								|| !checkWeapons(player, skinShield, WeaponType.BIGSWORD, WeaponType.BIGSWORD) //
								|| !checkWeapons(player, skinShield, WeaponType.FIST, WeaponType.FIST) //
								|| !checkWeapons(player, skinShield, WeaponType.BIGBLUNT, WeaponType.BIGBLUNT))
							{
								player.sendMessage("This skin is not suitable for your weapon type.");
								player.sendPacket(new ExShowScreenMessage("This skin is not suitable for your weapon type.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								showMySkinList(player, page);
								return;
							}
							
							player.setShieldSkinOption(id);
						}
						
						break;
				}
				
				player.broadcastUserInfo();
				showMySkinList(player, page);
			}

		
		}
		else if (_command.startsWith("player_help "))
		{
			final String path = _command.substring(12);
			if (path.indexOf("..") != -1)
				return;
			
			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/help/" + cmd[0]);
			if (cmd.length > 1)
			{
				final int itemId = Integer.parseInt(cmd[1]);
				html.setItemId(itemId);
				
				if (itemId == 7064 && cmd[0].equalsIgnoreCase("lidias_diary/7064-16.htm"))
				{
					final QuestState qs = player.getQuestState("Q023_LidiasHeart");
					if (qs != null && qs.getInt("cond") == 5 && qs.getInt("diary") == 0)
						qs.set("diary", "1");
				}
			}
			html.disableValidation();
			player.sendPacket(html);
		}
		else if (_command.startsWith("tele_tournament"))
		{
			if (player.isOlympiadProtection())
			{
				player.sendMessage("Are you participating in the Olympiad..");
				return;
			}
			
			for (Gatekeeper knownChar : player.getKnownTypeInRadius(Gatekeeper.class, 300))
			{
				if (knownChar != null)
				{
					player.teleportTo(Config.Tournament_locx + Rnd.get(-100, 100), Config.Tournament_locy + Rnd.get(-100, 100), Config.Tournament_locz, 0);
					player.setTournamentTeleport(true);
				}
			}
		}		
		else if (_command.startsWith("_autofarm"))
		{
			if (player.isAutoFarm())
			{
				VoicedAutoFarm.stopAutoFarm(player, null);
			}
			else
			{
				if (player.isDead())
				{
					player.sendMessage("No podes activar Auto Farm estando muerto.");
					return;
				}
				if (player.isInsideZone(net.sf.l2j.gameserver.enums.ZoneId.PEACE))
				{
					player.sendMessage("No podes activar Auto Farm en zona peace.");
					return;
				}
				if (!VoicedAutoFarm.canStartAutoFarm(player))
					return;

				bot.start();
				player.setAutoFarm(true);
				if (!VoicedAutoFarm._playersFarm.contains(player))
					VoicedAutoFarm._playersFarm.add(player);
			}
			
		}
		
		if (_command.startsWith("_pageAutoFarm"))
		{
			StringTokenizer st = new StringTokenizer(_command, " ");
			st.nextToken();
			try
			{
				String param = st.nextToken();
				
				if (param.startsWith("inc_page") || param.startsWith("dec_page"))
				{
					int newPage;
					
					if (param.startsWith("inc_page"))
					{
						newPage = player.getPage() + 1;
					}
					else
					{
						newPage = player.getPage() - 1;
					}
					
					if (newPage >= 0 && newPage <= 9)
					{
						String[] pageStrings =
						{
							"F1",
							"F2",
							"F3",
							"F4",
							"F5",
							"F6",
							"F7",
							"F8",
							"F9",
							"F10"
						};
						
						player.setPage(newPage);
						player.sendPacket(new ExShowScreenMessage("Auto Farm Skill Bar " + pageStrings[newPage], 3 * 1000, SMPOS.TOP_CENTER, false));
						player.saveAutoFarmSettings();
						
					}
					
				}
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		else if (_command.startsWith("voiced_"))
        {
            String command = _command.split(" ")[0];

            IVoicedCommandHandler ach = VoicedCommandHandler.getInstance().getHandler(_command.substring(7));

            if (ach == null)
            {
            	player.sendMessage("The command " + command.substring(7) + " does not exist!");
                LOGGER.warn("No handler registered for command '" + _command + "'");
                return;
            }
            ach.useVoicedCommand(_command.substring(7), player, null);
        }
		else if (SellBuffBypassHandler.isSellBuffCommand(_command))
		{
			SellBuffBypassHandler.handleBypass(_command, player);
		}
		else if (RaidInfoBypassHandler.isRaidInfoCommand(_command))
		{
			RaidInfoBypassHandler.handleBypass(_command, player);
		}
		else if (_command.startsWith("npc_"))
		{
			if (!player.validateBypass(_command))
				return;
			
			player.setIsUsingCMultisell(false);
			
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);
			
			try
			{
				final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
				
				if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(player))
					((Npc) object).onBypassFeedback(player, _command.substring(endOfId + 1));
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			WorldObject object = player.getTarget();
			if (object instanceof Npc)
				((Npc) object).onBypassFeedback(player, _command);
		}
		else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
		{
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		}
		else if (_command.startsWith("Quest "))
		{
			if (!player.validateBypass(_command))
				return;
			
			String[] str = _command.substring(6).trim().split(" ", 2);
			if (str.length == 1)
				player.processQuestEvent(str[0], "");
			else
				player.processQuestEvent(str[0], str[1]);
		}
		else if (_command.startsWith("_match"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroFights(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("_diary"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("skin"))
		{
			MultiManager.getInstance().Skin(_command, player);
		}
		else if (_command.startsWith("active"))
		{
			MultiManager.getInstance().ActiveSkill(_command, player);
		}
		else if (_command.startsWith("passive"))
		{
			MultiManager.getInstance().PassiveSkill(_command, player);
		}
		else if (_command.startsWith("services"))
		{
			MultiManager.getInstance().Page(_command, player);
		}
		else if (_command.startsWith("namecolors"))
		{
			MultiManager.getInstance().showNameColorManager(player);
		}
		else if (_command.startsWith("titlecolors"))
		{
			MultiManager.getInstance().showTitleColorManager(player);
		}
		else if (net.sf.l2j.gameserver.donate.DonateSkillEnchant.isShopBypass(_command))
		{
			// SanSara: Donate Shop - Skill Enchant al maximo
			if (!net.sf.l2j.gameserver.events.eventgear.EventGearManager.blockAction(player))
				net.sf.l2j.gameserver.donate.DonateSkillEnchant.onBypass(player, _command);
		}
		else if (net.sf.l2j.gameserver.donate.DonateAttributeShop.isShopBypass(_command) && net.sf.l2j.gameserver.events.eventgear.EventGearManager.blockAction(player))
		{
			// Event Gear: el Donate Shop de sockets/elementos no se usa durante el evento.
		}
		else if (net.sf.l2j.gameserver.donate.DonateAttributeShop.isShopBypass(_command))
		{
			// SanSara: Donate Shop - Socket Perfecto / Elemento Perfecto
			net.sf.l2j.gameserver.donate.DonateAttributeShop.onBypass(player, _command);
		}
		else if (_command.equals("donaciones"))
		{
			MultiManager.getInstance().showDonaciones(player);
		}
		else if (_command.equals("hero"))
		{
			DonationManager.buyHero30(player);
		}
		else if (_command.equals("nobles"))
		{
			DonationManager.buyNobles(player);
		}
		else if (_command.equals("combo"))
		{
			DonationManager.buyCombo30(player);
		}
		else if (_command.startsWith("namecolor"))
		{
			MultiManager.getInstance().NameColor(_command, player);
		}
		else if (_command.startsWith("titlecolor"))
		{
			MultiManager.getInstance().TitleColor(_command, player);
		}
		else if (_command.startsWith("linkface"))
		{
			Franchus.getInstance().LinkFace(_command, player);
		}
		else if (_command.startsWith("linkweb"))
		{
			Franchus.getInstance().LinkWeb(_command, player);
		}
		else if (_command.startsWith("linkdiscord"))
		{
			Franchus.getInstance().LinkDiscord(_command, player);
		}
		else if (_command.startsWith("linkgrupo"))
		{
			Franchus.getInstance().LinkGrupo(_command, player);
		}
		else if (_command.startsWith("arenachange")) // change
		{
			final boolean isManager = player.getCurrentFolk() instanceof OlympiadManagerNpc;
			if (!isManager)
			{
				// Without npc, command can be used only in observer mode on arena
				if (!player.isInObserverMode() || player.isInOlympiadMode() || player.getOlympiadGameId() < 0)
					return;
			}

			if (OlympiadManager.getInstance().isRegisteredInComp(player))
			{
				player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
				return;
			}

			final int arenaId = Integer.parseInt(_command.substring(12).trim());
			player.enterOlympiadObserverMode(arenaId);
		}
		else if (_command.startsWith("tournament_observe"))
		{
/*			if (player._inEventTvT)
			{
				player.sendMessage("You already participated in the event tvt/ctf/pvp event!");
				return;
			}
	*/		
			StringTokenizer st = new StringTokenizer(_command);
			st.nextToken();
			
			final int x = Integer.parseInt(st.nextToken());
			final int y = Integer.parseInt(st.nextToken());
			final int z = Integer.parseInt(st.nextToken());
			// if ((x == -114413 && y == -213241 && z == -3331) || (x == -81748 && y == -245950 && z == -3331) || (x == -120324 && y == -225077 && z == -3331) && (activeChar.isInsideZone(ZoneId.TOURNAMENT) || activeChar.isInsideZone(ZoneId.ARENA_EVENT)))
			// {
			player.setArenaObserv(true);
			player.enterTvTObserverMode(x, y, z);
			// }
		}
	}

	public static String getItemNameById(int itemId)
	{
		Item item = ItemTable.getInstance().getTemplate(itemId);
		
		String itemName = "NoName";
		
		if (itemId != 0)
		{
			itemName = item.getName();
		}
		
		return itemName;
	}


	public static void showDressMeMainPage(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		String text = HtmCache.getInstance().getHtm("data/html/dressme/index.htm");
		
		htm.setHtml(text);
		
		{
			htm.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
			htm.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
			
		}
		
		player.sendPacket(htm);
	}

	static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

	private static void showSkinList(Player player, String type, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		
		html.setFile("data/html/dressme/allskins.htm");
		
		html.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
		html.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
		
		final int ITEMS_PER_PAGE = 8;
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		int itemId = 0;
		
		final StringBuilder sb = new StringBuilder();
		
		List<SkinPackage> tempList = null;
		switch (type.toLowerCase())
		{
			case "armor":
				tempList = DressMeData.getInstance().getArmorSkinOptions().values().stream().filter(s -> !player.hasArmorSkin(s.getId())).collect(Collectors.toList());
				break;
			case "weapon":
				tempList = DressMeData.getInstance().getWeaponSkinOptions().values().stream().filter(s -> !player.hasWeaponSkin(s.getId())).collect(Collectors.toList());
				break;
			case "hair":
				tempList = DressMeData.getInstance().getHairSkinOptions().values().stream().filter(s -> !player.hasHairSkin(s.getId())).collect(Collectors.toList());
				break;
			case "face":
				tempList = DressMeData.getInstance().getFaceSkinOptions().values().stream().filter(s -> !player.hasFaceSkin(s.getId())).collect(Collectors.toList());
				break;
			case "shield":
				tempList = DressMeData.getInstance().getShieldSkinOptions().values().stream().filter(s -> !player.hasShieldSkin(s.getId())).collect(Collectors.toList());
				break;
		}
		
		if (tempList != null && !tempList.isEmpty())
		{
			for (SkinPackage sp : tempList)
			{
				if (sp == null)
				{
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				if (myPage != page)
				{
					i++;
					if (i == ITEMS_PER_PAGE)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				switch (type.toLowerCase())
				{
					case "armor":
						itemId = sp.getChestId();
						break;
					case "weapon":
						itemId = sp.getWeaponId();
						break;
					case "hair":
						itemId = sp.getHairId();
						break;
					case "face":
						itemId = sp.getFaceId();
						break;
					case "shield":
						itemId = sp.getShieldId();
						break;
				}
				
				sb.append("<table border=0 cellspacing=0 cellpadding=2 height=36><tr>");
				sb.append("<td width=32 align=center>" + "<button width=32 height=32 back=" + Item.getItemIcon(itemId) + " fore=" + Item.getItemIcon(itemId) + ">" + "</td>");
				sb.append("<td width=124>" + sp.getName() + "<br1> <font color=999999>Price:</font> <font color=339966>" + Item.getItemNameById(sp.getPriceId()) + "</font> (<font color=LEVEL>" + sp.getPriceCount() + "</font>)</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Buy\" action=\"bypass -h dressme " + page + " buyskin  " + sp.getId() + " " + type + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Try\" action=\"bypass -h dressme " + page + " tryskin  " + sp.getId() + " " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				
				sb.append("</tr></table>");
				sb.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
				shown++;
			}
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=center width=70>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass -h dressme " + (page - 1) + " skinlist " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=140>Page: " + page + "</td>");
		sb.append("<td align=center width=70>" + (hasMore ? "<button value=\"NEXT >\" action=\"bypass -h dressme " + (page + 1) + " skinlist " + type + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%showList%", sb.toString());
		player.sendPacket(html);
	}

	private static void showMySkinList(Player player, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/dressme/myskins.htm");
		
		html.replace("%time%", sdf.format(new Date(System.currentTimeMillis())));
		html.replace("%dat%", (new SimpleDateFormat("dd/MM/yyyy")).format(new Date(System.currentTimeMillis())));
		
		final int ITEMS_PER_PAGE = 8;
		int itemId = 0;
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		
		final StringBuilder sb = new StringBuilder();
		
		List<SkinPackage> armors = DressMeData.getInstance().getArmorSkinOptions().values().stream().filter(s -> player.hasArmorSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> weapons = DressMeData.getInstance().getWeaponSkinOptions().values().stream().filter(s -> player.hasWeaponSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> hairs = DressMeData.getInstance().getHairSkinOptions().values().stream().filter(s -> player.hasHairSkin(s.getId())).collect(Collectors.toList());
		// List<SkinPackage> faces = DressMeData.getInstance().getFaceSkinOptions().values().stream().filter(s -> player.hasFaceSkin(s.getId())).collect(Collectors.toList());
		List<SkinPackage> shield = DressMeData.getInstance().getShieldSkinOptions().values().stream().filter(s -> player.hasShieldSkin(s.getId())).collect(Collectors.toList());
		
		List<SkinPackage> list = Stream.concat(armors.stream(), weapons.stream()).collect(Collectors.toList());
		shield.stream().collect(Collectors.toList());
		List<SkinPackage> list2 = Stream.concat(hairs.stream(), shield.stream()).collect(Collectors.toList());
		
		List<SkinPackage> allLists = Stream.concat(list.stream(), list2.stream()).collect(Collectors.toList());
		
		if (!allLists.isEmpty())
		{
			for (SkinPackage sp : allLists)
			{
				if (sp == null)
				{
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				if (myPage != page)
				{
					i++;
					if (i == ITEMS_PER_PAGE)
					{
						myPage++;
						i = 0;
					}
					continue;
				}
				
				if (shown == ITEMS_PER_PAGE)
				{
					hasMore = true;
					break;
				}
				
				switch (sp.getType().toLowerCase())
				{
					case "armor":
						itemId = sp.getChestId();
						break;
					case "weapon":
						itemId = sp.getWeaponId();
						break;
					case "hair":
						itemId = sp.getHairId();
						break;
					case "face":
						itemId = sp.getFaceId();
						break;
					case "shield":
						itemId = sp.getShieldId();
						break;
				}
				
				sb.append("<table border=0 cellspacing=0 cellpadding=2 height=36><tr>");
				sb.append("<td width=32 align=center>" + "<button width=32 height=32 back=" + Item.getItemIcon(itemId) + " fore=" + Item.getItemIcon(itemId) + ">" + "</td>");
				sb.append("<td width=124>" + sp.getName() + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Equip\" action=\"bypass -h dressme " + page + " setskin " + sp.getId() + " " + sp.getType() + " " + itemId + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("<td align=center width=65>" + "<button value=\"Remove\" action=\"bypass -h dressme " + page + " clean " + sp.getType() + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" + "</td>");
				sb.append("</tr></table>");
				sb.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
				shown++;
			}
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=center width=70>" + (page > 1 ? "<button value=\"< PREV\" action=\"bypass -h dressme " + (page - 1) + " myskinlist\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=140>Page: " + page + "</td>");
		sb.append("<td align=center width=70>" + (hasMore ? "<button value=\"NEXT >\" action=\"bypass -h dressme " + (page + 1) + " myskinlist\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		html.replace("%showList%", sb.toString());
		player.sendPacket(html);
	}

	public static boolean checkWeapons(Player player, ItemInstance skin, WeaponType weapon1, WeaponType weapon2)
	{
		if (player.getActiveWeaponItem().getItemType() == weapon1 && skin.getItem().getItemType() != weapon2)
		{
			return false;
		}
		
		return true;
	}

}