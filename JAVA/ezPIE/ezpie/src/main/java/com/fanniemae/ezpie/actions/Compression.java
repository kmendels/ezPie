/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.common.ZipUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-06
 * 
 */

public class Compression extends Action {

	protected boolean _zip;
	protected boolean _deep = true;

	protected String _zipFilename;
	protected String _source;
	protected String _destination;

	protected Pattern[] _excludeRegex = null;
	protected Pattern[] _includeRegex = null;

	public Compression(SessionManager session, Element action) {
		super(session, action, false);

		_zip = "Zip".equals(action.getNodeName());

		_zipFilename = requiredAttribute("ZipFilename");
		if(!_zipFilename.endsWith(".zip")){
			_zipFilename += ".zip";
		} 
		if (!_zip && FileUtilities.isInvalidFile(_zipFilename)) {
			throw new PieException(String.format("%s file not found.", _zipFilename));
		}

		if (_zip) {
			_source = requiredAttribute("Source");
			if (FileUtilities.isInvalidDirectory(_source)) {
				throw new PieException("Zip error: Source does not exist.");
			}
		} else {
			_destination = requiredAttribute("Destination");
			if (FileUtilities.isInvalidDirectory(_destination)) {
				File fi = new File(_destination);
				fi.mkdir();
				//throw new PieException("UnZip error: Destination does not exist.");
			}
		}
		
		_deep = "false".equalsIgnoreCase(optionalAttribute("Deep", "true")) ? false : true;
		
		
		NodeList nlChildren = XmlUtilities.selectNodes(_action, "*");
		int length = nlChildren.getLength();
		if (length > 0) {
			List<Pattern> exclude = new ArrayList<Pattern>();
			List<Pattern> include = new ArrayList<Pattern>();
			for (int i = 0; i < length; i++) {
				String name = nlChildren.item(i).getNodeName();
				Element child = (Element) nlChildren.item(i);
				switch (name) {
				case "Exclude":
					exclude.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "Include":
					include.add(Pattern.compile(_session.getAttribute(child, "Regex")));
					break;
				case "ExcludeDirectory":
				case "IncludeDirectory":
				case "ExcludeFile":
				case "IncludeFile":
					throw new PieException(String.format("%s child element no longer supported. Use Include or Exclude.", name));
				default:
					_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, name, "Operation not currently supported.");
				}
			}
			_excludeRegex = new Pattern[exclude.size()];
			_excludeRegex = exclude.toArray(_excludeRegex);
			_includeRegex = new Pattern[include.size()];
			_includeRegex = include.toArray(_includeRegex);
			if(_excludeRegex.length > 0 && _includeRegex.length > 0){
				throw new PieException("Cannot have both Exclude and Include child elements. Create a seperate element.");
			}
		}
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		try {
			if (_zip) {
				String filelist = ArrayUtilities.toString(ZipUtilities.zip(_source, _zipFilename, _includeRegex, _excludeRegex));
				_session.addLogMessageHtml("", "Files Compressed", filelist);
				_session.addLogMessage("", "Zipped Size", String.format("%,d bytes", FileUtilities.getLength(_zipFilename)));
				_session.addToken("File", _name, _zipFilename);
			} else {
				String[] list = ZipUtilities.unzip(_zipFilename, _destination, _includeRegex, _excludeRegex);
				String filelist = ArrayUtilities.toString(list);
				_session.addLogMessageHtml("", "Files Decompressed", filelist);
				_session.addLogMessage("", "Count", String.format("%,d files", list.length - 2));
			}
		} catch (IOException ex) {
			_session.addErrorMessage(ex);
		}
		_session.clearDataTokens();
		return null;
	}

}
