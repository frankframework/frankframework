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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

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
		List<String> authorities = remappedAuthorities.stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		assertEquals(4, authorities.size());

		assertAll(
			() -> assertTrue(authorities.contains("ROLE_IbisObserver")),
			() -> assertTrue(authorities.contains("ROLE_IbisAdmin")),
			() -> assertTrue(authorities.contains("ROLE_IbisDataAdmin")),
			() -> assertTrue(authorities.contains("ROLE_IbisTester"))
		);
	}

	public static List<Arguments> data() {
		return Arrays.asList(new Arguments[]{
				Arguments.of("roles", Map.of("roles", List.of("IbisAdmin"))),
				Arguments.of("realm_access.roles", Map.of("realm_access", Map.of("roles", List.of("IbisAdmin"))))
		});
	}

	@MethodSource("data")
	@ParameterizedTest
	void testAuthorityMapperWithOidcUserAuthority(String authoritiesClaimName, Map<String, Object> authorityClaims) throws IOException {
		// Arrange
		URL roleMappingFile = AuthorityMapperTest.class.getResource("/oauth-role-mapping.properties");
		Properties properties = new Properties();

		// This is replaced in the role-mapping file to map to ROLE_IbisAdmin
		properties.setProperty("otherProperty", "ROLE_IbisAdmin");

		AuthorityMapper mapper = new AuthorityMapper(roleMappingFile, ALL_USER_ROLES, properties, authoritiesClaimName);

		List<GrantedAuthority> providedAuthorities = new ArrayList<>();
		providedAuthorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
		providedAuthorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
		providedAuthorities.add(new OidcUserAuthority(
				new OidcIdToken("test", null, null, Map.of("claim1", "value1")),
				new OidcUserInfo(authorityClaims)
		));

		// Act
		Collection<? extends GrantedAuthority> mappedAuthorities = mapper.mapAuthorities(providedAuthorities);

		// Assert
		assertNotNull(mappedAuthorities);
		List<String> authorities = mappedAuthorities.stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		assertEquals(3, authorities.size());
	}

	@MethodSource("data")
	@ParameterizedTest
	void testAuthorityMapperWithOauthUserAuthority(String authoritiesClaimName, Map<String, Object> authorityClaims) throws IOException {
		// Arrange
		URL roleMappingFile = AuthorityMapperTest.class.getResource("/oauth-role-mapping.properties");
		Properties properties = new Properties();

		// This is replaced in the role-mapping file to map to ROLE_IbisAdmin
		properties.setProperty("otherProperty", "ROLE_IbisAdmin");

		AuthorityMapper mapper = new AuthorityMapper(roleMappingFile, ALL_USER_ROLES, properties, authoritiesClaimName);

		List<GrantedAuthority> providedAuthorities = new ArrayList<>();
		providedAuthorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
		providedAuthorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
		providedAuthorities.add(new OAuth2UserAuthority(authorityClaims));

		// Act
		Collection<? extends GrantedAuthority> mappedAuthorities = mapper.mapAuthorities(providedAuthorities);

		// Assert
		assertNotNull(mappedAuthorities);
		List<String> authorities = mappedAuthorities.stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		assertEquals(3, authorities.size());
	}
}
