package net.sf.l2j.gameserver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.Config;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.MultisellData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.VoicedAutoFarm;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * SanSara - Comandos que manda la interface ACDC (Interlude Essence).<br>
 * La interface original estaba hecha para Lucera (scripts_services.Interface, _bpUI, _LotusUI, BuffEngine...). Aca se traducen todos a lo que ya tiene SanSara (aCis).<br>
 * <ul>
 * <li>Config: config/HadesProject/InterfaceACDC.properties (se recarga sola si cambias el archivo).</li>
 * <li>Teleports de la ventana Teleport2k2Wnd: data/xml/teleportLocationsinterface.xml (los id tienen que coincidir con el teleport.ini del cliente).</li>
 * <li>Seguridad: openMultisell y openHtml solo abren lo que esta en la lista blanca del properties.</li>
 * </ul>
 */
public final class InterfaceBypassHandler
{
	private static final CLogger LOGGER = new CLogger(InterfaceBypassHandler.class.getName());

	private static final String CONFIG_FILE = "./config/HadesProject/InterfaceACDC.properties";
	private static final String TELEPORT_FILE = "./data/xml/teleportLocationsinterface.xml";

	// Config
	private static boolean _enabled = true;
	private static boolean _teleportEnabled = true;
	private static int _teleportItemId = 57;
	private static boolean _teleportBlockFlag = true;
	private static boolean _teleportBlockPk = true;
	private static boolean _teleportBlockCombat = true;
	private static boolean _teleportOnlyPeace = false;
	private static int _teleportMinLevel = 1;
	private static int _teleportFreeLevel = 0;
	private static int _teleportEffectSkill = 2036;
	private static boolean _dispelEnabled = true;
	private static boolean _dispelDances = true;
	private static String _donateCommand = "_bbsdonate";
	private static String _rankingCommand = "_bbsrank";
	private static String _bossCommand = "raidinfo;easy";
	private static boolean _augmentEnabled = true;
	private static boolean _augmentOnlyPeace = true;
	private static Set<String> _allowedMultisells = Collections.emptySet();
	private static Set<String> _allowedHtmls = Collections.emptySet();
	private static int _radiusStep = 200;

	private static long _configStamp = -1;
	private static long _teleportStamp = -1;
	private static final Map<Integer, TeleLoc> _teleports = new HashMap<>();

	private static final class TeleLoc
	{
		final int x;
		final int y;
		final int z;
		final int price;
		final boolean noble;
		final int effect;

		TeleLoc(int x, int y, int z, int price, boolean noble, int effect)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.price = price;
			this.noble = noble;
			this.effect = effect;
		}
	}

	private InterfaceBypassHandler()
	{
	}

	/**
	 * @param command : el bypass que llego del cliente.
	 * @return true si es un comando de la interface ACDC.
	 */
	public static boolean isInterfaceCommand(String command)
	{
		return command.startsWith("scripts_services.") || command.startsWith("BuffEngine_Dispel=") || command.startsWith("_bpUI ") || command.startsWith("_LotusUI ") || command.startsWith("GetDropInfo") || command.startsWith("goto ") || command.startsWith("_radiusAutoFarm") || command.equals("_enableSummonAttack") || command.equals("_enableBuffProtect") || command.equals("_enableRespectHunt") || command.equals("_infosettings") || command.equals("_instancesinterface") || command.equals("_huntpassinterface") || command.equals("_specialcraftinterface");
	}

	public static void handle(Player player, String command, GameClient client)
	{
		reloadIfChanged();
		if (!_enabled)
			return;

		try
		{
			// --- scripts_services.Interface:xxx (Lucera) ---
			if (command.startsWith("scripts_services.Interface:"))
			{
				String rest = command.substring("scripts_services.Interface:".length()).trim();
				String name = rest;
				String args = "";
				final int sp = rest.indexOf(' ');
				if (sp > 0)
				{
					name = rest.substring(0, sp);
					args = rest.substring(sp + 1).trim();
				}
				// "GkGo: 5" / "autoShot: ..." vienen con dos puntos
				if (name.endsWith(":"))
					name = name.substring(0, name.length() - 1);

				switch (name)
				{
					case "autoFarm":
						toggleAutoFarm(player);
						break;
					case "autoFarmInfo":
						autoFarmInfo(player);
						break;
					case "radiusAutoFarm":
						radiusAutoFarm(player, args);
						break;
					case "GkGo":
						teleport(player, args);
						break;
					case "bossStatus":
						RaidInfoBypassHandler.show(player, _bossCommand);
						break;
					case "augmentOpen":
						openAugment(player);
						break;
					case "openHtml":
						openHtml(player, args);
						break;
					case "interfaceDonate":
						community(player, client, _donateCommand);
						break;
					case "dispellEffect":
						dispel(player, args);
						break;
					case "openMultisell":
						openMultisell(player, args);
						break;
					case "autoShot":
						// Auto soulshots de la interface: eliminado en SanSara (se usa el auto shot normal del cliente).
						break;
					default:
						unknown(player, command);
						break;
				}
			}
			else if (command.startsWith("scripts_services.TopPvPPKService:"))
				community(player, client, _rankingCommand);
			else if (command.startsWith("BuffEngine_Dispel="))
				dispel(player, command.substring("BuffEngine_Dispel=".length()));
			else if (command.startsWith("_bpUI ") || command.startsWith("_LotusUI "))
			{
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				final String sub = st.hasMoreTokens() ? st.nextToken() : "";
				final String args = st.hasMoreTokens() ? command.substring(command.indexOf(sub) + sub.length()).trim() : "";
				if (sub.equalsIgnoreCase("teleport"))
					teleport(player, args);
				else if (sub.equalsIgnoreCase("buffDispel") || sub.equalsIgnoreCase("RemoveBuff"))
					dispel(player, args);
				else if (sub.equalsIgnoreCase("autofarm") || sub.equalsIgnoreCase("FarmInfo"))
					VoicedAutoFarm.showAutoFarm(player);
				else
					unknown(player, command);
			}
			else if (command.startsWith("goto "))
				teleport(player, command.substring(5));
			else if (command.startsWith("_radiusAutoFarm"))
				radiusAutoFarm(player, command.substring("_radiusAutoFarm".length()).trim());
			else if (command.equals("_infosettings"))
				VoicedAutoFarm.showAutoFarm(player);
			else if (command.startsWith("GetDropInfo"))
			{
				// La ventana de drop lee mobdrop.ini / mobspoil.ini del cliente: no hace falta nada del server.
			}
			else
				player.sendMessage("Esta opcion todavia no esta disponible en SanSara.");
		}
		catch (Exception e)
		{
			LOGGER.warn("InterfaceACDC: error con el comando '{}' de {}.", e, command, player.getName());
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void unknown(Player player, String command)
	{
		if (player.isGM())
			player.sendMessage("InterfaceACDC: comando sin uso en SanSara: " + command);
	}

	// ------------------------------------------------------------------------
	// Auto Farm (usa el auto farm que ya tiene SanSara)
	// ------------------------------------------------------------------------

	private static void toggleAutoFarm(Player player)
	{
		if (!Config.AUTOFARM_ENABLED)
		{
			player.sendMessage("Auto Farm esta desactivado.");
			return;
		}

		if (player.isAutoFarm())
		{
			VoicedAutoFarm.stopAutoFarm(player, null);
			player.sendPacket(new ExShowScreenMessage("Auto Farm OFF", 3000, SMPOS.TOP_CENTER, false));
			return;
		}

		if (player.isDead())
		{
			player.sendMessage("No podes activar Auto Farm estando muerto.");
			return;
		}
		if (player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("No podes activar Auto Farm en zona peace.");
			return;
		}
		if (!VoicedAutoFarm.canStartAutoFarm(player))
			return;
		if (Config.AUTOFARM_TIME_ENABLED && player.getAutoFarmTotalLeft() <= 0)
		{
			player.sendMessage("No te queda tiempo de Auto Farm. Escribi .farm para sumar tiempo.");
			return;
		}

		player.getBot().start();
		player.setAutoFarm(true);
		player.applyAutoFarmVisual();
		if (!VoicedAutoFarm._playersFarm.contains(player))
			VoicedAutoFarm._playersFarm.add(player);
		player.sendPacket(new ExShowScreenMessage("Auto Farm ON", 3000, SMPOS.TOP_CENTER, false));
	}

	private static void autoFarmInfo(Player player)
	{
		final String time = Config.AUTOFARM_TIME_ENABLED ? Player.formatAutoFarmTime(player.getAutoFarmTotalLeft()) : "Ilimitado";
		player.sendPacket(new ExShowScreenMessage("Auto Farm " + (player.isAutoFarm() ? "ON" : "OFF") + " | Radio: " + player.getRadius() + " | Tiempo: " + time, 4000, SMPOS.TOP_CENTER, false));
	}

	private static void radiusAutoFarm(Player player, String args)
	{
		if (args.startsWith("inc_radius"))
			player.setRadius(player.getRadius() + _radiusStep);
		else if (args.startsWith("dec_radius"))
			player.setRadius(player.getRadius() - _radiusStep);
		else
			return;
		player.saveAutoFarmSettings();
		player.sendPacket(new ExShowScreenMessage("Auto Farm Radio: " + player.getRadius(), 2000, SMPOS.TOP_CENTER, false));
	}

	// ------------------------------------------------------------------------
	// Teleport (ventana Teleport2k2Wnd)
	// ------------------------------------------------------------------------

	private static void teleport(Player player, String args)
	{
		if (!_teleportEnabled)
		{
			player.sendMessage("El teleport de la interface esta desactivado.");
			return;
		}

		// Llega "_5", "5" o "GkGo: _5": nos quedamos con el numero
		final String digits = args.replaceAll("[^0-9]", "");
		if (digits.isEmpty())
			return;
		final int id = Integer.parseInt(digits.length() > 9 ? digits.substring(0, 9) : digits);

		reloadTeleportsIfChanged();
		final TeleLoc loc = _teleports.get(id);
		if (loc == null)
		{
			player.sendMessage("La ubicacion de teletransporte no existe.");
			return;
		}

		if (player.isDead() || player.isAlikeDead())
		{
			player.sendMessage("No podes usar el teleport muerto.");
			return;
		}
		if (player.isSitting())
		{
			player.sendMessage("No podes usar el teleport sentado.");
			return;
		}
		if (player.isInJail() || player.isInsideZone(ZoneId.JAIL))
		{
			player.sendMessage("No podes usar el teleport en la carcel.");
			return;
		}
		if (player.isInOlympiadMode() || player.isOlympiadProtection())
		{
			player.sendMessage("No podes usar el teleport en olimpiadas.");
			return;
		}
		if (player.isInDuel())
		{
			player.sendMessage("No podes usar el teleport en duelo.");
			return;
		}
		if (player.isInObserverMode() || player.isInStoreMode() || player.isProcessingTransaction() || player.isTeleporting() || player.isCastingNow() || player.isFlying() || player.isMounted() || player.isInBoat() || player.isFestivalParticipant() || player.isCursedWeaponEquipped())
		{
			player.sendMessage("Ahora no podes usar el teleport.");
			return;
		}
		if (_teleportBlockCombat && player.isInCombat())
		{
			player.sendMessage("No podes usar el teleport en combate.");
			return;
		}
		if (_teleportBlockFlag && player.getPvpFlag() > 0)
		{
			player.sendMessage("Estas en modo flag, no podes usar el teleport.");
			return;
		}
		if (_teleportBlockPk && player.getKarma() > 0)
		{
			player.sendMessage("Estas en modo PK, no podes usar el teleport.");
			return;
		}
		if (_teleportOnlyPeace && !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("El teleport solo se puede usar en zona peace.");
			return;
		}
		if (player.isInsideZone(ZoneId.BOSS) || player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendMessage("No podes usar el teleport en esta zona.");
			return;
		}
		if (player.getLevel() < _teleportMinLevel)
		{
			player.sendMessage("Necesitas nivel " + _teleportMinLevel + " para usar el teleport.");
			return;
		}
		if (loc.noble && !player.isNoble())
		{
			player.sendMessage("Solo los nobles pueden ir a esta zona.");
			return;
		}

		final int price = (player.getLevel() <= _teleportFreeLevel) ? 0 : loc.price;
		if (price > 0 && !player.destroyItemByItemId("InterfaceTeleport", _teleportItemId, price, player, true))
		{
			player.sendMessage("Necesitas " + price + " " + itemName(_teleportItemId) + " para este teleport.");
			return;
		}

		final int effect = (loc.effect > 0) ? loc.effect : _teleportEffectSkill;
		if (effect > 0)
			player.broadcastPacket(new MagicSkillUse(player, player, effect, 1, 0, 0));
		player.teleportTo(loc.x, loc.y, loc.z, 20);
	}

	// ------------------------------------------------------------------------
	// Sacar un buff haciendo click (ventana de buffs)
	// ------------------------------------------------------------------------

	private static void dispel(Player player, String args)
	{
		if (!_dispelEnabled)
			return;

		final String digits = args.trim().split("[^0-9]")[0];
		if (digits.isEmpty())
			return;
		int skillId = Integer.parseInt(digits);
		// La ventana de debuffs le suma 1000000 al id
		if (skillId >= 1000000)
			skillId -= 1000000;

		for (L2Effect e : player.getAllEffects())
		{
			if (e == null || e.getSkill() == null || e.getSkill().getId() != skillId)
				continue;

			final L2Skill skill = e.getSkill();
			// Solo buffs: nada de debuffs, toggles, pasivas, ni pociones/hierbas
			if (skill.isDebuff() || skill.isOffensive() || skill.isToggle() || skill.isPassive() || skill.isPotion())
				return;
			if (skill.isDance() && !_dispelDances)
				return;

			e.exit();
		}
	}

	// ------------------------------------------------------------------------
	// Ventanas
	// ------------------------------------------------------------------------

	private static void openAugment(Player player)
	{
		if (!_augmentEnabled)
		{
			player.sendMessage("El augment desde la interface esta desactivado.");
			return;
		}
		if (_augmentOnlyPeace && !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("El augment solo se puede abrir en zona peace.");
			return;
		}
		if (player.isInCombat() || player.isInOlympiadMode() || player.isInStoreMode())
		{
			player.sendMessage("Ahora no podes abrir el augment.");
			return;
		}
		player.sendPacket(ExShowVariationMakeWindow.STATIC_PACKET);
	}

	/** Seguridad: solo se abren los html de la lista blanca (InterfaceAllowedHtml). */
	private static void openHtml(Player player, String args)
	{
		String path = args.trim().replace('\\', '/');
		if (path.isEmpty())
			return;

		final String key = path.toLowerCase();
		if (path.contains("..") || path.startsWith("/") || path.indexOf(':') >= 0 || !(key.endsWith(".htm") || key.endsWith(".html")) || !_allowedHtmls.contains(key))
		{
			LOGGER.warn("InterfaceACDC: {} intento abrir un html que no esta permitido: {}", player.getName(), path);
			return;
		}

		final String file = "data/html/" + path;
		if (!HtmCache.getInstance().isLoadable(file))
		{
			player.sendMessage("Esta opcion todavia no esta disponible.");
			return;
		}
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(file);
		html.replace("%charname%", player.getName());
		player.sendPacket(html);
	}

	/** Seguridad: solo se abren los multisell de la lista blanca (InterfaceAllowedMultisells). */
	private static void openMultisell(Player player, String args)
	{
		final String list = args.trim();
		if (list.isEmpty() || !_allowedMultisells.contains(list.toLowerCase()))
		{
			LOGGER.warn("InterfaceACDC: {} intento abrir el multisell '{}' que no esta en la lista permitida.", player.getName(), list);
			return;
		}
		if (player.isInCombat() || player.isInOlympiadMode() || player.isInStoreMode() || player.isDead())
		{
			player.sendMessage("Ahora no podes abrir la tienda.");
			return;
		}
		MultisellData.getInstance().separateAndSend(list, player, null, false);
	}

	private static void community(Player player, GameClient client, String command)
	{
		if (command == null || command.isEmpty())
			return;
		if (command.startsWith("_bbs"))
			CommunityBoard.getInstance().handleCommands(client, command);
		else if (command.startsWith("raidinfo"))
			RaidInfoBypassHandler.show(player, command);
		else
		{
			final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler(command);
			if (vch != null)
				vch.useVoicedCommand(command, player, null);
		}
	}

	private static String itemName(int itemId)
	{
		final Item item = ItemTable.getInstance().getTemplate(itemId);
		return (item != null) ? item.getName() : "Adena";
	}

	// ------------------------------------------------------------------------
	// Carga de config y teleports
	// ------------------------------------------------------------------------

	public static synchronized void reload()
	{
		_configStamp = -1;
		_teleportStamp = -1;
		reloadIfChanged();
		reloadTeleportsIfChanged();
	}

	private static synchronized void reloadIfChanged()
	{
		final File f = new File(CONFIG_FILE);
		final long stamp = f.exists() ? f.lastModified() : 0;
		if (stamp == _configStamp)
			return;
		_configStamp = stamp;

		final Properties p = new Properties();
		if (f.exists())
		{
			try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))
			{
				p.load(r);
			}
			catch (Exception e)
			{
				LOGGER.warn("InterfaceACDC: no se pudo leer {}.", e, CONFIG_FILE);
			}
		}

		_enabled = bool(p, "InterfaceEnabled", true);
		_teleportEnabled = bool(p, "TeleportEnabled", true);
		_teleportItemId = integer(p, "TeleportItemId", 57);
		_teleportBlockFlag = bool(p, "TeleportBlockFlag", true);
		_teleportBlockPk = bool(p, "TeleportBlockPk", true);
		_teleportBlockCombat = bool(p, "TeleportBlockCombat", true);
		_teleportOnlyPeace = bool(p, "TeleportOnlyPeace", false);
		_teleportMinLevel = integer(p, "TeleportMinLevel", 1);
		_teleportFreeLevel = integer(p, "TeleportFreeUntilLevel", 0);
		_teleportEffectSkill = integer(p, "TeleportEffectSkill", 2036);
		_dispelEnabled = bool(p, "BuffDispelEnabled", true);
		_dispelDances = bool(p, "BuffDispelDances", true);
		_donateCommand = p.getProperty("DonateCommand", "_bbsdonate").trim();
		_rankingCommand = p.getProperty("RankingCommand", "_bbsrank").trim();
		_bossCommand = p.getProperty("BossCommand", "raidinfo;easy").trim();
		_augmentEnabled = bool(p, "AugmentEnabled", true);
		_augmentOnlyPeace = bool(p, "AugmentOnlyPeace", true);
		_radiusStep = integer(p, "AutoFarmRadiusStep", 200);
		_allowedMultisells = list(p, "InterfaceAllowedMultisells");
		_allowedHtmls = list(p, "InterfaceAllowedHtml");

		LOGGER.info("InterfaceACDC: config cargada ({} multisell y {} html permitidos).", _allowedMultisells.size(), _allowedHtmls.size());
	}

	private static synchronized void reloadTeleportsIfChanged()
	{
		final File f = new File(TELEPORT_FILE);
		final long stamp = f.exists() ? f.lastModified() : 0;
		if (stamp == _teleportStamp)
			return;
		_teleportStamp = stamp;
		_teleports.clear();
		if (!f.exists())
		{
			LOGGER.warn("InterfaceACDC: falta {}.", TELEPORT_FILE);
			return;
		}

		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setExpandEntityReferences(false);
			final Document doc = factory.newDocumentBuilder().parse(f);
			for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if (!"list".equalsIgnoreCase(list.getNodeName()))
					continue;
				for (Node n = list.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if (!"teleport".equalsIgnoreCase(n.getNodeName()))
						continue;
					final NamedNodeMap a = n.getAttributes();
					final int id = Integer.parseInt(a.getNamedItem("id").getNodeValue());
					final int x = Integer.parseInt(a.getNamedItem("x").getNodeValue());
					final int y = Integer.parseInt(a.getNamedItem("y").getNodeValue());
					final int z = Integer.parseInt(a.getNamedItem("z").getNodeValue());
					final int price = (a.getNamedItem("price") != null) ? Integer.parseInt(a.getNamedItem("price").getNodeValue()) : 0;
					final boolean noble = (a.getNamedItem("isNoble") != null) && Boolean.parseBoolean(a.getNamedItem("isNoble").getNodeValue());
					final int effect = (a.getNamedItem("SkillEffectId") != null) ? Integer.parseInt(a.getNamedItem("SkillEffectId").getNodeValue()) : 0;
					_teleports.put(id, new TeleLoc(x, y, z, price, noble, effect));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("InterfaceACDC: error leyendo {}.", e, TELEPORT_FILE);
		}
		LOGGER.info("InterfaceACDC: {} teleports de la interface cargados.", _teleports.size());
	}

	private static boolean bool(Properties p, String key, boolean def)
	{
		final String v = p.getProperty(key);
		return (v == null) ? def : Boolean.parseBoolean(v.trim());
	}

	private static int integer(Properties p, String key, int def)
	{
		try
		{
			final String v = p.getProperty(key);
			return (v == null) ? def : Integer.parseInt(v.trim());
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}

	private static Set<String> list(Properties p, String key)
	{
		final Set<String> set = new HashSet<>();
		final String v = p.getProperty(key, "");
		for (String s : v.split("[,;]"))
		{
			final String t = s.trim().replace('\\', '/').toLowerCase();
			if (!t.isEmpty())
				set.add(t);
		}
		return set;
	}
}
