package org.frankframework.http.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

public class MockAuthenticatedService {

	public static final String AUTHORIZATION_HEADER="Authorization";

	public static final String SCENARIO_CONNECTION_RESET="Connection Reset";
	public static final String SCENARIO_STATE_RESET_CONNECTION="Reset Connection";

	public static final String BASIC_PATH = "/basic";
	public static final String OAUTH_PATH = "/oauth";
	public static final String OAUTH_RESOURCE_NOT_FOUND_PATH = "/oauth/resourceNotFound";
	public static final String BASIC_UNCHALLENGED_PATH = "/basicUnchallenged";
	public static final String OAUTH_UNCHALLENGED_PATH = "/oauthUnchallenged";
	public static final String FAILING_PATH = "/FAILING_PATH";
	public static final String ANY_PATH = "/any";

	public static void createStubs(WireMockExtension extension) {
		extension.stubFor(any(urlPathMatching(ANY_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Basic realm=test")
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"no authorization header\"}")));
		extension.stubFor(any(urlPathMatching(ANY_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Basic ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		extension.stubFor(any(urlPathMatching(ANY_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		extension.stubFor(any(urlPathMatching("/*"))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Basic realm=test")
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"token expired\"}")));

		extension.stubFor(any(urlPathMatching(BASIC_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Basic realm=test")
						.withBody("{\"message\":\"no basic authorization header\"}")));
		extension.stubFor(any(urlPathMatching(BASIC_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Basic ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));

		extension.stubFor(any(urlPathMatching(OAUTH_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"no bearer authorization header\"}")));
		extension.stubFor(any(urlPathMatching(OAUTH_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer " + MockTokenServer.VALID_TOKEN))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		extension.stubFor(any(urlPathMatching(OAUTH_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer " + MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"token expired\"}")));

		extension.stubFor(any(urlPathMatching(OAUTH_RESOURCE_NOT_FOUND_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"no bearer authorization header\"}")));
		extension.stubFor(any(urlPathMatching(OAUTH_RESOURCE_NOT_FOUND_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(404)
						.withBody("")));
		extension.stubFor(any(urlPathMatching(OAUTH_RESOURCE_NOT_FOUND_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"token expired\"}")));

		extension.stubFor(any(urlPathMatching(BASIC_UNCHALLENGED_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"no basic authorization header\"}")));
		extension.stubFor(any(urlPathMatching(BASIC_UNCHALLENGED_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Basic ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));

		extension.stubFor(any(urlPathMatching(OAUTH_UNCHALLENGED_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"no bearer authorization header\"}")));
		extension.stubFor(any(urlPathMatching(OAUTH_UNCHALLENGED_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		extension.stubFor(any(urlPathMatching(OAUTH_UNCHALLENGED_PATH))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"token expired\"}")));

		extension.stubFor(any(urlPathMatching(FAILING_PATH))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"on this endpoint the authentication will always fail\"}")));

		extension.stubFor(any(urlPathMatching(BASIC_PATH)).inScenario(SCENARIO_CONNECTION_RESET)
					.whenScenarioStateIs(SCENARIO_STATE_RESET_CONNECTION)
					.willSetStateTo(Scenario.STARTED)
					.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		extension.stubFor(any(urlPathMatching(OAUTH_PATH)).inScenario(SCENARIO_CONNECTION_RESET)
					.whenScenarioStateIs(SCENARIO_STATE_RESET_CONNECTION)
					.willSetStateTo(Scenario.STARTED)
					.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
	}
}
