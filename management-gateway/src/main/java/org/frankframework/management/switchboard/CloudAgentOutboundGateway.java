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
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.security.AbstractJwtKeyGenerator;
import org.frankframework.management.security.JwtGeneratorFactoryBean;

@Log4j2
public class CloudAgentOutboundGateway implements InitializingBean, OutboundGateway {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final MtlsHelper mtlsHelper;
	private final Set<UUID> clientsThatReceivedPublicKey = new HashSet<>();
	private static final String PUBLIC_KEY_FIELD = "publicKey";
	private AbstractJwtKeyGenerator jwtKeyGenerator;

	@Autowired
	public CloudAgentOutboundGateway(JwtGeneratorFactoryBean keyGeneratorFactory) {
		this.jwtKeyGenerator = keyGeneratorFactory.getObject();
		this.mtlsHelper = new MtlsHelper();
		this.httpClient = mtlsHelper.getHttpClient();
	}

	protected CloudAgentOutboundGateway(JwtGeneratorFactoryBean keyFactory, MtlsHelper mtlsHelper, HttpClient httpClient) {
		this.jwtKeyGenerator = keyFactory.getObject();
		this.mtlsHelper = mtlsHelper;
		this.httpClient = httpClient;
	}

	@Override
	public void afterPropertiesSet() {
		// Do nothing
	}

	@Override
	@Nonnull
	public List<ClusterMember> getMembers() {
		log.info("Retrieving members registered to switchboard");
		try {
			URI fullUri = URI.create(mtlsHelper.getSslProperties().getApiUri().toString() + "/connections/org");
			HttpRequest request = HttpRequest.newBuilder()
					.uri(fullUri)
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				List<ClusterMember> members = mapMembers(response.body());
				sendPublicKeyToInboundGateways(members);
				return members;
			} else if (response.statusCode() == 401) {
				log.warn("No connected clients found");
			} else {
				log.warn("Failed to retrieve members: HTTP {}", response.statusCode());
			}
		} catch (IOException | GeneralSecurityException e) {
			log.error("Error fetching cluster members", e);
		} catch (InterruptedException e) {
			log.error("Error fetching cluster members", e);
			Thread.currentThread().interrupt();
		}
		return List.of();
	}

	private void sendPublicKeyToInboundGateways(List<ClusterMember> members) throws GeneralSecurityException, IOException, InterruptedException {
		String encodedPublicKey = Base64.getEncoder().encodeToString(mtlsHelper.getPublicKey().getEncoded());
		Message<PublicKeyMessage> message = MessageBuilder
				.withPayload(new PublicKeyMessage(encodedPublicKey))
				.build();

		for (ClusterMember member : members) {
			if (clientsThatReceivedPublicKey.contains(member.getId())) {
				continue;
			}

			String base64Key = member.getAttributes().get(PUBLIC_KEY_FIELD);
			byte[] decodedKey = Base64.getDecoder().decode(base64Key);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
			PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

			String json = objectMapper.writeValueAsString(message);
			byte[] encryptedPayload = mtlsHelper.encryptHybrid(
					json.getBytes(StandardCharsets.UTF_8), pubKey);

			RelayEnvelope envelope = new RelayEnvelope(
					message.getHeaders().getId(), encryptedPayload);
			byte[] envelopeBytes = objectMapper.writeValueAsBytes(envelope);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(createMemberURI(member.getId().toString(), "sync"))
					.header("Content-Type", "application/octet-stream")
					.POST(HttpRequest.BodyPublishers.ofByteArray(envelopeBytes))
					.build();

			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				log.warn(
						"Failed to send public key to {}: HTTP {} | {}",
						member.getId(), response.statusCode(), new String(response.body(), StandardCharsets.UTF_8)
				);
			} else {
				clientsThatReceivedPublicKey.add(member.getId());
				log.info("Public key sent to {}", member.getId());
			}
		}
	}

	private List<ClusterMember> mapMembers(String responseBody) {
		List<ClusterMember> members = new ArrayList<>();
		try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody))) {
			JsonArray jsonArray = jsonReader.readArray();
			for (JsonValue jsonValue : jsonArray) {
				JsonObject jsonObject = jsonValue.asJsonObject();

				String id = jsonObject.getString("clientId");
				ClusterMember member = new ClusterMember();
				member.setId(UUID.fromString(id));
				member.setAddress(createMemberAddress(id, "sync"));
				member.setLocalMember(false);
				member.setType(jsonObject.getString("instanceType"));
				member.setAttributes(Map.of(
						"name", jsonObject.getString("instanceName"),
						"version", jsonObject.getString("instanceVersion"),
						PUBLIC_KEY_FIELD, jsonObject.getString(PUBLIC_KEY_FIELD)
				));
				members.add(member);
			}
		}
		return members;
	}

	private String createMemberAddress(String clientId, String type) {
		return mtlsHelper.getSslProperties()
				.getApiUri()
				.toString() + "/send/" + type + "/" + clientId;
	}

	private URI createMemberURI(String clientId, String type) {
		return URI.create(this.createMemberAddress(clientId, type));
	}

	@Nonnull
	@Override
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		UUID messageId = null;
		try {
			byte[] encryptedData = createByteEnvelope(in);
			String targetId = extractTargetId(in);
			URI uri = createMemberURI(targetId, "sync");
			HttpRequest request = buildHttpRequest(uri, encryptedData);

			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			RelayEnvelope envelope = parseRelayEnvelope(response.body());
			messageId = envelope.getMessageId();

			return decryptAndConvert(envelope);
		} catch (GeneralSecurityException | InterruptedException | IOException e) {
			throw new BusException("Failed to send/receive message | Message id: " + messageId, e);
		}
	}

	private <I> String extractTargetId(Message<I> in) {
		Object targetHeader = in.getHeaders().get("target");
		return Objects.requireNonNull(targetHeader, "'target' header is missing").toString();
	}

	private HttpRequest buildHttpRequest(URI uri, byte[] payload) {
		return HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/octet-stream")
				.POST(HttpRequest.BodyPublishers.ofByteArray(payload))
				.build();
	}

	private RelayEnvelope parseRelayEnvelope(byte[] responseBody) throws IOException {
		return objectMapper.readValue(responseBody, RelayEnvelope.class);
	}

	private <O> Message<O> decryptAndConvert(RelayEnvelope envelope)
			throws GeneralSecurityException, JsonProcessingException {
		byte[] decryptedBytes = mtlsHelper.decryptHybrid(envelope.getPayload());
		String json = new String(decryptedBytes, StandardCharsets.UTF_8);

		JavaType msgType = objectMapper.getTypeFactory()
				.constructParametricType(SwitchBoardMessage.class, Object.class);
		return objectMapper.readValue(json, msgType);
	}

	private <I> byte[] createByteEnvelope(Message<I> message) throws GeneralSecurityException, JsonProcessingException {
		String json = objectMapper.writeValueAsString(message);
		byte[] encrypted = mtlsHelper.encryptHybrid(json.getBytes(StandardCharsets.UTF_8));

		UUID messageId = message.getHeaders().getId();
		RelayEnvelope envelope = new RelayEnvelope(
				messageId,
				encrypted,
				jwtKeyGenerator.create()
		);
		return objectMapper.writeValueAsBytes(envelope);
	}

	@Override
	public <I> void sendAsyncMessage(Message<I> in) {
		UUID messageId = null;
		try {
			byte[] encryptedData = createByteEnvelope(in);
			messageId = in.getHeaders().getId();

			String targetId = extractTargetId(in);

			URI uri = createMemberURI(targetId, "async");
			HttpRequest request = buildHttpRequest(uri, encryptedData);

			var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			if (response.statusCode() != 200) {
				log.warn("Failed to send async message to {}: HTTP {} | {}", targetId, response.statusCode(), response.body());
			} else {
				log.info("Async message sent to {} (messageId={})", targetId, messageId);
			}

		} catch (IOException | InterruptedException | GeneralSecurityException e) {
			throw new BusException("Failed to send message with id: " + messageId, e);
		}
	}
}
