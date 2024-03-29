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

package com.fanniemae.ezpie.common;

import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-23
 * 
 */

public final class DataUtilities {

	private DataUtilities() {
	}

	public static Map<String, String> dataRowToTokenHash(String[][] schema, Object[] dataRow) {
		HashMap<String, String> dataTokens = new HashMap<String, String>();

		for (int i = 0; i < schema.length; i++) {
			String dataType = schema[i][1];
			dataType = (dataType == null) ? "string" : dataType.toLowerCase();
			//@formatter:off
			if ((dataRow[i] == null) && (dataType.contains("double") 
                                     || dataType.contains("int") 
                                     || dataType.contains("float"))) {
				dataTokens.put(schema[i][0], "0");
			} else if ((dataRow[i] == null) && dataType.contains("short") 
                                            || dataType.contains("byte")) {
				dataTokens.put(schema[i][0], "0");
			} else if (dataRow[i] == null) {
				dataTokens.put(schema[i][0], "");
			} else if (dataType.contains("date") || dataType.contains("time")) {
				dataTokens.put(schema[i][0], DateUtilities.toIsoString((Date) dataRow[i]));
			} else {
				dataTokens.put(schema[i][0], dataRow[i].toString());
			}
			//@formatter:on
		}
		return dataTokens;
	}

	public static int dbStringTypeToJavaSqlType(String dataType) {
		switch (dataType.toLowerCase()) {
		case "bigint":
		case "unsignedbigint":
		case "unsignedint":
			return Types.BIGINT;
		case "binary":
			return Types.BINARY;
		case "bstr":
			return Types.LONGNVARCHAR;
		case "boolean":
			return Types.BOOLEAN;
		case "char":
			return Types.CHAR;
		case "date":
		case "dbtime":
			return Types.DATE;
		case "dbtimestamp":
			return Types.TIMESTAMP;
		case "currency":
		case "decimal":
		case "numeric":
		case "varnumeric":
			return Types.DECIMAL;
		case "double":
			return Types.DOUBLE;
		case "filetime":
			return Types.TIME;
		case "int":
		case "integer":
		case "single":
		case "smallint":
		case "tinyint":
		case "unsignedsmallint":
		case "unsignedtinyint":
			return Types.INTEGER;
		case "longvarbinary":
			return Types.LONGVARBINARY;
		case "longvarchar":
			return Types.LONGVARCHAR;
		case "longvarwchar":
			return Types.LONGNVARCHAR;
		case "varbinary":
			return Types.VARBINARY;
		case "varchar":
		case "guid":
			return Types.VARCHAR;
		case "variant":
			return Types.OTHER;
		case "varwchar":
			return Types.NVARCHAR;
		case "wchar":
			return Types.NCHAR;
		default:
			return Types.VARCHAR;
		}
	}

	public static DataType dataTypeToEnum(String sTypeName) {
		if ((sTypeName == null) || sTypeName.isEmpty()) {
			return DataType.StringData;
		}
		switch (sTypeName) {
		case "BigDecimal":
		case "java.math.BigDecimal":
		case "BigDecimalData":
			return DataType.BigDecimalData;
		case "Boolean":
		case "Bool":
		case "java.lang.Boolean":
		case "BooleanData":
			return DataType.BooleanData;
		case "Byte":
		case "java.lang.Byte":
		case "ByteData":
			return DataType.ByteData;
		case "Char":
		case "Character":
		case "java.lang.Character":
		case "CharData":
			return DataType.CharData;
		case "Date":
		case "java.util.Date":
		case "java.sql.Date":
		case "DateData":
			return DataType.DateData;
		case "Double":
		case "java.lang.Double":
		case "DoubleData":
			return DataType.DoubleData;
		case "Float":
		case "java.lang.Float":
		case "FloatData":
			return DataType.FloatData;
		case "Int":
		case "Integer":
		case "java.lang.Integer":
		case "java.lang.int":
		case "IntegerData":
			return DataType.IntegerData;
		case "Long":
		case "java.lang.Long":
		case "LongData":
			return DataType.LongData;
		case "Short":
		case "java.lang.Short":
		case "ShortData":
			return DataType.ShortData;
		case "String":
		case "java.lang.String":
		case "StringData":
			return DataType.StringData;
		case "Timestamp":
		case "java.sql.Timestamp":
		case "SqlTimestampData":
			return DataType.SqlTimestampData;
		case "Object":
		case "java.lang.Object":
		case "ObjectData":
			return DataType.ObjectData;
		case "OracleClob":
		case "oracle.jdbc.OracleClob":
		case "ClobData":
			return DataType.ClobData;
		default:
			throw new PieException(String.format("Error during DataTypeToEnum conversion. %s type name not supported.", sTypeName));
		}
	}

	public static Class<?> stringNameToJavaType(String typeName) {
		try {
			if (typeName == null) {
				return Class.forName("java.lang.String");
			}
			switch (typeName.toLowerCase()) {
			case "java.math.bigdecimal":
			case "bigdecimaldata":
			case "bigdecimal":
				return Class.forName("java.math.BigDecimal");
			case "java.lang.boolean":
			case "booleandata":
			case "boolean":
				return Class.forName("java.lang.Boolean");
			case "java.lang.byte":
			case "bytedata":
			case "byte":
				return Class.forName("java.lang.Byte");
			case "java.lang.character":
			case "chardata":
			case "char":
				return Class.forName("java.lang.Character");
			case "java.util.date":
			case "datedata":
			case "date":
				return Class.forName("java.util.Date");
			case "java.lang.double":
			case "doubledata":
			case "double":
				return Class.forName("java.lang.Double");
			case "java.lang.float":
			case "floatdata":
			case "float":
				return Class.forName("java.lang.Float");
			case "java.lang.integer":
			case "integerdata":
			case "integer":
			case "int":
				return Class.forName("java.lang.Integer");
			case "java.lang.long":
			case "longdata":
			case "long":
				return Class.forName("java.lang.Long");
			case "java.lang.short":
			case "shortdata":
			case "short":
				return Class.forName("java.lang.Short");
			case "java.lang.string":
			case "stringdata":
			case "string":
				return Class.forName("java.lang.String");
			case "java.sql.timestamp":
			case "sqltimestampdata":
			case "sqltimestamp":
				return Class.forName("java.sql.Timestamp");
			case "java.lang.object":
			case "objectdata":
			case "object":
				return Class.forName("java.lang.Object");
			default:
				throw new PieException(String.format("Error during DataTypeToEnum conversion. %s type name not supported.", typeName));
			}
		} catch (ClassNotFoundException e) {
			throw new PieException(String.format("Could not convert %s into a java type name. %s", typeName, e.getMessage()), e);
		}
	}
}
