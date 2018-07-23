package com.fanniemae.ezpie.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;

public class RestConverter {

	protected SessionManager _session;

	protected String[][] _schema;

	//protected Set<String> _uniquekeys = new HashSet<String>();
	protected Map<String, String> _jsonSchema = null; // new HashMap<String, String>();
	protected List<Map<String, Object>> _rawData = new ArrayList<Map<String, Object>>();

	public RestConverter(SessionManager session, Element dataSource) {
		_session = session;
	}

	public Map<String, String> getSchema() {
		if (_jsonSchema == null) {
			throw new PieException("JsonConverter.getData method must be called before JsonConverter.getSchema.");
		}
		return _jsonSchema;
	}

	public List<Map<String, Object>> getData(String jsonString) {
		if (jsonString == null) {
			return new ArrayList<Map<String, Object>>();
		}

		JSONArray json;
		if (jsonString.startsWith("[")) {
			json = new JSONArray(jsonString);
		} else if (jsonString.startsWith("{")) {
			json = new JSONArray();
			json.put(new JSONObject(jsonString));
		} else {
			throw new PieException("Returned string does not match JSON specifications.");
		}

		_jsonSchema = new HashMap<String, String>();

		int length = json.length();
		for (int i = 0; i < length; i++) {
			List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
			Map<String, Object> starterRow = new HashMap<String, Object>();
			rows.add(starterRow);
			rows = toRows("", json.getJSONObject(i), rows);

			for (int x = 0; x < rows.size(); x++) {
				_rawData.add(rows.get(x));
			}
		}

		// Setup the schema and order the column names.
		_schema = new String[_jsonSchema.size()][2];
		int i = 0;
		for (Map.Entry<String, String> kvp : _jsonSchema.entrySet()) {
			_schema[i][0] = kvp.getKey();
			_schema[i][1] = kvp.getValue();
			i++;
		}
		
//		Arrays.sort(_schema, (a, b) -> {
//			if ((a[0] == null) && (b[0] == null)) {
//				return 0;
//			} else if (a[0] != null) {
//				return a[0].compareTo(b[0]);
//			}
//			return -1;
//		});

		return _rawData;
	}

	// Seems to work with a variety of JSON objects, does not work with JSONArray of values. e.g. "key": [5,3,2,1,3,2]
	protected List<Map<String, Object>> toRows(String prefix, JSONObject json, List<Map<String, Object>> rows) {
		Iterator<String> keys = json.keys();

		// Pull all the keys from this level of the JSON.
		while (keys.hasNext()) {
			String key = keys.next();
			String fullkey = "".equals(prefix) ? key : prefix + key;
			Object value = json.get(key);

			if (value == null) {
				continue;
			} else if (value instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) value;
				int length = jsonArray.length();
				if (length == 0) {
					continue;
				}

				List<Map<String, Object>> modifiedRows = new ArrayList<Map<String, Object>>();
				for (int i = 0; i < length; i++) {
					List<Map<String, Object>> arrayRows = new ArrayList<Map<String, Object>>();
					Map<String, Object> arrayStarterRow = new HashMap<String, Object>();
					arrayRows.add(arrayStarterRow);
					arrayRows = toRows(fullkey + "_", (JSONObject) jsonArray.getJSONObject(i), arrayRows);
					int arrayLength = arrayRows.size();
					if (arrayLength > 0) {
						for (int x = 0; x < arrayLength; x++) {
							int rowCount = rows.size();
							Map<String, Object> currentRow = arrayRows.get(x);
							for (int y = 0; y < rowCount; y++) {
								Map<String, Object> parentRow = rows.get(y);
								for (Entry<String, Object> kvp : parentRow.entrySet()) {
									currentRow.put(kvp.getKey(), kvp.getValue());
								}
								modifiedRows.add(currentRow);
							}
						}
					}
				}
				rows = modifiedRows;
			} else if (value instanceof JSONObject) {
				rows = toRows(fullkey + "_", (JSONObject) value, rows);
			} else {
				//_uniquekeys.add(fullkey);

				String valueType = value == null ? "" : value.getClass().getName();
				if (!"".equals(valueType) || !_jsonSchema.containsKey(fullkey)) {
					_jsonSchema.put(fullkey, valueType);
				}
				for (int i = 0; i < rows.size(); i++) {
					rows.get(i).put(fullkey, value);
				}
			}
		}
		return rows;
	}
}
