/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import lombok.Getter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.XmlUtils;

@BusAware("frank-management-bus")
public class SecurityItems extends BusEndpointBase {
	private List<String> securityRoles;

	@TopicSelector(BusTopic.SECURITY_ITEMS)
	public Message<String> getSecurityItems(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("securityRoles", getSecurityRoles());
		returnMap.put("jmsRealms", addJmsRealms());
		returnMap.put("datasources", addDataSources());
		returnMap.put("sapSystems", addSapSystems());
		returnMap.put("authEntries", addAuthEntries());
		returnMap.put("xmlComponents", XmlUtils.getVersionInfo());

		return new JsonResponseMessage(returnMap);
	}

	@Override
	protected void doAfterPropertiesSet() {
		try {
			ServletManager servletManager = getApplicationContext().getBean(ServletManager.class);
			securityRoles = servletManager.getDeclaredRoles();
		} catch (Exception e) { //TODO make IbisTester run without SpringEnvironmentContext
			securityRoles = Collections.emptyList();
		}
	}

	private List<SecurityRolesDTO> getSecurityRoles() {
		return securityRoles.stream()
				.map(SecurityRolesDTO::new)
				.collect(Collectors.toList());
	}

	public static class SecurityRolesDTO {
		private final @Getter String name;
		private final @Getter boolean allowed;

		public SecurityRolesDTO(String role) {
			this.name = role;
			this.allowed = BusMessageUtils.hasRole(role);
		}
	}

	private ArrayList<Object> addJmsRealms() {
		List<String> jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		ArrayList<Object> jmsRealmList = new ArrayList<>();

		for (String realmName : jmsRealms) {
			Map<String, Object> realm = new HashMap<>();
			JmsRealm jmsRealm = JmsRealmFactory.getInstance().getJmsRealm(realmName);

			String dsName = jmsRealm.getDatasourceName();
			String qcfName = jmsRealm.getQueueConnectionFactoryName();
			String tcfName = jmsRealm.getTopicConnectionFactoryName();
			String cfInfo = null;

			if(StringUtils.isNotEmpty(dsName)) {
				realm = mapDataSource(realmName, dsName);
			} else {
				JmsSender js = new JmsSender();
				js.setJmsRealm(realmName);
				if (StringUtils.isNotEmpty(tcfName)) {
					js.setDestinationType(DestinationType.TOPIC);
				}
				try {
					cfInfo = js.getConnectionFactoryInfo();
				} catch (JmsException e) {
					log.debug("no connectionFactory ("+ClassUtils.nameOf(e)+"): "+e.getMessage());
				}
				if (StringUtils.isNotEmpty(qcfName)) {
					realm.put("name", realmName);
					realm.put("queueConnectionFactoryName", qcfName);
					realm.put("info", cfInfo);
				} else if (StringUtils.isNotEmpty(tcfName)) {
					realm.put("name", realmName);
					realm.put("topicConnectionFactoryName", tcfName);
					realm.put("info", cfInfo);
				}
			}
			jmsRealmList.add(realm);
		}

		return jmsRealmList;
	}

	private ArrayList<Object> addDataSources() {
		IDataSourceFactory dataSourceFactory = getBean("dataSourceFactory", IDataSourceFactory.class);
		List<String> dataSourceNames = dataSourceFactory.getDataSourceNames();
		dataSourceNames.sort(Comparator.naturalOrder()); //AlphaNumeric order

		ArrayList<Object> dsList = new ArrayList<>();
		for(String datasourceName : dataSourceNames) {
			dsList.add(mapDataSource(null, datasourceName));
		}
		return dsList;
	}

	private Map<String, Object> mapDataSource(String jmsRealm, String datasourceName) {
		Map<String, Object> realm = new HashMap<>();
		FixedQuerySender qs = createBean(FixedQuerySender.class);

		qs.setQuery("select datasource from database");
		if(StringUtils.isNotEmpty(jmsRealm)) {
			realm.put("name", jmsRealm);
			qs.setJmsRealm(jmsRealm);
		} else {
			qs.setDatasourceName(datasourceName);
		}

		String dsInfo = null;
		try {
			qs.configure();
			dsInfo = qs.getDatasourceInfo();
		} catch (JdbcException | ConfigurationException e) {
			log.debug("no datasource ("+ClassUtils.nameOf(e)+"): "+e.getMessage());
		}
		realm.put("datasourceName", datasourceName);
		realm.put("info", dsInfo);

		return realm;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ArrayList<Object> addSapSystems() {
		ArrayList<Object> sapSystemList = new ArrayList<>();
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
				Map<String, Object> ss = new HashMap<>();
				String name = iter.next();
				ss.put("name", name);
				try {
					ss.put("info", factoryGetSapSystemInfo.invoke(sapSystemFactory, name));
				} catch (Exception e) {
					ss.put("info", "*** ERROR ***");
				}
				sapSystemList.add(ss);
			}
		}
		return sapSystemList;
	}

	private List<String> getAuthEntries() {
		List<String> entries = new LinkedList<>();
		try {
			Collection<String> knownAliases = CredentialFactory.getConfiguredAliases();
			if (knownAliases!=null) {
				entries.addAll(knownAliases); // start with all aliases in the CredentialProvider
				Collections.sort(entries, Comparator.naturalOrder());
			}
		} catch (Exception e) {
			log.warn("could not retrieve aliases from CredentialFactory", e);
		}
		// and add all aliases that are used in the configuration
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			String configString = configuration.getLoadedConfiguration();
			if(StringUtils.isEmpty(configString)) continue; //If a configuration can't be found, continue...

			try {
				Collection<String> c = XmlUtils.evaluateXPathNodeSet(configString, "//@*[starts-with(name(),'authAlias') or ends-with(name(),'AuthAlias')]");
				if (c != null && !c.isEmpty()) {
					for (Iterator<String> cit = c.iterator(); cit.hasNext();) {
						String entry = cit.next();
						if (!entries.contains(entry)) {
							entries.add(entry);
						}
					}
				}
			} catch (Exception e) {
				log.warn("an error occurred while evaluating 'authAlias' xPathExpression", e);
			}
		}
		return entries;
	}

	private List<Object> addAuthEntries() {
		List<Object> authEntries = new LinkedList<>();

		for(String authAlias : getAuthEntries()) {
			Map<String, Object> ae = new HashMap<>();

			ae.put("alias", authAlias);
			CredentialFactory cf = new CredentialFactory(authAlias);

			String userName;
			String passWord;
			try {
				userName = cf.getUsername();
				passWord = cf.getPassword();
				passWord = (passWord==null) ? "no password found" : StringUtils.repeat("*", cf.getPassword().length());
			} catch (Exception e) {
				log.warn(e.getMessage());
				userName = "*** ERROR ***";
				passWord = "*** ERROR ***";
			}

			ae.put("username", userName);
			ae.put("password", passWord);
			authEntries.add(ae);
		}

		return authEntries;
	}
}
