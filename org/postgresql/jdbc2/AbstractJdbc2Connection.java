package org.postgresql.jdbc2;


import java.io.*;
import java.net.ConnectException;
import java.sql.*;
import org.postgresql.util.PSQLException;

/* $Header$
 * This class defines methods of the jdbc2 specification.  This class extends
 * org.postgresql.jdbc1.AbstractJdbc1Connection which provides the jdbc1
 * methods.  The real Connection class (for jdbc2) is org.postgresql.jdbc2.Jdbc2Connection
 */
public abstract class AbstractJdbc2Connection extends org.postgresql.jdbc1.AbstractJdbc1Connection
{
	/*
	 * The current type mappings
	 */
	protected java.util.Map typemap;

	public java.sql.Statement createStatement() throws SQLException
	{
		// The spec says default of TYPE_FORWARD_ONLY but everyone is used to
		// using TYPE_SCROLL_INSENSITIVE
		return createStatement(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY);
	}

	public abstract java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException;

	public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return prepareStatement(sql, java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY);
	}

	public abstract java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;

	public java.sql.CallableStatement prepareCall(String sql) throws SQLException
	{
		return prepareCall(sql, java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY);
	}

	public abstract java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;

	public java.util.Map getTypeMap() throws SQLException
	{
		return typemap;
	}


	public void setTypeMap(java.util.Map map) throws SQLException
	{
		typemap = map;
	}

	public void cancelQuery() throws SQLException
	{
		org.postgresql.PG_Stream cancelStream = null;
		try
		{
			cancelStream = new org.postgresql.PG_Stream(PG_HOST, PG_PORT);
		}
		catch (ConnectException cex)
		{
			// Added by Peter Mount <peter@retep.org.uk>
			// ConnectException is thrown when the connection cannot be made.
			// we trap this an return a more meaningful message for the end user
			throw new PSQLException ("postgresql.con.refused");
		}
		catch (IOException e)
		{
			throw new PSQLException ("postgresql.con.failed", e);
		}

		// Now we need to construct and send a cancel packet
		try
		{
			cancelStream.SendInteger(16, 4);
			cancelStream.SendInteger(80877102, 4);
			cancelStream.SendInteger(pid, 4);
			cancelStream.SendInteger(ckey, 4);
			cancelStream.flush();
		}
		catch (IOException e)
		{
			throw new PSQLException("postgresql.con.failed", e);
		}
		finally
		{
			try
			{
				if (cancelStream != null)
					cancelStream.close();
			}
			catch (IOException e)
			{} // Ignore
		}
	}


	/*
	 * This overides the standard internal getObject method so that we can
	 * check the jdbc2 type map first
	 */
	public Object getObject(String type, String value) throws SQLException
	{
		if (typemap != null)
		{
			SQLData d = (SQLData) typemap.get(type);
			if (d != null)
			{
				// Handle the type (requires SQLInput & SQLOutput classes to be implemented)
				throw org.postgresql.Driver.notImplemented();
			}
		}

		// Default to the original method
		return super.getObject(type, value);
	}


	//Because the get/setLogStream methods are deprecated in JDBC2
	//we use the get/setLogWriter methods here for JDBC2 by overriding
	//the base version of this method
	protected void enableDriverManagerLogging()
	{
		if (DriverManager.getLogWriter() == null)
		{
			DriverManager.setLogWriter(new PrintWriter(System.out));
		}
	}


	/*
	 * This implemetation uses the jdbc2Types array to support the jdbc2
	 * datatypes.  Basically jdbc1 and jdbc2 are the same, except that
	 * jdbc2 adds the Array types.
	 */
	public int getSQLType(String pgTypeName)
	{
		int sqlType = Types.OTHER; // default value
		for (int i = 0;i < jdbc2Types.length;i++)
		{
			if (pgTypeName.equals(jdbc2Types[i]))
			{
				sqlType = jdbc2Typei[i];
				break;
			}
		}
		return sqlType;
	}

	/*
	 * This table holds the org.postgresql names for the types supported.
	 * Any types that map to Types.OTHER (eg POINT) don't go into this table.
	 * They default automatically to Types.OTHER
	 *
	 * Note: This must be in the same order as below.
	 *
	 * Tip: keep these grouped together by the Types. value
	 */
	private static final String jdbc2Types[] = {
				"int2",
				"int4", "oid",
				"int8",
				"cash", "money",
				"numeric",
				"float4",
				"float8",
				"bpchar", "char", "char2", "char4", "char8", "char16",
				"varchar", "text", "name", "filename",
				"bytea",
				"bool",
				"date",
				"time",
				"abstime", "timestamp", "timestamptz",
				"_bool", "_char", "_int2", "_int4", "_text",
				"_oid", "_varchar", "_int8", "_float4", "_float8",
				"_abstime", "_date", "_time", "_timestamp", "_numeric",
				"_bytea"
			};

	/*
	 * This table holds the JDBC type for each entry above.
	 *
	 * Note: This must be in the same order as above
	 *
	 * Tip: keep these grouped together by the Types. value
	 */
	private static final int jdbc2Typei[] = {
												Types.SMALLINT,
												Types.INTEGER, Types.INTEGER,
												Types.BIGINT,
												Types.DOUBLE, Types.DOUBLE,
												Types.NUMERIC,
												Types.REAL,
												Types.DOUBLE,
												Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR,
												Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
												Types.BINARY,
												Types.BIT,
												Types.DATE,
												Types.TIME,
												Types.TIMESTAMP, Types.TIMESTAMP, Types.TIMESTAMP,
												Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY,
												Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY,
												Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY,
												Types.ARRAY
											};




}


