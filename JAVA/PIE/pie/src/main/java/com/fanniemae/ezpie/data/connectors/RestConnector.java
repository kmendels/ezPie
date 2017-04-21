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

package com.fanniemae.ezpie.data.connectors;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.RestUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.utilities.TreeNode;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Tara Tritt
 * @since 2016-04-29
 * 
 */

public class RestConnector extends DataConnector {
	protected Element _conn;
	protected String _connID;
	protected String _url;
	protected String _username;
	protected String _password;
	protected String _proxyHost;
	protected int _proxyPort;
	protected String _proxyUsername;
	protected String _proxyPassword;
	protected NodeList _columns;
	
	protected Object[][]  _rows;
	protected int _index = 0;
	protected DataType[] _dataTypes;
	

	public RestConnector(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
		_connID = _session.getAttribute(dataSource, "ConnectionID");
		_session.addLogMessage("", "ConnectionID", _connID);
		_conn = _session.getConnection(_connID);
		_username = _session.getAttribute(_conn, "Username");
		_password = _session.getAttribute(_conn, "Password");
		_proxyHost = _session.getAttribute(_conn, "ProxyHost");
		_proxyPort = StringUtilities.toInteger(_session.getAttribute(_conn, "ProxyPort"));
		_proxyUsername = _session.getAttribute(_conn, "ProxyUsername");
		_proxyPassword = _session.getAttribute(_conn, "ProxyPassword");
		_url = _session.getAttribute(_conn, "URL");
		if (StringUtilities.isNullOrEmpty(_url))
			throw new RuntimeException(String.format("%s URL element not found in the definition file.", _url));
		_columns = XmlUtilities.selectNodes(_dataSource, "*");
		
	}

	@Override
	public Boolean open() {
		try{
			String response = RestUtilities.sendGetRequest(_url, _proxyHost, _proxyPort, _proxyUsername, _proxyPassword, _username, _password);
			_session.addLogMessage("", "RestConnector", String.format("View Response"), "file://" + RestUtilities.writeResponseToFile(response, FileUtilities.getRandomFilename(_session.getLogPath(), "txt")));
			
			int numColumns = _columns.getLength();
			_session.addLogMessage("", "RestConnector", String.format("%,d columns found", numColumns));
			
			//get columns from definition file and split 
			ArrayList<TreeNode<String>> columns = new ArrayList<TreeNode<String>>();
			ArrayList<String[]> tempPaths = new ArrayList<String[]>();
			for (int i = 0; i < numColumns; i++) {
				String path = _session.getAttribute(_columns.item(i), "JsonPath");
				String[] pathParts = path.split("\\.");
				tempPaths.add(pathParts);
			}
			
			//build tree that will contain all of the JSON attributes to return
			for(int m = 0; m < tempPaths.size(); m++){
				boolean found = false;
				for(int n = 0; n < columns.size(); n++){
					if(searchTree(columns.get(n), tempPaths.get(m), 0)){
						found = true;
					}	
				}
				if(!found){
					columns.add(new TreeNode<String>(tempPaths.get(m)[0]));
					searchTree(columns.get(columns.size()-1), tempPaths.get(m), 0);
				}
			}
			
			TreeNode<String> root = new TreeNode<String>("root");	
			for(int n = 0; n < columns.size(); n++){
				root.addChild(columns.get(n));
			}
			
			Object json = new JSONTokener(response).nextValue();
			
			//create rows by parsing JSON and using the tree with the attributes to search for
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			if (json instanceof JSONObject){
				rows = buildRows((JSONObject) json, rows, root);
			} else if (json instanceof JSONArray){
				ArrayList<ArrayList<String>> parentRows = new ArrayList<ArrayList<String>>(rows);
				for (int i = 0; i < ((JSONArray) json).length(); i++) {
					rows.addAll(buildRows(((JSONArray) json).getJSONObject(i), parentRows, root));
				}
			}
		
			//determine data types and cast values to correct type
			dataTypeRows(rows);

			
		} catch (JSONException ex){
			throw new RuntimeException("Error while trying to make REST request: " + ex.getMessage(), ex);
		}
		return true;
	}

	@Override
	public Boolean eof() {
		if(_index < _rows.length){
			return false;
		} else{ 
			return true;
		}
	}

	@Override
	public Object[] getDataRow() {
		Object[] row = _rows[_index];
		_index++;
		return row;
	}

	@Override
	public void close() {
		
	}
	
	private void dataTypeRows(ArrayList<ArrayList<String>> rows){
		//discover longest row
		int numColumns = 0;
		for(int i = 0; i < rows.size(); i++){
			ArrayList<String> row = rows.get(i);
			for(int j = 0; j < row.size(); j++){
				numColumns = numColumns > row.size() ? numColumns : row.size();
			}
		}
		_dataTypes = new DataType[numColumns];
		_rows = new Object[rows.size()][numColumns];
		for(int i = 0; i < rows.size(); i++){
			ArrayList<String> row = rows.get(i);
			for(int j = 0; j < row.size(); j++){
				String value = row.get(j);
				String dataType = StringUtilities.getDataType(value, j > 0 ? row.get(j-1) : "");
				_dataTypes[j] = DataUtilities.DataTypeToEnum(dataType);
				_rows[i][j] = castValue(j, value);
			}
		}
	}
	
	protected Object castValue(int i, String value) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return null;
		}

		switch (_dataTypes[i]) {
		case StringData:
			return value;
		case DateData:
			return StringUtilities.toDate(value);
		case IntegerData:
			return StringUtilities.toInteger(value);
		case LongData:
			return StringUtilities.toLong(value);
		case DoubleData:
			return StringUtilities.toDouble(value);
		case BigDecimalData:
			return StringUtilities.toBigDecimal(value);
		case BooleanData:
			return StringUtilities.toBoolean(value);
		default:
			throw new RuntimeException(String.format("%s string conversion not currently available.", DataType.values()[i]));
		}
	}
	
	private ArrayList<ArrayList<String>> buildRows(JSONObject json, ArrayList<ArrayList<String>> parentRows, TreeNode<String> column){
		Object j= null;
		if(column.getData().equals("root")){
			j = json;
		} else {
			j = json.get(column.getData());
		}
		
		if (j instanceof JSONArray) {
			// It's an array
			ArrayList<ArrayList<String>> oldParentRows = parentRows;
			ArrayList<ArrayList<String>> childRows = new ArrayList<ArrayList<String>>();
			for(int m = 0; m < ((JSONArray)j).length(); m++){
				parentRows = oldParentRows;
				JSONObject jj = ((JSONArray) j).getJSONObject(m);
				Iterator<TreeNode<String>> iter = column.iterator();
				while(iter.hasNext()){
					ArrayList<ArrayList<String>> returned = buildRows(jj, parentRows, iter.next());
					parentRows = returned;
				}
				childRows.addAll(parentRows);
			}
			
			return childRows;
		} else if (j instanceof JSONObject) {
		    // It's an object
			ArrayList<ArrayList<String>> childRows = new ArrayList<ArrayList<String>>();
			Iterator<TreeNode<String>> iter = column.iterator();
			while(iter.hasNext()){
				ArrayList<ArrayList<String>> returned = buildRows((JSONObject)j, parentRows, iter.next());
				parentRows = returned;
			}
			childRows.addAll(parentRows);
			
			return childRows;
		} else {
			// It's a string, number etc.
			ArrayList<ArrayList<String>> childRows = new ArrayList<ArrayList<String>>();
			for(int n = 0; n < parentRows.size(); n++){
				ArrayList<String> temp = new ArrayList<String>(parentRows.get(n));
				temp.add(j.toString());
				childRows.add(temp);
			}
			if(parentRows.size() == 0){
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(j.toString());
				childRows.add(temp);
			}
			
			return childRows;
		}
	}
	
	private boolean searchTree(TreeNode<String> node, String[] pathParts, int index){
		if(pathParts[index].equals(node.getData())){
			index++;
			if(pathParts.length <= index){
				return true;
			}
			boolean found = false;
			Iterator<TreeNode<String>> iter = node.getChildren().iterator();
			while(iter.hasNext()){
				TreeNode<String> next = iter.next();
				if(searchTree(next, pathParts, index))
					found = true;
			}
			if(!found){
				node.addChild(pathParts[index]);
				searchTree(node.getChildren().get(node.getChildren().size()-1), pathParts, index);
			}
			return true;
		} else {
			return false;
		}
	}

}