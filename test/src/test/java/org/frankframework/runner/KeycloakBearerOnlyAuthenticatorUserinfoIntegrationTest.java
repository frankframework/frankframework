package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.frankframework.util.AppConstants;

/**
 * Tests whether bearer only request authentication works with Keycloak and the application. This test looks a lot like the other one, but
 * this one uses the userinfo endpoint to obtain user details, instead of relying solely on the JWT token.
 *
 * @author erik.van.dongen
 * @see "https://github.com/dasniko/testcontainers-keycloak"
 */
@Tag("integration")
@DisabledWithoutDocker
public class KeycloakBearerOnlyAuthenticatorUserinfoIntegrationTest extends KeycloakBearerOnlyBase{

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setup() throws IOException {
		// Set system properties for the application to use the Keycloak container and start the framework initializer
		System.setProperty("application.security.console.authentication.type", "BEARER_ONLY");
		System.setProperty("application.security.console.authentication.issuerUri", "http://localhost:%s/realms/test".formatted(httpPort));
		System.setProperty("application.security.console.authentication.userNameAttributeName", "preferred_username");
		System.setProperty("application.security.console.authentication.authoritiesClaimName", "realm_access.roles");
		System.setProperty("application.security.console.authentication.userInfoUri", "http://localhost:%s/realms/test/protocol/openid-connect/userinfo?scope=openid".formatted(httpPort));

		SpringApplication springApplication = IafTestInitializer.configureApplication();

		applicationContext = springApplication.run();
	}

	@AfterAll
	static void tearDown() {
		if (applicationContext != null) {
			applicationContext.close();
		}

		System.clearProperty("application.security.console.authentication.type");
		System.clearProperty("application.security.console.authentication.issuerUri");
		System.clearProperty("application.security.console.authentication.userNameAttributeName");
		System.clearProperty("application.security.console.authentication.authoritiesClaimName");
		System.clearProperty("application.security.console.authentication.userInfoUri");

		// Make sure to clear the app constants as well
		AppConstants.removeInstance();
	}

	@Test
	void testAuthentication() throws URISyntaxException {
		// first, get a token from keycloak
		RestTemplate restTemplate = new RestTemplate();

		TokenResponse tokenResponse = restTemplate.postForObject(
				getTokenEndpoint(),
				getRequestEntity(),
				TokenResponse.class);

		assertNotNull(tokenResponse, "Token response should not be null");
		assertNotNull(tokenResponse.getAccessToken(), "Access token should not be null");

		// then, use that token to access a protected resource in the application
		RestTemplate restTemplateFramework = new RestTemplate();

		String url = getFrameworkUrl();

		// Expect an error since the Authorization header is missing
		assertThrows(HttpClientErrorException.class, () -> restTemplateFramework.getForObject(url, String.class));

		// Now, access the protected resource with the token
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(tokenResponse.getAccessToken());

		ResponseEntity<String> response = restTemplate.exchange(RequestEntity
						.get(new URI(url))
						.headers(headers).build(), String.class);

		assertNotNull(response, "Response should not be null");

		// Try to access the server info endpoint, which uses the same authentication but relies on user roles being present
		String serverInfoUrl = url + "/server/info";
		ResponseEntity<GetServerInfoResponse> serverInfoResponse = restTemplate.exchange(RequestEntity
				.get(new URI(serverInfoUrl))
				.headers(headers).build(), GetServerInfoResponse.class);

		assertNotNull(serverInfoResponse, "Server info response should not be null");
		assertEquals("testuser", serverInfoResponse.getBody().getUserName(), "Username should match the authenticated user");
	}
}
