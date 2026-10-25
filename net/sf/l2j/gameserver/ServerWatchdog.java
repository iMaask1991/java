package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.model.World;

/**
 * Writes heartbeat and death reasons to files. A freeze leaves no console error;
 * last_alive.txt stopping is the proof.
 */
public final class ServerWatchdog extends Thread
{
	private static final CLogger LOGGER = new CLogger(ServerWatchdog.class.getName());
	private static final File ALIVE = new File("./log/last_alive.txt");
	private static final File EVENT = new File("./log/server_event.txt");
	
	private boolean _loginWasUp;
	
	private ServerWatchdog()
	{
		super("ServerWatchdog");
		setDaemon(true);
	}
	
	public static void startWatch()
	{
		Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
		{
			note("UNCAUGHT in " + thread.getName() + ": " + error);
			error.printStackTrace();
		});
		new ServerWatchdog().start();
		note("WATCHDOG started");
	}
	
	public static synchronized void note(String message)
	{
		final String line = stamp() + " " + message;
		LOGGER.warn(line);
		EVENT.getParentFile().mkdirs();
		try (PrintWriter out = new PrintWriter(new FileWriter(EVENT, true)))
		{
			out.println(line);
			out.flush();
		}
		catch (Exception e)
		{
		}
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				final Runtime rt = Runtime.getRuntime();
				final long free = rt.freeMemory() / 1024 / 1024;
				final long total = rt.totalMemory() / 1024 / 1024;
				final long max = rt.maxMemory() / 1024 / 1024;
				final int online = World.getInstance().getPlayers().size();
				final boolean login = LoginServerThread.getInstance().isLoginConnected();
				
				ALIVE.getParentFile().mkdirs();
				try (PrintWriter out = new PrintWriter(new FileWriter(ALIVE, false)))
				{
					out.println(stamp() + " online=" + online + " mem=" + (total - free) + "/" + total + " max=" + max + "MB login=" + login);
					out.flush();
				}
				
				if (login)
					_loginWasUp = true;
				else if (_loginWasUp)
				{
					_loginWasUp = false;
					note("Login socket DOWN. Players see the server offline.");
				}
				
				Thread.sleep(15000);
			}
			catch (InterruptedException e)
			{
				return;
			}
			catch (Throwable t)
			{
				note("WATCHDOG error: " + t);
			}
		}
	}
	
	private static String stamp()
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}
}
