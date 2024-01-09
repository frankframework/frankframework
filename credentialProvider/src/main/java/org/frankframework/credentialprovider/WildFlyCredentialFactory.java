/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.credentialprovider;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.credentialprovider.util.CredentialConstants;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;

public class WildFlyCredentialFactory implements ICredentialFactory {
	protected Logger log = Logger.getLogger(this.getClass().getName());

	private static final ServiceName SERVICE_NAME_CRED_STORE = ServiceName.of("org", "wildfly", "security", "credential-store");

	public static final String WILDFLY_CREDENTIALSTORE_PROPERTY="credentialFactory.wildfly.credentialStore";

	private String credentialStore = "CS";

	private CredentialStore cs=null;

	@Override
	public void initialize() {
		log.info("Initializing WildFlyCredentialFactory");
		CredentialConstants appConstants = CredentialConstants.getInstance();
		credentialStore = appConstants.getProperty(WILDFLY_CREDENTIALSTORE_PROPERTY, credentialStore);
		if (StringUtils.isEmpty(credentialStore)) {
			throw new IllegalStateException("No valid property ["+WILDFLY_CREDENTIALSTORE_PROPERTY+"] found");
		}
	}

	@Override
	public ICredentials getCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		CredentialStore cs = getCredentialStore(credentialStore);
		if (cs==null) {
			throw new NoSuchElementException("CredentialStore [" + credentialStore + "] not found");
		}
		return new WildFlyCredentials(cs, alias, defaultUsernameSupplier, defaultPasswordSupplier);
	}

	@Override
	public boolean hasCredentials(String alias) {
		CredentialStore cs = getCredentialStore(credentialStore);
		try {
			return cs!=null && (cs.exists(alias, PasswordCredential.class) || cs.exists(alias+"/username", PasswordCredential.class));
		} catch (CredentialStoreException e) {
			log.fine(()->"exception testing for alias ["+alias+"] ("+e.getClass().getName()+") :"+e.getMessage());
			return false;
		}
	}

	@Override
	public Collection<String> getConfiguredAliases() throws UnsupportedOperationException, CredentialStoreException {
		CredentialStore cs = getCredentialStore(credentialStore);
		if (cs==null) {
			return null;
		}
		Set<String> result = new LinkedHashSet<>();
		Set<String> aliases = cs.getAliases();
		if (aliases!=null) {
			for (String csAlias : aliases) {
				if (csAlias.endsWith("/username")) {
					csAlias = csAlias.substring(0, csAlias.length()-9);
				}
				result.add(csAlias);
			}
		}
		return result;
	}

	private CredentialStore getCredentialStore(String credentialStore) {
		if (cs==null) {
			ServiceContainer registry = getServiceContainer();

			if (registry==null) {
				throw new IllegalStateException("no ServiceContainer registry found");
			}
			ServiceController<?> credStoreService = registry.getService(ServiceName.of(SERVICE_NAME_CRED_STORE, credentialStore));
			if (credStoreService == null) {
				throw new NoSuchElementException("ServiceController for CredentialStore [" + credentialStore + "] not found");
			}
			cs = (CredentialStore) credStoreService.getValue();
		}
		return cs;
	}

	//Make method mockable
	protected ServiceContainer getServiceContainer() {
		return CurrentServiceContainer.getServiceContainer();
	}
}
