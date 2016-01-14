package com.fanniemae.automation.common;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-15
 * 
 */
public class StringUtilities {

	// @formatter:off
	// Source: http://docs.oracle.com/javase/6/docs/api/java/lang/Double.html#valueOf%28java.lang.String%29
	protected static String _DoubleRegex = "[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*";
	
	protected static String _BooleanValues = "|true|false|t|f|0|1|yes|no|on|off|y|n|";
	
	protected static String[] _SupportedDateFormats = new String[] { "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", 
                                                            "yyyy/MM/dd HH:mm:ss", "MM-dd-yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss",
                                                            "dd/MM/yyyy HH:mm:ss", "MM-yyyy HH:mm:ss", "MM/yyyy HH:mm:ss", "yyyy-MM-dd'T'HH:mm", 
                                                            "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "MM-dd-yyyy HH:mm",
                                                            "MM/dd/yyyy HH:mm", "dd-MM-yyyy HH:mm", "dd/MM/yyyy HH:mm", "MM-yyyy HH:mm", "MM/yyyy HH:mm",
                                                            "yyyy-MM-dd'T'HH", "yyyy-MM-dd HH", "yyyy/MM/dd HH", "yyyy-MM-dd HH", "yyyy/MM/dd HH", 
                                                            "MM-dd-yyyy HH", "MM/dd/yyyy HH", "dd-MM-yyyy HH", "dd/MM/yyyy HH", "MM-yyyy HH", "MM/yyyy HH",
                                                            "yyyy-MM-dd", "yyyy/MM/dd", "MM-dd-yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "dd/MM/yyyy", "MM-yyyy",
                                                            "MM/yyyy", "yyyy-M-dd'T'HH:M:ss", "yyyy-M-dd HH:M:ss", "yyyy/M/dd HH:M:ss", "yyyy-M-dd HH:M:ss", 
                                                            "yyyy/M/dd HH:M:ss", "M-dd-yyyy HH:M:ss", "M/dd/yyyy HH:M:ss", "dd-M-yyyy HH:M:ss", "dd/M/yyyy HH:M:ss",
                                                            "M-yyyy HH:M:ss", "M/yyyy HH:M:ss", "yyyy-M-dd'T'HH:M", "yyyy-M-dd HH:M", "yyyy/M/dd HH:M", 
                                                            "yyyy-M-dd HH:M", "yyyy/M/dd HH:M", "M-dd-yyyy HH:M", "M/dd/yyyy HH:M", "dd-M-yyyy HH:M",
                                                            "dd/M/yyyy HH:M", "M-yyyy HH:M", "M/yyyy HH:M", "yyyy-M-dd'T'HH", "yyyy-M-dd HH", "yyyy/M/dd HH", 
                                                            "yyyy-M-dd HH", "yyyy/M/dd HH", "M-dd-yyyy HH", "M/dd/yyyy HH", "dd-M-yyyy HH", "dd/M/yyyy HH",
                                                            "M-yyyy HH", "M/yyyy HH", "yyyy-M-dd", "yyyy/M/dd", "M-dd-yyyy", "M/dd/yyyy", "dd-M-yyyy",
                                                            "dd/M/yyyy", "M-yyyy", "M/yyyy", "yyyy-M-d'T'HH:M:ss", "yyyy-M-d HH:M:ss", "yyyy/M/d HH:M:ss", 
                                                            "yyyy-M-d HH:M:ss", "yyyy/M/d HH:M:ss", "M-d-yyyy HH:M:ss", "M/d/yyyy HH:M:ss", "d-M-yyyy HH:M:ss",
                                                            "d/M/yyyy HH:M:ss", "M-yyyy HH:M:ss", "M/yyyy HH:M:ss", "yyyy-M-d'T'HH:M", "yyyy-M-d HH:M", 
                                                            "yyyy/M/d HH:M", "yyyy-M-d HH:M", "yyyy/M/d HH:M", "M-d-yyyy HH:M", "M/d/yyyy HH:M", "d-M-yyyy HH:M",
		                                                    "d/M/yyyy HH:M", "M-yyyy HH:M", "M/yyyy HH:M", "yyyy-M-d'T'HH", "yyyy-M-d HH", "yyyy/M/d HH", 
                                                            "yyyy-M-d HH", "yyyy/M/d HH", "M-d-yyyy HH", "M/d/yyyy HH", "d-M-yyyy HH", "d/M/yyyy HH", "M-yyyy HH",
                                                            "M/yyyy HH", "yyyy-M-d", "yyyy/M/d", "M-d-yyyy", "M/d/yyyy", "d-M-yyyy", "d/M/yyyy", "M-yyyy",
                                                            "M/yyyy", "yyyy-MM-d'T'HH:mm:ss", "yyyy-MM-d HH:mm:ss", "yyyy/MM/d HH:mm:ss", "yyyy-MM-d HH:mm:ss", 
                                                            "yyyy/MM/d HH:mm:ss", "MM-d-yyyy HH:mm:ss", "MM/d/yyyy HH:mm:ss", "d-MM-yyyy HH:mm:ss",
                                                            "d/MM/yyyy HH:mm:ss", "MM-yyyy HH:mm:ss", "MM/yyyy HH:mm:ss", "yyyy-MM-d'T'HH:mm", "yyyy-MM-d HH:mm", 
                                                            "yyyy/MM/d HH:mm", "yyyy-MM-d HH:mm", "yyyy/MM/d HH:mm", "MM-d-yyyy HH:mm", "MM/d/yyyy HH:mm",
                                                            "d-MM-yyyy HH:mm", "d/MM/yyyy HH:mm", "MM-yyyy HH:mm", "MM/yyyy HH:mm", "yyyy-MM-d'T'HH", 
                                                            "yyyy-MM-d HH", "yyyy/MM/d HH", "yyyy-MM-d HH", "yyyy/MM/d HH", "MM-d-yyyy HH", "MM/d/yyyy HH",
                                                            "d-MM-yyyy HH", "d/MM/yyyy HH", "MM-yyyy HH", "MM/yyyy HH", "yyyy-MM-d", "yyyy/MM/d", "MM-d-yyyy",
                                                            "MM/d/yyyy", "d-MM-yyyy", "d/MM/yyyy", "MM-yyyy", "MM/yyyy", "EEE, d MMM yyyy HH:mm:ss",
                                                            "d MMM yyyy HH:mm:ss", "d MMM yyyy", "MMM d, yyyy", "MMM dd, yyyy", "yyyy/MM", "yyyy/M",
                                                            "yyyy-MM", "yyyy-M"};
	// @formatter:on

	public static boolean isNotNullOrEmpty(String value) {
		return !isNullOrEmpty(value);
	}

	public static boolean isNullOrEmpty(String value) {
		return (value == null) || value.isEmpty();
	}

	public static boolean isFormattedDate(String value) {
		// Check for ISO 8601 Date Format (yyyy-MM-ddTHH:mm:ss)
		if (isNotNullOrEmpty(value) && (value.length() > 14)) {
			String sCheck = value.substring(4, 5) + value.substring(7, 8) + value.substring(10, 11) + value.substring(13, 14);
			return sCheck.equals("--T:");
		}
		return false;
	}

	public static boolean isDate(String value) {
		try {
			DateUtils.parseDateStrictly(value, _SupportedDateFormats);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static boolean isLong(String value) {
		return isNullOrEmpty(value) ? false : (Pattern.matches("^[+-]?\\d+$", value) && (value.length() <= 17));
	}

	public static boolean isInteger(String value) {
		return isNullOrEmpty(value) ? false : (Pattern.matches("^[+-]?\\d+$", value) && (value.length() <= 8));
	}

	public static boolean isDouble(String value) {
		return isNullOrEmpty(value) ? false : Pattern.matches(_DoubleRegex, value);
	}

	public static boolean isBigDecimal(String value) {
		return isNullOrEmpty(value) ? false : Pattern.matches(_DoubleRegex, value);
	}

	public static boolean isFormula(String value) {
		char[] aSymbols = ".*/+-()=<>!^#&@$%\\|{}'?".toCharArray();
		for (int i = 0; i < aSymbols.length; i++) {
			if (value.indexOf(aSymbols[i]) != -1) {
				return true;
			}
		}
		return false;
	}

	public static boolean isBoolean(String value) {
		return (isNullOrEmpty(value) || (_BooleanValues.indexOf("|" + value.toLowerCase() + "|") == -1)) ? false : true;
	}

	public static boolean toBoolean(String value) {
		return toBoolean(value, false);
	}

	public static boolean toBoolean(String value, Boolean defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		else if ("|true|t|y|1|on|yes|".indexOf("|" + value.toLowerCase() + "|") > -1)
			return true;
		else if ("|false|f|n|0|off|no|".indexOf("|" + value.toLowerCase() + "|") > -1)
			return false;
		return defaultValue;
	}

	public static int toInteger(String value) {
		return toInteger(value, 0);
	}

	public static int toInteger(String value, int defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static long toLong(String value) {
		return toLong(value, 0L);
	}

	public static long toLong(String value, long defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static double toDouble(String value) {
		return toDouble(value, 0.0);
	}

	public static double toDouble(String value, double defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static BigDecimal toBigDecimal(String value) {
		return toBigDecimal(value, new BigDecimal("0.0"));
	}

	public static BigDecimal toBigDecimal(String value, BigDecimal defaultValue) {
		if (isNullOrEmpty(value))
			return defaultValue;
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static Date toDate(String value) {
		return toDate(value, null);
	}

	public static Date toDate(String value, Date defaultValue) {
		try {
			return DateUtils.parseDateStrictly(value, _SupportedDateFormats);
		} catch (Exception ex) {
			return defaultValue;
		}
	}

	public static Object toObject(String typeName, String value) {
		if ((value == null) || value.isEmpty()) {
			return "";
		}

		switch (typeName) {
		case "Boolean":
			return Boolean.parseBoolean(value);
		case "Byte":
		case "SByte":
			return Byte.parseByte(value);
		case "Byte[]":
			return value.toCharArray();
		case "Char":
			return value.charAt(0);
		case "Char[]":
			return value.toCharArray();
		case "DateTime":
			Date dtValue = new Date(Long.parseLong(value));
			if (dtValue == new Date(Long.MIN_VALUE)) {
				return null;
			}
			return dtValue;
		case "Decimal":
		case "Double":
		case "Single":
			return Double.parseDouble(value);
		case "Float":
			return Float.parseFloat(value);
		case "UUID":
			return UUID.fromString(value);
		case "Int":
		case "Integer":
		case "Int16":
		case "Int32":
		case "UInt16":
			return Integer.parseInt(value);
		case "Int64":
		case "Long":
		case "UInt32":
		case "UInt64":
			return Long.parseLong(value);
		case "NCLOB":
			return "NCLOB";
		case "Short":
			return Short.parseShort(value);
		case "String":
			return value;
		case "TimeSpan":
			return value;
		default:
			return null;
		}
	}

	public static String wrapValue(String value) {
		if ((value == null) || (value.indexOf(' ') == -1))
			return value;
		return String.format("\"%s\"", value);
	}

	public static String getDataType(String value, String previousType) {
		if (isNullOrEmpty(value)) {
			return previousType;
		} else if (isNullOrEmpty(previousType)) {
			if (isBoolean(value)) {
				return "BooleanData";
			} else if (isDate(value)) {
				return "DateTimeData";
			} else if (isInteger(value)) {
				return "IntegerData";
			} else if (isLong(value)) {
				return "LongData";
			} else if (isDouble(value)) {
				return "DoubleData";
			} else if (isBigDecimal(value)) {
				return "BigDecimal";
			} else {
				return "StringData";
			}
		} else if (previousType.equals("StringData")) {
			return previousType;
		} else if (previousType.equals("BooleanData")) {
			return isBoolean(value) ? "BooleanData" : "StringData";
		} else if (previousType.equals("DateTimeData")) {
			return isDate(value) ? previousType : "StringData";
		} else if (previousType.equals("IntegerData") && isInteger(value)) {
			return previousType;
		} else if ((previousType.equals("IntegerData") || previousType.equals("LongData")) && isLong(value)) {
			return "LongData";
		} else if (("|IntegerData|LongData|DoubleData|".indexOf("|" + previousType + "|") > -1) && isDouble(value)) {
			return "DoubleData";
		} else if ("|IntegerData|LongData|DoubleData|BigDecimalData".indexOf("|" + previousType + "|") > -1) {
			return isBigDecimal(value) ? "BigDecimalData" : "StringData";
		} else if (isNotNullOrEmpty(previousType)) {
			return "StringData";
		} else {
			throw new RuntimeException("Unable to detect delimited file schema format.");
		}
	}
}
