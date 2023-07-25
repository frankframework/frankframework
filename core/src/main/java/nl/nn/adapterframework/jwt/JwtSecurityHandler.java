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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;

public class JwtSecurityHandler implements ISecurityHandler {

	private @Getter Map<String, Object> claimsSet;
	private @Getter String roleClaim;

	public JwtSecurityHandler(Map<String, Object> claimsSet, String roleClaim) {
		this.claimsSet = claimsSet;
		this.roleClaim = roleClaim;
	}

	//JWTClaimNames#AUDIENCE claim may be a String or List<String>. Others are either a String or Long (epoch date)
	@Override
	public boolean isUserInRole(String role, PipeLineSession session) {
		Object claim = getClaimsSet().get(roleClaim);
		if(claim instanceof String) {
			return role.equals(claim);
		} else if(claim instanceof List) {
			List<String> claimList = (List) claim;
			return claimList.stream().anyMatch(role::equals);
		}
		return false;
	}

	@Override
	public Principal getPrincipal(PipeLineSession session) {
		Principal principal = new Principal() {

			@Override
			public String getName() {
				return (String) getClaimsSet().get("sub");
			}

		};
		return principal;
	}

	public void validateClaims(String requiredClaims, String exactMatchClaims) throws AuthorizationException {
		// verify required claims exist
		if(StringUtils.isNotEmpty(requiredClaims)) {
			List<String> claims = Stream.of(requiredClaims.split("\\s*,\\s*")).collect(Collectors.toList());
			for (String claim : claims) {
				if(!claimsSet.containsKey(claim)) {
					throw new AuthorizationException("JWT missing required claims: ["+claim+"]");
				}
			}
		}

		// verify claims have expected values
		if(StringUtils.isNotEmpty(exactMatchClaims)) {
			Map<String, String> claims = Stream.of(exactMatchClaims.split("\\s*,\\s*"))
					.map(s -> s.split("\\s*=\\s*")).collect(Collectors.toMap(item -> item[0], item -> item[1]));
			for (String key : claims.keySet()) {
				String expectedValue = claims.get(key);
				Object value = claimsSet.get(key);
				if(!expectedValue.equals(value)) { //Value may be a List<String>, Long or String
					throw new AuthorizationException("JWT "+key+" claim has value ["+value+"], must be ["+expectedValue+"]");
				}
			}
		}
	}

}
