package com.fanniemae.automation.actions;

import org.w3c.dom.Element;

import com.fanniemae.automation.SessionManager;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-29
 * 
 */
public class LogComment extends Action {
	
	public LogComment(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String execute() {
		_Session.addLogMessage("", "Message", _Session.getAttribute(_Action, "Message"));
		return "";
	}
}
