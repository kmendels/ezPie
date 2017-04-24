/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;

/**
*
* @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
* @since 2015-12-21
* 
*/
public class LocalTokens extends Action {

	public LocalTokens(SessionManager session, Element action) {
		super(session, action, false);
	}

	@Override
	public String executeAction() {
		_session.addTokens("Local", _action);
		return "";
	}

}