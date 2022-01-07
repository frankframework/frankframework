package nl.nn.adapterframework.http.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import lombok.Getter;

public class MockAuthenticatedService extends WireMockRule {

	private String AUTHORIZATION_HEADER="Authorization";
	private @Getter int port;
	private boolean mockServer = true;
	
	String REAL_SERVER="http://localhost:8888";

	private @Getter String basicPath = "/basic";
	private @Getter String oauthPath = "/oauth";
	private @Getter String anyPath   = "/any";
	
	public MockAuthenticatedService() {
		this(8886);
	}
	public MockAuthenticatedService(int port) {
		super(port);
		this.port=port;
	}

	@Override
	public void start() {
		stubFor(any(urlPathMatching("/*"))
				  .willReturn(aResponse()
				  .withStatus(401)
				  .withHeader("WWW-Authenticate", "Basic realm=test")
				  .withHeader("WWW-Authenticate", "Bearer realm=test")
				  .withBody("{\"message\":\"no authorization header\"}")));
		stubFor(any(urlPathMatching(basicPath))
				  .willReturn(aResponse()
				  .withStatus(401)
				  .withHeader("WWW-Authenticate", "Basic realm=test")
				  .withBody("{\"message\":\"no basic authorization header\"}")));
		stubFor(any(urlPathMatching(oauthPath))
				  .willReturn(aResponse()
				  .withStatus(401)
				  .withHeader("WWW-Authenticate", "Bearer realm=test")
				  .withBody("{\"message\":\"no bearer authorization header\"}")));
		stubFor(any(urlPathMatching("/*"))
				  .withHeader(AUTHORIZATION_HEADER, matching("(Basic)|(Bearer) .*"))
				  .willReturn(aResponse()
				  .withStatus(200)
				  .withHeader("Content-Type", "application/json")
				  .withBody("{}")));
		stubFor(any(urlPathMatching(basicPath))
				  .withHeader(AUTHORIZATION_HEADER, matching("Basic .*"))
				  .willReturn(aResponse()
				  .withStatus(200)
				  .withHeader("Content-Type", "application/json")
				  .withBody("{}")));
		stubFor(any(urlPathMatching(oauthPath))
				  .withHeader(AUTHORIZATION_HEADER, matching("Bearer .*"))
				  .willReturn(aResponse()
				  .withStatus(200)
				  .withHeader("Content-Type", "application/json")
				  .withBody("{}")));
		super.start();
	}

	
	public String getServer() {
		return mockServer ? "http://localhost:"+port : REAL_SERVER;
	}
	public String getBasicEndpoint() {
		return getServer() + basicPath;
	}
	public String getOAuthEndpoint() {
		return getServer() + oauthPath;
	}
	

}
