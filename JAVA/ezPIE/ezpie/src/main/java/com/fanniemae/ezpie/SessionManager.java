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

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.ezpie.common.CryptoUtilities;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.DataTable;
import com.fanniemae.ezpie.common.Encryption;
import com.fanniemae.ezpie.common.ExceptionUtilities;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.Miscellaneous;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-15
 * 
 */

public class SessionManager {
	private static final String TOKEN_PREFIX_ATTRIBUTE = "TokenPrefix";
	private static final String TOKEN_SUFFIX_ATTRIBUTE = "TokenSuffix";
	private static final String ENCRYPTED_PREFIX = "{ENCRYPT1}";
	private static final String SECURE_SUFFIX = "Secure";
	private static final String HIDE_SUFFIX = "Hide";

	protected String _logFilename;
	protected String _jobFilename;

	protected String _appPath;
	protected String _definitionPath;
	protected String _logPath;
	protected String _stagingPath;
	protected String _templatePath;
	protected String _pathSeparator = System.getProperty("file.separator");
	protected String _tokenPrefix = "[";
	protected String _tokenSuffix = "]";
	protected String _hiddenValueMessage = "-- Value Hidden --";

	protected String _jobRescanFilename = null;

	protected int _memoryLimit = 20;
	protected int _cacheMinutes = 30;
	protected int _jobKey = -1;

	protected Document _settingsDoc;
	protected Element _settings;
	protected Element _job;
	protected Element _connScanManager;

	protected LogManager _logger;
	protected TokenManager _tokenizer;

	protected Boolean _dataCachingEnabled = false;
	protected Boolean _updateScanManager = false;
	protected Boolean _lastAttributeSecure = false;

	protected Map<String, DataStream> _dataSets = new HashMap<String, DataStream>();

	protected DataTable _codeLocations = null;

	protected byte[][] _encryptionKey = null;

	public SessionManager(String settingsFilename, String jobFilename, List<String> args) {
		if (StringUtilities.isNullOrEmpty(settingsFilename)) {
			throw new PieException("Settings file is not defined.  Please provide the full path to the _settings.xml file.");
		}

		if (!FileUtilities.isValidFile(settingsFilename)) {
			if (FileUtilities.isValidFile(Miscellaneous.getApplicationRoot() + settingsFilename)) {
				settingsFilename = Miscellaneous.getApplicationRoot() + settingsFilename;
			} else {
				throw new PieException(String.format("Settings file not found in %s", settingsFilename));
			}
		}
		DefinitionManager dm = new DefinitionManager();
		Document xSettings = dm.loadFile(settingsFilename);
		if (xSettings == null) {
			throw new PieException("No settings information found.");
		}

		_settingsDoc = xSettings;
		_settings = xSettings.getDocumentElement();
		Node nodeConfig = XmlUtilities.selectSingleNode(_settings, "Configuration");
		if (nodeConfig == null) {
			throw new PieException("Settings file is missing the Configuration element.  Please update the settings file.");
		}

		Element eleConfig = (Element) nodeConfig;

		if ((args != null) && (!args.isEmpty())) {
			for (int i = 0; i < args.size(); i++) {
				String[] keyValuePair = args.get(i).split("=");
				if ("DefinitionFile".equals(keyValuePair[0])) {
					_jobRescanFilename = keyValuePair[1];
					break;
				}
			}
		}

		Boolean randomLogFilename = StringUtilities.toBoolean(eleConfig.getAttribute("RandomLogFileName"), true);
		_dataCachingEnabled = StringUtilities.toBoolean(eleConfig.getAttribute("DataCacheEnabled"), true);
		_appPath = FileUtilities.formatPath(eleConfig.getAttribute("ApplicationPath"), System.getProperty("user.dir"), "ApplicationPath");
		_stagingPath = FileUtilities.formatPath(eleConfig.getAttribute("StagingPath"), String.format("%1$s_Staging", _appPath), "StagingPath");
		_logPath = FileUtilities.formatPath(eleConfig.getAttribute("LogPath"), String.format("%1$s_Logs", _appPath), "LogPath");
		_tokenPrefix = eleConfig.hasAttribute(TOKEN_PREFIX_ATTRIBUTE) ? eleConfig.getAttribute(TOKEN_PREFIX_ATTRIBUTE) : _tokenPrefix;
		_tokenSuffix = eleConfig.hasAttribute(TOKEN_SUFFIX_ATTRIBUTE) ? eleConfig.getAttribute(TOKEN_SUFFIX_ATTRIBUTE) : _tokenSuffix;
		String logFormat = eleConfig.getAttribute("LogFormat");
		String logLevel = eleConfig.getAttribute("LogLevel");
		String logFileExtension = "Text".equalsIgnoreCase(logFormat) ? "txt" : "html";
		_definitionPath = FileUtilities.formatPath(eleConfig.getAttribute("DefinitionPath"), String.format("%1$s_Definitions", _appPath), "DefinitionPath");
		_templatePath = FileUtilities.formatPath(eleConfig.getAttribute("TemplatePath"), String.format("%1$s_Templates", _appPath), "TemplatePath");
		if (!randomLogFilename && StringUtilities.isNotNullOrEmpty(_jobRescanFilename)) {
			_logFilename = String.format("%1$s%2$s.%3$s", _logPath, FileUtilities.getFilenameWithoutExtension(_jobRescanFilename), logFileExtension);
		} else if (!randomLogFilename) {
			_logFilename = String.format("%1$s%2$s.%3$s", _logPath, FileUtilities.getFilenameWithoutExtension(jobFilename), logFileExtension);
		} else {
			_logFilename = FileUtilities.getRandomFilename(_logPath, logFileExtension);
		}

		_cacheMinutes = StringUtilities.toInteger(eleConfig.getAttribute("CacheMinutes"), 30);
		String encryptionKey = eleConfig.getAttribute("EncryptionKey");
		if (StringUtilities.isNotNullOrEmpty(encryptionKey)) {
			_encryptionKey = Encryption.setupKey(encryptionKey);
		}

		// Create Log page.
		_logger = new LogManager(_templatePath, _logFilename, logFormat, logLevel);
		_logger.addMessage("", "Data Caching", _dataCachingEnabled ? "Enabled" : "Disabled");

		if ((jobFilename != null) && !jobFilename.toLowerCase().endsWith(".xml")) {
			jobFilename += ".xml";
		}

		if (FileUtilities.isInvalidFile(jobFilename)) {
			String sAdjustedDefinitionFilename = _definitionPath + jobFilename;
			if (FileUtilities.isValidFile(sAdjustedDefinitionFilename)) {
				jobFilename = sAdjustedDefinitionFilename;
			} else {
				RuntimeException exRun = new PieException(String.format("Definition file %s not found.", jobFilename));
				_logger.addErrorMessage(exRun);
				throw exRun;
			}
		}
		_jobFilename = jobFilename;

		try {
			_logger.addMessage("Setup Token Dictionary", "Load Tokens", "Read values from settings file.");
			_tokenizer = new TokenManager(_settings, _logger, _encryptionKey, _tokenPrefix, _tokenSuffix);

			_logger.addFileDetails(_jobFilename, "Definition Details");
			dm = new DefinitionManager(this, _encryptionKey);
			Document xmlJobDefinition = dm.loadFile(_jobFilename);
			if (xmlJobDefinition == null) {
				throw new PieException("No settings information found.");
			}

			_job = xmlJobDefinition.getDocumentElement();
			String finalJobDefinition = _logger.logExternalFiles() ? FileUtilities.writeRandomFile(_logPath, ".txt", XmlUtilities.xmlDocumentToString(xmlJobDefinition)) : "";
			_logger.addMessage("", "Prepared Definition", "View Definition", "file://" + finalJobDefinition);
			_logger.addMessage("", "Adjusted Size", String.format("%,d bytes", XmlUtilities.getOuterXml(_job).length()));
			// Check for definition specific token prefix and suffix configuration
			if (_job.hasAttribute(TOKEN_PREFIX_ATTRIBUTE)) {
				_tokenPrefix = _job.getAttribute(TOKEN_PREFIX_ATTRIBUTE);
				_tokenizer.setTokenPrefix(_tokenPrefix);
				_logger.addMessage("", "Token Prefix", String.format("This definition changed the token prefix to %s", _tokenPrefix));
			}
			if (_job.hasAttribute(TOKEN_SUFFIX_ATTRIBUTE)) {
				_tokenSuffix = _job.getAttribute(TOKEN_SUFFIX_ATTRIBUTE);
				_tokenizer.setTokenSuffix(_tokenSuffix);
				_logger.addMessage("", "Token Suffix", String.format("This definition changed the token suffix to %s", _tokenSuffix));
			}
			_tokenizer.addToken("Application", "LogFilename", FileUtilities.getFilenameOnly(_logFilename));
		} catch (Exception ex) {
			_logger.addErrorMessage(ex);
			throw ex;
		}

		// TODO: Remove exception block and make connection optional.
		try {
			_updateScanManager = false;
			_connScanManager = getConnection("JavaScanManager");
			_updateScanManager = StringUtilities.toBoolean(getTokenValue("Configuration", "UpdateScanManager"), false);
		} catch (Exception ex) {
			ExceptionUtilities.goSilent(ex);
		}
	}

	public TokenManager getTokenizer() {
		return _tokenizer;
	}

	public Element getJobDefinition() {
		return _job;
	}

	public String getApplicationPath() {
		return _appPath;
	}

	public String getJarPath() {
		return _appPath + "_bin";
	}

	public String getStagingPath() {
		return _stagingPath;
	}

	public String getLogPath() {
		return _logPath;
	}

	public String getLogFilename() {
		return _logFilename;
	}

	public int getMemoryLimit() {
		return _memoryLimit;
	}

	public String getLineSeparator() {
		return System.lineSeparator();
	}

	public String getHiddenMessage() {
		return _hiddenValueMessage;
	}

	public Element getConnectionScanManager() {
		return _connScanManager;
	}

	public Boolean updateScanManager() {
		return _updateScanManager;
	}

	public Boolean lastAttributeSecure() {
		return _lastAttributeSecure;
	}

	public String getAttribute(Node ele, String name) {
		return getAttribute(ele, name, "");
	}

	public String getAttribute(Element ele, String name) {
		return getAttribute(ele, name, "");
	}

	public String getAttribute(Node ele, String name, String defaultValue) {
		return getAttribute((Element) ele, name, defaultValue);
	}

	public String getAttribute(Element ele, String name, String defaultValue) {
		if (ele == null) {
			return "";
		}

		_lastAttributeSecure = false;
		if (!ele.hasAttribute(name)) {
			// Check for a secure version of the attribute name
			String secureName = String.format("%s%s", name, SECURE_SUFFIX);
			String hideName = String.format("%s%s", name, HIDE_SUFFIX);
			if (ele.hasAttribute(secureName)) {
				name = secureName;
				_lastAttributeSecure = true;
			} else if (ele.hasAttribute(hideName)) {
				name = hideName;
				_lastAttributeSecure = true;
			}
		}

		String value = ele.getAttribute(name);
		if (_lastAttributeSecure && (value != null) && value.startsWith(ENCRYPTED_PREFIX)) {
			// Need to decrypt this value if it is encrypted.
			if (_encryptionKey == null) {
				throw new PieException("No encryption key defined in settings file.");
			}
			value = Encryption.decryptToString(value.substring(10), _encryptionKey);
		}

		if (StringUtilities.isNullOrEmpty(value)) {
			return defaultValue;
		}

		if ((value.indexOf(_tokenPrefix) == -1) || (value.indexOf(_tokenSuffix) == -1)) {
			return value;
		}

		int tokenSplit = 0;
		int tokenEnd = 0;
		String[] aTokens = value.split("\\Q" + _tokenPrefix + "\\E");

		for (int i = 0; i < aTokens.length; i++) {
			tokenSplit = aTokens[i].indexOf('.');
			tokenEnd = aTokens[i].indexOf(_tokenSuffix);
			if ((tokenSplit == -1) || (tokenEnd == -1)) {
				continue;
			}
			if (tokenSplit > tokenEnd) {
				continue;
			}

			String fullToken = _tokenPrefix + aTokens[i].substring(0, tokenEnd + 1);
			String tokenGroup = aTokens[i].substring(0, tokenSplit);
			String tokenKey = aTokens[i].substring(tokenSplit + 1, tokenEnd);

			// Skip everything but DataSet tokens - others are resolved in
			// Tokenizer.
			if (!"DataSet".equals(tokenGroup)) {
				continue;
			} else if (!_dataSets.containsKey(tokenKey)) {
				throw new PieException(String.format("Could not find any DataSet object named %s", tokenKey));
			} else {
				String dataFilename = _dataSets.get(tokenKey).getFilename();
				if (StringUtilities.isNullOrEmpty(dataFilename)) {
					value = value.replace(fullToken, String.format("Memory Stream (%,d bytes)", _dataSets.get(tokenKey).getSize()));
				} else {
					value = value.replace(fullToken, _dataSets.get(tokenKey).getFilename());
				}
			}
		}
		return _tokenizer.resolveTokens(value);
	}

	public void addLogMessage(String logGroup, String event, String description) {
		addLogMessage(logGroup, event, description, "");
	}

	public void addLogMessage(String logGroup, String event, String description, String cargo) {
		_logger.addMessage(logGroup, event, description, cargo);
	}

	public void addLogMessageHtml(String logGroup, String event, String description) {
		addLogMessageHtml(logGroup, event, description, "");
	}

	public void addLogMessageHtml(String logGroup, String event, String description, String cargo) {
		_logger.addHtmlMessage(logGroup, event, description, cargo);
	}

	public void addErrorMessage(Exception ex) {
		_logger.addErrorMessage(ex);
	}

	public Element getConnection(String connectionName) {
		if (StringUtilities.isNullOrEmpty(connectionName)) {
			return null;
		}

		Node nodeConnection = XmlUtilities.selectSingleNode(_settings, String.format("..//Connections/Connection[@Name='%s']", connectionName));
		if (nodeConnection == null) {
			throw new PieException(String.format("Requested connection %s was not found in the settings file.", connectionName));
		}
		return (Element) nodeConnection;
	}

	public String getFilenameHash(String value) {
		return String.format("%1$s%2$s%3$s%4$s.dat", _appPath, "_DataCache", _pathSeparator, CryptoUtilities.hashValue(value));
	}

	public String getRequiredTokenValue(String tokenType, String tokenKey) {
		String value = getTokenValue(tokenType, tokenKey);
		if (StringUtilities.isNullOrEmpty(value)) {
			throw new PieException(String.format("No value is defined for the %s%s.%s%s token.", _tokenPrefix, tokenType, tokenKey, _tokenSuffix));
		}
		return value;
	}

	public String getTokenValue(String tokenType, String tokenKey) {
		return _tokenizer.getTokenValue(tokenType, tokenKey);
	}

	public String resolveTokens(String value) {
		return _tokenizer.resolveTokens(value);
	}

	public void addTokens(Node node) {
		_tokenizer.addTokens(node);
	}

	public void addTokens(Map<String, String> newTokens) {
		_tokenizer.addTokens(newTokens);
	}

	public void addTokens(String tokenType, Map<String, String> newTokens) {
		_tokenizer.addTokens(tokenType, newTokens);
	}

	public void addTokens(String tokenType, String[][] kvps) {
		_tokenizer.addTokens(tokenType, kvps);
	}

	public void addToken(String tokenType, String key, String value) {
		_tokenizer.addToken(tokenType, key, value);
	}

	public void addLogMessagePreserveLayout(String logGroup, String event, String description) {
		_logger.addMessagePreserveLayout(logGroup, event, description);
	}

	public void addDataSet(String name, DataStream ds) {
		_dataSets.put(name, ds);
	}

	public List<String> getDataStreamList() {
		List<String> dataSets = new ArrayList<String>();
		if ((_dataSets == null) || (_dataSets.size() == 0)) {
			return dataSets;
		}

		for (Map.Entry<String, DataStream> kvp : _dataSets.entrySet()) {
			dataSets.add(kvp.getKey());
		}
		return dataSets;
	}

	public DataStream getDataStream(String name) {
		return getDataStream(name, false);
	}

	public DataStream getDataStream(String name, boolean silent) {
		if (StringUtilities.isNullOrEmpty(name)) {
			throw new PieException("Missing required DataSetName value.");
		}

		if (!_dataSets.containsKey(name)) {
			throw new PieException(String.format("DataSetName %s was not found in the list of available data sets.", name));
		}

		DataStream dataStream = _dataSets.get(name);
		if (!silent) {
			if (dataStream.IsMemory()) {
				addLogMessage("", "DataStream Details", String.format("MemoryStream of %,d bytes", dataStream.getSize()));
			} else {
				addLogMessage("", "DataStream Details", String.format("FileStream (%s) of %,d bytes", dataStream.getFilename(), dataStream.getSize()));
			}
		}
		return dataStream;
	}

	public void setCodeLocations(DataTable dt) {
		_codeLocations = dt;
	}

	public DataTable getCodeLocations() {
		return _codeLocations;
	}

	public String getTokenPrefix() {
		return _tokenPrefix;
	}

	public String getTokenSuffix() {
		return _tokenSuffix;
	}

	public void setDataTokens(Map<String, String> dataTokens) {
		_tokenizer.setDataTokens(dataTokens);
	}

	public void clearDataTokens() {
		_tokenizer.clearDataTokens();
	}

	public String optionalAttribute(Node node, String attributeName, String defaultValue) {
		return optionalAttribute((Element) node, attributeName, defaultValue);
	}

	public String optionalAttribute(Element element, String attributeName) {
		return optionalAttribute(element, attributeName, null);
	}

	public String optionalAttribute(Element element, String attributeName, String defaultValue) {
		String value = getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			value = resolveTokens(defaultValue);
		} else if (_lastAttributeSecure || "UserID".equals(attributeName) || "Password".equals(attributeName)) {
			addLogMessage("", attributeName, getHiddenMessage());
		} else if ("DataCacheEnabled".equals(attributeName) || "DataCacheMinutes".equals(attributeName)) {
			// Skip these -- special case, need to show even if not set.
		} else {
			addLogMessage("", attributeName, value);
		}
		return value;
	}

	public String requiredAttribute(Node node, String attributeName) {
		return requiredAttribute((Element) node, attributeName);
	}

	public String requiredAttribute(Element element, String attributeName) {
		String errorMessage = String.format("Missing a value for %s on the %s element.", attributeName, element.getNodeName());
		return requiredAttribute(element, attributeName, errorMessage);
	}

	public String requiredAttribute(Node node, String attributeName, String errorMessage) {
		return requiredAttribute((Element) node, attributeName, errorMessage);
	}

	public String requiredAttribute(Element element, String attributeName, String errorMessage) {
		String value = getAttribute(element, attributeName);
		if (StringUtilities.isNullOrEmpty(value)) {
			throw new PieException(errorMessage);
		} else if (_lastAttributeSecure || "UserID".equals(attributeName) || "Password".equals(attributeName)) {
			addLogMessage("", attributeName, getHiddenMessage());
		} else {
			addLogMessage("", attributeName, value);
		}
		return value;
	}

	public Boolean cachingEnabled() {
		return _dataCachingEnabled;
	}

	public int getCacheMinutes() {
		return _cacheMinutes;
	}

	protected String getJarDirectory() {
		try {
			File fi = new File(SessionManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			return fi.getAbsolutePath();
		} catch (URISyntaxException e) {
			throw new PieException("Could not read the jar location.", e);
		}
	}
}
