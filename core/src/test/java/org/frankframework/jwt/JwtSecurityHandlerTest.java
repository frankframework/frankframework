/*
   Copyright 2024 WeAreFrank!

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtSecurityHandlerTest {
	private final String PRINCIPAL = "TST9";
	private final String PRINCIPAL_CLAIM = "principal";

	private final String ROLE = "TestRole";
	private final String ROLE_CLAIM = "roles";

	private final String NO_MATCH = "NO_MATCH";
	private HashMap<String, Object> claims;

	private String claim(String key, String value){
		return key+"="+value;
	}
	private String claims(String... claims){
		return String.join(",", claims);
	}

	@BeforeEach
	void beforeEach(){
		claims = new HashMap<>();
	}

	@Test
	void principalClaim(){
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, null, PRINCIPAL_CLAIM);

		// Expect
		String principal = securityHandler.getPrincipal().getName();
		assertEquals(PRINCIPAL, principal);
	}

	@Test
	void roleClaim(){
		// Given
		claims.put(ROLE_CLAIM, ROLE);

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		// Expect
		boolean isUserInRole = securityHandler.isUserInRole(ROLE);
		assertTrue(isUserInRole);
	}

	@Test
	void roleClaimAsList(){
		// Given
		claims.put(ROLE_CLAIM, Arrays.asList(ROLE));

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		// Expect
		boolean isUserInRole = securityHandler.isUserInRole(ROLE);
		assertTrue(isUserInRole);
	}

	@Test
	void requiredSingleClaim() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM));
	}

	@Test
	void requiredSingleClaimMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM),
				"JWT missing required claims: ["+PRINCIPAL_CLAIM+"]");
	}

	@Test
	void requiredClaims() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateRequiredClaims(claims(PRINCIPAL_CLAIM, ROLE_CLAIM)));
	}

	@Test
	void requiredClaimsAllMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class,
				() -> securityHandler.validateRequiredClaims(claims(PRINCIPAL_CLAIM, ROLE_CLAIM)),
				"JWT missing required claims: ["+PRINCIPAL_CLAIM+", "+ROLE_CLAIM+"]");
	}

	@Test
	void requiredClaimsOneMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);
		claims.put(ROLE_CLAIM, PRINCIPAL);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateRequiredClaims(claims(PRINCIPAL_CLAIM, ROLE_CLAIM)),
				"JWT missing required claims: ["+PRINCIPAL_CLAIM+"]");
	}

	@Test
	void exactMatchSingleClaim() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateExactMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)));
	}

	@Test
	void exactMatchClaimNotMatching() {
		// Given
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)),
				"JWT "+PRINCIPAL_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+PRINCIPAL+"]");
	}

	@Test
	void exactMatchClaimMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)),
				"JWT "+PRINCIPAL_CLAIM+" claim has value [null], must be ["+PRINCIPAL+"]");
	}

	@Test
	void exactMatchClaims() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))));
	}

	@Test
	void exactMatchClaimsAllNotMatching() {
		// Given
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		claims.put(ROLE_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+PRINCIPAL_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+PRINCIPAL+"]");
	}

	@Test
	void exactMatchClaimsOneNotMatching() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+ROLE_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+ROLE+"]");
	}

	@Test
	void exactMatchClaimsAllMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+PRINCIPAL_CLAIM+" claim has value [null], must be ["+PRINCIPAL+"]");
	}

	@Test
	void exactMatchClaimsOneMissing() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+ROLE_CLAIM+" claim has value [null], must be ["+ROLE+"]");
	}

	@Test
	void anyMatchSingleClaim() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateAnyMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)));
	}

	@Test
	void anyMatchSingleClaimNotMatching() {
		// Given
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateAnyMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)),
				"JWT does not match one of: ["+claim(PRINCIPAL_CLAIM, PRINCIPAL)+"]");
	}

	@Test
	void anyMatchSingleClaimMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateAnyMatchClaims(claim(PRINCIPAL_CLAIM, ROLE)),
				"JWT does not match one of: ["+claim(PRINCIPAL_CLAIM, ROLE)+"]");
	}

	@Test
	void anyMatchOutOfTwoClaims() {
		// Given
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateAnyMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))));
	}

	@Test
	void anyMatchOutOfTwoClaimsOneMissing() {
		// Given
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateAnyMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))));
	}

	@Test
	void anyMatchOutOfTwoClaimsAllMissing() {
		// Given
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);


		//Expect
		assertThrows(AuthorizationException.class, () -> securityHandler.validateAnyMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT does not match one of: ["+claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))+"]");
	}
}
