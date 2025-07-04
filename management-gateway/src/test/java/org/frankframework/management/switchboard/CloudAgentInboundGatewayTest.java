package org.frankframework.management.switchboard;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class CloudAgentInboundGatewayTest {

	private CloudAgentInboundGateway gateway;
	private WebSocket webSocket;
	private MtlsHelper mtlsHelper;

	@BeforeEach
	void setup() {
		mtlsHelper = mock(MtlsHelper.class);
		HttpClient httpClient = mock(HttpClient.class);
		when(mtlsHelper.getHttpClient()).thenReturn(httpClient);
		gateway = new CloudAgentInboundGateway(mtlsHelper);
		webSocket = mock(WebSocket.class);

		when(webSocket.sendBinary(any(ByteBuffer.class), eq(true)))
				.thenReturn(CompletableFuture.completedFuture(webSocket));
	}

	@Test
	@DisplayName("When receiving a public key message, Then it replies with an encrypted OK response")
	void handleSwitchboardMessage_sendsOKReply() throws Exception {
		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		String base64Key = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

		UUID messageId = UUID.randomUUID();
		RelayEnvelope envelope = new RelayEnvelope(messageId, new byte[]{});

		gateway.handleSwitchboardMessage(webSocket, envelope, Map.of("publicKey", base64Key)).toCompletableFuture().join();

		verify(webSocket).sendBinary(any(ByteBuffer.class), eq(true));
	}

	@Test
	@DisplayName("When WebSocket opens, Then it sends INSTANCE_INFO")
	void onOpen_sendsInstanceInfo() throws Exception {
		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		PublicKey testPubKey = keyPair.getPublic();
		when(mtlsHelper.getPublicKey()).thenReturn(testPubKey);

		CloudAgentInboundGateway.SimpleListener listener = gateway.new SimpleListener();

		when(webSocket.sendText(anyString(), eq(true)))
				.thenReturn(CompletableFuture.completedFuture(webSocket));

		listener.onOpen(webSocket);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(webSocket).sendText(jsonCaptor.capture(), eq(true));

		String sentJson = jsonCaptor.getValue();

		assertTrue(sentJson.contains("\"type\":\"INSTANCE_INFO\""), "Must send INSTANCE_INFO command");
		assertTrue(sentJson.contains("\"clientId\":"), "Must include clientId");
		assertTrue(sentJson.contains("\"instanceName\":"), "Must include instanceName");
		assertTrue(sentJson.contains("\"instanceVersion\":"), "Must include instanceVersion");

		String base64Key = Base64.getEncoder().encodeToString(testPubKey.getEncoded());
		assertTrue(sentJson.contains(base64Key), "Must include the Base64-encoded publicKey");
	}

	@Test
	@DisplayName("unwrapPayloadIfStream should read InputStream payload into a String")
	void unwrapPayloadIfStream_readsStreamPayload() throws Exception {
		String content = "hello stream";
		InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		Message<InputStream> msg = MessageBuilder.withPayload(is).build();

		Method unwrap = CloudAgentInboundGateway.class
				.getDeclaredMethod("unwrapPayloadIfStream", Message.class);
		unwrap.setAccessible(true);

		Object result = unwrap.invoke(gateway, msg);

		assertInstanceOf(String.class, result, "Result should be a String");
		assertEquals(content, result);
	}
}
