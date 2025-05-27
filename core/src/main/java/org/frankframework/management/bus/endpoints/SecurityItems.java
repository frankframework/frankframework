/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.jdbc.datasource.ObjectFactory.ObjectInfo;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.lifecycle.ServletManager;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.XmlUtils;

@Log4j2
@BusAware("frank-management-bus")
public class SecurityItems extends BusEndpointBase {
	private List<String> securityRoles;

	@TopicSelector(BusTopic.SECURITY_ITEMS)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getSecurityItems(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("securityRoles", getSecurityRoles());
		returnMap.put("jmsRealms", addJmsRealms());
		returnMap.put("resourceFactories", addResourceFactories());
		returnMap.put("sapSystems", addSapSystems());
		returnMap.put("authEntries", addAuthEntries());
		returnMap.put("xmlComponents", XmlUtils.getVersionInfo());
		returnMap.put("supportedConnectionOptions", getSupportedProtocolsAndCyphers());

		return new JsonMessage(returnMap);
	}

	@Override
	protected void doAfterPropertiesSet() {
		try {
			ServletManager servletManager = getApplicationContext().getBean(ServletManager.class);
			securityRoles = servletManager.getDeclaredRoles();
		} catch (Exception e) { // TODO make IbisTester run without SpringEnvironmentContext
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

	private Map<String, String> addJmsRealms() {
		List<String> jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		return jmsRealms.stream()
				.map(e -> JmsRealmFactory.getInstance().getJmsRealm(e))
				.collect(Collectors.toMap(JmsRealm::getRealmName, JmsRealm::toString));
	}

	@SuppressWarnings("rawtypes")
	private List<ObjectFactoryDTO> addResourceFactories() {
		Map<String, ObjectFactory> objectFactories = getApplicationContext().getBeansOfType(ObjectFactory.class);
		List<ObjectFactoryDTO> mappedFactories = new ArrayList<>();

		for (Entry<String, ObjectFactory> entry : objectFactories.entrySet()) {
			final ObjectFactory<?, ?> factory = entry.getValue();
			final List<ObjectInfo> objects = factory.getObjectInfo();

			if (!objects.isEmpty()) {
				mappedFactories.add(new ObjectFactoryDTO(factory.getDisplayName(), objects));
			}
		}

		return mappedFactories;
	}

	public static record ObjectFactoryDTO(String name, List<ObjectInfo> resources) {}

	@SuppressWarnings({ "unchecked", "rawtypes", "java:S1181" })
	private ArrayList<Object> addSapSystems() {
		ArrayList<Object> sapSystemList = new ArrayList<>();
		List<String> sapSystems = null;
		Object sapSystemFactory = null;
		Method factoryGetSapSystemInfo = null;
		try {
			Class<?> c = Class.forName("org.frankframework.extensions.sap.SapSystemFactory");
			Method factoryGetInstance = c.getMethod("getInstance");
			sapSystemFactory = factoryGetInstance.invoke(null, (Object[])null);
			Method factoryGetRegisteredSapSystemsNamesAsList = c.getMethod("getRegisteredSapSystemsNamesAsList");

			sapSystems = (List) factoryGetRegisteredSapSystemsNamesAsList.invoke(sapSystemFactory, (Object[])null);
			factoryGetSapSystemInfo = c.getMethod("getSapSystemInfo", String.class);
		} catch (Throwable t) {
			log.debug("Caught NoClassDefFoundError, just no sapSystem available: {}", t.getMessage());
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
		List<String> entries = new ArrayList<>();
		try {
			Collection<String> knownAliases = CredentialFactory.getConfiguredAliases();
			entries.addAll(knownAliases); // start with all aliases in the CredentialProvider
			entries.sort(Comparator.naturalOrder());
		} catch (Exception e) {
			log.warn("could not retrieve aliases from CredentialFactory", e);
		}
		// and add all aliases that are used in the configuration
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			String configString = configuration.getLoadedConfiguration();
			if(StringUtils.isEmpty(configString)) continue; // If a configuration can't be found, continue...

			try {
				Collection<String> c = XmlUtils.evaluateXPathNodeSet(configString, "//@*[starts-with(name(),'authAlias') or ends-with(name(),'AuthAlias')]");
				if (!c.isEmpty()) {
					for (String entry : c) {
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
		List<Object> authEntries = new ArrayList<>();

		for(String authAlias : getAuthEntries()) {
			Map<String, Object> ae = new HashMap<>();

			ae.put("alias", authAlias);

			String userName;
			String passWord;
			try {
				CredentialFactory cf = new CredentialFactory(authAlias);

				userName = cf.getUsername();
				passWord = cf.getPassword();
				passWord = passWord==null ? "no password found" : StringUtils.repeat("*", cf.getPassword().length());
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

	private Map<String, String[]> getSupportedProtocolsAndCyphers() {
		Map<String, String[]> supportedOptions = new HashMap<>();
		try {
			SSLParameters supportedSSLParameters = SSLContext.getDefault().getSupportedSSLParameters();
			supportedOptions.put("protocols", supportedSSLParameters.getProtocols());
			supportedOptions.put("cyphers", supportedSSLParameters.getCipherSuites());
		} catch (NoSuchAlgorithmException e) {
			supportedOptions.put("protocols", new String[0]);
			supportedOptions.put("cyphers", new String[0]);
		}
		return supportedOptions;
	}
}
