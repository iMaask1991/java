package net.sf.l2j.gameserver.events.multievent;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.sf.l2j.commons.config.ExProperties;

public class MultiEventConfig
{
	protected static final Logger _log = Logger.getLogger(MultiEventConfig.class.getName());

	private static final String MULTI_EVENT_FILE = "./config/HadesProject/Events/MultiEvent.properties";

	public static boolean ENABLED;
	public static int NPC_ID;
	public static int[] NPC_COORDINATES = new int[4];

	public static void init()
	{
		ExProperties events = load(MULTI_EVENT_FILE);
		ENABLED = events.getProperty("MultiEventNpcEnabled", true);
		NPC_ID = events.getProperty("MultiEventNpcId", 60007);

		final String[] coords = events.getProperty("MultiEventNpcCoordinates", "82856,148626,-3469,0").split(",");
		if (coords.length >= 3)
		{
			NPC_COORDINATES[0] = Integer.parseInt(coords[0].trim());
			NPC_COORDINATES[1] = Integer.parseInt(coords[1].trim());
			NPC_COORDINATES[2] = Integer.parseInt(coords[2].trim());
			NPC_COORDINATES[3] = coords.length > 3 ? Integer.parseInt(coords[3].trim()) : 0;
		}
	}

	public static ExProperties load(String filename)
	{
		return load(new File(filename));
	}

	public static ExProperties load(File file)
	{
		ExProperties result = new ExProperties();
		try
		{
			result.load(file);
		}
		catch (IOException e)
		{
			_log.warning("Error loading config : " + file.getName() + "!");
		}
		return result;
	}
}
