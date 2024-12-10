package org.frankframework.http.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

public class MockTokenServer {
	public static final String VALID_TOKEN = "fakeValidAccessToken";
	public static final String EXPIRED_TOKEN = "fakeExpiredAccessToken";

	public static final String SCENARIO_CONNECTION_RESET = "Connection Reset";
	public static final String SCENARIO_STATE_RESET_CONNECTION = "Reset Connection";

	public static final String accessTokenResponseValid = "{\"access_token\":\""+VALID_TOKEN+"\",	\"refresh_expires_in\":0,\"scope\":\"profile email\",\"not-before-policy\":0,\"token_type\":\"Bearer\",\"expires_in\":300}";
	public static final String accessTokenResponseExpired = "{\"access_token\":\""+EXPIRED_TOKEN+"\",\"refresh_expires_in\":0,\"scope\":\"profile email\",\"not-before-policy\":0,\"token_type\":\"Bearer\",\"expires_in\":0}";

	public static final String PATH = "/token";
	public static final String EXPIRED_PATH = "/firstExpired";
	public static final String DELAYED_PATH = "/delayed";

	public static final String CLIENT_ID = "testiaf-client";
	public static final String CLIENT_SECRET = "testiaf-client-pwd";

	public static void createStubs(WireMockExtension extension) {
		extension.stubFor(any(urlPathMatching(PATH))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseValid)));
		extension.stubFor(any(urlPathMatching(EXPIRED_PATH))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseValid)));
		extension.stubFor(any(urlPathMatching(EXPIRED_PATH)).inScenario("expiration")
					.whenScenarioStateIs(Scenario.STARTED)
					.willSetStateTo("valid")
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseExpired)));
		extension.stubFor(any(urlPathMatching(DELAYED_PATH))
				.willReturn(aResponse()
						.withFixedDelay(5000)
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(accessTokenResponseValid)));
		extension.stubFor(any(urlPathMatching(PATH)).inScenario(SCENARIO_CONNECTION_RESET)
					.whenScenarioStateIs(SCENARIO_STATE_RESET_CONNECTION)
					.willSetStateTo(Scenario.STARTED)
					.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
		extension.stubFor(any(urlEqualTo(PATH+"/xxxxx"))
					.willReturn(aResponse()
						.withStatus(404)));
	}
}
