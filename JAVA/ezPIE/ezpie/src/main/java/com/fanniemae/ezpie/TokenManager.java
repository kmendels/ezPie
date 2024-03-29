/**
 * 
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.Encryption;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-14
 * 
 */
public class TokenManager {

	protected HashMap<String, HashMap<String, String>> _tokens = new HashMap<String, HashMap<String, String>>();
	protected Map<String, String> _dataTokens = null;

	protected LogManager _logger;
	protected Date _startDateTime = new Date();

	protected String _tokenPrefix = "[";
	protected String _tokenSuffix = "]";
	
	protected static final String TOKEN_ADDED = "Token Added";
	protected static final String TOKENS_ADDED = "Tokens Added";
	protected static final String TOKEN_ADDED_FORMAT_STRING = "%1$s%2$s.%3$s%4$s = %5$s";

	protected byte[][] _encryptionKey = null;

	protected enum LogVisibility {
		NONE, TOKEN_NAME, FULL
	}

	public TokenManager(Element eleSettings, LogManager logger, byte[][] encryptionKey, String tokenPrefix, String tokenSuffix) {
		_logger = logger;
		_encryptionKey = encryptionKey;
		_tokenPrefix = tokenPrefix;
		_tokenSuffix = tokenSuffix;

		loadTokenValues(eleSettings, "Configuration");

		NodeList nl = eleSettings.getElementsByTagName("Tokens");
		int length = nl.getLength();
		for (int i = 0; i < length; i++) {
			loadTokenValues(nl.item(i));
		}
	}

	public void setDataTokens(Map<String, String> dataTokens) {
		_dataTokens = dataTokens;
	}

	public void clearDataTokens() {
		_dataTokens = null;
	}

	public void addToken(String tokenType, String key, String value) {
		HashMap<String, String> aTokenValues = new HashMap<String, String>();
		if (_tokens.containsKey(tokenType)) {
			aTokenValues = _tokens.get(tokenType);
		}

		aTokenValues.put(key, value);
		_tokens.put(tokenType, aTokenValues);
		if (hideIt(key)) {
			_logger.addMessage("", TOKEN_ADDED, String.format(TOKEN_ADDED_FORMAT_STRING, _tokenPrefix, tokenType, key, _tokenSuffix, Constants.VALUE_HIDDEN_MESSAGE));
			return;
		}
		_logger.addMessage("", TOKEN_ADDED, String.format(TOKEN_ADDED_FORMAT_STRING, _tokenPrefix, tokenType, key, _tokenSuffix, value));
	}

	public void addTokens(Node tokenNode) {
		loadTokenValues(tokenNode);
	}

	public void addTokens(Map<String, String> newTokens) {
		addTokens("Local", newTokens);
	}

	public void addTokens(String tokenType, Map<String, String> newTokens) {
		HashMap<String, String> domainTokens = new HashMap<String, String>();
		if (_tokens.containsKey(tokenType)) {
			domainTokens = _tokens.get(tokenType);
		}

		int added = 0;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : newTokens.entrySet()) {
			if (added > 0) {
				sb.append(_logger.getNewLineTab());
			}
			String key = entry.getKey();
			String value = entry.getValue();
			domainTokens.put(key, value);
			sb.append(String.format(TOKEN_ADDED_FORMAT_STRING, _tokenPrefix, tokenType, key, _tokenSuffix, value));
			added++;
		}
		_tokens.put(tokenType, domainTokens);
		_logger.addMessage("", added == 1 ? TOKEN_ADDED : TOKENS_ADDED, sb.toString());
	}

	public void addTokens(String tokenType, String[][] kvps) {
		loadTokenValues(tokenType, kvps);
	}

	public String getAttribute(Element ele, String sName) {
		return resolveTokens(ele.getAttribute(sName));
	}

	public String getTokenValue(String tokenType, String tokenKey) {
		if (_tokens.containsKey(tokenType) && _tokens.get(tokenType).containsKey(tokenKey)) {
			return _tokens.get(tokenType).get(tokenKey);
		}
		return "";
	}

	public String getTokenPrefix() {
		return _tokenPrefix;
	}

	public void setTokenPrefix(String value) {
		_tokenPrefix = value;
	}

	public String getTokenSuffix() {
		return _tokenSuffix;
	}

	public void setTokenSuffix(String value) {
		_tokenSuffix = value;
	}

	public String resolveTokens(String value) {
		if (value == null) {
			return value;
		}

		String rawString = (_dataTokens == null) ? value.replace(String.format("%sData.", _tokenPrefix), "|Data|") : value;

		int tokenStart = rawString.indexOf(_tokenPrefix);
		if (tokenStart == -1) {
			return value;
		}

		int tokenMid = rawString.indexOf(".", tokenStart);
		if (tokenMid == -1) {
			return value;
		}

		int tokenEnd = rawString.indexOf(_tokenSuffix, tokenMid);
		if (tokenEnd == -1) {
			return value;
		}

		int tokenSplit = 0;
		int iTokenEnd = 0;
		String[] aTokens = value.split(String.format("\\Q%s\\E", _tokenPrefix));

		for (int i = 0; i < aTokens.length; i++) {
			tokenSplit = aTokens[i].indexOf('.');
			iTokenEnd = aTokens[i].indexOf(_tokenSuffix);
			if ((tokenSplit == -1) || (iTokenEnd == -1)) {
				continue;
			}

			String fullToken = _tokenPrefix + aTokens[i].substring(0, iTokenEnd + 1);
			String tokenGroup = aTokens[i].substring(0, tokenSplit);
			String tokenKey = aTokens[i].substring(tokenSplit + 1, iTokenEnd);

			// Skip data tokens if no row of data is provided.
			if ((_dataTokens == null) && "Data".equals(tokenGroup)) {
				continue;
			} else if ("Data".equals(tokenGroup) && _dataTokens.containsKey(tokenKey)) {
				value = value.replace(fullToken, _dataTokens.get(tokenKey));
			} else if ("System".equals(tokenGroup)) {
				// System tokens call methods
				SimpleDateFormat sdf;
				switch (tokenKey) {
				case "CurrentDateTimeString":
					sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
					value = value.replace(fullToken, sdf.format(new Date()));
					break;
				case "StartDateTimeString":
					sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
					value = value.replace(fullToken, sdf.format(_startDateTime));
					break;
				case "ISOStartDateTime":
					sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					value = value.replace(fullToken, sdf.format(_startDateTime));
					break;
				case "Year":
					sdf = new SimpleDateFormat("yyyy");
					value = value.replace(fullToken, sdf.format(_startDateTime));
					break;
				case "Month":
					sdf = new SimpleDateFormat("MM");
					value = value.replace(fullToken, sdf.format(_startDateTime));
					break;
				case "Day":
					sdf = new SimpleDateFormat("dd");
					value = value.replace(fullToken, sdf.format(_startDateTime));
					break;
				case "ElapsedTime": 
					// returns minutes.
					Date dtCurrent = new Date();
					double minutes = (dtCurrent.getTime() - _startDateTime.getTime()) / 60000.0;
					value = value.replace(fullToken, String.format("%f", minutes));
					break;
				case "UUID":
					value = value.replace(fullToken, UUID.randomUUID().toString());
					break;
				}
			} else if (_tokens.containsKey(tokenGroup) && _tokens.get(tokenGroup).containsKey(tokenKey)) {
				value = value.replace(fullToken, _tokens.get(tokenGroup).get(tokenKey));
			} else {
				// if the token is not found, it evaluates to empty string.
				value = value.replace(fullToken, "");
			}
		}
		return resolveTokens(value);
	}

	protected void loadTokenValues(String tokenType, String[][] kvps) {
		HashMap<String, String> tokenKeyValues;
		if (_tokens.containsKey(tokenType)) {
			tokenKeyValues = _tokens.get(tokenType);
		} else {
			tokenKeyValues = new HashMap<String, String>();
		}

		StringBuilder sb = new StringBuilder();
		int length = kvps.length;
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				sb.append(_logger.getNewLineTab());
			}
			String name = kvps[i][0];
			String value = kvps[i][1];
			tokenKeyValues.put(name, value);
			if (hideIt(name)) {
				sb.append(String.format("%1$s%2$s.%3$s%4$s", _tokenPrefix, tokenType, name, _tokenSuffix));
			} else {
				sb.append(String.format(TOKEN_ADDED_FORMAT_STRING, _tokenPrefix, tokenType, name, _tokenSuffix, value));
			}
		}
		_tokens.put(tokenType, tokenKeyValues);
		_logger.addMessage("", length == 1 ? TOKEN_ADDED : TOKENS_ADDED, sb.toString());

	}

	protected void loadTokenValues(Node tokenNode) {
		loadTokenValues(tokenNode, "*");
	}

	protected void loadTokenValues(Node tokenNode, String xpath) {
		if (tokenNode == null) {
			return;
		}

		boolean isTokenNode = "Token".equals(tokenNode.getLocalName());

		NodeList nl = XmlUtilities.selectNodes(tokenNode, xpath);
		int nodeCount = nl.getLength();
		if (nodeCount == 0) {
			return;
		}

		LogVisibility defaultVisibility = hideStatus(((Element) tokenNode).getAttribute("Hide"), LogVisibility.FULL);
		int linesAdded = 0;
		int tokensAdded = 0;
		Boolean addNewLine = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < nodeCount; i++) {
			String tokenType = nl.item(i).getNodeName();
			String tokenParentName = nl.item(i).getParentNode().getNodeName();
			if ("DataSource".equals(tokenType)) {
				continue;
			}
			if (isTokenNode && (Constants.TOKEN_TYPES_RESERVED.indexOf(tokenType.toLowerCase()) != -1)) {
				throw new PieException(String.format("%s is one of the reserved token types.  Please rename your token type.", tokenType));
			}
			LogVisibility showLevel = hideStatus(((Element) nl.item(i)).getAttribute("Hide"), defaultVisibility);
			NamedNodeMap attributes = nl.item(i).getAttributes();
			int attrCount = attributes.getLength();
			if (attrCount == 0) {
				continue;
			}

			HashMap<String, String> tokenKeyValues = _tokens.containsKey(tokenType) ? _tokens.get(tokenType) : new HashMap<String, String>();
			for (int x = 0; x < attrCount; x++) {
				boolean hideValue = false;
				Node xA = attributes.item(x);
				String name = xA.getNodeName();
				String value = "StaticTokens".equals(tokenParentName) ? resolveTokens(xA.getNodeValue()) : xA.getNodeValue();

				if (!Constants.SECURE_SUFFIX.equals(name) && name.endsWith(Constants.SECURE_SUFFIX)) {
					name = name.substring(0, name.length() - Constants.SECURE_SUFFIX_LENGTH);
					if ((value != null) && value.startsWith(Constants.ENCRYPTED_PREFIX) && (_encryptionKey != null)) {
						value = Encryption.decryptToString(value.substring(Constants.ENCRYPTED_PREFIX_LENGTH), _encryptionKey);
					}
					hideValue = true;
				} else if (!Constants.HIDE_SUFFIX.equals(name) && name.endsWith(Constants.HIDE_SUFFIX)) {
					name = name.substring(0, name.length() - Constants.HIDE_SUFFIX_LENGTH);
					hideValue = true;
				}

				if ("Hide".equals(name)) {
					continue;
				}

				tokenKeyValues.put(name, value);
				tokensAdded++;
				if (showLevel == LogVisibility.NONE) {
					continue;
				}

				if (addNewLine) {
					sb.append(_logger.getNewLineTab());
				}

				if (hideValue || hideIt(name) || (showLevel == LogVisibility.TOKEN_NAME)) {
					sb.append(String.format(TOKEN_ADDED_FORMAT_STRING, _tokenPrefix, tokenType, name, _tokenSuffix, Constants.VALUE_HIDDEN_MESSAGE));
				} else {
					sb.append(String.format(TOKEN_ADDED_FORMAT_STRING, _tokenPrefix, tokenType, name, _tokenSuffix, value));
				}
				linesAdded++;
				addNewLine = true;
			}
			_tokens.put(tokenType, tokenKeyValues);
		}
		if ((tokensAdded == 0) && (linesAdded == 0)) {
			// Nothing added, no log message needed.
		} else if ((tokensAdded > 0) && (linesAdded == 0)) {
			_logger.addMessage("", tokensAdded == 1 ? TOKEN_ADDED : TOKENS_ADDED, String.format("%,d tokens added", tokensAdded));
		} else {
			_logger.addMessage("", linesAdded == 1 ? TOKEN_ADDED : TOKENS_ADDED, sb.toString());
		}
	}

	protected boolean hideIt(String value) {
		if (value == null) {
			return false;
		}

		value = value.toLowerCase();
		if (value.contains("password")) {
			return true;
		} else if (value.contains("user")) {
			return true;
		} else if (value.contains("encryptionkey")) {
			return true;
		}
		return false;
	}

	protected LogVisibility hideStatus(String value, LogVisibility defaultVisibility) {
		if (StringUtilities.isNullOrEmpty(value)) {
			return defaultVisibility;
		}

		if ("value".equalsIgnoreCase(value) || "values".equalsIgnoreCase(value)) {
			return LogVisibility.TOKEN_NAME;
		} else if ("token".equalsIgnoreCase(value) || "tokens".equalsIgnoreCase(value)) {
			return LogVisibility.NONE;
		}

		return defaultVisibility;
	}
}
