package org.frankframework.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.nimbusds.jwt.SignedJWT;

import org.frankframework.util.SpringUtils;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class HmacJwtGeneratorTest {

	@Test
	@WithMockUser
	void testInit() throws Exception {
		byte[] secret = "secret0000secret0000secret0000secret0000secret0000secret0000secret0000secret".getBytes();

		GenericApplicationContext ac = new GenericApplicationContext();
		ac.setDisplayName("bla bla appl");
		ac.refresh();
		HmacJwtGenerator generator = new HmacJwtGenerator(secret);
		SpringUtils.autowireByType(ac, generator);

		String jwt = generator.createJWT();
		assertNotNull(jwt);

		assertNotNull(generator.jwtHeader, "jwtHeader should be set");
		assertEquals("HS512", generator.jwtHeader.getAlgorithm().getName(), "Should use HS512");
		Map<String, Object> claims = SignedJWT.parse(jwt).getPayload().toJSONObject();
		assertEquals("user", claims.get("sub"));
		assertEquals("[ROLE_USER]", claims.get("scope").toString());

		JwtVerifier verifier = new JwtVerifier(secret);

		Authentication authObject = verifier.verify(jwt);
		assertNotNull(authObject);
		assertEquals("user", authObject.getName());
	}
}
