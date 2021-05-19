/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.secrets;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class CredentialFactory {
	protected Logger log = LogUtil.getLogger(this);

	private final String PROPERTY_CREDENTIAL_FACTORY="credentialFactory.class";
	private final String DEFAULT_CREDENTIAL_FACTORY=WebSphereCredentialFactory.class.getName();

	private @Getter ICredentialFactory delegate;

	private static CredentialFactory self;

	public static CredentialFactory getInstance() {
		if (self==null) {
			self=new CredentialFactory();
		}
		return self;
	}

	private CredentialFactory() {
		String factoryClassName = AppConstants.getInstance().getProperty(PROPERTY_CREDENTIAL_FACTORY, DEFAULT_CREDENTIAL_FACTORY);
		if (StringUtils.isNotEmpty(factoryClassName)) {
			try {
				Class<ICredentialFactory> factoryClass = (Class<ICredentialFactory>)Class.forName(factoryClassName);
				delegate = factoryClass.newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				log.warn("Cannot instantiate CredentialFactory [{}]", factoryClassName, e);
			}
		}
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
