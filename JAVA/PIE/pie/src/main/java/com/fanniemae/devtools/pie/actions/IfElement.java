package com.fanniemae.devtools.pie.actions;

import java.io.File;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.ScriptUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

public class IfElement extends Action {

	protected String _expression = "false";

	public IfElement(SessionManager session, Element action) {
		super(session, action, false);

		_expression = "";
		String jsFile = optionalAttribute("JavaScriptFile", null);
		if (jsFile != null) {
			if (FileUtilities.isInvalidFile(jsFile)) {
				String resourceDir = String.format("@Configuration.ApplicationPath~%s_Resources%s%s", File.separator, File.separator, jsFile);
				resourceDir = _session.resolveTokens(resourceDir);
				if (FileUtilities.isValidFile(resourceDir)) {
					jsFile = resourceDir;
				} else {
					throw new RuntimeException(String.format("JavaScript file %s was not found.", jsFile));
				}
			}
			_expression = FileUtilities.loadFile(jsFile) + "\n";
		}

		_expression += requiredAttribute("Expression");
		if (_expression.toLowerCase().endsWith(".js") && FileUtilities.isValidFile(_expression)) {
			_expression = FileUtilities.loadFile(_expression);
		}
	}

	public boolean evalToBoolean() {
		Boolean result = false;
		if (StringUtilities.isBoolean(_expression)) {
			result = StringUtilities.toBoolean(_expression);
		} else {
			result = ScriptUtilities.evalToBoolean(_session.resolveTokens(_expression));
		}
		_session.addLogMessage("", "Result", result.toString());
		return result;
	}

	@Override
	public String executeAction() {
		// Element is used to control definition branching, no actual action.
		return "";
	}

}
