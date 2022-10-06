package nl.nn.adapterframework.http.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import lombok.Getter;

public class MockTokenServer extends WireMockRule {

	private boolean mockServer = true;

	String KEYCLOAK_SERVER = "http://localhost:8888";
	String KEYCLOAK_PATH = "/auth/realms/iaf-test/protocol/openid-connect/token";

	public String SCENARIO_CONNECTION_RESET = "Connection Reset";
	public String SCENARIO_STATE_RESET_CONNECTION = "Reset Connection";

	String LOCAL_PATH = "/token";

	public static final String VALID_TOKEN = "fakeValidAccessToken";
	public static final String EXPIRED_TOKEN = "fakeExpiredAccessToken";

	private @Getter String path = mockServer ? LOCAL_PATH : KEYCLOAK_PATH;

	private @Getter String clientId = "testiaf-client";
	private @Getter String clientSecret = "testiaf-client-pwd";

	private String accessTokenResponseValid	 = "{\"access_token\":\""+VALID_TOKEN+"\",	\"refresh_expires_in\":0,\"scope\":\"profile email\",\"not-before-policy\":0,\"token_type\":\"Bearer\",\"expires_in\":300}";
	private String accessTokenResponseExpired = "{\"access_token\":\""+EXPIRED_TOKEN+"\",\"refresh_expires_in\":0,\"scope\":\"profile email\",\"not-before-policy\":0,\"token_type\":\"Bearer\",\"expires_in\":0}";

	public MockTokenServer() {
		super(wireMockConfig().dynamicPort());
	}

	public MockTokenServer(int port) {
		super(port);
	}

	@Override
	public void start() {
		stubFor(any(urlEqualTo(path))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseValid)));
		stubFor(any(urlEqualTo("/firstExpired"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseValid)));
		stubFor(any(urlEqualTo("/firstExpired")).inScenario("expiration")
					.whenScenarioStateIs(Scenario.STARTED)
					.willSetStateTo("valid")
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseExpired)));
		stubFor(any(urlPathMatching(path)).inScenario(SCENARIO_CONNECTION_RESET)
					.whenScenarioStateIs(SCENARIO_STATE_RESET_CONNECTION)
					.willSetStateTo(Scenario.STARTED)
					.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
		stubFor(any(urlEqualTo("/token/xxxxx"))
					.willReturn(aResponse()
						.withStatus(404)));
		super.start();
	}

	public String getServer() {
		return mockServer ? "http://localhost:"+port() : KEYCLOAK_SERVER;
	}
	public String getEndpoint() {
		return getServer()+getPath();
	}
	public String getEndpointFirstExpired() {
		return getServer()+"/firstExpired";
	}
}
