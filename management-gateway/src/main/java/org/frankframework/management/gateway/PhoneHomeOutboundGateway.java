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
package org.frankframework.management.gateway;

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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.security.JwtKeyGeneratorSupplier;

@Log4j2
public class PhoneHomeOutboundGateway implements InitializingBean, ApplicationContextAware, OutboundGateway {

	private ApplicationContext applicationContext;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final MtlsHelper mtlsHelper;
	private final Set<UUID> clientsThatReceivedPublicKey = new HashSet<>();

	@Autowired
	private JwtKeyGeneratorSupplier jwtKeyGeneratorSupplier;

	public PhoneHomeOutboundGateway(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.mtlsHelper = new MtlsHelper();
		this.httpClient = mtlsHelper.getHttpClient();
	}

	@Override
	public void afterPropertiesSet() {
		getMembers();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

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
			} else {
				log.warn("Failed to retrieve members: HTTP {}", response.statusCode());
			}
		} catch (IOException | InterruptedException e) {
			log.error("Error fetching cluster members", e);
		} catch (Exception e) {
			throw new RuntimeException("Unexpected error in getMembers()", e);
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

			String base64Key = member.getAttributes().get("publicKey");
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
					.uri(URI.create(member.getAddress()))
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
						"publicKey", jsonObject.getString("publicKey")
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

	@Nonnull
	@Override
	public <I, O> Message<O> sendSyncMessage(Message<I> in) {
		UUID messageId = null;
		try {
			byte[] encryptedData = createByteEnvelope(in);
			String targetId = extractTargetId(in);
			String url = createMemberAddress(targetId, "sync");
			HttpRequest request = buildHttpRequest(url, encryptedData);

			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			RelayEnvelope envelope = parseRelayEnvelope(response.body());
			messageId = envelope.getMessageId();

			return decryptAndConvert(envelope);
		} catch (JsonProcessingException | GeneralSecurityException e) {
			throw new RuntimeException("Failed to send/receive message | Message id: " + messageId, e);
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException("IO error during sendSyncMessage", e);
		}
	}

	private <I> String extractTargetId(Message<I> in) {
		Object targetHeader = in.getHeaders().get("target");
		return Objects.requireNonNull(targetHeader, "'target' header is missing").toString();
	}

	private HttpRequest buildHttpRequest(String url, byte[] payload) {
		return HttpRequest.newBuilder()
				.uri(URI.create(url))
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
				.constructParametricType(GenericMessage.class, Object.class);
		return objectMapper.readValue(json, msgType);
	}

	private <I> byte[] createByteEnvelope(Message<I> message)
			throws GeneralSecurityException, JsonProcessingException {
		String json = objectMapper.writeValueAsString(message);
		byte[] encrypted = mtlsHelper.encryptHybrid(json.getBytes(StandardCharsets.UTF_8));

		UUID messageId = message.getHeaders().getId();
		RelayEnvelope envelope = new RelayEnvelope(
				messageId,
				encrypted,
				jwtKeyGeneratorSupplier.getJwtKeyGenerator().create()
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

			String url = createMemberAddress(targetId, "async");
			HttpRequest request = buildHttpRequest(url, encryptedData);

			var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			if (response.statusCode() != 200) {
				log.warn("Failed to send async message to {}: HTTP {} | {}", targetId, response.statusCode(), response.body());
			} else {
				log.info("Async message sent to {} (messageId={})", targetId, messageId);
			}

		} catch (JsonProcessingException | GeneralSecurityException e) {
			throw new RuntimeException("Failed to encrypt async message | Message id: " + messageId, e);
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException("IO error during sendAsyncMessage | Message id: " + messageId, e);
		}
	}

}
