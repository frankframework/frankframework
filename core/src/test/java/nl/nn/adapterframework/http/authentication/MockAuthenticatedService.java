package nl.nn.adapterframework.http.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import lombok.Getter;

public class MockAuthenticatedService extends WireMockRule {

	private String AUTHORIZATION_HEADER="Authorization";
	private boolean mockServer = true;

	public String SCENARIO_CONNECTION_RESET="Connection Reset";
	public String SCENARIO_STATE_RESET_CONNECTION="Reset Connection";


	String REAL_SERVER="http://localhost:8888";

	private @Getter String basicPath = "/basic";
	private @Getter String oauthPath = "/oauth";
	private @Getter String oauthPathResourceNotFound = "/oauth/resourceNotFound";
	private @Getter String basicPathUnchallenged = "/basicUnchallenged";
	private @Getter String oauthPathUnchallenged = "/oauthUnchallenged";
	private @Getter String failing = "/failing";
	private @Getter String anyPath	 = "/any";

	public MockAuthenticatedService() {
		super(wireMockConfig()
				.dynamicPort());
	}
	public MockAuthenticatedService(int port) {
		super(port);
	}

	@Override
	public void start() {
		stubFor(any(urlPathMatching(anyPath))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Basic realm=test")
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"no authorization header\"}")));
		stubFor(any(urlPathMatching(anyPath))
					.withHeader(AUTHORIZATION_HEADER, matching("Basic ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		stubFor(any(urlPathMatching(anyPath))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		stubFor(any(urlPathMatching("/*"))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Basic realm=test")
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"token expired\"}")));

		stubFor(any(urlPathMatching(basicPath))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Basic realm=test")
						.withBody("{\"message\":\"no basic authorization header\"}")));
		stubFor(any(urlPathMatching(basicPath))
					.withHeader(AUTHORIZATION_HEADER, matching("Basic ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));

		stubFor(any(urlPathMatching(oauthPath))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"no bearer authorization header\"}")));
		stubFor(any(urlPathMatching(oauthPath))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		stubFor(any(urlPathMatching(oauthPath))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"token expired\"}")));

		stubFor(any(urlPathMatching(oauthPathResourceNotFound))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"no bearer authorization header\"}")));
		stubFor(any(urlPathMatching(oauthPathResourceNotFound))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(404)
						.withBody("")));
		stubFor(any(urlPathMatching(oauthPathResourceNotFound))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withHeader("WWW-Authenticate", "Bearer realm=test")
						.withBody("{\"message\":\"token expired\"}")));

		stubFor(any(urlPathMatching(basicPathUnchallenged))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"no basic authorization header\"}")));
		stubFor(any(urlPathMatching(basicPathUnchallenged))
					.withHeader(AUTHORIZATION_HEADER, matching("Basic ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));

		stubFor(any(urlPathMatching(oauthPathUnchallenged))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"no bearer authorization header\"}")));
		stubFor(any(urlPathMatching(oauthPathUnchallenged))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer ([A-Za-z0-9]+)"))
					.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{}")));
		stubFor(any(urlPathMatching(oauthPathUnchallenged))
					.withHeader(AUTHORIZATION_HEADER, matching("Bearer "+MockTokenServer.EXPIRED_TOKEN))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"token expired\"}")));

		stubFor(any(urlPathMatching(failing))
					.willReturn(aResponse()
						.withStatus(401)
						.withBody("{\"message\":\"on this endpoint the authentication will always fail\"}")));

		stubFor(any(urlPathMatching(basicPath)).inScenario(SCENARIO_CONNECTION_RESET)
					.whenScenarioStateIs(SCENARIO_STATE_RESET_CONNECTION)
					.willSetStateTo(Scenario.STARTED)
					.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		stubFor(any(urlPathMatching(oauthPath)).inScenario(SCENARIO_CONNECTION_RESET)
					.whenScenarioStateIs(SCENARIO_STATE_RESET_CONNECTION)
					.willSetStateTo(Scenario.STARTED)
					.willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
		super.start();
	}


	public String getServer() {
		return mockServer ? "http://localhost:"+port() : REAL_SERVER;
	}
	public String getBasicEndpoint() {
		return getServer() + basicPath;
	}
	public String getOAuthEndpoint() {
		return getServer() + oauthPath;
	}
	public String getOAuthResourceNotFoundEndpoint() {
		return getServer() + oauthPathResourceNotFound;
	}
	public String getBasicEndpointUnchallenged() {
		return getServer() + basicPathUnchallenged;
	}
	public String getOAuthEndpointUnchallenged() {
		return getServer() + oauthPathUnchallenged;
	}
	public String gethEndpointFailing() {
		return getServer() + failing;
	}
	public String getMultiAuthEndpoint() {
		return getServer() + anyPath;
	}


}
