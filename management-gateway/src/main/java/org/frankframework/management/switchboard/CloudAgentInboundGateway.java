/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.management.switchboard;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.BusException;

@Log4j2
public class CloudAgentInboundGateway extends MessagingGatewaySupport {

	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
	private final SSLProperties sslProperties = new SSLProperties();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final MtlsHelper mtlsHelper;
	private final HttpClient httpClient;
	private PublicKey consolePubKey;
	private CloudAgentJwtVerifier jwtVerifier;

	@Value("${instance.name:}")
	private String instanceName;

	@Value("${instance.version:}")
	private String instanceVersion;

	@Value("${client.ssl.client-id:#{T(java.util.UUID).randomUUID()}}")
	private UUID clientId;

	public CloudAgentInboundGateway(MtlsHelper mtlsHelper) {
		this.mtlsHelper = mtlsHelper;
		this.httpClient = mtlsHelper.getHttpClient();
	}

	public CloudAgentInboundGateway() {
		this.mtlsHelper = new MtlsHelper();
		this.httpClient = mtlsHelper.getHttpClient();
	}

	@Override
	protected void onInit() {
		log.info("Init PhoneHomeInboundGateway, clientId={}", clientId);
		setRequestChannel(resolveRequestChannel(getApplicationContext()));
		setErrorChannel(null);

		jwtVerifier = new CloudAgentJwtVerifier(mtlsHelper);

		try {
			connectWebSocket();
		} catch (CompletionException ce) {
			handleInitException(ce);
		}

		log.info("InboundGateway Init Complete");
		super.onInit();
	}

	private MessageChannel resolveRequestChannel(ApplicationContext ctx) {
		return ctx.getBean("frank-management-bus", MessageChannel.class);
	}

	private void connectWebSocket() {
		httpClient.newWebSocketBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.buildAsync(sslProperties.getWebsocketUri(), new SimpleListener());
	}

	private CompletionStage<?> handleMessage(WebSocket ws, byte[] rawMessage) { // NOSONAR
		long startTime = System.nanoTime();
		try {
			RelayEnvelope envelope = deserializeEnvelope(rawMessage);
			byte[] encryptedPayload = envelope.getPayload();
			log.info("Unwrapped envelope: messageId={} payloadLen={}", envelope.getMessageId(), encryptedPayload.length);

			byte[] decrypted = mtlsHelper.decryptHybrid(encryptedPayload);
			String inboundJson = new String(decrypted, StandardCharsets.UTF_8);

			SwitchBoardMessage<?> inboundMsg = parseGenericMessage(inboundJson);

			if (inboundMsg.getPayload() instanceof Map<?, ?> payload && payload.containsKey("publicKey")) {
				@SuppressWarnings("unchecked")
				var publicKeyPayload = (Map<String, String>) payload;
				return handleSwitchboardMessage(ws, envelope, publicKeyPayload);
			}

			if (consolePubKey == null) {
				log.error("Dropping message because public key is not set: messageId={}", envelope.getMessageId());
				return CompletableFuture.completedFuture(null);
			}

			return handleRoutedMessage(ws, envelope, inboundMsg, startTime);
		} catch (Exception e) {
			log.error("Error in handleMessage", e);
			return CompletableFuture.completedFuture(null);
		}
	}

	private RelayEnvelope deserializeEnvelope(byte[] raw) throws IOException {
		return objectMapper.readValue(raw, RelayEnvelope.class);
	}

	private SwitchBoardMessage<?> parseGenericMessage(String json) throws JsonProcessingException {
		return objectMapper.readValue(json, SwitchBoardMessage.class);
	}

	CompletionStage<?> handleSwitchboardMessage(WebSocket ws, RelayEnvelope envelope, Map<String, String> payload) {
		try {
			consolePubKey = decodePublicKey(payload.get("publicKey"));
			byte[] reply = buildEncryptedStatusOK(envelope.getMessageId(), consolePubKey);
			return sendBinary(ws, reply)
					.thenRun(() -> log.info("Sent OK response for publicKey (msgId={})", envelope.getMessageId()))
					.exceptionally(ex -> {
						log.error("Failed to send OK response for publicKey", ex);
						return null;
					});
		} catch (JsonProcessingException | GeneralSecurityException e) {
			throw new BusException("Failed to handle switchboard message", e);
		}
	}

	private PublicKey decodePublicKey(String base64Key)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] decoded = Base64.getDecoder().decode(base64Key);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
		return KeyFactory.getInstance("RSA").generatePublic(keySpec);
	}

	private byte[] buildEncryptedStatusOK(UUID messageId, PublicKey pubKey)
			throws GeneralSecurityException, JsonProcessingException {
		String okJson = objectMapper.writeValueAsString(Map.of("status", "OK"));
		byte[] encrypted = mtlsHelper.encryptHybrid(okJson.getBytes(StandardCharsets.UTF_8), pubKey);
		RelayEnvelope responseEnvelope = new RelayEnvelope(messageId, encrypted);
		return objectMapper.writeValueAsBytes(responseEnvelope);
	}

	private CompletionStage<?> handleRoutedMessage(
			WebSocket ws, RelayEnvelope envelope, SwitchBoardMessage<?> inboundMsg, long startTime) {
		try {
			Authentication auth = createAuthentication(envelope.getAuthentication());
			propagateAuthenticationContext(auth);

			if (Objects.equals(envelope.getType(), "async")) {
				log.info("Sending async message");
				super.send(inboundMsg);
			} else {
				log.info("Sending sync message");
				return sendSynchronousMessage(ws, envelope, inboundMsg, startTime);
			}
		} catch (IOException e) {
			log.error("Failed to handle routed message", e);
		}
		return CompletableFuture.completedFuture(null);
	}

	private CompletionStage<?> sendSynchronousMessage(WebSocket ws, RelayEnvelope envelope, SwitchBoardMessage<?> inboundMsg, long startTime) {
		try {

			Message<?> response = super.sendAndReceiveMessage(inboundMsg);
			Object payload = unwrapPayloadIfStream(response);
			Message<?> responseWithPayload = rebuildResponseIfNeeded(response, payload);

			byte[] replyBytes = buildEncryptedReply(envelope.getMessageId(), responseWithPayload);
			log.info("Sending response: {} bytes", replyBytes.length);

			return sendBinary(ws, replyBytes)
					.thenRun(() -> {
						long durationMs = (System.nanoTime() - startTime) / 1_000_000;
						log.info("WebSocket reply sent (total {} ms)", durationMs);
					})
					.exceptionally(ex -> {
						log.error("Failed to send reply envelope", ex);
						return null;
					});
		} catch (GeneralSecurityException | JsonProcessingException e) {
			log.error("Error handling routed message", e);
			return CompletableFuture.completedFuture(null);
		}
	}

	private Authentication createAuthentication(Object authObj) throws IOException {
		if (authObj instanceof Authentication auth) {
			return auth;
		}
		if (authObj instanceof String jwt) {
			return jwtVerifier.verify(jwt);
		}
		throw new IOException("No valid authentication object found");
	}

	private void propagateAuthenticationContext(Authentication authentication) {
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		securityContextHolderStrategy.setContext(context);
	}


	Object unwrapPayloadIfStream(Message<?> response) {
		Object payload = response.getPayload();
		if (payload instanceof InputStream inputStream) {
			try (inputStream) {
				return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				log.error("Error reading InputStream payload", e);
				return "Failed to read stream: " + e.getMessage();
			}
		}
		return payload;
	}

	private Message<?> rebuildResponseIfNeeded(Message<?> original, Object newPayload) {
		if (!original.getPayload().equals(newPayload)) {
			return MessageBuilder.withPayload(newPayload)
					.copyHeaders(original.getHeaders())
					.build();
		}
		return original;
	}

	private byte[] buildEncryptedReply(UUID messageId, Message<?> response)
			throws GeneralSecurityException, JsonProcessingException {
		String jsonReply = objectMapper.writeValueAsString(response);
		byte[] encrypted = mtlsHelper.encryptHybrid(
				jsonReply.getBytes(StandardCharsets.UTF_8), consolePubKey);
		RelayEnvelope envelopeOut = new RelayEnvelope(messageId, encrypted);
		return objectMapper.writeValueAsBytes(envelopeOut);
	}

	private CompletionStage<WebSocket> sendBinary(WebSocket ws, byte[] data) {
		return ws.sendBinary(ByteBuffer.wrap(data), true);
	}

	private void handleInitException(CompletionException ce) {
		Throwable cause = ce.getCause();
		if (cause instanceof java.net.UnknownHostException) {
			log.error("DNS lookup failed – host [{}] unreachable: {}", sslProperties.getWebsocketUri().getHost(), cause.getMessage());
		} else if (cause instanceof java.net.ConnectException) {
			log.error("TCP connection failed – cannot reach switchboard at {}", sslProperties.getWebsocketUri());
		} else if (cause instanceof java.net.http.WebSocketHandshakeException) {
			log.error("WebSocket handshake failed: {}", cause.getMessage());
		} else if (cause instanceof java.net.http.HttpConnectTimeoutException) {
			log.error("Connection timed out after 5 seconds");
		} else if (cause instanceof javax.net.ssl.SSLException) {
			log.error("SSL error (certificate/truststore): {}", cause.getMessage());
		} else {
			log.error("Unexpected failure establishing WebSocket", cause);
		}
		System.exit(1);
	}

	class SimpleListener implements WebSocket.Listener {
		@Override
		public void onOpen(WebSocket socket) {
			socket.request(Long.MAX_VALUE);
			sendInstanceInfo(socket);
		}

		private void sendInstanceInfo(WebSocket websocket) {
			try {
				InstanceInfoDto info = InstanceInfoDto.builder()
						.clientId(clientId)
						.instanceName(instanceName)
						.instanceVersion(instanceVersion)
						.publicKey(mtlsHelper.getPublicKey())
						.build();

				SwitchBoardCommand<InstanceInfoDto> command = new SwitchBoardCommand<>(SwitchBoardCommandType.INSTANCE_INFO, info);
				String jsonCmd = objectMapper.writeValueAsString(command);
				websocket.sendText(jsonCmd, true);
			} catch (Exception e) {
				log.error("Failed to send instance-info command", e);
			}
		}

		@Override
		public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
			log.info("Received raw text on WS: {}", data);
			socket.request(1);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer data, boolean last) {
			byte[] raw = new byte[data.remaining()];
			data.get(raw);
			socket.request(1);
			return handleMessage(socket, raw);
		}

		@Override
		public CompletionStage<?> onClose(WebSocket socket, int code, String reason) {
			log.info("WebSocket closed: {} {}", code, reason);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void onError(WebSocket socket, Throwable error) {
			log.error("WebSocket error", error);
		}
	}
}
