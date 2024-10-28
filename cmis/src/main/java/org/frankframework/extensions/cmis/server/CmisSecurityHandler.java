/*
   Copyright 2019 Nationale-Nederlanden, 2024 WeAreFrank

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
package org.frankframework.extensions.cmis.server;

import java.security.Principal;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.commons.lang3.NotImplementedException;

import org.frankframework.core.ISecurityHandler;
import org.frankframework.util.CredentialFactory;

/**
 * Wraps the CMIS SecurityContext in an ISecurityHandler.
 *
 * @author Niels Meijer
 *
 */
public class CmisSecurityHandler implements ISecurityHandler {

	private CredentialFactory credentials = null;

	public CmisSecurityHandler(CallContext callContext) {
		this.credentials = new CredentialFactory(null, callContext.getUsername(), callContext.getPassword());
	}

	@Override
	public boolean isUserInRole(String role) throws NotImplementedException {
		return false;
	}

	@Override
	public Principal getPrincipal() throws NotImplementedException {

		return new Principal() {
			@Override
			public String getName() {
				return getCredentials().getUsername();
			}
		};
	}

	/**
	 * Can be used to authenticate the user against another system or pass the authentication context through.
	 * @return the user's credentials wrapped in a {@link CredentialFactory}
	 */
	public CredentialFactory getCredentials() {
		return credentials;
	}
}
