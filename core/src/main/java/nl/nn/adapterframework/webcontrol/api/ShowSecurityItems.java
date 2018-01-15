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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Shows the used certificate.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowSecurityItems extends Base {
	public static final String AUTHALIAS_XSLT = "xml/xsl/authAlias.xsl";
	@Context ServletConfig servletConfig;
	@Context HttpServletRequest httpServletRequest;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/securityitems")
	@Relation("securityitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSecurityItems() throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}

		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("securityRoles", addApplicationDeploymentDescriptor());
		returnMap.put("securityRoleBindings", getSecurityRoleBindings());
		returnMap.put("jmsRealms", addJmsRealms());
		returnMap.put("sapSystems", addSapSystems());
		returnMap.put("authEntries", addAuthEntries());
		returnMap.put("serverProps", addServerProps());

		return Response.status(Response.Status.CREATED).entity(returnMap).build();
	}

	private Map<String, Object> addApplicationDeploymentDescriptor() {
		String appDDString = null;
		Map<String, Object> resultList = new HashMap<String, Object>();
		Document xmlDoc = null;

		try {
			appDDString = Misc.getApplicationDeploymentDescriptor();
			appDDString = XmlUtils.skipXmlDeclaration(appDDString);
			appDDString = XmlUtils.skipDocTypeDeclaration(appDDString);
			appDDString = XmlUtils.removeNamespaces(appDDString);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(appDDString));
			xmlDoc = dBuilder.parse(inputSource);
			xmlDoc.getDocumentElement().normalize();
		}
		catch (Exception e) {
			return null;
		}
		
		Map<String, Map<String, List<String>>> secBindings = getSecurityRoleBindings();

		NodeList rowset = xmlDoc.getElementsByTagName("security-role");
		for (int i = 0; i < rowset.getLength(); i++) {
			Element row = (Element) rowset.item(i);
			NodeList fieldsInRowset = row.getChildNodes();
			if (fieldsInRowset != null && fieldsInRowset.getLength() > 0) {
				Map<String, Object> tmp = new HashMap<String, Object>();
				for (int j = 0; j < fieldsInRowset.getLength(); j++) {
					if (fieldsInRowset.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element field = (Element) fieldsInRowset.item(j);
						tmp.put(field.getNodeName(), field.getTextContent());
					}
				}
				if(secBindings.containsKey(row.getAttribute("id")))
					tmp.putAll(secBindings.get(row.getAttribute("id")));
				try {
					if(tmp.containsKey("role-name")) {
						String role = (String) tmp.get("role-name");
						tmp.put("allowed", httpServletRequest.isUserInRole(role));
					}
				} catch(Exception e) {};
				resultList.put(row.getAttribute("id"), tmp);
			}
		}

		return resultList;
	}

	private Map<String, Map<String, List<String>>> getSecurityRoleBindings() {
		String appBndString = null;
		Map<String, Map<String, List<String>>> resultList = new HashMap<String, Map<String, List<String>>>();
		Document xmlDoc = null;

		try {
			appBndString = Misc.getDeployedApplicationBindings();
			appBndString = XmlUtils.removeNamespaces(appBndString);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource inputSource = new InputSource(new StringReader(appBndString));
			xmlDoc = dBuilder.parse(inputSource);
			xmlDoc.getDocumentElement().normalize();
		}
		catch (Exception e) {
			return null;
		}

		NodeList rowset = xmlDoc.getElementsByTagName("authorizations");
		for (int i = 0; i < rowset.getLength(); i++) {
			Element row = (Element) rowset.item(i);
			NodeList fieldsInRowset = row.getChildNodes();
			if (fieldsInRowset != null && fieldsInRowset.getLength() > 0) {
				String role = null;
				List<String> roles = new ArrayList<String>();
				List<String> specialSubjects = new ArrayList<String>();
				for (int j = 0; j < fieldsInRowset.getLength(); j++) {
					if (fieldsInRowset.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element field = (Element) fieldsInRowset.item(j);

						if(field.getNodeName() == "role") {
							role = field.getAttribute("href");
							if(role.indexOf("#") > -1)
								role = role.substring(role.indexOf("#")+1);
						}
						else if(field.getNodeName() == "specialSubjects") {
							specialSubjects.add(field.getAttribute("name"));
						}
						else if(field.getNodeName() == "groups") {
							roles.add(field.getAttribute("name"));
						}
					}
				}
				if(role != null && role != "") {
					Map<String, List<String>> roleBinding = new HashMap<String, List<String>>();
					roleBinding.put("groups", roles);
					roleBinding.put("specialSubjects", specialSubjects);
					resultList.put(role, roleBinding);
				}
			}
		}
		return resultList;
	}

	private ArrayList<Object> addJmsRealms() {
		List<String> jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		ArrayList<Object> jmsRealmList = new ArrayList<Object>();
		String confResString;

		try {
			confResString = Misc.getConfigurationResources();
			if (confResString!=null) {
				confResString = XmlUtils.removeNamespaces(confResString);
			}
		} catch (IOException e) {
			log.warn("error getting configuration resources ["+e+"]");
			confResString = null;
		}

		for (int j = 0; j < jmsRealms.size(); j++) {
			Map<String, Object> realm = new HashMap<String, Object>();
			String jmsRealm = (String) jmsRealms.get(j);

			String dsName = null;
			String qcfName = null;
			String tcfName = null;
			String dsInfo = null;
			String qcfInfo = null;

			DirectQuerySender qs = (DirectQuerySender) ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			qs.setJmsRealm(jmsRealm);
			try {
				dsName = qs.getDataSourceNameToUse();
				dsInfo = qs.getDatasourceInfo();
			} catch (JdbcException jdbce) {
				// no datasource
			}
			if (StringUtils.isNotEmpty(dsName)) {
				realm.put("name", jmsRealm);
				realm.put("datasourceName", dsName);
				realm.put("info", dsInfo);

				if (confResString!=null) {
					String connectionPoolProperties = Misc.getConnectionPoolProperties(confResString, "JDBC", dsName);
					if (StringUtils.isNotEmpty(connectionPoolProperties)) {
						realm.put("connectionPoolProperties", connectionPoolProperties);
					}
				}
			}

			JmsSender js = new JmsSender();
			js.setJmsRealm(jmsRealm);
			try {
				qcfName = js.getConnectionFactoryName();
				qcfInfo = js.getConnectionFactoryInfo();
			} catch (JmsException jmse) {
				// no connectionFactory
			}
			if (StringUtils.isNotEmpty(qcfName)) {
				realm.put("name", jmsRealm);
				realm.put("queueConnectionFactoryName", qcfName);
				realm.put("info", qcfInfo);

				if (confResString!=null) {
					String connectionPoolProperties = Misc.getConnectionPoolProperties(confResString, "JMS", qcfName);
					if (StringUtils.isNotEmpty(connectionPoolProperties)) {
						realm.put("connectionPoolProperties", connectionPoolProperties);
					}
				}
			}
			tcfName = js.getTopicConnectionFactoryName();
			if (StringUtils.isNotEmpty(tcfName)) {
				realm.put("name", jmsRealm);
				realm.put("topicConnectionFactoryName", tcfName);
			}
			jmsRealmList.add(realm);
		}

		return jmsRealmList;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ArrayList<Object> addSapSystems() {
		ArrayList<Object> sapSystemList = new ArrayList<Object>();
		List<String> sapSystems = null;
		Object sapSystemFactory = null;
		Method factoryGetSapSystemInfo = null;
		try {
			Class<?> c = Class.forName("nl.nn.adapterframework.extensions.sap.SapSystemFactory");
			Method factoryGetInstance = c.getMethod("getInstance");
			sapSystemFactory = factoryGetInstance.invoke(null, (Object[])null);
			Method factoryGetRegisteredSapSystemsNamesAsList = c.getMethod("getRegisteredSapSystemsNamesAsList");

			sapSystems = (List) factoryGetRegisteredSapSystemsNamesAsList.invoke(sapSystemFactory, (Object[])null);
			factoryGetSapSystemInfo = c.getMethod("getSapSystemInfo", String.class);
		} catch (Throwable t) {
			log.debug("Caught NoClassDefFoundError, just no sapSystem available: " + t.getMessage());
		}
		
		if (sapSystems!=null) {
			Iterator<String> iter = sapSystems.iterator();
			while (iter.hasNext()) {
				Map<String, Object> ss = new HashMap<String, Object>();
				String name = (String) iter.next();
				ss.put("name", name);
				try {
					ss.put("info", (String) factoryGetSapSystemInfo.invoke(sapSystemFactory, name));
				} catch (Exception e) {
					ss.put("info", "*** ERROR ***");
				}
				sapSystemList.add(ss);
			}
		}
		return sapSystemList;
	}

	private ArrayList<Object> addAuthEntries() {
		ArrayList<Object> authEntries = new ArrayList<Object>();
		Collection<Node> entries = null;
		try {
			URL url = ClassUtils.getResourceURL(this, AUTHALIAS_XSLT);
			if (url != null) {
				for (Configuration configuration : ibisManager.getConfigurations()) {
					Transformer t = XmlUtils.createTransformer(url, true);
					String configString = configuration.getOriginalConfiguration();
					configString = StringResolver.substVars(configString, AppConstants.getInstance());
					configString = ConfigurationUtils.getActivatedConfiguration(configuration, configString);
					String ae = XmlUtils.transformXml(t, configString);
					Element authEntriesElement = XmlUtils.buildElement(ae);
					if (entries == null) {
						entries = XmlUtils.getChildTags(authEntriesElement, "entry");
					} else {
						entries.addAll(XmlUtils.getChildTags(authEntriesElement, "entry"));
					}
				}
			}
		} catch (Exception e) {
			authEntries.add("*** ERROR ***");
		}

		if (entries != null) {
			Iterator<Node> iter = entries.iterator();
			while (iter.hasNext()) {
				Map<String, Object> ae = new HashMap<String, Object>();
				Element itemElement = (Element) iter.next();
				String alias = itemElement.getAttribute("alias");
				ae.put("alias", alias);
				CredentialFactory cf = new CredentialFactory(alias, null, null);

				String userName;
				String passWord;
				try {
					userName = cf.getUsername();
					passWord = StringUtils.repeat("*", cf.getPassword().length());
				} catch (Exception e) {
					userName = "*** ERROR ***";
					passWord = "*** ERROR ***";
				}

				ae.put("username", userName);
				ae.put("password", passWord);
				authEntries.add(ae);
			}
		}
		return authEntries;
	}

	private Map<String, Object> addServerProps() {
		Map<String, Object> serverProps = new HashMap<String, Object>(2);

		String totalTransactionLifetimeTimeout;
		try {
			totalTransactionLifetimeTimeout = Misc.getTotalTransactionLifetimeTimeout();
		} catch (Exception e) {
			totalTransactionLifetimeTimeout = "*** ERROR ***";
		}
		if(totalTransactionLifetimeTimeout == null) totalTransactionLifetimeTimeout = "-";

		serverProps.put("totalTransactionLifetimeTimeout", totalTransactionLifetimeTimeout);
		String maximumTransactionTimeout;
		try {
			maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
		} catch (Exception e) {
			maximumTransactionTimeout = "*** ERROR ***";
		}
		if(maximumTransactionTimeout == null) maximumTransactionTimeout = "-";
		serverProps.put("maximumTransactionTimeout", maximumTransactionTimeout);
		return serverProps;
	}
}
