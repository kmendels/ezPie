package com.fanniemae.automation.datafiles;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fanniemae.automation.common.CryptoUtilities;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.datafiles.lowlevel.BinaryOutputStream;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums;
import com.fanniemae.automation.datafiles.lowlevel.DataFileEnums.DataType;
import com.fanniemae.automation.datafiles.lowlevel.DataFormat;
import com.fanniemae.automation.datafiles.lowlevel.DataRow;
import com.fanniemae.automation.datafiles.lowlevel.FieldBigDecimal;
import com.fanniemae.automation.datafiles.lowlevel.FieldBoolean;
import com.fanniemae.automation.datafiles.lowlevel.FieldByte;
import com.fanniemae.automation.datafiles.lowlevel.FieldChar;
import com.fanniemae.automation.datafiles.lowlevel.FieldDate;
import com.fanniemae.automation.datafiles.lowlevel.FieldDouble;
import com.fanniemae.automation.datafiles.lowlevel.FieldFloat;
import com.fanniemae.automation.datafiles.lowlevel.FieldInteger;
import com.fanniemae.automation.datafiles.lowlevel.FieldLong;
import com.fanniemae.automation.datafiles.lowlevel.FieldReadWrite;
import com.fanniemae.automation.datafiles.lowlevel.FieldShort;
import com.fanniemae.automation.datafiles.lowlevel.FieldSqlTimestamp;
import com.fanniemae.automation.datafiles.lowlevel.FieldString;
import com.fanniemae.automation.datafiles.lowlevel.FieldStringEncrypted;
import com.fanniemae.automation.datafiles.lowlevel.FieldUUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-28
 * 
 */
public class DataWriter extends DataFormat {
	private final BinaryOutputStream _bos;
	protected FieldReadWrite[] _WriteMethods = null;

	protected int _ColumnCount = 0;
	protected Map<String, String[]> _GlobalValues = new HashMap<>();
	protected Map<String, List<String[]>> _ColumProfiles = new HashMap<>();

	public DataWriter(String filename) throws IOException {
		this(filename, 20, "", null, false);
	}

	public DataWriter(String filename, boolean isDynamicSqlBuffer) throws IOException {
		this(filename, 0, "", null, isDynamicSqlBuffer);
	}

	public DataWriter(String filename, int memoryLimitInMegabytes) throws IOException {
		this(filename, memoryLimitInMegabytes, "", null, false);
	}

	public DataWriter(String filename, int memoryLimitInMegabytes, boolean isDynamicSqlBuffer) throws IOException {
		this(filename, memoryLimitInMegabytes, "", null, isDynamicSqlBuffer);
	}

	public DataWriter(String filename, int memoryLimitInMegabytes, String sourceDataFilename, UUID fingerPrint, boolean isDynamicSqlBuffer) throws IOException {
		_Filename = filename;
		_SourceDataFilename = sourceDataFilename;
		_byteFileType = 1;

		if (isDynamicSqlBuffer) {
			_IndexInterval = 500L; // Used by index to determine how often to add
								// entry.
			_NextBreak = 500L; // Next row count to add an index entry.
		}

		if (fingerPrint == null) {
			_byteFileType = 0;
			_FingerPrint = UUID.randomUUID().toString();
		} else {
			_FingerPrint = fingerPrint.toString();
		}

		// If this is a View file, then get the data filename only.
		if (StringUtilities.isNotNullOrEmpty(_SourceDataFilename) && _SourceDataFilename.contains(File.separator)) {
			File oFile = new File(_SourceDataFilename);
			_SourceDataFilename = oFile.getName();
		}

		_bos = new BinaryOutputStream(_Filename, memoryLimitInMegabytes, _FingerPrint);
		writeInitialHeader(); // Place holder for final information.
	}

	@Override
	public void close() throws IOException {
		if ((_bos != null) && (!_Disposed)) {
			try {
				writeFooter();
				writeFinalHeader();
			} finally {
				_bos.close();
				_Disposed = true;
			}
		}
	}

	public void setDataColumns(String[] columnNames, DataType[] dataTypes) throws IOException {
		_DataRow = new DataRow(columnNames.length);
		for (int i = 0; i < columnNames.length; i++) {
			defineDataColumn(i, columnNames[i], dataTypes[i]);
		}
		setupColumnWriters();
	}

	public void setDataColumns(String[][] columnNamesAndTypes) throws IOException {
		int columnCount = columnNamesAndTypes.length;
		_DataRow = new DataRow(columnCount);

		for (int i = 0; i < columnCount; i++) {
			defineDataColumn(i, columnNamesAndTypes[i][0], columnNamesAndTypes[i][1]);
		}
		setupColumnWriters();
	}

	public void setGlobalValue(String columnName, String dataType, String value) {
		_GlobalValues.put(columnName, new String[] { dataType, value });
	}

	public void setColumnProfile(String columnName, List<String[]> profile) {
		_ColumProfiles.put(columnName, profile);
	}

	public void writeDataRow(Object[] data) throws IOException {
		if (_CurrentRowNumber == _NextBreak) {
			IndexEntry ie = new IndexEntry();
			ie.RowNumber = _CurrentRowNumber;
			ie.OffSet = _bos.getPosition();
			_IndexBlock.add(ie);
			_NextBreak += _IndexInterval;
		}

		for (int i = 0; i < _ColumnCount; i++) {
			Boolean isNull = false;
			if (data[i] == null) {
				isNull = true;
			}
			_WriteMethods[i].Write(data[i], isNull);
		}
		_CurrentRowNumber++;
	}

	public DataStream getDataStream() throws IOException {
		this.close();
		if (_bos == null) {
			throw new IOException("No data written to either memory or file.");
		}

		if (_bos.IsFilestream()) {
			return new DataStream(_Filename, _HeaderInformation);
		} else {
			return new DataStream(_bos.getBuffer(),_HeaderInformation);
		}
	}

	public byte[] getDataBuffer() throws IOException {
		if (_bos == null) {
			throw new IOException("No data written to either memory or file.");
		}

		if (_bos.IsFilestream()) {
			throw new IOException("getDataBuffer is only available when data is written to memory.");
		}

		return _bos.getBuffer();
	}

	public boolean isFilestream() {
		return _bos.IsFilestream();
	}

	public Map<DataFileEnums.BinaryFileInfo, Object> getHeader() {
		populateHeaderInformation();
		return _HeaderInformation;
	}

	protected void setupColumnWriters() throws IOException {
		int iCnt = _DataRow.getColumnCount();
		_WriteMethods = new FieldReadWrite[iCnt];
		for (int i = 0; i < iCnt; i++) {
			_WriteMethods[i] = getWriteMethod(_DataRow.getDataType(i));
			_DataRow.setDataType(i, adjustedDataType(_DataRow.getDataType(i)));
		}
		_ColumnCount = _DataRow.getColumnCount();
	}

	protected void writeInitialHeader() throws IOException {
		_bos.write(buildHeader());
	}

	protected void writeFinalHeader() throws IOException {
		_bos.writeFinalHeader(buildHeader());
	}

	protected byte[] buildHeader() throws IOException {
		byte[] aHeader;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeByte(_byteFileType); // (byte) Data file type 0=Data,
											// 1=View
			dos.writeBoolean(_isEncrypted); // (Boolean) Encrypted True/False
			dos.writeUTF(_FingerPrint); // (string) The internal UUID used to
											// identify this file.
			dos.writeUTF(_SourceDataFilename); // (string) Write the name of
												// the source dat file. '' if
												// this is a dat file.
			dos.writeBoolean(_FullRowCountKnown); // (boolean) Only used by
													// ActiveSQL connections -
													// may or may not know row
													// count.
			dos.writeLong(_FullRowCount); // (long) Row count of full record
											// set
			dos.writeLong(_FirstRow); // (long) Row number of first data row in
										// this file
			dos.writeLong(_LastRow); // (long) Row number of last data row in
										// this file
			dos.writeLong(_IndexStart); // (long) Offset to start of direct
											// access index (Int64)
			dos.writeLong(_SchemaStart); // (long) Offset to start of
											// Information block (Xml format)
			dos.writeLong(_DateCreated.getTime()); // (DateTime/long) Datetime
													// file created
			dos.writeLong(_DateExpires.getTime()); // (DateTime/long) Datetime
													// file expires
			baos.flush();
			aHeader = baos.toByteArray();
		}
		return aHeader;
	}

	protected void writeFooter() throws IOException {
		// Write the index
		_IndexStart = _bos.getPosition();
		for (IndexEntry indexEntry : _IndexBlock) {
			long lRowNum = indexEntry.RowNumber;
			long lOffSet = indexEntry.OffSet;
			_bos.writeLong(lRowNum);
			_bos.writeLong(lOffSet);
		}

		// Write the InfoBlock
		_SchemaStart = _bos.getPosition();
		Document xmlSchemaDoc = XmlUtilities.CreateXMLDocument("<FileInfo><DataInfo /></FileInfo>");
		if ((_DataRow != null) && (_DataRow.getColumnNames() != null)) {
			int columnCount = _DataRow.getColumnCount();
			for (int i = 0; i < columnCount; i++) {
				String columnName = _DataRow.getColumnName(i);
				Element eleCol = xmlSchemaDoc.createElement("DataColumn");
				eleCol.setAttribute("Name", columnName);
				eleCol.setAttribute("DataType", _DataRow.getDataType(i).toString());
				eleCol.setAttribute("ColumnType", _DataRow.getColumnType(i).toString());
				// eleCol.SetAttribute("GlobalValue",
				// _DataRow.aValues[i].ToString());
				// eleCol.SetAttribute("NullCount",
				// _aColumnDetails[i].NullCount.ToString());
				// eleCol.SetAttribute("MinValue", _aColumnDetails[i].MinValue);
				// eleCol.SetAttribute("MaxValue", _aColumnDetails[i].MaxValue);
				// eleCol.SetAttribute("MinLength",
				// _aColumnDetails[i].MinLength.ToString());
				// eleCol.SetAttribute("MaxLength",
				// _aColumnDetails[i].MaxLength.ToString());
				// eleCol.SetAttribute("Sum",
				// _aColumnDetails[i].Sum.ToString());
				// eleCol.SetAttribute("Average",
				// ComputeAverage(_aColumnDetails[i].Sum).ToString());

				if (_ColumProfiles.containsKey(columnName)) {
					List<String[]> columnProfiles = _ColumProfiles.get(columnName);
					for (String[] profile : columnProfiles) {
						Element eleProfile = xmlSchemaDoc.createElement(profile[0]);
						eleProfile.setAttribute("DataType", profile[1]);
						eleProfile.setAttribute("Value", profile[2]);
						eleCol.appendChild(eleProfile);
					}
				}
				xmlSchemaDoc.getDocumentElement().getFirstChild().appendChild(eleCol); // .documentElement.FirstChild.AppendChild(eleCol);
			}

			for (Map.Entry<String, String[]> kvp : _GlobalValues.entrySet()) {
				Element eleCol = xmlSchemaDoc.createElement("DataColumn");
				eleCol.setAttribute("Name", kvp.getKey());
				eleCol.setAttribute("DataType", kvp.getValue()[0]);
				eleCol.setAttribute("ColumnType", "GlobalValue");
				eleCol.setAttribute("GlobalValue", kvp.getValue()[1]);
				xmlSchemaDoc.getDocumentElement().getFirstChild().appendChild(eleCol); // .DocumentElement.FirstChild.AppendChild(eleCol);
			}
		}
		_SchemaXML = XmlUtilities.XMLDocumentToString(xmlSchemaDoc);

		if (_isEncrypted) {
			_bos.writeUTF(CryptoUtilities.EncryptDecrypt(_SchemaXML));
		} else {
			_bos.writeUTF(_SchemaXML);
		}

		_DateCreated = new Date();
	}

	private DataType adjustedDataType(DataType ColumnDataType) {
		// Simplified code to convert some types into others. E.g. Byte, Int16,
		// SByte ==> Int32
		switch (ColumnDataType) {
		case BigDecimalData:
			return DataType.DoubleData;
		case ByteData:
			return DataType.IntegerData;
		case FloatData:
			return DataType.DoubleData;
		case ShortData:
			return DataType.IntegerData;
		case SqlTimestampData:
			return DataType.DateData;
		default:
			return ColumnDataType;
		}
	}

	private FieldReadWrite getWriteMethod(DataType ColumnDataType) throws IOException {
		// Simplified code to convert some types into others. E.g. Byte, Int16,
		// SByte ==> Int32
		switch (ColumnDataType) {
		case BigDecimalData:
			return new FieldBigDecimal(_bos);
		case BooleanData:
			return new FieldBoolean(_bos);
		case ByteData:
			return new FieldByte(_bos);
		case CharData:
			return new FieldChar(_bos);
		case DateData:
			return new FieldDate(_bos);
		case DoubleData:
			return new FieldDouble(_bos);
		case FloatData:
			return new FieldFloat(_bos);
		case IntegerData:
			return new FieldInteger(_bos);
		case LongData:
			return new FieldLong(_bos);
		case ShortData:
			return new FieldShort(_bos);
		case SqlTimestampData:
			return new FieldSqlTimestamp(_bos);
		case StringData:
			if (_isEncrypted) {
				return new FieldStringEncrypted(_bos);
			} else {
				return new FieldString(_bos);
			}
		case UUIDData:
			return new FieldUUID(_bos);
		default:
			throw new IOException("Data type " + ColumnDataType.toString() + " is not currently supported by the data engine.");
		}
	}

}
