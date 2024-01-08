/*
   Copyright 2023 Nationale-Nederlanden

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

import java.util.List;

public abstract class SecurityHandlerBase implements ISecurityHandler {
	public boolean isUserInAnyRole(List<String> roles, PipeLineSession session) {
		return roles.stream().anyMatch(role -> this.isUserInRole(role, session));
	}

	public String inWhichRoleIsUser(List<String> roles, PipeLineSession session) {
		return roles.stream().filter(role -> this.isUserInRole(role, session)).findFirst().get();
	}
}
