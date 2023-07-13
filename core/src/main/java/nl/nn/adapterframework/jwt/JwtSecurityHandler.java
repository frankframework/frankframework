/*
   Copyright 2021, 2023 WeAreFrank!

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.StringUtil;

public class JwtSecurityHandler implements ISecurityHandler {

	private final @Getter Map<String, Object> claimsSet;
	private final @Getter String roleClaim;
	private final @Getter String principalNameClaim;

	public JwtSecurityHandler(Map<String, Object> claimsSet, String roleClaim, String principalNameClaim) {
		this.claimsSet = claimsSet;
		this.roleClaim = roleClaim;
		this.principalNameClaim = principalNameClaim;
	}

	@Override
	public boolean isUserInRole(String role, PipeLineSession session) {
		String claim = (String) getClaimsSet().get(roleClaim);
		return role.equals(claim);
	}

	@Override
	public Principal getPrincipal(PipeLineSession session) {
		return () -> (String) getClaimsSet().get(principalNameClaim);
	}

	public void validateClaims(String requiredClaims, String exactMatchClaims) throws AuthorizationException {
		// verify required claims exist
		if(StringUtils.isNotEmpty(requiredClaims)) {
			List<String> claims = StringUtil.split(requiredClaims);
			for (String claim : claims) {
				if(!claimsSet.containsKey(claim)) {
					throw new AuthorizationException("JWT missing required claims: ["+claim+"]");
				}
			}
		}

		// verify claims have expected values
		if(StringUtils.isNotEmpty(exactMatchClaims)) {
			Map<String, String> claims = StringUtil.splitToStream(exactMatchClaims)
					.map(s -> StringUtil.split(s, "="))
					.collect(Collectors.toMap(item -> item.get(0), item -> item.get(1)));
			for (Map.Entry<String, String> entry : claims.entrySet()) {
				String key = entry.getKey();
				String expectedValue = entry.getValue();
				String value = (String) claimsSet.get(key);
				if(!value.equals(expectedValue)) {
					throw new AuthorizationException("JWT "+key+" claim has value ["+value+"], must be ["+expectedValue+"]");
				}
			}
		}
	}

}
