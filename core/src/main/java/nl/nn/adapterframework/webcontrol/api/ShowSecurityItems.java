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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
* Shows the used certificate.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowSecurityItems extends Base {
	public static final String AUTHALIAS_XSLT = "xml/xsl/authAlias.xsl";
	public static final String GETCONNPOOLPROP_XSLT = "xml/xsl/getConnectionPoolProperties.xsl";

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/securityitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSecurityItems(@Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}

		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("applicationDeploymentDescriptor", addApplicationDeploymentDescriptor());
		returnMap.put("securityRoleBindings", addSecurityRoleBindings());
		returnMap.put("jmsRealms", addJmsRealms());
		returnMap.put("sapSystems", addSapSystems());
		returnMap.put("authEntries", addAuthEntries());
		returnMap.put("serverProps", addServerProps());

		return Response.status(Response.Status.CREATED).entity(returnMap).build();
	}

	private String addApplicationDeploymentDescriptor() {
		String appDDString = null;
		try {
			appDDString = Misc.getApplicationDeploymentDescriptor();
			appDDString = XmlUtils.skipXmlDeclaration(appDDString);
			appDDString = XmlUtils.skipDocTypeDeclaration(appDDString);
			appDDString = XmlUtils.removeNamespaces(appDDString);
		} catch (IOException e) {
			appDDString = "*** ERROR ***";
		}
		return appDDString;
	}

	private String addSecurityRoleBindings() {
		String appBndString = null;
		try {
			appBndString = Misc.getDeployedApplicationBindings();
			appBndString = XmlUtils.removeNamespaces(appBndString);
		} catch (IOException e) {
			appBndString = "*** ERROR ***";
		}
		return appBndString;
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
					String connectionPoolProperties = getConnectionPoolProperties(confResString, "JDBC", dsName);
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
					String connectionPoolProperties = getConnectionPoolProperties(confResString, "JMS", qcfName);
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

	private String getConnectionPoolProperties(String confResString, String providerType, String jndiName) {
		String connectionPoolProperties = null;
		try {
			URL url = ClassUtils.getResourceURL(this, GETCONNPOOLPROP_XSLT);
			if (url != null) {
				Transformer t = XmlUtils.createTransformer(url, true);
				Map<String, Object> parameters = new Hashtable<String, Object>();
				parameters.put("providerType", providerType);
				parameters.put("jndiName", jndiName);
				XmlUtils.setTransformerParameters(t, parameters);
				connectionPoolProperties = XmlUtils.transformXml(t, confResString);
			}
		} catch (Exception e) {
			connectionPoolProperties = "*** ERROR ***";
		}
		return connectionPoolProperties;
	}

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

				ae.put("userName", userName);
				ae.put("passWord", passWord);
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
		serverProps.put("totalTransactionLifetimeTimeout", totalTransactionLifetimeTimeout);
		String maximumTransactionTimeout;
		try {
			maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
		} catch (Exception e) {
			maximumTransactionTimeout = "*** ERROR ***";
		}
		serverProps.put("maximumTransactionTimeout", maximumTransactionTimeout);
		return serverProps;
	}
}
