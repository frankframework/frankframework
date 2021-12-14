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
package nl.nn.adapterframework.jwt;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;

public class JwtSecurityHandler implements ISecurityHandler {

	private @Getter Map<String, Object> claimsSet;
	private @Getter String roleClaim;

	public JwtSecurityHandler(Map<String, Object> claimsSet, String roleClaim) throws Exception {
		this.claimsSet = claimsSet;
		this.roleClaim = roleClaim;
	}

	@Override
	public boolean isUserInRole(String role, PipeLineSession session) throws NotImplementedException {
		String claim = (String) getClaimsSet().get(roleClaim);
		if (StringUtils.isNotEmpty(claim)) {
			claim.contains(role);
		}
		return true;
	}

	@Override
	public Principal getPrincipal(PipeLineSession session) throws NotImplementedException {
		Principal principal = new Principal() {

			@Override
			public String getName() {
				return (String) getClaimsSet().get("sub");
			}

		};
		return principal;
	}

}
