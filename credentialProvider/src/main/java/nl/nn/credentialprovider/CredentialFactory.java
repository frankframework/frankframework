/*
   Copyright 2021 Nationale-Nederlanden

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
package nl.nn.credentialprovider;



import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.credentialprovider.util.AppConstants;

public class CredentialFactory {
	protected Logger log = LogManager.getLogger(this);

	private final String PROPERTY_CREDENTIAL_FACTORY="credentialFactory.class";
	private final String DEFAULT_CREDENTIAL_FACTORY1=FileSystemCredentialFactory.class.getName();
	private final String DEFAULT_CREDENTIAL_FACTORY2=WebSphereCredentialFactory.class.getName();

	private @Getter ICredentialFactory delegate;

	private static CredentialFactory self;

	public static CredentialFactory getInstance() {
		if (self==null) {
			self=new CredentialFactory();
		}
		return self;
	}

	private CredentialFactory() {
		String factoryClassName = AppConstants.getInstance().getProperty(PROPERTY_CREDENTIAL_FACTORY);
		if (tryFactory(factoryClassName)) {
			return;
		}
		if (tryFactory(DEFAULT_CREDENTIAL_FACTORY1)) {
			return;
		}
		if (tryFactory(DEFAULT_CREDENTIAL_FACTORY2)) {
			return;
		}
		log.warn("No CredentialFactory installed");
	}

	private boolean tryFactory(String factoryClassName) {
		if (StringUtils.isNotEmpty(factoryClassName)) {
			try {
				Class<ICredentialFactory> factoryClass = (Class<ICredentialFactory>)Class.forName(factoryClassName);
				ICredentialFactory candidate = factoryClass.newInstance();
				if (candidate.init()) {
					log.info("installing CredentialFactory [{}]", factoryClassName);
					delegate = candidate;
					return true;
				}
				log.warn("Cannot initialize CredentialFactory [{}]", factoryClassName);
			} catch (Exception e) {
				log.warn("Cannot instantiate CredentialFactory [{}]", factoryClassName, e);
			}
		}
		return false;
	}
	
	public static ICredentials getCredentials(String alias, String defaultUsername, String defaultPassword) {
		ICredentialFactory delegate = getInstance().getDelegate();
		if (delegate!=null) {
			ICredentials result = delegate.getCredentials(alias, defaultUsername, defaultPassword);
			if (result!=null) {
				return result;
			}
		}
		return new Credentials(alias, defaultUsername, defaultPassword);
	}

}
