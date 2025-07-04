package org.frankframework.management.switchboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.security.AbstractJwtKeyGenerator;
import org.frankframework.management.security.JwtGeneratorFactoryBean;

class CloudAgentOutboundGatewayTest {

	private CloudAgentOutboundGateway gateway;
	private MtlsHelper mtlsHelper;
	private HttpClient httpClient;
	private HttpResponse<String> httpResponse;

	@BeforeEach
	void setup() throws Exception {
		JwtGeneratorFactoryBean keyFactory = mock(JwtGeneratorFactoryBean.class);
		AbstractJwtKeyGenerator keyGenerator = mock(AbstractJwtKeyGenerator.class);
		when(keyFactory.getObject()).thenReturn(keyGenerator);

		mtlsHelper = mock(MtlsHelper.class);
		httpClient = mock(HttpClient.class);
		httpResponse = mock(HttpResponse.class);

		gateway = new CloudAgentOutboundGateway(keyFactory, mtlsHelper, httpClient);

		var props = mock(SSLProperties.class);
		when(props.getApiUri()).thenReturn(new URI("https://fake-api"));
		when(mtlsHelper.getSslProperties()).thenReturn(props);

		PublicKey dummyKey = mock(PublicKey.class);
		when(dummyKey.getEncoded()).thenReturn(new byte[256]);
		when(mtlsHelper.getPublicKey()).thenReturn(dummyKey);
	}


	@Test
	@DisplayName("When a valid client response is received, Then a ClusterMember is parsed and the public key is sent")
	void getMembersTest() throws Exception {
		// Generate a real RSA key pair
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		String encodedPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

		// Create JSON response with the actual public key
		JsonObjectBuilder obj = Json.createObjectBuilder()
				.add("clientId", UUID.randomUUID().toString())
				.add("instanceType", "some-type")
				.add("instanceName", "client-name")
				.add("instanceVersion", "1.0")
				.add("publicKey", encodedPublicKey);

		JsonArrayBuilder arr = Json.createArrayBuilder().add(obj);
		String body = arr.build().toString();

		// Mock HTTP response
		when(httpResponse.statusCode()).thenReturn(200);
		when(httpResponse.body()).thenReturn(body);
		when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

		// Mock getPublicKey (for outbound encryption)
		PublicKey myPubKey = keyPair.getPublic();
		when(mtlsHelper.getPublicKey()).thenReturn(myPubKey);

		var members = gateway.getMembers();

		assertEquals(1, members.size());
		OutboundGateway.ClusterMember member = members.get(0);
		assertNotNull(member.getId());
		assertEquals("some-type", member.getType());
		assertEquals("client-name", member.getAttributes().get("name"));
		assertTrue(member.getAddress().contains("/send/sync/"));
	}

}
