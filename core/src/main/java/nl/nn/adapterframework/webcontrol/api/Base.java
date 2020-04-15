/*
Copyright 2016-2020 Integration Partners B.V.

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
import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Baseclass to fetch ibisContext + ibisManager
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

public abstract class Base {
	@Context ServletConfig servletConfig;

	protected Logger log = LogUtil.getLogger(this);
	private IbisContext ibisContext = null;
	protected static String HATEOASImplementation = AppConstants.getInstance().getString("ibis-api.hateoasImplementation", "default");

	private static final String ADAPTER2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/config2dot.xsl";
	private static final String CONFIGURATION2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/ibis2dot.xsl";

	/**
	 * Retrieves the IbisContext from <code>servletConfig</code>.
	 */
	private void retrieveIbisContextFromServlet() {
		if(servletConfig == null) {
			throw new ApiException(new IllegalStateException("no ServletConfig found to retrieve IbisContext from"));
		}

		ibisContext = IbisApplicationServlet.getIbisContext(servletConfig.getServletContext());
	}

	public IbisContext getIbisContext() {
		if(ibisContext == null) {
			retrieveIbisContextFromServlet();
		}

		if(ibisContext.getBootState().getException() != null) {
			throw new ApiException(ibisContext.getBootState().getException());
		}

		return ibisContext;
	}

	/**
	 * Retrieves the IbisManager from the IbisContext
	 */
	public IbisManager getIbisManager() {
		IbisManager ibisManager = getIbisContext().getIbisManager();

		if (ibisManager==null) {
			throw new ApiException(new IllegalStateException("Could not retrieve ibisManager from context"));
		}

		return ibisManager;
	}

	public ClassLoader getClassLoader() {
		return this.getClass().getClassLoader();
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
			URL xsltSource = ClassUtils.getResourceURL(this.getClass().getClassLoader(), xslt);
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			String dotOutput = XmlUtils.transformXml(transformer, dotInput, true);

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


	/**
	 * Method that return a string from MultipartBody, if it does not exist, an Optional empty will be returned
	 */
	protected Optional<String> resolveStringFromMap(MultipartBody inputDataMap, String key) throws ApiException {
		Optional<String> result = Optional.empty();
		try {
			result = Optional.ofNullable(inputDataMap.getAttachmentObject(key, String.class));
		}catch(Exception e) {
			log.error("Failed to parse parameter ["+key+"]", e);
		}
		return result;
	}

	/**
	 * Method that return a T type value (the type depends on the clazz parameter) from MultipartBody, if it does not exist, an Optional empty will be returned
	 */
	protected <T> Optional<T> resolveTypeFromMap(MultipartBody inputDataMap, String key, Class<T> clazz) {
		Optional<T> value = Optional.empty();
		try {
			value = Optional.ofNullable(inputDataMap.getAttachmentObject(key, clazz));
		} catch (Exception e) {
			log.debug("Failed to parse parameter ["+key+"]", e);
		}
		return value;
	}

	/**
	 * Method that return a string from MultipartBody, if it does not exist, an exception will be thrown
	 */
	protected String resolveStringFromMapMandatory(MultipartBody inputDataMap, String key) throws ApiException {
		return this.resolveTypeFromMapMandatory(inputDataMap, key, String.class).get();
	}

	/**
	 * Method that return a T type value (the type depends on the clazz parameter) from MultipartBody, if it does not exist, an exception will be thrown
	 */
	protected <T> Optional<T> resolveTypeFromMapMandatory(MultipartBody inputDataMap, String key, Class<T> clazz) throws ApiException {
		Optional<T> value = Optional.empty();
		value = Optional.ofNullable(inputDataMap.getAttachmentObject(key, clazz));		
		if (!value.isPresent()) {
			throw new ApiException("Key ["+key+"] not defined", 400);
		}
		return value;
	}	
	
	protected String getAdapterName(MultipartBody inputDataMap, String adapter) {
		String adapterName = null;
		try {
			adapterName = IOUtils.toString(inputDataMap.getAttachment("adapter").getDataHandler().getDataSource().getInputStream());
		}catch(Exception e) {
			log.debug("Failed to parse parameter ["+adapter+"]", e);
			throw new ApiException("Key ["+adapter+"] not defined", 400);
		}
		return adapterName;
	}
	
	
	
}
