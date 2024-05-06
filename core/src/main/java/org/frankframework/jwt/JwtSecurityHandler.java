/*
   Copyright 2021, 2023-2024 WeAreFrank!

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
package org.frankframework.jwt;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.ISecurityHandler;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

import lombok.Getter;

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
	public boolean isUserInRole(String role) {
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
	public Principal getPrincipal() {
		return () -> (String) claimsSet.get(principalNameClaim);
	}

	public void validateClaims(String requiredClaims, String exactMatchClaims, String anyMatchClaims) throws AuthorizationException {
		// verify required claims exist
		if(StringUtils.isNotEmpty(requiredClaims)) {
			validateRequiredClaims(requiredClaims);
		}

		// verify claims have expected values
		if(StringUtils.isNotEmpty(exactMatchClaims)) {
			validateExactMatchClaims(exactMatchClaims);
		}

		// verify matchOneOf claims
		if(StringUtils.isNotEmpty(anyMatchClaims)) {
			validateAnyMatchClaims(anyMatchClaims);
		}
	}

	void validateRequiredClaims(@Nonnull String requiredClaims) throws AuthorizationException {
		List<String> missingClaims = StringUtil.splitToStream(requiredClaims)
				.filter(claim -> !claimsSet.containsKey(claim))
				.collect(Collectors.toList());

		if(!missingClaims.isEmpty()){
			throw new AuthorizationException("JWT missing required claims: " + missingClaims);
		}
	}

	void validateExactMatchClaims(@Nonnull String exactMatchClaims) throws AuthorizationException {
		Optional<Map.Entry<String, String>> nonMatchingClaim = splitClaims(exactMatchClaims)
				.filter(entry -> !entry.getValue().equals(getClaimAsString(entry.getKey())))
				.findFirst();

		if(nonMatchingClaim.isPresent()){
			String key = nonMatchingClaim.get().getKey();
			String expectedValue = nonMatchingClaim.get().getValue();
			throw new AuthorizationException("JWT "+key+" claim has value ["+claimsSet.get(key)+"], must be ["+expectedValue+"]");
		}
	}

	void validateAnyMatchClaims(@Nonnull String anyMatchClaims) throws AuthorizationException {
		Map<String, Set<String>> allowedValuesByClaim = splitClaims(anyMatchClaims)
				.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
		boolean matchesOneOf = allowedValuesByClaim
				.entrySet()
				.stream()
				.anyMatch(entry -> entry.getValue().contains(getClaimAsString(entry.getKey())));

		if(!matchesOneOf){
			throw new AuthorizationException("JWT does not match one of: ["+ anyMatchClaims +"]");
		}
	}

	@Nonnull
	private String getClaimAsString(String claim) {
		Object value = claimsSet.get(claim);
		if (value == null) {
			return "";
		}
		return String.valueOf(value);
	}

	private Stream<Map.Entry<String, String>> splitClaims(String claimsToSplit){
		return StringUtil.splitToStream(claimsToSplit)
				.map(s -> StringUtil.split(s, "="))
				.filter(this::isValidKeyValuePair)
				.map(pair -> ImmutablePair.of(pair.get(0), pair.get(1)));
	}

	private boolean isValidKeyValuePair(List<String> pair) {
		if (pair.size() != 2 || pair.stream().anyMatch(String::isEmpty)) {
			log.warn("Skipping claim validation for [{}] because it's not a valid key/value pair!", pair);
			return false;
		}
		return true;
	}

}
