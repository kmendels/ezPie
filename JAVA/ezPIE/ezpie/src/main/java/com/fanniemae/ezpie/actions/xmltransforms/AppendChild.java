/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.Constants;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-09-20
 * 
 */

public class AppendChild extends XmlTransform {

	public AppendChild(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
		_xPath = optionalAttribute("XPath", "");
		_xmlString = requiredAttribute("XmlString");
		_required = StringUtilities.toBoolean(optionalAttribute("Required", ""), true);
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		Document tempDoc = XmlUtilities.createXMLDocument(String.format("<temp>%s</temp>", _xmlString));
		NodeList nlNew = XmlUtilities.selectNodes(tempDoc.getDocumentElement(), "*");
		int length = nlNew.getLength();
		if (_required && (length == 0)) {
			throw new RuntimeException("XmlString does not contain any nodes to append.");
		} else if (length == 0) {
			_session.addLogMessage(Constants.LOG_WARNING_MESSAGE, "Nodes", "XmlString does not contain any nodes to append.");
			return xmlDocument;
		}

		NodeList targetNodes = null;
		Node targetNode = null;
		if (StringUtilities.isNullOrEmpty(_xPath)) {
			targetNode = xmlDocument.getDocumentElement();
			for (int i = 0; i < length; i++) {
				targetNode.appendChild(xmlDocument.adoptNode(nlNew.item(i).cloneNode(true)));
			}
		} else {
			targetNodes = XmlUtilities.selectNodes(xmlDocument, _xPath);
			if (_required && (targetNodes.getLength() == 0)) {
				throw new PieException(String.format("%s did not return any matching nodes.", _xPath));
			}
			int targetLength = targetNodes.getLength();
			for (int x = 0; x < targetLength; x++) {
				targetNode = targetNodes.item(x);
				for (int i = 0; i < length; i++) {
					targetNode.appendChild(xmlDocument.adoptNode(nlNew.item(i).cloneNode(true)));
				}
			}
		}
		return xmlDocument;
	}

}
