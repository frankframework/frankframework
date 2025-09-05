package org.frankframework.credentialprovider.delinea;

import static org.frankframework.credentialprovider.delinea.DelineaClient.SECRETS_ACCESS_REQUESTS_URI;
import static org.frankframework.credentialprovider.delinea.DelineaClient.SECRETS_URI;
import static org.frankframework.credentialprovider.delinea.DelineaClient.SECRET_ID_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DelineaClientTest {

	private DelineaClient delineaClient;

	@BeforeEach
	public void beforeEach() {
		DelineaClientSettings settings = new DelineaClientSettings(
				"",
				"/delinea",
				"",
				"",
				"/oauth",
				"user",
				"pass",
				"",
				""
		);

		// We only want to mock one method, and not the others
		DelineaClientFactory factory = spy(new DelineaClientFactory(settings));

		// mock the getAccessGrant method, we don't want to test the oauth functionality
		doReturn(new DelineaClientFactory.AccessGrant("token", "refreshToken", "type", 3600))
				.when(factory).getAccessGrant();

		delineaClient = spy(factory.getObject());
	}

	@Test
	void testGetSecret() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		DelineaSecretDto secret = objectMapper.readValue(getContents("delinea/secret_3.json"), DelineaSecretDto.class);

		doReturn(secret)
				.when(delineaClient)
				.getForObject(eq(SECRET_ID_URI), eq(DelineaSecretDto.class), anyString());

		DelineaSecretDto secretFromClient = delineaClient.getSecret("3", null);

		assertNotNull(secretFromClient);
		assertEquals(3, secretFromClient.id());
	}

	@Test
	void testGetSecretWithComment() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		DelineaSecretDto secret = objectMapper.readValue(getContents("delinea/secret_3.json"), DelineaSecretDto.class);

		// mock the comment request
		doReturn(DelineaClient.EXPECTED_VIEW_COMMENT_RESPONSE)
				.when(delineaClient)
				.postForObject(eq(SECRETS_ACCESS_REQUESTS_URI), any(), any(), eq("3"));

		// mock the secret request
		doReturn(secret)
				.when(delineaClient)
				.getForObject(eq(SECRET_ID_URI), eq(DelineaSecretDto.class), anyString());

		DelineaSecretDto secretFromClient = delineaClient.getSecret("3", "test with comment!");

		assertNotNull(secretFromClient);
		assertEquals(3, secretFromClient.id());
	}

	@Test
	void testGetSecretWithInvalidCommentResponse() {
		// mock the comment request
		doReturn("false")
				.when(delineaClient)
				.postForObject(eq(SECRETS_ACCESS_REQUESTS_URI), any(), any(), eq("3"));

		DelineaSecretDto secretFromClient = delineaClient.getSecret("3", "test with comment!");

		// Expect null because the comment request failed
		assertNull(secretFromClient);
	}

	@Test
	void testGetSecrets() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		SecretsListDto page1 = objectMapper.readValue(getContents("delinea/page1.json"), SecretsListDto.class);
		SecretsListDto page2 = objectMapper.readValue(getContents("delinea/page2.json"), SecretsListDto.class);

		// mock config to return different pages based on the skip parameter
		doReturn(page1)
				.when(delineaClient)
				.getForObject(eq(SECRETS_URI), eq(SecretsListDto.class), argThat((Map<String, ?> map) -> map.get("skip").equals("0")));

		doReturn(page2)
				.when(delineaClient)
				.getForObject(eq(SECRETS_URI), eq(SecretsListDto.class), argThat((Map<String, ?> map) -> map.get("skip").equals("1")));

		List<String> secrets = delineaClient.getSecrets();

		assertEquals(2, secrets.size());

		// These are the id's as defined in the page1.json and page2.json
		assertTrue(secrets.contains("1"));
		assertTrue(secrets.contains("2"));
	}

	private String getContents(String filename) throws IOException {
		ClassPathResource resource = new ClassPathResource(filename);

		return resource.getContentAsString(StandardCharsets.UTF_8);
	}
}
