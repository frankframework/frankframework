package org.frankframework.credentialprovider.delinea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.frankframework.credentialprovider.ICredentials;
import org.frankframework.credentialprovider.util.CredentialConstants;

/**
 * Test class for {@link DelineaCredentialFactory} with auto comment value enabled. Since Autocomment is a separate setting in the platform which we can't
 * test in the feedback from the server, this test was created to make sure that we actually send the auto comment value to the server if it's configured as
 * a non-empty value.
 *
 * @author evandongen
 */
public class DelineaCredentialFactoryWithAutoCommentTest {

	private DelineaCredentialFactory credentialFactory;
	private DelineaClient client;
	private static final String AUTO_COMMENT_VALUE = "autoCommentValue";

	@BeforeEach
	public void beforeEach() {
		// setup constants
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.API_ROOT_URL_KEY, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.OAUTH_TOKEN_URL_KEY, "http://localhost:8080");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.TENANT_KEY, "testTenant");
		CredentialConstants.getInstance().setProperty(DelineaCredentialFactory.USE_AUTO_COMMENT_VALUE, AUTO_COMMENT_VALUE);

		client = mock(DelineaClient.class);
		credentialFactory = new DelineaCredentialFactory();
		credentialFactory.setDelineaClient(client);
		credentialFactory.initialize();
	}

	@AfterAll
	public static void tearDown() {
		// Since credential constants is a singleton, make sure to clean up what we set up in the beforeEach
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.API_ROOT_URL_KEY);
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.OAUTH_TOKEN_URL_KEY);
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.TENANT_KEY);
		CredentialConstants.getInstance().remove(DelineaCredentialFactory.USE_AUTO_COMMENT_VALUE);
	}

	@Test
	void testGetSecret() {
		Secret secret1 = DelineaCredentialFactoryTest.createSecret(1, 11, "user1", "password1");

		ArgumentCaptor<String> autoCommentCaptor = forClass(String.class);
		when(client.getSecret(eq("1"), autoCommentCaptor.capture())).thenReturn(secret1);

		ICredentials credentials = credentialFactory.getCredentials("1", () -> null, () -> null);
		assertEquals(AUTO_COMMENT_VALUE, autoCommentCaptor.getValue());

		assertNotNull(credentials);
		assertEquals("user1", credentials.getUsername());

		// Get a non-existing secret
		ICredentials credentials2 = credentialFactory.getCredentials("16", () -> null, () -> null);

		// Expect a null return value, because alias 16 does not exist
		assertNull(credentials2);
	}
}
