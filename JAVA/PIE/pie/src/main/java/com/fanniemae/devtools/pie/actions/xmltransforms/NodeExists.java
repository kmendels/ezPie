/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-11-10
 * 
 */

public class NodeExists extends XmlTransform {
	
	protected String _id = "NodeExists";

	public NodeExists(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
		_xPath = requiredAttribute("XPath");
		_id = requiredAttribute("ID");
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		NodeList targetNodes = XmlUtilities.selectNodes(xmlDocument, _xPath);
		if ((targetNodes == null) || (targetNodes.getLength() == 0)) {
			_session.addToken("NodeExists", _id, "false");
		} else {
			_session.addToken("NodeExists", _id, "true");
		}
		return xmlDocument;
	}

}
