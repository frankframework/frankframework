/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.lifecycle.servlets;

import lombok.Getter;

// LdapAuthenticationProvider
public enum AuthenticationType {
	AD(ActiveDirectoryAuthenticator.class),
	CONTAINER(JeeAuthenticator.class),
	IN_MEMORY(InMemoryAuthenticator.class),
	OAUTH2(OAuth2Authenticator.class),
	YML(YmlFileAuthenticator.class),
	YAML(YmlFileAuthenticator.class),
	NONE(NoOpAuthenticator.class),
	SEALED(SealedAuthenticator.class);

	/**
	 * NB. Should be initialized with a Spring AutoWired /Value enabled PostProcessor.
	 */
	private final @Getter Class<? extends IAuthenticator> authenticator;

	AuthenticationType(Class<? extends IAuthenticator> clazz) {
		authenticator = clazz;
	}
}
