package net.sf.l2j;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class L2DatabaseFactory
{
	protected static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());
	
	private ComboPooledDataSource _source;
	
	public static L2DatabaseFactory getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			_source = new ComboPooledDataSource();
			
			_source.setAutoCommitOnClose(true);
			_source.setInitialPoolSize(10);
			_source.setMinPoolSize(10);
			_source.setMaxPoolSize(Math.min(30, Math.max(10, Config.DATABASE_MAX_CONNECTIONS)));
			_source.setAcquireRetryAttempts(1);
			_source.setAcquireRetryDelay(200);
			_source.setCheckoutTimeout(2000);
			_source.setAcquireIncrement(5);
			_source.setTestConnectionOnCheckin(false);
			_source.setIdleConnectionTestPeriod(60);
			_source.setMaxIdleTime(180);
			_source.setUnreturnedConnectionTimeout(60);
			_source.setMaxStatementsPerConnection(100);
			_source.setBreakAfterAcquireFailure(false); // never fail if any way possible
			_source.setDriverClass("com.mysql.cj.jdbc.Driver");
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			
			/* Test the connection */
			_source.getConnection().close();
		}
		catch (SQLException x)
		{
			throw x;
		}
		catch (Exception e)
		{
			throw new SQLException("could not init DB connection:" + e);
		}
	}
	
	public void shutdown()
	{
		try
		{
			_source.close();
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
		
		try
		{
			_source = null;
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
	}
	
	/**
	 * Use brace as a safty precaution in case name is a reserved word.
	 * @param whatToCheck the list of arguments.
	 * @return the list of arguments between brackets.
	 */
	public static final String safetyString(String... whatToCheck)
	{
		final StringBuilder sb = new StringBuilder();
		for (String word : whatToCheck)
		{
			if (sb.length() > 0)
				sb.append(", ");
			
			sb.append('`');
			sb.append(word);
			sb.append('`');
		}
		return sb.toString();
	}
	
	public Connection getConnection()
	{
		// Leftover interrupt from Future.cancel(true) must not kill checkout.
		Thread.interrupted();
		
		try
		{
			return _source.getConnection();
		}
		catch (SQLException e)
		{
			_log.warning("L2DatabaseFactory: getConnection() failed: " + e.getMessage());
			throw new IllegalStateException("Could not get DB connection", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final L2DatabaseFactory _instance;
		
		static
		{
			try
			{
				_instance = new L2DatabaseFactory();
			}
			catch (Exception e)
			{
				throw new ExceptionInInitializerError(e);
			}
		}
	}
}