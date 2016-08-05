package com.fanniemae.devtools.pie;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-14
 * 
 */
public class TokenManager {

	protected HashMap<String, HashMap<String, String>> _tokens = new HashMap<String, HashMap<String, String>>();
	protected LogManager _logger;
	protected Date _startDateTime = new Date();
	
	public TokenManager(Element eleSettings, LogManager logger) {
		_logger = logger;
		NodeList nl = eleSettings.getChildNodes();
		int iLength = nl.getLength();
		for (int i = 0; i < iLength; i++) {
			if (nl.item(i).getNodeType() != ELEMENT_NODE)
				continue;

			switch (nl.item(i).getNodeName()) {
			case "Configuration":
				loadTokenValues("Configuration", nl.item(i));
				break;
			case "Constants":
				loadTokenValues("Constants", nl.item(i));
				break;
			case "CAST":
				loadTokenValues("CAST", nl.item(i));
				break;
			case "Git":
				loadTokenValues("Git", nl.item(i));
				break;
			case "SelfServiceScan":
				loadSelfServiceScanTokens("ScanManager", nl.item(i));
				break;
			}
		}
	}

	public void addToken(String tokenType, String key, String value) {
		HashMap<String, String> aTokenValues = new HashMap<String, String>();
		if (_tokens.containsKey(tokenType))
			aTokenValues = _tokens.get(tokenType);

		aTokenValues.put(key, value);
		_tokens.put(tokenType, aTokenValues);
		if (key.toLowerCase().equals("password"))
			return;
		_logger.addMessage("", "Token Added", String.format("@%s.%s~ = %s", tokenType, key, value));
	}

	public void addTokens(Node tokenNode) {
		loadTokenValues(tokenNode);
	}

	public void addTokens(String tokenType, Node nodeTokenValues) {
		loadTokenValues(tokenType, nodeTokenValues);
	}

	public void addTokens(String tokenType, String[][] kvps) {
		loadTokenValues(tokenType, kvps);
	}

	public String getAttribute(Element ele, String sName) {
		return resolveTokens(ele.getAttribute(sName));
	}

	public String resolveTokens(String sValue) {
		return resolveTokens(sValue, null);
	}

	public String resolveTokens(String value, Object[] dataRow) {
		if (value == null)
			return value;

		int tokenStart = value.indexOf("@");
		if (tokenStart == -1)
			return value;
		int tokenMid = value.indexOf(".", tokenStart);
		if (tokenMid == -1)
			return value;
		int tokenEnd = value.indexOf("~", tokenMid);
		if (tokenEnd == -1)
			return value;

		int iTokenSplit = 0;
		int iTokenEnd = 0;
		String[] aTokens = value.split("@");

		for (int i = 0; i < aTokens.length; i++) {
			iTokenSplit = aTokens[i].indexOf('.');
			iTokenEnd = aTokens[i].indexOf('~');
			if ((iTokenSplit == -1) || (iTokenEnd == -1))
				continue;

			String sFullToken = "@" + aTokens[i].substring(0, iTokenEnd + 1);
			String sGroup = aTokens[i].substring(0, iTokenSplit);
			String sKey = aTokens[i].substring(iTokenSplit + 1, iTokenEnd);

			// Skip data tokens if no row of data is provided.
			if ((dataRow == null) && sGroup.equals("Data"))
				continue;

			// System tokens call methods
			if ("System".equals(sGroup)) {
				switch (sKey) {
				case "CurrentDateTimeString":
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
					value = value.replace(sFullToken, sdf.format(new Date()));
					break;
				case "StartDateTimeString":
					SimpleDateFormat sdfStart = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
					value = value.replace(sFullToken, sdfStart.format(_startDateTime));
					break;					
				}
			} else if (_tokens.containsKey(sGroup) && _tokens.get(sGroup).containsKey(sKey)) {
				value = value.replace(sFullToken, _tokens.get(sGroup).get(sKey));
			} else {
				// if the token is not found, it evaluates to empty string.
				value = value.replace(sFullToken, "");
			}
		}
		return resolveTokens(value, dataRow);
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
			if (i > 0)
				sb.append("\n");
			String name = kvps[i][0];
			String value = kvps[i][1];
			tokenKeyValues.put(name, value);
			sb.append(String.format("@%s.%s~ = %s", tokenType, name, value));
		}
		_tokens.put(tokenType, tokenKeyValues);
		_logger.addMessage("", length == 1 ? "Token Added" : "Tokens Added", sb.toString());

	}

	protected void loadTokenValues(Node tokenNode) {
		if (tokenNode == null)
			return;

		NodeList nl = XmlUtilities.selectNodes(tokenNode, "*");
		int nodeCount = nl.getLength();
		if (nodeCount == 0)
			return;

		int added = 0;
		Boolean addNewLine = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < nodeCount; i++) {
			String tokenType = nl.item(i).getNodeName();
			NamedNodeMap attributes = nl.item(i).getAttributes();
			int attrCount = attributes.getLength();
			if (attrCount == 0)
				continue;

			HashMap<String, String> tokenKeyValues = _tokens.containsKey(tokenType) ? _tokens.get(tokenType) : new HashMap<String, String>();
			for (int x = 0; x < attrCount; x++) {
				Node xA = attributes.item(x);
				String name = xA.getNodeName();
				String value = xA.getNodeValue();

				tokenKeyValues.put(name, value);
				if (addNewLine)
					sb.append("\n");
				sb.append(String.format("@%s.%s~ = %s", tokenType, name, value));
				added++;
				addNewLine = true;
			}
			_tokens.put(tokenType, tokenKeyValues);
		}
		_logger.addMessage("", added == 1 ? "Token Added" : "Tokens Added", sb.toString());
	}

	protected void loadTokenValues(String tokenType, Node node) {
		HashMap<String, String> tokenKeyValues;
		if (_tokens.containsKey(tokenType)) {
			tokenKeyValues = _tokens.get(tokenType);
		} else {
			tokenKeyValues = new HashMap<String, String>();
		}

		NamedNodeMap attributes = node.getAttributes();

		int added = 0;
		int length = attributes.getLength();
		Boolean addNewLine = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			Node xA = attributes.item(i);
			String name = xA.getNodeName();
			String value = xA.getNodeValue();
			if ("id".equals(name.toLowerCase()) || "password".equals(name.toLowerCase())) {
				continue;
			}
			tokenKeyValues.put(name, value);
			if (addNewLine)
				sb.append("\n");
			sb.append(String.format("@%s.%s~ = %s", tokenType, name, value));
			added++;
			addNewLine = true;
		}
		_tokens.put(tokenType, tokenKeyValues);
		_logger.addMessage("", added == 1 ? "Token Added" : "Tokens Added", sb.toString());
	}

	protected void loadSelfServiceScanTokens(String tokenType, Node node) {
		if ((node == null) || !node.hasChildNodes())
			return;

		NodeList queries = XmlUtilities.selectNodes(node, "*");
		int length = queries.getLength();

		if (length == 0)
			return;

		HashMap<String, String> tokenKeyValues;
		if (_tokens.containsKey(tokenType)) {
			tokenKeyValues = _tokens.get(tokenType);
		} else {
			tokenKeyValues = new HashMap<String, String>();
		}

		int added = 0;
		Boolean addNewLine = false;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			Element query = (Element) queries.item(i);
			if (!query.hasAttributes())
				continue;

			String name = query.getNodeName();
			String value = query.getAttribute("SqlQuery");
			if (StringUtilities.isNullOrEmpty(value))
				continue;

			tokenKeyValues.put(name, value);
			added++;

			if (addNewLine)
				sb.append("\n");

			sb.append(String.format("@%s.%s~", tokenType, name));
			addNewLine = true;
		}
		_tokens.put(tokenType, tokenKeyValues);
		_logger.addMessage("", added == 1 ? "Token Added" : "Tokens Added", sb.toString());
	}
}
