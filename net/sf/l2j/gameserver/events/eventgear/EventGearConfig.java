package net.sf.l2j.gameserver.events.eventgear;

import java.io.File;
import java.util.logging.Logger;

import net.sf.l2j.commons.config.ExProperties;

/**
 * Configuracion del Event Gear (set fijo + nick "SanSara" en TvT / DM / LM / CTF).<br>
 * Archivo: config/HadesProject/Events/EventGear.properties
 */
public final class EventGearConfig
{
	private static final Logger LOG = Logger.getLogger(EventGearConfig.class.getName());

	public static final String FILE = "./config/HadesProject/Events/EventGear.properties";

	public static boolean ENABLED;
	public static boolean TVT;
	public static boolean DM;
	public static boolean LM;
	public static boolean CTF;

	public static String FAKE_NAME;
	public static String TITLE_FORMAT;
	public static boolean GM_SEE_REAL_NAME;
	public static boolean HIDE_CLAN;
	public static boolean HIDE_HERO_AURA;
	public static boolean FORCE_COLORS;
	public static int NAME_COLOR;
	public static int TITLE_COLOR;

	public static int DEFAULT_ENCHANT;

	public static String MSG_ENTER;
	public static String MSG_EXIT;
	public static String MSG_NO_EQUIP;
	public static String MSG_BLOCKED;
	public static String MSG_NO_SKIN;

	private EventGearConfig()
	{
	}

	public static void load()
	{
		final ExProperties p = new ExProperties();
		try
		{
			p.load(new File(FILE));
		}
		catch (Exception e)
		{
			LOG.warning("EventGear: no se encontro " + FILE + ", se usan valores por defecto.");
		}

		ENABLED = p.getProperty("EventGearEnabled", true);
		TVT = p.getProperty("EventGearTvT", true);
		DM = p.getProperty("EventGearDM", true);
		LM = p.getProperty("EventGearLM", true);
		CTF = p.getProperty("EventGearCTF", true);

		FAKE_NAME = p.getProperty("FakeName", "SanSara");
		TITLE_FORMAT = p.getProperty("TitleFormat", "Kills: %kills%");
		GM_SEE_REAL_NAME = p.getProperty("GmSeeRealName", true);
		HIDE_CLAN = p.getProperty("HideClan", true);
		HIDE_HERO_AURA = p.getProperty("HideHeroAura", true);
		FORCE_COLORS = p.getProperty("ForceColors", true);
		NAME_COLOR = parseColor(p.getProperty("NameColor", "FFFFFF"), 0xFFFFFF);
		TITLE_COLOR = parseColor(p.getProperty("TitleColor", "FFFF77"), 0xFFFF77);

		DEFAULT_ENCHANT = p.getProperty("DefaultEnchant", 10);

		MSG_ENTER = p.getProperty("MsgEnter", "Recibiste el equipo del evento.");
		MSG_EXIT = p.getProperty("MsgExit", "Se te devolvio tu equipo.");
		MSG_NO_EQUIP = p.getProperty("MsgNoEquip", "No podes cambiar el equipo durante el evento.");
		MSG_BLOCKED = p.getProperty("MsgBlocked", "No podes hacer eso durante el evento.");
		MSG_NO_SKIN = p.getProperty("MsgNoSkin", "No podes usar skins durante el evento.");
	}

	private static int parseColor(String value, int def)
	{
		try
		{
			return Integer.decode("0x" + value.trim().replace("0x", "").replace("#", ""));
		}
		catch (Exception e)
		{
			return def;
		}
	}

	/**
	 * @param event : "TvT", "DM", "LM" o "CTF".
	 * @return true si el Event Gear esta activo para ese evento.
	 */
	public static boolean isEnabledFor(String event)
	{
		if (!ENABLED || event == null)
			return false;

		switch (event)
		{
			case "TvT":
				return TVT;
			case "DM":
				return DM;
			case "LM":
				return LM;
			case "CTF":
				return CTF;
		}
		return false;
	}
}
