package net.sf.l2j.gameserver.antiinterface;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Anti interface: verifica que el jugador use la interface del server.
 * <ul>
 * <li>Al entrar, el server le manda a la interface un desafio oculto por chat ("#AI:numero").</li>
 * <li>La interface del server (ChatWnd.uc) lo intercepta, calcula la respuesta con las claves y la manda con el bypass "_ai respuesta".</li>
 * <li>Si no responde bien a tiempo: LOG (solo registra) o KICK (lo saca del juego).</li>
 * </ul>
 * Config: ./config/HadesProject/AntiInterface.properties (se recarga con //reload config).
 * IMPORTANTE: Key1/Key2 tienen que ser las mismas que en ChatWnd.uc. Si se cambian, hay que recompilar la interface.
 */
public final class AntiInterfaceManager
{
	private static final Logger LOGGER = Logger.getLogger(AntiInterfaceManager.class.getName());
	
	public static final String FILE = "./config/HadesProject/AntiInterface.properties";
	private static final String LOG_FILE = "./log/antiinterface.log";
	private static final String HTML_FILE = "data/html/mods/protect/antiinterface.htm";
	
	/** Marca que busca la interface en el chat. */
	public static final String MARKER = "#AI:";
	
	private static final int MOD = 999983;
	
	private static boolean _enabled = false;
	private static boolean _kick = false;
	private static int _timeout = 30;
	private static int _firstDelay = 5;
	private static int _retries = 3;
	private static int _key1 = 18022;
	private static int _key2 = 17000;
	
	private static final Map<Integer, Check> CHECKS = new ConcurrentHashMap<>();
	
	private static final class Check
	{
		final int nonce;
		volatile boolean verified;
		volatile int sent;
		volatile ScheduledFuture<?> task;
		
		Check(int nonce)
		{
			this.nonce = nonce;
		}
	}
	
	static
	{
		load();
	}
	
	private AntiInterfaceManager()
	{
	}
	
	public static synchronized void load()
	{
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(FILE); InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
		{
			props.load(reader);
		}
		catch (Exception e)
		{
			_enabled = false;
			LOGGER.warning("AntiInterface: no se pudo leer " + FILE + " (" + e.getMessage() + "). Desactivado.");
			return;
		}
		
		_enabled = Boolean.parseBoolean(get(props, "AntiInterfaceEnabled", "False"));
		_kick = "KICK".equalsIgnoreCase(get(props, "AntiInterfaceAction", "LOG"));
		_timeout = Math.max(10, getInt(props, "AntiInterfaceTimeout", 30));
		_firstDelay = Math.max(1, getInt(props, "AntiInterfaceFirstDelay", 5));
		_retries = Math.max(1, getInt(props, "AntiInterfaceRetries", 3));
		_key1 = getInt(props, "AntiInterfaceKey1", 18022);
		_key2 = getInt(props, "AntiInterfaceKey2", 17000);
		
		LOGGER.info("AntiInterface: " + (_enabled ? "activado (" + (_kick ? "KICK" : "LOG") + ", timeout " + _timeout + "s)" : "desactivado") + ".");
	}
	
	/**
	 * Llamado al final de EnterWorld.
	 * @param player
	 */
	public static void onEnterWorld(Player player)
	{
		if (!_enabled || player == null || player.isGM() || player.getClient() == null || player.getClient().isDetached())
			return;
		
		final int objectId = player.getObjectId();
		final Check old = CHECKS.remove(objectId);
		if (old != null && old.task != null)
			old.task.cancel(false);
		
		final Check check = new Check(ThreadLocalRandom.current().nextInt(100000, 999999999));
		CHECKS.put(objectId, check);
		
		// Reenvia el desafio varias veces (por si la interface tarda en cargar) y al final decide.
		final int interval = Math.max(1, (_timeout - _firstDelay) / _retries);
		check.task = ThreadPool.scheduleAtFixedRate(() -> tick(player, check), _firstDelay * 1000L, interval * 1000L);
	}
	
	private static void tick(Player player, Check check)
	{
		if (check.verified || CHECKS.get(player.getObjectId()) != check || !player.isOnline() || player.getClient() == null)
		{
			finish(player, check);
			return;
		}
		
		if (check.sent < _retries)
		{
			check.sent++;
			player.sendPacket(new CreatureSay(0, 0, "", MARKER + check.nonce));
			return;
		}
		
		// Se acabaron los intentos y no respondio bien.
		finish(player, check);
		punish(player);
	}
	
	private static void finish(Player player, Check check)
	{
		if (check.task != null)
			check.task.cancel(false);
		CHECKS.remove(player.getObjectId(), check);
	}
	
	/**
	 * Llamado desde RequestBypassToServer con "_ai respuesta".
	 * @param player
	 * @param command
	 */
	public static void onBypass(Player player, String command)
	{
		final Check check = CHECKS.get(player.getObjectId());
		if (check == null || check.verified)
			return;
		
		int answer;
		try
		{
			answer = Integer.parseInt(command.substring(3).trim());
		}
		catch (Exception e)
		{
			return;
		}
		
		if (answer == calc(check.nonce))
		{
			check.verified = true;
			finish(player, check);
		}
		// Respuesta incorrecta: no se avisa nada, sigue el conteo y al final se castiga.
	}
	
	/**
	 * La misma cuenta que hace ChatWnd.uc. Todo queda por debajo de 2^31 (sin overflow).
	 * @param nonce
	 * @return
	 */
	public static int calc(int nonce)
	{
		final String s = String.valueOf(nonce);
		int h = _key1 % MOD;
		for (int i = 0; i < s.length(); i++)
			h = (h * 131 + s.charAt(i) + _key2) % MOD;
		return (h * 7 + _key1) % MOD;
	}
	
	private static void punish(Player player)
	{
		final String info = player.getName() + " (cuenta " + player.getAccountName() + ", IP " + getIp(player) + ", HWID " + getHwid(player) + ")";
		log("SIN INTERFACE DEL SERVER: " + info + (_kick ? " -> KICK" : " -> solo log"));
		LOGGER.warning("AntiInterface: " + info + " no respondio el desafio" + (_kick ? ", kick." : "."));
		
		if (_kick && player.isOnline())
		{
			// Ventana de aviso (logo del server + mensaje) y kick a los pocos segundos para que alcance a leerla.
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(HTML_FILE);
			player.sendPacket(html);
			player.sendMessage("Tenes que usar la interface oficial del server. Descarga el parche desde el Discord.");
			ThreadPool.schedule(() -> player.logout(true), 8000);
		}
	}
	
	private static String getIp(Player player)
	{
		try
		{
			return player.getClient().getConnection().getInetAddress().getHostAddress();
		}
		catch (Exception e)
		{
			return "?";
		}
	}
	
	private static String getHwid(Player player)
	{
		try
		{
			return String.valueOf(player.getClient().getHWID());
		}
		catch (Exception e)
		{
			return "?";
		}
	}
	
	private static synchronized void log(String text)
	{
		try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true)))
		{
			out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] " + text);
		}
		catch (Exception e)
		{
			LOGGER.warning("AntiInterface: no se pudo escribir " + LOG_FILE + ": " + e.getMessage());
		}
	}
	
	private static String get(Properties props, String key, String def)
	{
		final String value = props.getProperty(key);
		return (value == null) ? def : value.trim();
	}
	
	private static int getInt(Properties props, String key, int def)
	{
		try
		{
			return Integer.parseInt(get(props, key, String.valueOf(def)));
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}
}
