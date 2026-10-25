package net.sf.l2j.gameserver.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;

/**
 * Stores player buffer profiles (scheme names "1"/"2"/"3") in memory + DB.
 */
public class BufferTable
{
	private static final CLogger LOGGER = new CLogger(BufferTable.class.getName());
	
	private static final Map<Integer, HashMap<String, ArrayList<Integer>>> _schemesTable = new ConcurrentHashMap<>();
	
	private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
	private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
	private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)";
	private static final String DELETE_SCHEME = "DELETE FROM buffer_schemes WHERE object_id=? AND scheme_name=?";
	
	protected BufferTable()
	{
		int count = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(LOAD_SCHEMES);
			ResultSet rs = st.executeQuery())
		{
			while (rs.next())
			{
				final int objectId = rs.getInt("object_id");
				final String schemeName = rs.getString("scheme_name");
				final String skillsRaw = rs.getString("skills");
				
				final ArrayList<Integer> schemeList = new ArrayList<>();
				if (skillsRaw != null && !skillsRaw.isEmpty())
				{
					for (String skill : skillsRaw.split(","))
					{
						if (skill.isEmpty() || schemeList.size() >= Config.BUFFER_MAX_SKILLS)
							break;
						
						try
						{
							schemeList.add(Integer.valueOf(skill.trim()));
						}
						catch (NumberFormatException e)
						{
							LOGGER.warn("Invalid skill id '{}' for profile {} (objectId={}).", skill, schemeName, objectId);
						}
					}
				}
				
				setScheme(objectId, schemeName, schemeList);
				count++;
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to load buffer profiles: {}", e.getMessage());
		}
		LOGGER.info("Loaded {} buffer profiles.", count);
	}
	
	public void saveSchemes()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement st = con.prepareStatement(DELETE_SCHEMES))
			{
				st.execute();
			}
			
			try (PreparedStatement st = con.prepareStatement(INSERT_SCHEME))
			{
				for (Map.Entry<Integer, HashMap<String, ArrayList<Integer>>> player : _schemesTable.entrySet())
				{
					for (Map.Entry<String, ArrayList<Integer>> scheme : player.getValue().entrySet())
					{
						st.setInt(1, player.getKey());
						st.setString(2, scheme.getKey());
						st.setString(3, skillsToString(scheme.getValue()));
						st.executeUpdate();
						st.clearParameters();
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Error while saving buffer profiles: {}", e.getMessage());
		}
	}
	
	public void persistScheme(int playerId, String schemeName)
	{
		final List<Integer> skills = getScheme(playerId, schemeName);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement del = con.prepareStatement(DELETE_SCHEME))
			{
				del.setInt(1, playerId);
				del.setString(2, schemeName);
				del.executeUpdate();
			}
			
			try (PreparedStatement ins = con.prepareStatement(INSERT_SCHEME))
			{
				ins.setInt(1, playerId);
				ins.setString(2, schemeName);
				ins.setString(3, skillsToString(skills));
				ins.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Error persisting profile '{}' for objectId={}: {}", schemeName, playerId, e.getMessage());
		}
	}
	
	public boolean setScheme(int playerId, String schemeName, ArrayList<Integer> list)
	{
		HashMap<String, ArrayList<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			schemes = new HashMap<>();
			_schemesTable.put(playerId, schemes);
		}
		else if (!schemes.containsKey(schemeName) && schemes.size() >= Config.BUFFER_MAX_SCHEMES)
			return false;
		
		schemes.put(schemeName, list);
		return true;
	}
	
	public List<Integer> getScheme(int playerId, String schemeName)
	{
		final HashMap<String, ArrayList<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null || schemes.get(schemeName) == null)
			return Collections.emptyList();
		
		return schemes.get(schemeName);
	}
	
	/**
	 * Retail max for NPC/CB buffers. Extra XML levels (Might 4, Haste 3, etc.) are AIO-only.
	 */
	public static int getRetailBuffLevel(int skillId)
	{
		switch (skillId)
		{
			case 1036:
				return 2; // Magic Barrier
			case 1040:
				return 3; // Shield
			case 1045:
				return 6; // Blessed Body
			case 1059:
				return 3; // Empower
			case 1062:
				return 2; // Berserker Spirit
			case 1068:
				return 3; // Might
			case 1077:
				return 3; // Focus
			case 1085:
				return 3; // Acumen
			case 1086:
				return 2; // Haste
			case 1087:
				return 3; // Agility
			case 1204:
				return 2; // Wind Walk
			case 1240:
				return 3; // Guidance
			case 1242:
				return 3; // Death Whisper
			case 1388:
				return 3; // Greater Might
			case 1389:
				return 3; // Greater Shield
			default:
				return SkillTable.getInstance().getMaxLevel(skillId);
		}
	}
	
	private static String skillsToString(List<Integer> skills)
	{
		if (skills == null || skills.isEmpty())
			return "";
		
		final StringBuilder sb = new StringBuilder();
		for (int skillId : skills)
			StringUtil.append(sb, skillId, ",");
		
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		
		return sb.toString();
	}
	
	public static BufferTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BufferTable INSTANCE = new BufferTable();
	}
}
