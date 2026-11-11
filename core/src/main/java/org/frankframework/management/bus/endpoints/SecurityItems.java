/*
   Copyright 2022-2026 WeAreFrank!

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
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.FrankElement;
import org.frankframework.core.HasSender;
import org.frankframework.encryption.CorePkiUtil;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
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
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.XmlUtils;

@Log4j2
@BusAware("frank-management-bus")
@SuppressWarnings("java:S1181") // We do want to catch Throwable in a number of places because SecurityItems call should always return something, not an error
public class SecurityItems extends BusEndpointBase {

	private final Duration expiringCertificatesDuration;

	private List<String> securityRoles;

	public SecurityItems() {
		int expiringCertAge = AppConstants.getInstance().getInt("SecurityItems.expiringCertificatesDuration", 31);
		if (expiringCertAge > 0) {
			expiringCertificatesDuration = Duration.ofDays(expiringCertAge);
		} else {
			expiringCertificatesDuration = null;
		}
	}

	@TopicSelector(BusTopic.SECURITY_ITEMS)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getSecurityItems(Message<?> message) {
		// This method should not fail, otherwise the front-end does not know the user's security-roles
		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("securityRoles", getSecurityRoles());
		returnMap.put("jmsRealms", addJmsRealms());
		returnMap.put("resourceFactories", addResourceFactories());
		returnMap.put("sapSystems", addSapSystems());
		returnMap.put("authEntries", addAuthEntries());
		returnMap.put("xmlComponents", XmlUtils.getVersionInfo());
		returnMap.put("supportedConnectionOptions", getSupportedProtocolsAndCyphers());
		returnMap.put("expiringCertificates", getExpiringCertificates());

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
				.toList();
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
		try {
			List<String> jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
			return jmsRealms.stream()
					.map(e -> JmsRealmFactory.getInstance().getJmsRealm(e))
					.collect(Collectors.toMap(JmsRealm::getRealmName, JmsRealm::toString));
		} catch (Throwable e) {
			log.warn("Could not get JMS realms", e);
			return Map.of();
		}
	}

	@SuppressWarnings("rawtypes")
	private List<ObjectFactoryDTO> addResourceFactories() {
		List<ObjectFactoryDTO> mappedFactories = new ArrayList<>();
		try {
			Map<String, ObjectFactory> objectFactories = getApplicationContext().getBeansOfType(ObjectFactory.class);

			for (Entry<String, ObjectFactory> entry : objectFactories.entrySet()) {
				final ObjectFactory<?, ?> factory = entry.getValue();
				final List<ObjectInfo> objects = factory.getObjectInfo();

				if (!objects.isEmpty()) {
					mappedFactories.add(new ObjectFactoryDTO(factory.getDisplayName(), objects));
				}
			}
		} catch (Throwable e) {
			log.warn("Cannot get resource factories", e);
		}

		return mappedFactories;
	}

	public record ObjectFactoryDTO(String name, List<ObjectInfo> resources) {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private @NonNull List<Object> addSapSystems() {
		List<String> sapSystems;
		Object sapSystemFactory;
		Method factoryGetSapSystemInfo;
		try {
			Class<?> c = Class.forName("org.frankframework.extensions.sap.SapSystemFactory");
			Method factoryGetInstance = c.getMethod("getInstance");
			sapSystemFactory = factoryGetInstance.invoke(null, (Object[])null);
			Method factoryGetRegisteredSapSystemsNamesAsList = c.getMethod("getRegisteredSapSystemsNamesAsList");

			sapSystems = (List) factoryGetRegisteredSapSystemsNamesAsList.invoke(sapSystemFactory, (Object[])null);
			factoryGetSapSystemInfo = c.getMethod("getSapSystemInfo", String.class);
		} catch (Throwable t) {
			log.debug("Caught NoClassDefFoundError, just no sapSystem available: {}", t.getMessage());
			return List.of();
		}

		List<Object> sapSystemList = new ArrayList<>();
		if (sapSystems != null) {
			for (String sapSystem : sapSystems) {
				Map<String, Object> ss = new HashMap<>();
				ss.put("name", sapSystem);
				try {
					ss.put("info", factoryGetSapSystemInfo.invoke(sapSystemFactory, sapSystem));
				} catch (Throwable t) {
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
		} catch (Throwable t) {
			log.warn("could not retrieve aliases from CredentialFactory", t);
		}
		// and add all aliases that are used in the configuration
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			String configString = configuration.getLoadedConfiguration();
			if (StringUtils.isEmpty(configString)) continue; // If a configuration can't be found, continue...

			try {
				Collection<String> c = XmlUtils.evaluateXPathNodeSet(configString, "//@*[starts-with(name(),'authAlias') or ends-with(name(),'AuthAlias')]");
				if (!c.isEmpty()) {
					for (String entry : c) {
						if (!entries.contains(entry)) {
							entries.add(entry);
						}
					}
				}
			} catch (Throwable t) {
				log.warn("an error occurred while evaluating 'authAlias' xPathExpression", t);
			}
		}
		return entries;
	}

	@SuppressWarnings("java:S2068") // No actual hardcoded password here, ignore Sonar warning
	private List<Object> addAuthEntries() {
		List<Object> authEntries = new ArrayList<>();

		for (String authAlias : getAuthEntries()) {
			Map<String, Object> ae = new HashMap<>();

			ae.put("alias", authAlias);

			String userName;
			String passWord;
			try {
				CredentialFactory cf = new CredentialFactory(authAlias);

				userName = cf.getUsername();
				passWord = cf.getPassword();
				passWord = passWord==null ? "no password found" : StringUtils.repeat("*", cf.getPassword().length());
			} catch (Throwable t) {
				log.warn(t.getMessage());
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
		} catch (Throwable t) {
			log.warn("could not get supported SSL parameters", t);
			supportedOptions.put("protocols", new String[0]);
			supportedOptions.put("cyphers", new String[0]);
		}
		return supportedOptions;
	}

	// Pff so many ways to clean this mess up....
	private List<KeyStoreDTO> getExpiringCertificates() {
		if (expiringCertificatesDuration == null) {
			return Collections.emptyList();
		}

		List<KeyStoreDTO> expiringCertificates = new ArrayList<>();
		for (Configuration config : getIbisManager().getConfigurations()) {
			for (Adapter adapter : config.getRegisteredAdapters()) {
				List<String> listWithExpiredCertificates = adapter.getPipeLine()
						.getPipes()
						.stream()
						.filter(this::hasKeyOrTrustStore)
						.map(FrankElement.class::cast)
						.map(this::extractSender)
						.map(this::extractKeyStore)
						.filter(Objects::nonNull)
						.map(this::getExpiringCertificates)
						.flatMap(List::stream)
						.distinct()
						.toList();

				if (!listWithExpiredCertificates.isEmpty()) {
					String name = "%s/%s".formatted(config.getName(), adapter.getName());
					expiringCertificates.add(new KeyStoreDTO(name, listWithExpiredCertificates));
				}
			}
		}

		return expiringCertificates;
	}

	public record KeyStoreDTO(String name, List<String> certificates) {}

	private boolean hasKeyOrTrustStore(Object bean) {
		if (bean instanceof HasKeystore keystoreOwner && StringUtils.isNotEmpty(keystoreOwner.getKeystore())) {
			return true;
		}

		// If the bean does not have a Keystore it may have a Truststore.
		if ((bean instanceof HasTruststore truststoreOwner && StringUtils.isNotEmpty(truststoreOwner.getTruststore()))) {
			return true;
		}

		// Lastly, bean may have a Sender that may have a KeyStore or TrustStore
		if (bean instanceof HasSender hasSender) {
			return hasKeyOrTrustStore(hasSender.getSender());
		}

		return false;
	}

	private FrankElement extractSender(FrankElement bean) {
		return (bean instanceof HasSender hasSender) ? hasSender.getSender() : bean;
	}

	private KeyStore extractKeyStore(FrankElement bean) {
		try {
			if (bean instanceof HasKeystore keystoreOwner && StringUtils.isNotEmpty(keystoreOwner.getKeystore())) {
				return CorePkiUtil.createKeyStore(keystoreOwner);
			}
			if (bean instanceof HasTruststore truststoreOwner && StringUtils.isNotEmpty(truststoreOwner.getTruststore())) {
				return CorePkiUtil.createKeyStore(truststoreOwner);
			}
		} catch (EncryptionException e) {
			log.info("unable to open keystore of FrankElement [{}]", bean, e);
		} catch (Throwable t) {
			log.warn("Something bad happened while opening keystore of FrankElement [{}]; invalid keystore?", bean, t);
		}
		return null;
	}

	private List<String> getExpiringCertificates(KeyStore keystore) {
		try {
			return CorePkiUtil.getExpiringCertificates(keystore, expiringCertificatesDuration);
		} catch (EncryptionException e) {
			log.debug("unable to find out of any certificates are due to expire", e);
			return List.of("");
		} catch (Throwable t) {
			log.warn("Something bad happened while checking certificates", t);
			return List.of();
		}
	}
}
