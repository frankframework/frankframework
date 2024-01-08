/*
   Copyright 2013 Nationale-Nederlanden

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
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Security handler that declares that each role is valid.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class AllowAllSecurityHandler implements ISecurityHandler {

	@Override
	public boolean isUserInAnyRole(List<String> roles) throws NotImplementedException {
		return true;
	}

	@Override
	public boolean isUserInRole(String role) {
		return true;
	}

	@Override
	public Principal getPrincipal(PipeLineSession session) throws NotImplementedException {
		throw new NotImplementedException("no default user available");
	}

}
