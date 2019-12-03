/*
Copyright 2016-2018 Integration Partners B.V.

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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Baseclass to fetch ibisContext + ibisManager
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

public abstract class Base {

	protected Logger log = LogUtil.getLogger(this);
	protected IbisContext ibisContext = null;
	protected IbisManager ibisManager = null;
	protected static String HATEOASImplementation = AppConstants.getInstance().getString("ibis-api.hateoasImplementation", "default");

	private static final String ADAPTER2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/config2dot.xsl";
	private static final String CONFIGURATION2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/ibis2dot.xsl";

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
			String msg = "Could not retrieve ibisManager from context";
			log.warn(msg);
			throw new ApiException(msg);
		}
	}

	protected String getFlow(IAdapter adapter) {
		try {
			return generateFlow(adapter.getAdapterConfigurationAsString(), ADAPTER2DOT_XSLT);
		} catch (ConfigurationException e) {
			throw new ApiException(e);
		}
	}

	protected String getFlow(Configuration config) {
		return generateFlow(config.getLoadedConfiguration(), CONFIGURATION2DOT_XSLT);
	}

	protected String getFlow(List<Configuration> configurations) {
		String dotInput = "<configs>";
		for (Configuration configuration : configurations) {
			dotInput = dotInput + XmlUtils.skipXmlDeclaration(configuration.getLoadedConfiguration());
		}
		dotInput = dotInput + "</configs>";

		return generateFlow(dotInput, CONFIGURATION2DOT_XSLT);
	}

	private String generateFlow(String dotInput, String xslt) {
		try {
			URL xsltSource = ClassUtils.getResourceURL(this, xslt);
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			String dotOutput = XmlUtils.transformXml(transformer, dotInput);

			return dotOutput;
		}
		catch (Exception e) {
			throw new ApiException(e);
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

	protected String resolveStringFromMap(Map<String, List<InputPart>> inputDataMap, String key) throws ApiException {
		return resolveStringFromMap(inputDataMap, key, null);
	}

	protected String resolveStringFromMap(Map<String, List<InputPart>> inputDataMap, String key, String defaultValue) throws ApiException {
		String result = resolveTypeFromMap(inputDataMap, key, String.class, null);
		if(StringUtils.isEmpty(result)) {
			if(defaultValue != null) {
				return defaultValue;
			}
			throw new ApiException("Key ["+key+"] may not be empty");
		}
		return result;
	}

	protected <T> T resolveTypeFromMap(Map<String, List<InputPart>> inputDataMap, String key, Class<T> clazz, T defaultValue) throws ApiException {
		try {
			if(inputDataMap.get(key) != null) {
				return inputDataMap.get(key).get(0).getBody(clazz, null);
			}
		} catch (Exception e) {
			throw new ApiException("Failed to parse parameter", e);
		}
		if(defaultValue != null) {
			return defaultValue;
		}
		throw new ApiException("Key ["+key+"] not defined", 400);
	}
}
