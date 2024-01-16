package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.frankframework.lifecycle.DynamicRegistration;

public class AuthorityMapperTest {
	private final Set<String> ALL_USER_ROLES = new HashSet<>(Arrays.asList(DynamicRegistration.ALL_IBIS_USER_ROLES));

	@Test
	public void testAuthorityMapper() throws IOException {
		// Arrange
		URL roleMappingFile = AuthorityMapperTest.class.getResource("/oauth-role-mapping.properties");
		Properties properties = new Properties();
		properties.setProperty("otherProperty", "dummyValue");

		AuthorityMapper mapper = new AuthorityMapper(roleMappingFile, ALL_USER_ROLES, properties);

		List<GrantedAuthority> providedAuthorities = new ArrayList<>();
		providedAuthorities.add(new SimpleGrantedAuthority("SCOPE_https://www.googleapis.com/auth/userinfo.email"));
		providedAuthorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
		providedAuthorities.add(new SimpleGrantedAuthority("dummyValue"));

		// Act
		Collection<? extends GrantedAuthority> remappedAuthorities = mapper.mapAuthorities(providedAuthorities);

		// Assert
		assertNotNull(remappedAuthorities);
		List<String> authorities = remappedAuthorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
		assertEquals(4, authorities.size());

		assertAll(
			() -> assertTrue(authorities.contains("ROLE_IbisObserver")),
			() -> assertTrue(authorities.contains("ROLE_IbisAdmin")),
			() -> assertTrue(authorities.contains("ROLE_IbisDataAdmin")),
			() -> assertTrue(authorities.contains("ROLE_IbisTester"))
		);
	}
}
