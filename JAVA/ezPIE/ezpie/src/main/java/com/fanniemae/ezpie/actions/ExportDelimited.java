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

package com.fanniemae.ezpie.actions;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DateUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.datafiles.DataReader;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-30
 * 
 */

public class ExportDelimited extends Action {

	protected String _filenameToken;
	protected String _outputFilename;
	protected String _delimiter = ",";
	protected String _lineSeparator = System.lineSeparator();
	protected String _dataSetName;

	protected DataStream _dataStream;

	protected int _outputLength;
	protected String[] _outputColumnNames;
	protected int[] _outputColumnIndexes;
	protected DataType[] _outputColumnDataTypes;

	protected boolean _trimSpaces = false;
	protected boolean _roundDoubles = false;
	protected boolean _appendData = false;
	protected boolean _writeColumnNames = true;
	protected boolean _removeCrLf = false;

	public ExportDelimited(SessionManager session, Element action) {
		super(session, action, false);

		_outputFilename = requiredAttribute("Filename");
		_dataSetName = requiredAttribute("DataSetName");

		_delimiter = optionalAttribute("Delimiter", _delimiter);
		_lineSeparator = optionalAttribute("LineSeparator",_lineSeparator);
		_trimSpaces = StringUtilities.toBoolean(optionalAttribute("TrimSpaces"), _trimSpaces);
		_appendData = StringUtilities.toBoolean(optionalAttribute("Append"), _appendData);
		_roundDoubles = StringUtilities.toBoolean(optionalAttribute("RoundDoubles"), _roundDoubles);
		_writeColumnNames = StringUtilities.toBoolean(optionalAttribute("IncludeColumnNames"), _writeColumnNames);
		_removeCrLf = StringUtilities.toBoolean(optionalAttribute("FlattenFieldStrings"), _removeCrLf);
		_filenameToken = _session.optionalAttribute(action, "Name","ExportDelimited");
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		_dataStream = _session.getDataStream(_dataSetName);

		try (DataReader dr = new DataReader(_dataStream); FileWriter fw = new FileWriter(_outputFilename, _appendData)) {
			defineOutputColumns(dr.getColumnNames());
			_outputColumnDataTypes = dr.getDataTypes();

			if (!_appendData && _writeColumnNames) {
				// Write Column Headers
				for (int i = 0; i < _outputLength; i++) {
					if (i > 0)
						fw.append(_delimiter);
					fw.append(wrapString(_outputColumnNames[i]));
				}
				fw.append(_lineSeparator);
			}

			int iRowCount = 0;
			// Write the data
			while (!dr.eof()) {
				Object[] dataRow = dr.getDataRow();

				for (int i = 0; i < _outputLength; i++) {
					if (i > 0)
						fw.append(_delimiter);

					if (_outputColumnIndexes[i] == -1) {
						fw.append("");
					} else if (dataRow[_outputColumnIndexes[i]] == null) {
						fw.append("");						
					} else if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.DateData) {
						fw.append(DateUtilities.toIsoString((Date) dataRow[_outputColumnIndexes[i]]));
					} else if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.StringData) {
						fw.append(wrapString(dataRow[_outputColumnIndexes[i]]));
					} else if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.DoubleData && _roundDoubles) {
						fw.append(doubleFormat(dataRow[_outputColumnIndexes[i]]));
					} else if (_outputColumnDataTypes[_outputColumnIndexes[i]] == DataType.DoubleData) {
						fw.append(StringUtilities.formatAsNumber((double)dataRow[_outputColumnIndexes[i]]));						
					} else {
						fw.append(dataRow[_outputColumnIndexes[i]].toString());
					}
				}
				fw.append(_lineSeparator);
				iRowCount++;
			}
			fw.close();
			dr.close();
			_session.addLogMessage("", "Data", String.format("%,d rows of data written.", iRowCount));
			_session.addToken(_filenameToken,"Filename",_outputFilename);
			_session.addLogMessage("", "Completed", String.format("Data saved to %s", _outputFilename));
		} catch (Exception e) {
			throw new PieException("Error while trying to export the data into a delimited file.", e);
		}
		_session.clearDataTokens();
		return _outputFilename;
	}

	protected void defineOutputColumns(String[] fileColumns) {
		List<String> inputColumnNames = Arrays.asList(fileColumns);

		NodeList outputColumnNodes = XmlUtilities.selectNodes(_action, "Column");
		int numberOfOutputColumns = outputColumnNodes.getLength();

		if (numberOfOutputColumns > 0) {
			_outputColumnNames = new String[numberOfOutputColumns];
			_outputColumnIndexes = new int[numberOfOutputColumns];

			for (int i = 0; i < numberOfOutputColumns; i++) {
				Element columnElement = (Element) outputColumnNodes.item(i);

				String inputName = _session.getAttribute(columnElement, "Name");
				String alais = _session.getAttribute(columnElement, "Alias");

				_outputColumnNames[i] = StringUtilities.isNotNullOrEmpty(alais) ? alais : inputName;
				_outputColumnIndexes[i] = inputColumnNames.indexOf(inputName);
				if (_outputColumnIndexes[i] == -1) {
					_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "Column", String.format("Column %s not found in the data set. Defaulting to empty string.", StringUtilities.isNullOrEmpty(inputName) ? alais : inputName));
				}
			}
		} else {
			numberOfOutputColumns = inputColumnNames.size();
			_outputColumnNames = new String[numberOfOutputColumns];
			_outputColumnIndexes = new int[numberOfOutputColumns];

			for (int i = 0; i < numberOfOutputColumns; i++) {
				_outputColumnNames[i] = inputColumnNames.get(i);
				_outputColumnIndexes[i] = i;
			}
		}
		_outputLength = _outputColumnIndexes.length;
	}

	protected String wrapString(Object objectValue) {
		if (objectValue == null) {
			return "";
		}
		
		boolean wrapDoubleQuotes = false;
		String value = objectValue.toString();

		if (value.contains(System.lineSeparator())) {
			value = value.replace(System.lineSeparator(), " ");
		}
		
		if (_trimSpaces) {
			value = value.trim();
		}
		if (StringUtilities.isNullOrEmpty(value)) {
			return "";
		} else if (value.contains("\"")) {
			value = value.replace("\"", "\"\"");
			wrapDoubleQuotes = true;
		}

		if (value.contains(_delimiter)) {
			wrapDoubleQuotes = true;
		}

		if (wrapDoubleQuotes) {
			value = "\"" + value + "\"";
		}
		return value;
	}

	protected String doubleFormat(Object value) {
		if (value == null) {
			return "";
		}
		return String.format("%.2f", (double) value);
	}
}
