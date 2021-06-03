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



import java.util.logging.Level;
import java.util.logging.Logger;

import nl.nn.credentialprovider.util.AppConstants;
import nl.nn.credentialprovider.util.Misc;

public class CredentialFactory {
	protected Logger log = Logger.getLogger(this.getClass().getCanonicalName());

	private final String PROPERTY_CREDENTIAL_FACTORY="credentialFactory.class";
	private final String DEFAULT_CREDENTIAL_FACTORY1=FileSystemCredentialFactory.class.getName();
	private final String DEFAULT_CREDENTIAL_FACTORY2=WebSphereCredentialFactory.class.getName();

	private ICredentialFactory delegate;

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
		log.warning("No CredentialFactory installed");
	}

	// package private method to force delegate for test purposes
	void forceDelegate(ICredentialFactory delegate) {
		this.delegate=delegate;
	}
	
	private boolean tryFactory(String factoryClassName) {
		if (Misc.isNotEmpty(factoryClassName)) {
			log.info("trying to configure CredentialFactory ["+factoryClassName+"]");
			try {
				Class<ICredentialFactory> factoryClass = (Class<ICredentialFactory>)Class.forName(factoryClassName);
				ICredentialFactory candidate = factoryClass.newInstance();
				if (candidate.init()) {
					log.info("installing CredentialFactory ["+factoryClassName+"]");
					delegate = candidate;
					return true;
				}
				log.warning("Cannot initialize CredentialFactory ["+factoryClassName+"]");
			} catch (Exception e) {
				log.log(Level.WARNING, "Cannot instantiate CredentialFactory ["+factoryClassName+"]", e);
			}
		}
		return false;
	}
	
	public static boolean hasCredential(String alias) {
		ICredentialFactory delegate = getInstance().delegate;
		return delegate==null || delegate.hasCredentials(alias);
	}
	
	public static ICredentials getCredentials(String alias, String defaultUsername, String defaultPassword) {
		ICredentialFactory delegate = getInstance().delegate;
		if (delegate!=null) {
			ICredentials result = delegate.getCredentials(alias, defaultUsername, defaultPassword);
			if (result!=null) {
				return result;
			}
		}
		return new Credentials(alias, defaultUsername, defaultPassword);
	}

}
