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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtSecurityHandlerTest {
	public final String PRINCIPAL = "TST9";
	public final String PRINCIPAL_CLAIM = "principal";

	public final String ROLE = "TestRole";
	public final String ROLE_CLAIM = "roles";

	public final String NO_MATCH = "NO_MATCH";

	public String claim(String key, String value){
		return key+"="+value;
	}

	public String claims(String... claims){
		return String.join(",", claims);
	}

	@Test
	public void principalClaim(){
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, null, PRINCIPAL_CLAIM);

		// Expect
		String principal = securityHandler.getPrincipal(null).getName();
		assertEquals(PRINCIPAL, principal);
	}

	@Test
	public void roleClaim(){
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(ROLE_CLAIM, ROLE);

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		// Expect
		boolean isUserInRole = securityHandler.isUserInRole(ROLE, null);
		assertTrue(isUserInRole);
	}

	@Test
	public void roleClaimAsList(){
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(ROLE_CLAIM, Arrays.asList(ROLE));

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		// Expect
		boolean isUserInRole = securityHandler.isUserInRole(ROLE, null);
		assertTrue(isUserInRole);
	}

	@Test
	public void requiredSingleClaim() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM));
	}

	@Test
	public void requiredSingleClaimMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM),
				"JWT missing required claims: ["+PRINCIPAL_CLAIM+"]");
	}

	@Test
	public void requiredClaims() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateRequiredClaims(claims(PRINCIPAL_CLAIM, ROLE_CLAIM)));
	}

	@Test
	public void requiredClaimsAllMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class,
				() -> securityHandler.validateRequiredClaims(claims(PRINCIPAL_CLAIM, ROLE_CLAIM)),
				"JWT missing required claims: ["+PRINCIPAL_CLAIM+", "+ROLE_CLAIM+"]");
	}

	@Test
	public void requiredClaimsOneMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);
		claims.put(ROLE_CLAIM, PRINCIPAL);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateRequiredClaims(claims(PRINCIPAL_CLAIM, ROLE_CLAIM)),
				"JWT missing required claims: ["+PRINCIPAL_CLAIM+"]");
	}

	@Test
	public void exactMatchSingleClaim() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateExactMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)));
	}

	@Test
	public void exactMatchClaimNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)),
				"JWT "+PRINCIPAL_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaimMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)),
				"JWT "+PRINCIPAL_CLAIM+" claim has value [null], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaims() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))));
	}

	@Test
	public void exactMatchClaimsAllNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		claims.put(ROLE_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+PRINCIPAL_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaimsOneNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+ROLE_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+ROLE+"]");
	}

	@Test
	public void exactMatchClaimsAllMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+PRINCIPAL_CLAIM+" claim has value [null], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaimsOneMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateExactMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT "+ROLE_CLAIM+" claim has value [null], must be ["+ROLE+"]");
	}

	@Test
	public void anyMatchSingleClaim() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateAnyMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)));
	}

	@Test
	public void anyMatchSingleClaimNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateAnyMatchClaims(claim(PRINCIPAL_CLAIM, PRINCIPAL)),
				"JWT does not match one of: ["+claim(PRINCIPAL_CLAIM, PRINCIPAL)+"]");
	}

	@Test
	public void anyMatchSingleClaimMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertThrows(AuthorizationException.class, () -> securityHandler.validateAnyMatchClaims(claim(PRINCIPAL_CLAIM, ROLE)),
				"JWT does not match one of: ["+claim(PRINCIPAL_CLAIM, ROLE)+"]");
	}

	@Test
	public void anyMatchOutOfTwoClaims() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateAnyMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))));
	}

	@Test
	public void anyMatchOutOfTwoClaimsOneMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect/When
		assertDoesNotThrow(() -> securityHandler.validateAnyMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))));
	}

	@Test
	public void anyMatchOutOfTwoClaimsAllMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);


		//Expect
		assertThrows(AuthorizationException.class, () -> securityHandler.validateAnyMatchClaims(claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))),
				"JWT does not match one of: ["+claims(claim(PRINCIPAL_CLAIM, PRINCIPAL), claim(ROLE_CLAIM, ROLE))+"]");
	}
}
