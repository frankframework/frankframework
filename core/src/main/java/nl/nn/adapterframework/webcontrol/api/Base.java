/*
Copyright 2016 Integration Partners B.V.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package nl.nn.adapterframework.webcontrol.api;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
* Baseclass to fetch ibisContext + ibisManager
* 
* @author	Niels Meijer
*/

public abstract class Base {

	protected Logger log = LogUtil.getLogger(this);
	protected IbisContext ibisContext = null;
	protected IbisManager ibisManager = null;

	/**
	 * Retrieves ibisContext and ibisManager from <code>servletConfig</code>.
	 *
	 * @param servletConfig serveletConfig to derive ibisContext from.
	 */
	protected void initBase(ServletConfig servletConfig) throws ApiException {
		String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
		ibisContext = (IbisContext) servletConfig.getServletContext().getAttribute(attributeKey);
		ibisManager = null;
		if (ibisContext != null) {
			ibisManager = ibisContext.getIbisManager();
		}
		if (ibisManager==null) {
			log.warn("Could not retrieve ibisManager from context");
			throw new ApiException("Config not found!");
		} else {
			log.debug("retrieved ibisManager ["+ClassUtils.nameOf(ibisManager)+"]["+ibisManager+"] from servlet context attribute ["+attributeKey+"]");
		}
	}

	protected List<Map<String, String>> XmlQueryResult2Map(String xml) {
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		Document xmlDoc = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(xml));
			xmlDoc = dBuilder.parse(inputSource);
			xmlDoc.getDocumentElement().normalize();
		}
		catch (Exception e) {
			return null;
		}
		NodeList rowset = xmlDoc.getElementsByTagName("row");
		for (int i = 0; i < rowset.getLength(); i++) {
			Element row = (Element) rowset.item(i);
			NodeList fieldsInRowset = row.getChildNodes();
			if (fieldsInRowset != null && fieldsInRowset.getLength() > 0) {
				Map<String, String> tmp = new HashMap<String, String>();
				for (int j = 0; j < fieldsInRowset.getLength(); j++) {
					if (fieldsInRowset.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element field = (Element) fieldsInRowset.item(j);
						tmp.put(field.getAttribute("name"), field.getTextContent());
					}
				}
				resultList.add(tmp);
			}
		}
		return resultList;
	}
}
