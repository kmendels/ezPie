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
import java.util.HashMap;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.PieException;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-24
 * 
 */

public class Rename extends Copy {

	public Rename(SessionManager session, Element action) {
		super(session, action);
		_countMessage = "renamed";
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);
		processFileSystem(_source, _destination);
		_session.clearDataTokens();
		return null;
	}

	@Override
	protected void processFileSystem(String source, String destination) {
		File originalName = new File(source);
		if (originalName.exists()) {
			File newName = new File(destination);
			if (newName.getParent() == null) {
				String fullPath = String.format("%s%s%s", originalName.getParent(), File.separator, newName.getName());
				newName = new File(fullPath);
			}
			originalName.renameTo(newName);
			_session.addLogMessage("", "Rename Complete", String.format("%s to %s", originalName, newName));
		} else {
			if (_required) {
				throw new PieException(String.format("%s does not exist.  To make this action optional, set the attribute Required to False.", source));
			}
			_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "File", String.format(" Nothing found to %s. %s does not exist.", _actionName, source));
		}
	}
}
