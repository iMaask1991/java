package net.sf.l2j.gameserver.customoly;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHero;
import net.sf.l2j.gameserver.model.Announcement;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

public class CustomOlyManager
{
	private static final Logger LOGGER = Logger.getLogger(CustomOlyManager.class.getName());
	
	private static final String CREATE_SEASON = "CREATE TABLE IF NOT EXISTS custom_oly_season (id INT NOT NULL AUTO_INCREMENT, phase VARCHAR(16) NOT NULL DEFAULT 'IDLE', start_time BIGINT NOT NULL DEFAULT 0, compete_end BIGINT NOT NULL DEFAULT 0, validation_end BIGINT NOT NULL DEFAULT 0, hero_give_time BIGINT NOT NULL DEFAULT 0, heroes_given TINYINT NOT NULL DEFAULT 0, PRIMARY KEY (id))";
	private static final String CREATE_NOBLES = "CREATE TABLE IF NOT EXISTS custom_oly_nobles (season_id INT NOT NULL, char_id INT NOT NULL, class_id INT NOT NULL, char_name VARCHAR(35) NOT NULL, points INT NOT NULL DEFAULT 0, fights INT NOT NULL DEFAULT 0, wins INT NOT NULL DEFAULT 0, losses INT NOT NULL DEFAULT 0, frozen TINYINT NOT NULL DEFAULT 0, freeze_time BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (season_id, char_id))";
	private static final String CREATE_HEROES = "CREATE TABLE IF NOT EXISTS custom_oly_heroes (char_id INT NOT NULL, class_id INT NOT NULL, char_name VARCHAR(35) NOT NULL, end_time BIGINT NOT NULL, season_id INT NOT NULL, PRIMARY KEY (char_id, season_id))";
	
	private final Map<Integer, CustomOlyPlayer> _nobles = new ConcurrentHashMap<>();
	private final List<HeroEntry> _activeHeroes = Collections.synchronizedList(new ArrayList<HeroEntry>());
	
	private int _seasonId;
	private CustomOlyPhase _phase = CustomOlyPhase.IDLE;
	private long _startTime;
	private long _competeEnd;
	private long _validationEnd;
	private long _heroGiveTime;
	private boolean _heroesGiven;
	
	private ScheduledFuture<?> _tick;
	
	protected CustomOlyManager()
	{
	}
	
	public void boot()
	{
		if (!Config.CUSTOM_OLY_ENABLED)
			return;
		
		ensureTables();
		loadSeason();
		loadNobles();
		loadHeroes();
		
		if (_seasonId == 0 && Config.CUSTOM_OLY_AUTO_START)
			startSeason();
		
		_tick = ThreadPool.scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				tick();
			}
		}, 10000, 30000);
		
		CustomOlyEvent.getInstance().startScheduler();
		CustomOlyArena.getInstance().start();
		LOGGER.info("Custom Olympiad: booted season=" + _seasonId + " phase=" + _phase);
	}
	
	private void ensureTables()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement())
		{
			st.execute(CREATE_SEASON);
			st.execute(CREATE_NOBLES);
			st.execute(CREATE_HEROES);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: table create failed", e);
		}
	}
	
	private void loadSeason()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM custom_oly_season ORDER BY id DESC LIMIT 1");
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				_seasonId = rs.getInt("id");
				_phase = CustomOlyPhase.valueOf(rs.getString("phase"));
				_startTime = rs.getLong("start_time");
				_competeEnd = rs.getLong("compete_end");
				_validationEnd = rs.getLong("validation_end");
				_heroGiveTime = rs.getLong("hero_give_time");
				_heroesGiven = rs.getInt("heroes_given") == 1;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: load season failed", e);
		}
	}
	
	private void loadNobles()
	{
		_nobles.clear();
		if (_seasonId <= 0)
			return;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM custom_oly_nobles WHERE season_id=?"))
		{
			ps.setInt(1, _seasonId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					CustomOlyPlayer n = new CustomOlyPlayer();
					n.charId = rs.getInt("char_id");
					n.classId = rs.getInt("class_id");
					n.name = rs.getString("char_name");
					n.points = rs.getInt("points");
					n.fights = rs.getInt("fights");
					n.wins = rs.getInt("wins");
					n.losses = rs.getInt("losses");
					n.frozen = rs.getInt("frozen") == 1;
					n.freezeTime = rs.getLong("freeze_time");
					_nobles.put(n.charId, n);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: load nobles failed", e);
		}
	}
	
	private void loadHeroes()
	{
		_activeHeroes.clear();
		long now = System.currentTimeMillis();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM custom_oly_heroes WHERE end_time>?"))
		{
			ps.setLong(1, now);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					HeroEntry h = new HeroEntry();
					h.charId = rs.getInt("char_id");
					h.classId = rs.getInt("class_id");
					h.name = rs.getString("char_name");
					h.endTime = rs.getLong("end_time");
					h.seasonId = rs.getInt("season_id");
					_activeHeroes.add(h);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: load heroes failed", e);
		}
	}
	
	public synchronized void startSeason()
	{
		long now = System.currentTimeMillis();
		_phase = CustomOlyPhase.COMPETITION;
		_startTime = now;
		_competeEnd = now + Config.CUSTOM_OLY_PERIOD_DAYS * 86400000L;
		_validationEnd = _competeEnd + Config.CUSTOM_OLY_VALIDATION_DAYS * 86400000L;
		_heroGiveTime = nextGiveTime(_validationEnd);
		_heroesGiven = false;
		_nobles.clear();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO custom_oly_season (phase,start_time,compete_end,validation_end,hero_give_time,heroes_given) VALUES (?,?,?,?,?,0)", Statement.RETURN_GENERATED_KEYS))
		{
			ps.setString(1, _phase.name());
			ps.setLong(2, _startTime);
			ps.setLong(3, _competeEnd);
			ps.setLong(4, _validationEnd);
			ps.setLong(5, _heroGiveTime);
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys())
			{
				if (rs.next())
					_seasonId = rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: start season failed", e);
		}
		
		Announcement.AnnounceEvents(Config.CUSTOM_OLY_NAME + ": nueva season #" + _seasonId + " arranco.");
	}
	
	private long nextGiveTime(long after)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(after);
		String[] hm = Config.CUSTOM_OLY_HERO_GIVE_HOUR.split(":");
		int hour = Integer.parseInt(hm[0].trim());
		int min = hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0;
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, 0);
		if (cal.getTimeInMillis() <= after)
			cal.add(Calendar.DAY_OF_MONTH, 1);
		return cal.getTimeInMillis();
	}
	
	private void saveSeason()
	{
		if (_seasonId <= 0)
			return;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE custom_oly_season SET phase=?,compete_end=?,validation_end=?,hero_give_time=?,heroes_given=? WHERE id=?"))
		{
			ps.setString(1, _phase.name());
			ps.setLong(2, _competeEnd);
			ps.setLong(3, _validationEnd);
			ps.setLong(4, _heroGiveTime);
			ps.setInt(5, _heroesGiven ? 1 : 0);
			ps.setInt(6, _seasonId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: save season failed", e);
		}
	}
	
	private void tick()
	{
		if (!Config.CUSTOM_OLY_ENABLED || _seasonId <= 0)
			return;
		
		long now = System.currentTimeMillis();
		if (_phase == CustomOlyPhase.COMPETITION && now >= _competeEnd)
		{
			_phase = CustomOlyPhase.VALIDATION;
			CustomOlyEvent.getInstance().forceStop();
			CustomOlyArena.getInstance().clearQueues();
			saveSeason();
			Announcement.AnnounceEvents(Config.CUSTOM_OLY_NAME + ": competencia cerrada. Dia de espera, Heroes proximamente.");
		}
		else if (_phase == CustomOlyPhase.VALIDATION && !_heroesGiven && now >= _heroGiveTime)
			giveHeroes();
		
		expireHeroes(now);
	}
	
	public synchronized void giveHeroes()
	{
		if (_seasonId <= 0)
			return;
		
		_heroesGiven = true;
		_phase = CustomOlyPhase.IDLE;
		saveSeason();
		
		for (ClassId cid : ClassId.values())
		{
			if (cid.level() != 3)
				continue;
			grantClassHeroes(cid.getId());
		}
		
		Announcement.AnnounceEvents(Config.CUSTOM_OLY_NAME + ": Heroes entregados. Duracion " + Config.CUSTOM_OLY_HERO_DAYS + " dia(s).");
		
		if (Config.CUSTOM_OLY_AUTO_START)
			startSeason();
	}
	
	private void grantClassHeroes(int classId)
	{
		List<CustomOlyPlayer> ranked = getRanking(classId, 50);
		int slots = 0;
		int rank = 0;
		for (CustomOlyPlayer n : ranked)
		{
			rank++;
			if (slots >= Config.CUSTOM_OLY_HERO_SLOTS)
				break;
			
			boolean byPoints = n.frozen || n.points >= Config.CUSTOM_OLY_HERO_POINTS;
			boolean byRank = rank <= Config.CUSTOM_OLY_RANK_SLOTS && n.points >= Config.CUSTOM_OLY_RANK_MIN_POINTS;
			if (!byPoints && !byRank)
				continue;
			
			applyHero(n);
			slots++;
		}
	}
	
	private void applyHero(CustomOlyPlayer n)
	{
		long end = System.currentTimeMillis() + Config.CUSTOM_OLY_HERO_DAYS * 86400000L;
		HeroEntry h = new HeroEntry();
		h.charId = n.charId;
		h.classId = n.classId;
		h.name = n.name;
		h.endTime = end;
		h.seasonId = _seasonId;
		_activeHeroes.add(h);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO custom_oly_heroes (char_id,class_id,char_name,end_time,season_id) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE end_time=VALUES(end_time),class_id=VALUES(class_id),char_name=VALUES(char_name)"))
		{
			ps.setInt(1, n.charId);
			ps.setInt(2, n.classId);
			ps.setString(3, n.name);
			ps.setLong(4, end);
			ps.setInt(5, _seasonId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: insert hero failed", e);
		}
		
		Player online = World.getInstance().getPlayer(n.charId);
		if (online != null && online.isOnline())
		{
			online.setHero(true, true);
			online.setHeroEndTime(end);
			online.broadcastUserInfo();
			online.sendMessage(Config.CUSTOM_OLY_NAME + ": sos Hero por " + Config.CUSTOM_OLY_HERO_DAYS + " dia(s).");
		}
		else
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET custom_hero=1, hero_end=? WHERE obj_id=?"))
			{
				ps.setLong(1, end);
				ps.setInt(2, n.charId);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Custom Olympiad: offline hero failed", e);
			}
		}
	}
	
	private void expireHeroes(long now)
	{
		List<HeroEntry> gone = new ArrayList<>();
		synchronized (_activeHeroes)
		{
			for (HeroEntry h : _activeHeroes)
			{
				if (h.endTime > 0 && now > h.endTime)
					gone.add(h);
			}
			_activeHeroes.removeAll(gone);
		}
		for (HeroEntry h : gone)
		{
			Player online = World.getInstance().getPlayer(h.charId);
			if (online != null && online.isOnline())
				new AdminHero().removeHero(null, online, "");
		}
	}
	
	public CustomOlyPlayer getOrCreate(Player player)
	{
		CustomOlyPlayer n = _nobles.get(player.getObjectId());
		if (n != null)
			return n;
		n = new CustomOlyPlayer();
		n.charId = player.getObjectId();
		n.classId = player.getClassId().getId();
		n.name = player.getName();
		_nobles.put(n.charId, n);
		storeNoble(n);
		return n;
	}
	
	public CustomOlyPlayer getNoble(int charId)
	{
		return _nobles.get(charId);
	}
	
	public boolean isFrozen(Player player)
	{
		CustomOlyPlayer n = _nobles.get(player.getObjectId());
		return n != null && n.frozen;
	}
	
	public synchronized void applyFightResult(Player winner, Player loser)
	{
		if (winner == null || loser == null || winner == loser)
			return;
		if (_phase != CustomOlyPhase.COMPETITION)
			return;
		
		CustomOlyPlayer w = getOrCreate(winner);
		CustomOlyPlayer l = getOrCreate(loser);
		if (w.frozen || l.frozen)
			return;
		
		int steal = Config.CUSTOM_OLY_STEAL_POINTS;
		int taken = Math.min(steal, l.points);
		l.points = Math.max(0, l.points - taken);
		w.points += taken > 0 ? taken : steal;
		
		w.fights++;
		l.fights++;
		w.wins++;
		l.losses++;
		
		checkFreeze(w);
		storeNoble(w);
		storeNoble(l);
		
		winner.sendMessage(Config.CUSTOM_OLY_NAME + ": +" + (taken > 0 ? taken : steal) + " pts (" + w.points + ")");
		loser.sendMessage(Config.CUSTOM_OLY_NAME + ": -" + taken + " pts (" + l.points + ")");
	}
	
	private void checkFreeze(CustomOlyPlayer n)
	{
		if (n.frozen || n.points < Config.CUSTOM_OLY_HERO_POINTS)
			return;
		
		int frozenOnClass = 0;
		for (CustomOlyPlayer o : _nobles.values())
		{
			if (o.classId == n.classId && o.frozen)
				frozenOnClass++;
		}
		if (frozenOnClass >= Config.CUSTOM_OLY_HERO_SLOTS)
			return;
		
		n.frozen = true;
		n.freezeTime = System.currentTimeMillis();
		Player online = World.getInstance().getPlayer(n.charId);
		if (online != null && online.isOnline())
		{
			online.sendMessage(Config.CUSTOM_OLY_NAME + ": llegaste a " + Config.CUSTOM_OLY_HERO_POINTS + " pts. Quedaste clasificado. No podes jugar mas esta season.");
			CustomOlyArena.getInstance().unregister(online);
		}
	}
	
	private void storeNoble(CustomOlyPlayer n)
	{
		if (_seasonId <= 0)
			return;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO custom_oly_nobles (season_id,char_id,class_id,char_name,points,fights,wins,losses,frozen,freeze_time) VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE class_id=VALUES(class_id),char_name=VALUES(char_name),points=VALUES(points),fights=VALUES(fights),wins=VALUES(wins),losses=VALUES(losses),frozen=VALUES(frozen),freeze_time=VALUES(freeze_time)"))
		{
			ps.setInt(1, _seasonId);
			ps.setInt(2, n.charId);
			ps.setInt(3, n.classId);
			ps.setString(4, n.name);
			ps.setInt(5, n.points);
			ps.setInt(6, n.fights);
			ps.setInt(7, n.wins);
			ps.setInt(8, n.losses);
			ps.setInt(9, n.frozen ? 1 : 0);
			ps.setLong(10, n.freezeTime);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Custom Olympiad: store noble failed", e);
		}
	}
	
	public List<CustomOlyPlayer> getRanking(int classId, int limit)
	{
		List<CustomOlyPlayer> list = new ArrayList<>();
		for (CustomOlyPlayer n : _nobles.values())
		{
			if (n.classId == classId)
				list.add(n);
		}
		Collections.sort(list, new Comparator<CustomOlyPlayer>()
		{
			@Override
			public int compare(CustomOlyPlayer a, CustomOlyPlayer b)
			{
				if (b.points != a.points)
					return b.points - a.points;
				if (b.wins != a.wins)
					return b.wins - a.wins;
				return a.fights - b.fights;
			}
		});
		if (list.size() > limit)
			return new ArrayList<>(list.subList(0, limit));
		return list;
	}
	
	public int getRank(Player player)
	{
		CustomOlyPlayer n = _nobles.get(player.getObjectId());
		if (n == null)
			return 0;
		List<CustomOlyPlayer> ranked = getRanking(n.classId, 9999);
		for (int i = 0; i < ranked.size(); i++)
		{
			if (ranked.get(i).charId == n.charId)
				return i + 1;
		}
		return 0;
	}
	
	public List<HeroEntry> getActiveHeroes()
	{
		return new ArrayList<>(_activeHeroes);
	}
	
	public boolean isOlyAwardedHero(int charId)
	{
		final long now = System.currentTimeMillis();
		synchronized (_activeHeroes)
		{
			for (HeroEntry h : _activeHeroes)
			{
				if (h.charId == charId && h.endTime > now)
					return true;
			}
		}
		return false;
	}
	
	public List<ClassId> getThirdClasses()
	{
		List<ClassId> list = new ArrayList<>();
		for (ClassId cid : ClassId.values())
		{
			if (cid.level() == 3)
				list.add(cid);
		}
		return list;
	}
	
	public CustomOlyPhase getPhase()
	{
		return _phase;
	}
	
	public int getSeasonId()
	{
		return _seasonId;
	}
	
	public String getPhaseText()
	{
		SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm");
		if (_phase == CustomOlyPhase.COMPETITION)
			return "Competencia hasta " + fmt.format(new Date(_competeEnd));
		if (_phase == CustomOlyPhase.VALIDATION)
			return "Espera. Heroes: " + fmt.format(new Date(_heroGiveTime));
		if (_heroesGiven)
			return "Season cerrada (Heroes entregados)";
		return "Inactiva";
	}
	
	public void resetPoints()
	{
		_nobles.clear();
		if (_seasonId > 0)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM custom_oly_nobles WHERE season_id=?"))
			{
				ps.setInt(1, _seasonId);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Custom Olympiad: reset failed", e);
			}
		}
	}
	
	public static CustomOlyManager getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final CustomOlyManager INSTANCE = new CustomOlyManager();
	}
	
	public static class HeroEntry
	{
		public int charId;
		public int classId;
		public String name;
		public long endTime;
		public int seasonId;
	}
}
