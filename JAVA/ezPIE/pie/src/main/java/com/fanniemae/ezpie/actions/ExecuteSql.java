/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.data.connectors.DataConnector;
import com.fanniemae.ezpie.data.connectors.SqlConnector;

/** 
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-07-06
 * 
 */

public class ExecuteSql extends Action {

	public ExecuteSql(SessionManager session, Element action) {
		super(session, action, false);
		if (StringUtilities.isNullOrEmpty(_name))
			_name = "LocalData";
	}

	@Override
	public String executeAction() {
		try (DataConnector sqlConnection = new SqlConnector(_session, _action, false)) {
			sqlConnection.open();
			String[][] columnNames = sqlConnection.getDataSourceSchema();
			String[][] kvps = new String[columnNames.length][2];
			if (!sqlConnection.eof()) {
				Object[] dataRow = sqlConnection.getDataRow();
				for (int i = 0; i < dataRow.length; i++) {
					String value = dataRow[i] == null ? "" : dataRow[i].toString();
					kvps[i][0] = columnNames[i][0];
					kvps[i][1] = value;
				}
				_session.addTokens(_name, kvps);
				if (sqlConnection.eof()) {
					_session.addLogMessage("", "End of Set", "Reached the end of the result set.");
				} else {
					_session.addLogMessage("", "End of Read", "Query returned more data, but only loading values from the first row into tokens.  Use DataSet element to work with multiple rows.");
				}
			}
			sqlConnection.close();
		} catch (Exception ex) {
			throw new RuntimeException("Error running ExecuteSql command. " + ex.getMessage(), ex);
		}
		return null;
	}

}