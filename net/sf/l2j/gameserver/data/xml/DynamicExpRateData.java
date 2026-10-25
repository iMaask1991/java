package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.Arrays;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.player.Experience;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Dynamic EXP/SP rates by player level (Mobius-style).<br>
 * Loaded from {@code ./config/DynamicExpRates.xml}. Requires {@code EnableDynamicRates=True}
 * in Rates.properties. Then, if any level has exp/sp != 1, it replaces RateXp/RateSp for mobs.
 */
public class DynamicExpRateData implements IXmlReader
{
	private static final int SIZE = Experience.MAX_LEVEL + 1;
	
	private final float[] _expRates = new float[SIZE];
	private final float[] _spRates = new float[SIZE];
	private boolean _enabled;
	
	protected DynamicExpRateData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		Arrays.fill(_expRates, 1f);
		Arrays.fill(_spRates, 1f);
		_enabled = false;
		parseFile("./config/DynamicExpRates.xml");
	}
	
	public void reload()
	{
		load();
	}
	
	@Override
	public void parseDocument(Document document, Path path)
	{
		int count = 0;
		
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (!"list".equals(n.getNodeName()))
				continue;
			
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (!"dynamic".equals(d.getNodeName()))
					continue;
				
				final NamedNodeMap attrs = d.getAttributes();
				final Integer levelObj = parseInteger(attrs, "level");
				if (levelObj == null)
					continue;
				
				final int level = levelObj.intValue();
				if (level < 0 || level >= SIZE)
				{
					LOGGER.warn("DynamicExpRateData: invalid level {} in {}.", level, path);
					continue;
				}
				
				final float exp = parseFloat(attrs, "exp", 1f);
				final float sp = parseFloat(attrs, "sp", 1f);
				_expRates[level] = exp;
				_spRates[level] = sp;
				
				if (exp != 1f || sp != 1f)
					count++;
			}
		}
		
		_enabled = count > 0;
		if (!Config.ENABLE_DYNAMIC_RATES)
			LOGGER.info("DynamicExpRateData: Disabled by config (EnableDynamicRates=False). XML kept for later.");
		else if (_enabled)
			LOGGER.info("DynamicExpRateData: Loaded dynamic rates for {} levels.", count);
		else
			LOGGER.info("DynamicExpRateData: Disabled (all rates are 1).");
	}
	
	public boolean isEnabled()
	{
		return Config.ENABLE_DYNAMIC_RATES && _enabled;
	}
	
	public float getDynamicExpRate(int level)
	{
		if (level < 0)
			level = 0;
		else if (level >= SIZE)
			level = SIZE - 1;
		return _expRates[level];
	}
	
	public float getDynamicSpRate(int level)
	{
		if (level < 0)
			level = 0;
		else if (level >= SIZE)
			level = SIZE - 1;
		return _spRates[level];
	}
	
	public static DynamicExpRateData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DynamicExpRateData INSTANCE = new DynamicExpRateData();
	}
}
