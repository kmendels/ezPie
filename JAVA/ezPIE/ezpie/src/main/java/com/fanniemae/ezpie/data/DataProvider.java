/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.data.jdbcproviders.JdbcProvider;
import com.fanniemae.ezpie.data.jdbcproviders.JdbcProviderFactory;
import com.fanniemae.ezpie.data.utilities.DriverShim;
import com.fanniemae.ezpie.data.utilities.FieldInfo;
import com.fanniemae.ezpie.data.utilities.SqlParameterInfo;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-22
 * 
 */

public class DataProvider {
	protected Element _connection = null;

	protected String _sqlDialect = "";
	protected String _connectionString = "";
	protected String _lastErrorMessage = "";
	protected String _jarFilename = "";
	protected String _className = "";
	protected String _url;

	protected int _CommandTimeout = 60;

	public DataProvider(SessionManager session, Element eleConnection) {
		if (eleConnection == null) {
			throw new PieException("No connection element provided.");
		}
		_connection = eleConnection;

		_sqlDialect = eleConnection.getAttribute("Dialect");
		if (StringUtilities.isNullOrEmpty(_sqlDialect)) {
			_sqlDialect = "SQL92";
		}

		_url = eleConnection.getAttribute("URL");
		_className = eleConnection.getAttribute("ClassName");
		_connectionString = eleConnection.getAttribute("ConnectionString");
		String connectionType = eleConnection.getAttribute("Type");
		JdbcProvider jdbcDriver = null;
		if (StringUtilities.isNullOrEmpty(_className) || StringUtilities.isNullOrEmpty(_url)) {
			jdbcDriver = JdbcProviderFactory.getProvider(session, connectionType);
			if (jdbcDriver != null) {
				_className = jdbcDriver.getClassName(_className);
				_url = jdbcDriver.getJarFilename(_url);
			}
		}

		if (StringUtilities.isNullOrEmpty(_className)) {
			throw new PieException("Missing class name for JDBC provider.");
		}
		if (StringUtilities.isNullOrEmpty(_connectionString)) {
			throw new PieException("Missing connection string to database.");
		}

	}

	public Connection getConnection() {
		Connection con = null;
		try {
			URL u = new URL(formatProviderUrl(_url));
			URLClassLoader ucl = new URLClassLoader(new URL[] { u });
			Class<?> dbClass = Class.forName(_className, true, ucl);
			Driver d = (Driver) dbClass.newInstance();
			DriverManager.registerDriver(new DriverShim(d));
			con = DriverManager.getConnection(_connectionString);
		} catch (ClassNotFoundException e) {
			throw new PieException(String.format("Specified database driver class (%s) was not found in %s.", _className, _url), e);
		} catch (SQLException e) {
			throw new PieException(String.format("SQL exception while preparing connection. %s", e.getMessage()), e);
		} catch (InstantiationException e) {
			throw new PieException(String.format("Instantiation exception while preparing connection. %s", e.getMessage()), e);
		} catch (IllegalAccessException e) {
			throw new PieException(String.format("Illegal access exception while preparing connection. %s", e.getMessage()), e);
		} catch (MalformedURLException e) {
			throw new PieException(String.format("Malformed URL exception while preparing connection. %s", e.getMessage()), e);
		}
		return con;
	}

	public List<FieldInfo> getSchema(String sqlCommand) {
		List<FieldInfo> results = new ArrayList<>();
		ResultSet rs = null;
		try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
			stmt.setMaxRows(1);
			stmt.setQueryTimeout(_CommandTimeout);
			rs = stmt.executeQuery(sqlCommand);

			ResultSetMetaData schemaMetaData = rs.getMetaData();
			for (int i = 1; i <= schemaMetaData.getColumnCount(); i++) {
				FieldInfo info = new FieldInfo();
				info.setName(schemaMetaData.getColumnName(i));
				info.setDbType(schemaMetaData.getColumnType(i));
				info.setDbTypeName(schemaMetaData.getColumnTypeName(i));
				results.add(info);
			}
		} catch (SQLException ex) {
			results = null;
			throw new PieException("Could not read schema information from SQL command.", ex);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					ExceptionUtilities.goSilent(e);
				}
			}
		}
		return results;
	}

	public CallableStatement deriveParameters(Connection con, String storedProcName) {
		CallableStatement cstmt = null;
		List<SqlParameterInfo> aParams = new ArrayList<>();
		Boolean bReturnValue = false;

		try {
			DatabaseMetaData dbmd = con.getMetaData();
			try (ResultSet rs = dbmd.getProcedureColumns(null, null, storedProcName, null)) {
				while (rs.next()) {
					if (rs.getInt(5) == DatabaseMetaData.procedureColumnReturn) {
						bReturnValue = true;
					}
					SqlParameterInfo oParam = new SqlParameterInfo();
					oParam.setName(rs.getString(4));
					oParam.setDirection(rs.getInt(5));
					oParam.setDbType(rs.getInt(6));
					oParam.setDbTypeName(rs.getString(7));
					aParams.add(oParam);
				}
			}

			if (aParams.isEmpty() && bReturnValue) {
				cstmt = con.prepareCall("{? = call " + storedProcName + "()}");
			} else if (aParams.isEmpty() && (!bReturnValue)) {
				cstmt = con.prepareCall("{call " + storedProcName + "()}");
			} else {
				int iParamCount = aParams.size();
				StringBuilder sb = new StringBuilder();
				sb.append("{");
				if (bReturnValue) {
					sb.append("? = ");
				}
				sb.append("call ");
				sb.append(storedProcName);
				sb.append("(");
				for (int i = 1; i < iParamCount; i++) {
					if (i > 1) {
						sb.append(", ");
					}
					sb.append("?");
				}
				sb.append(")}");
				cstmt = con.prepareCall(sb.toString());

				int iParam = 1;
				for (SqlParameterInfo oParam : aParams) {
					switch (oParam.getDirection()) {
					case DatabaseMetaData.procedureColumnIn:
						cstmt = addInParameter(cstmt, iParam, oParam.getDbType(), oParam.getDbTypeName());
						break;
					case DatabaseMetaData.procedureColumnInOut:
					case DatabaseMetaData.procedureColumnOut:
					case DatabaseMetaData.procedureColumnResult:
					case DatabaseMetaData.procedureColumnReturn:
						cstmt.registerOutParameter(iParam, oParam.getDbType());
						break;
					}
					iParam++;
				}

			}
		} catch (SQLException e) {
			throw new PieException("Could not derive stored procedure parameters.", e);
		}
		return cstmt;
	}

	protected CallableStatement addInParameter(CallableStatement cstmt, int param, int dbType, String dbTypeName) throws SQLException {
		switch (dbType) {
		case Types.BOOLEAN:
			cstmt.setBoolean(param, true);
			break;
		case Types.CHAR:
			cstmt.setString(param, "");
			break;
		case Types.DATE:
			cstmt.setDate(param, null);
			break;
		case Types.DECIMAL:
			// cstmt.setBigDecimal(iParam, BigDecimal.ZERO);
			cstmt.setDouble(param, 0.0);
			break;
		case Types.INTEGER:
			cstmt.setInt(param, 0);
			break;
		case Types.VARCHAR:
			cstmt.setString(param, "");
			break;
		default:
			throw new SQLException("Stored procedure parameter type " + dbTypeName + " is not a currently supported input type");
		}
		return cstmt;
	}

	protected String formatProviderUrl(String sUrl) {
		String sResult = sUrl;

		if (!sUrl.toLowerCase().startsWith("jar:file:")) {
			sResult = String.format("jar:file:%s!/", sUrl);
		}
		return sResult;
	}
}
