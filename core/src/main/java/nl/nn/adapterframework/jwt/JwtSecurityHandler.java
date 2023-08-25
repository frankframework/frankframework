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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringUtil;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.PipeLineSession;

import org.apache.logging.log4j.Logger;

public class JwtSecurityHandler implements ISecurityHandler {
	protected Logger log = LogUtil.getLogger(this);

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
			Optional<String> missingClaim  = StringUtil.splitToStream(requiredClaims)
					.filter(claim -> !claimsSet.containsKey(claim))
					.findFirst();

			if(missingClaim.isPresent()){
				throw new AuthorizationException("JWT missing required claims: ["+missingClaim.get()+"]");
			}
		}

		// verify claims have expected values
		if(StringUtils.isNotEmpty(exactMatchClaims)) {
			Optional<Map.Entry<String, String>> nonMatchingClaim = StringUtil.splitToStream(exactMatchClaims)
					.map(s -> StringUtil.split(s, "="))
					.filter(this::isValidKeyValuePair)
					.collect(Collectors.toMap(item -> item.get(0), item -> item.get(1)))
					.entrySet()
					.stream()
					.filter(entry -> !claimsSet.get(entry.getKey()).equals(entry.getValue()))
					.findFirst();

			if(nonMatchingClaim.isPresent()){
				String key = nonMatchingClaim.get().getKey();
				String expectedValue = nonMatchingClaim.get().getValue();
				throw new AuthorizationException("JWT "+key+" claim has value ["+claimsSet.get(key)+"], must be ["+expectedValue+"]");
			}
		}

		// verify matchOneOf claims
		if(StringUtils.isNotEmpty(matchOneOfClaims)) {
			Map<String, HashSet<String>> multiMap = new HashMap<>();

			StringUtil.splitToStream(matchOneOfClaims)
					.map(s -> StringUtil.split(s, "="))
					.filter(this::isValidKeyValuePair)
					.forEach(pair -> multiMap.computeIfAbsent(pair.get(0), key -> new HashSet<>()).add(pair.get(1)));

			boolean matchesOneOf = multiMap.keySet().stream().anyMatch(key -> multiMap.get(key).contains((String) claimsSet.get(key)));

			if(!matchesOneOf){
				throw new AuthorizationException("JWT does not match one of: ["+matchOneOfClaims+"]");
			}
		}
	}

	private boolean isValidKeyValuePair(List<String> pair) {
		if (pair.size() != 2 || pair.stream().anyMatch(String::isEmpty)) {
			log.warn("Skipping claim validation for [" + pair + "] because it's not a valid key/value pair!");
			return false;
		}
		return true;
	}

}
