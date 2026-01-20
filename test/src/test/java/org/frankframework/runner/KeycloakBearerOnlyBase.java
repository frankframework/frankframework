package org.frankframework.runner;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Create an abstract base class for Keycloak integration tests, that sets up a Testcontainer.
 * <br>
 * This setup makes sure that the container is only started once per test run, for multiple extending classes.
 * See: <a href="https://testcontainers.com/guides/testcontainers-container-lifecycle/">testcontainers lifecycle</a>>
 */
public abstract class KeycloakBearerOnlyBase {
	public static final KeycloakContainer keycloak = new KeycloakContainer()
			.withRealmImportFile("/test-realm.json");

	static {
		keycloak.start();
	}

	static @Nullable ConfigurableApplicationContext applicationContext = null;

	static final String TOKEN_ENDPOINT_FORMAT = "http://localhost:%s/realms/test/protocol/openid-connect/token";

	String getFrameworkUrl() {
		TomcatServletWebServerFactory tomcat = applicationContext.getBean("tomcat", TomcatServletWebServerFactory.class);
		return String.format("http://localhost:%d%s/iaf/api", tomcat.getPort(), tomcat.getContextPath());
	}

	/**
	 * @return the request entity for the token request to Keycloak with the necessary headers and form parameters
	 */
	HttpEntity<MultiValueMap<String, String>> getRequestEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// JacksonMapper explicitly expects a MultiValueMap, HashMap won't work
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		parameters.add("grant_type", "password");
		parameters.add("client_id", "admin-cli");
		parameters.add("username", "testuser");
		parameters.add("password", "testuser");
		parameters.add("scope", "openid");

		return new HttpEntity<>(parameters, headers);
	}

	/**
	 * DTO for the token response from Keycloak.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class TokenResponse {
		@JsonProperty("access_token")
		private @Nullable String accessToken;

		@Nullable
		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(@Nullable String accessToken) {
			this.accessToken = accessToken;
		}
	}

	/**
	 * DTO for the server info response from the application.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class GetServerInfoResponse {
		private @Nullable String userName;

		@Nullable
		public String getUserName() {
			return userName;
		}

		public void setUserName(@Nullable String userName) {
			this.userName = userName;
		}
	}

	String getTokenEndpoint(int httpPort) {
		return String.format(TOKEN_ENDPOINT_FORMAT, httpPort);
	}
}
