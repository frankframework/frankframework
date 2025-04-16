/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.core;

import java.security.Principal;
import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * ISecurityHandler implementation that uses Spring Security's SecurityContextHolder to retrieve the user information. Since the application is fully
 * based on spring and spring security, the SecurityContextHolder should always be filled when security is enabled.
 *
 * @author evandongen
 */
public class ContextSecurityHandler implements ISecurityHandler {
	@Override
	public boolean isUserInRole(String role) throws NotImplementedException {
		return getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring(5))
				.anyMatch(authority -> authority.equalsIgnoreCase(role));
	}

	Collection<? extends GrantedAuthority> getAuthorities() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities();
	}

	@Override
	public Principal getPrincipal() throws NotImplementedException {
		return SecurityContextHolder.getContext().getAuthentication();
	}
}
