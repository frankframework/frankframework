package org.frankframework.jwt;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class JwtSecurityHandlerTest {
	public final String PRINCIPAL = "TST9";
	public final String PRINCIPAL_CLAIM = "principal";

	public final String ROLE = "TestRole";
	public final String ROLE_CLAIM = "roles";

	public final String NO_MATCH = "NO_MATCH";

	@Test
	public void principalClaim(){
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);

		// When
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, null, PRINCIPAL_CLAIM);

		// Expect
		String principal = securityHandler.getPrincipal(null).getName();
		assertEquals(principal, PRINCIPAL);
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

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM);
		});
	}

	@Test
	public void requiredSingleClaimMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM);
		});

		assertEquals(exception.getMessage(), "JWT missing required claims: ["+PRINCIPAL_CLAIM+"]");
	}

	@Test
	public void requiredClaims() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM+","+ROLE_CLAIM);
		});
	}

	@Test
	public void requiredClaimsAllMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM+","+ROLE_CLAIM);
		});

		assertEquals(exception.getMessage(), "JWT missing required claims: ["+PRINCIPAL_CLAIM+", "+ROLE_CLAIM+"]");
	}

	@Test
	public void requiredClaimsOneMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);
		claims.put(ROLE_CLAIM, PRINCIPAL);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateRequiredClaims(PRINCIPAL_CLAIM+","+ROLE_CLAIM);
		});

		assertEquals(exception.getMessage(), "JWT missing required claims: ["+PRINCIPAL_CLAIM+"]");
	}

	@Test
	public void exactMatchSingleClaim() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL);
		});
	}

	@Test
	public void exactMatchClaimNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL);
		});

		assertEquals(exception.getMessage(), "JWT "+PRINCIPAL_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaimMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL);
		});

		assertEquals(exception.getMessage(), "JWT "+PRINCIPAL_CLAIM+" claim has value [null], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaims() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});
	}

	@Test
	public void exactMatchClaimsAllNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		claims.put(ROLE_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});

		assertEquals(exception.getMessage(), "JWT "+PRINCIPAL_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaimsOneNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});

		assertEquals(exception.getMessage(), "JWT "+ROLE_CLAIM+" claim has value ["+NO_MATCH+"], must be ["+ROLE+"]");
	}

	@Test
	public void exactMatchClaimsAllMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});

		assertEquals(exception.getMessage(), "JWT "+PRINCIPAL_CLAIM+" claim has value [null], must be ["+PRINCIPAL+"]");
	}

	@Test
	public void exactMatchClaimsOneMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateExactMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});

		assertEquals(exception.getMessage(), "JWT "+ROLE_CLAIM+" claim has value [null], must be ["+ROLE+"]");
	}

	@Test
	public void anyMatchSingleClaim() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateAnyMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL);
		});
	}

	@Test
	public void anyMatchSingleClaimNotMatching() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, NO_MATCH);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
		});
		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateAnyMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL);
		});
		assertEquals(exception.getMessage(), "JWT does not match one of: ["+PRINCIPAL_CLAIM+"="+PRINCIPAL+"]");
	}

	@Test
	public void anyMatchSingleClaimMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateAnyMatchClaims(PRINCIPAL_CLAIM+"="+ROLE);
		});
		assertEquals(exception.getMessage(), "JWT does not match one of: ["+PRINCIPAL_CLAIM+"="+ROLE+"]");
	}

	@Test
	public void anyMatchOutOfTwoClaims() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(PRINCIPAL_CLAIM, PRINCIPAL);
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateAnyMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});
	}

	@Test
	public void anyMatchOutOfTwoClaimsOneMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		claims.put(ROLE_CLAIM, ROLE);
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
			securityHandler.validateAnyMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});
	}

	@Test
	public void anyMatchOutOfTwoClaimsAllMissing() {
		// Given
		HashMap<String, Object> claims = new HashMap<>();
		JwtSecurityHandler securityHandler = new JwtSecurityHandler(claims, ROLE_CLAIM, PRINCIPAL_CLAIM);

		//Expect
		assertDoesNotThrow(() -> {
			// When
		});
		//Expect
		AuthorizationException exception = assertThrows(AuthorizationException.class, () -> {
			// When
			securityHandler.validateAnyMatchClaims(PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE);
		});
		assertEquals(exception.getMessage(), "JWT does not match one of: ["+PRINCIPAL_CLAIM+"="+PRINCIPAL+","+ROLE_CLAIM+"="+ROLE+"]");
	}
}
