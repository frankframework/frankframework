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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.StringUtil;

public class JwtSecurityHandler implements ISecurityHandler {

	private final @Getter Map<String, Object> claimsSet;
	private final @Getter String roleClaim;
	private final @Getter String principalNameClaim; //Defaults to JWTClaimNames#SUBJECT

	public JwtSecurityHandler(Map<String, Object> claimsSet, String roleClaim, String principalNameClaim) {
		this.claimsSet = claimsSet;
		this.roleClaim = roleClaim;
		this.principalNameClaim = principalNameClaim;
	}

	//JWTClaimNames#AUDIENCE claim may be a String or List<String>. Others are either a String or Long (epoch date)
	@Override
	public boolean isUserInRole(String role, PipeLineSession session) {
		Object claim = claimsSet.get(roleClaim);
		if(claim instanceof String) {
			return role.equals(claim);
		} else if(claim instanceof List) {
			List<String> claimList = (List<String>) claim;
			return claimList.stream().anyMatch(role::equals);
		}
		return false;
	}

	@Override
	public Principal getPrincipal(PipeLineSession session) {
		return () -> (String) claimsSet.get(principalNameClaim);
	}

	public void validateClaims(String requiredClaims, String exactMatchClaims, String matchOneOfClaims) throws AuthorizationException {
		// verify required claims exist
		if(StringUtils.isNotEmpty(requiredClaims)) {
			Optional<String> missingClaim  = StringUtil.split(requiredClaims)
					.stream()
					.filter(claim -> !claimsSet.containsKey(claim))
					.findFirst();

			if(missingClaim.isPresent()){
				throw new AuthorizationException("JWT missing required claims: ["+missingClaim.get()+"]");
			}
		}

		// verify claims have expected values
		if(StringUtils.isNotEmpty(exactMatchClaims)) {
			Optional<Map.Entry<String, String>> nonMatchingClaim = splitClaims(exactMatchClaims)
					.filter(entry -> !claimsSet.get(entry.getKey()).equals(entry.getValue()))
					.findFirst();

			if(nonMatchingClaim.isPresent()){
				String key = nonMatchingClaim.get().getKey();
				String expectedValue = nonMatchingClaim.get().getValue();
				throw new AuthorizationException("JWT "+key+" claim has value ["+claimsSet.get(key)+"], must be ["+expectedValue+"]");
			}
		}

		if(StringUtils.isNotEmpty(matchOneOfClaims)) {
			boolean noneMatch = splitClaims(matchOneOfClaims)
					.noneMatch(entry -> claimsSet.get(entry.getKey()).equals(entry.getValue()));

			if(noneMatch){
				throw new AuthorizationException("JWT does not contain any of the following claims ["+matchOneOfClaims+"]");
			}
		}
	}

	private Stream<Map.Entry<String, String>> splitClaims(String claimsToSplit){
		return StringUtil.splitToStream(claimsToSplit)
				.map(s -> StringUtil.split(s, "="))
				.collect(Collectors.toMap(item -> item.get(0), item -> item.get(1)))
				.entrySet()
				.stream();
	}

}
