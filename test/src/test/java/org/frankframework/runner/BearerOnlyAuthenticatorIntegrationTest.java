package org.frankframework.runner;

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
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.log4j.Log4j2;

/**
 * Tests whether bearer only request authentication works with Keycloak and the application.
 *
 * @author erik.van.dongen
 * @see "https://github.com/dasniko/testcontainers-keycloak"
 */
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
@Log4j2
public class BearerOnlyAuthenticatorIntegrationTest {

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer()
				.withRealmImportFile("/keycloak-test-realm.json");

	private static ConfigurableApplicationContext applicationContext = null;

	private static final String TOKEN_ENDPOINT_FORMAT = "http://localhost:%s/realms/test/protocol/openid-connect/token";

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
 	@BeforeAll
	static void setup() throws IOException {
		// Set system properties for the application to use the Keycloak container and start the framework initializer
		System.setProperty("application.security.console.authentication.type", "BEARER_ONLY");
		System.setProperty("application.security.console.authentication.issuerUri", "http://localhost:%s/realms/test".formatted(keycloak.getHttpPort())); // works

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
	}

	private String getFrameworkUrl() {
		TomcatServletWebServerFactory tomcat = applicationContext.getBean("tomcat", TomcatServletWebServerFactory.class);
		return String.format("http://localhost:%d%s/iaf/api", tomcat.getPort(), tomcat.getContextPath());
	}

	/**
	 * @return the request entity for the token request to Keycloak with the necessary headers and form parameters
	 */
	private HttpEntity<MultiValueMap<String, String>> getRequestEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// JacksonMapper explicitly expects a MultiValueMap, HashMap won't work
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		parameters.add("grant_type", "password");
		parameters.add("client_id", "admin-cli");
		parameters.add("username", "testuser");
		parameters.add("password", "testuser");

		return new HttpEntity<>(parameters, headers);
	}


	/**
	 * DTO for the token response from Keycloak.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class TokenResponse {
		@JsonProperty("access_token")
		private String accessToken;

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}
	}

	private String getTokenEndpoint() {
		return String.format(TOKEN_ENDPOINT_FORMAT, keycloak.getHttpPort());
	}
}
