package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.parameters.Parameter;

public class JwtPipeTest extends PipeTestBase<JwtPipe> {
	private static final String DUMMY_SECRET = "PotatoSecretMustBeAtLeast265Bits";
	private static final String DUMMY_INPUT = "InputMessage";
	private static final String BASE64_HEADER_HS256 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9";
	private static final String RIGHT_PADDED_SHORT_SECRET = "Potato\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";

	private static ConfigurableJWTProcessor<SecurityContext> JWT_PROCESSOR;

	@BeforeAll
	public static void setupAll() {
		JWT_PROCESSOR = new DefaultJWTProcessor<>();
		SecretKey key = new SecretKeySpec(DUMMY_SECRET.getBytes(), "HmacSHA256");
		JWKSource<SecurityContext> immutableSecret = new ImmutableSecret<>(key);

		JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.HS256, immutableSecret);
		JWT_PROCESSOR.setJWSKeySelector(keySelector);
	}

	@Override
	public JwtPipe createPipe() {
		return new JwtPipe();
	}

	@Test
	void noSecret() {
		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(ex.getMessage(), Matchers.containsString("must either provide a [sharedSecret] (alias) or parameter"));
	}

	@Test
	void secretTooShortShouldThrow() {
		// Secrets need to be 256 bits when JwtAllowWeakSecrets is false
		pipe.setSharedSecret("Potato");
		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(ex.getMessage(), Matchers.containsString("must be at least 256 bits"));
	}

	@Test
	void secretParamTooShortShouldThrow() throws ConfigurationException {
		// Secrets as parameter need to be 256 bits when JwtAllowWeakSecrets is false
		pipe.addParameter(new Parameter(JwtPipe.SHARED_SECRET_PARAMETER_NAME, "Potato"));
		configureAndStartPipe();

		PipeRunException ex = assertThrows(PipeRunException.class, () -> doPipe(DUMMY_INPUT));
		assertThat(ex.getMessage(), Matchers.containsString("must be at least 256 bits"));
	}

	@Test
	void secretTooShortShouldBePadded() throws Exception {
		// When jwtAllowWeakSecrets is true, short secrets should be padded to meet minimum length
		pipe.setJwtAllowWeakSecrets(true);
		pipe.setSharedSecret("Potato");
		configureAndStartPipe();
		String jwt1 = doPipe(DUMMY_INPUT).getResult().asString();

		// A manually padded secret should produce the same signature as the auto-padded secret
		pipe.setSharedSecret("Potato\0\0\0\0");
		configureAndStartPipe();
		String jwt2 = doPipe(DUMMY_INPUT).getResult().asString();

		assertValidTokenSignature(jwt1, RIGHT_PADDED_SHORT_SECRET);
		assertValidTokenSignature(jwt2, RIGHT_PADDED_SHORT_SECRET);
	}

	@Test
	void authAliasTooShortShouldPadPassword() throws Exception {
		// Arrange
		pipe.setJwtAllowWeakSecrets(true);
		pipe.setSharedSecret(null);
		pipe.setAuthAlias("alias1");

		// Act && Assert: flow should work and message returns.
		configureAndStartPipe();
		String result = doPipe(DUMMY_INPUT).getResult().asString();
		assertNotNull(result);
	}

	@Test
	void authAliasTooShortShouldFailConfiguration() {
		// Arrange
		pipe.setAuthAlias("alias1");

		// Act && Assert: the warning message shows a non-padded secret
		ConfigurationException ex =  assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(ex.getMessage(), Matchers.containsString("must be at least 256 bits"));
	}

	@Test
	void secretParamTooShortShouldBePadded() throws Exception {
		// Generate JWT using a short secret (passed as a parameter) with padding allowed
		pipe.setJwtAllowWeakSecrets(true);
		Parameter potatoParam = new Parameter(JwtPipe.SHARED_SECRET_PARAMETER_NAME, "Potato");
		pipe.addParameter(potatoParam);
		configureAndStartPipe();
		String jwt1 = doPipe(DUMMY_INPUT).getResult().asString();

		// Generate JWT using a manually padded version of the same secret (also as parameter)
		pipe.setSharedSecret(null);
		pipe.getParameterList().clear();
		potatoParam.setValue("Potato\0\0\0\0");
		pipe.addParameter(potatoParam);
		configureAndStartPipe();
		String jwt2 = doPipe(DUMMY_INPUT).getResult().asString();

		// The resulting JWT signatures should be identical
		assertValidTokenSignature(jwt1, RIGHT_PADDED_SHORT_SECRET);
		assertValidTokenSignature(jwt2, RIGHT_PADDED_SHORT_SECRET);
	}

	@Test
	void secretPaddedIsTheSame() throws Exception {
		// Run with secret, 32 chars long (OK)
		pipe.setSharedSecret(DUMMY_SECRET);
		configureAndStartPipe();
		String jwt1 = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt1, Matchers.startsWith(BASE64_HEADER_HS256));

		// Run with padded secret, 34 chars long (OK)
		pipe.setSharedSecret(DUMMY_SECRET + "\0\0");
		configureAndStartPipe();
		String jwt2 = doPipe(DUMMY_INPUT).getResult().asString();

		// Assert
		assertThat(jwt2, Matchers.startsWith(BASE64_HEADER_HS256));
		assertValidTokenSignature(jwt1, DUMMY_SECRET);
		assertValidTokenSignature(jwt2, DUMMY_SECRET);
	}

	@Test
	void secretAsAttribute() throws Exception {
		pipe.setSharedSecret(DUMMY_SECRET);
		configureAndStartPipe();

		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith(BASE64_HEADER_HS256));
	}

	@Test
	void secretAsParameter() throws Exception {
		pipe.addParameter(new Parameter("sharedSecret", DUMMY_SECRET));
		configureAndStartPipe();

		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith(BASE64_HEADER_HS256));
	}

	@Test
	void attributeAndParameter() throws Exception {
		// Add a parameter for the shared secret (this should take priority)
		pipe.addParameter(new Parameter("sharedSecret", DUMMY_SECRET)); // Priority should be given to parameter

		// Set a different shared secret attribute (should not overwrite the parameter)
		pipe.setSharedSecret("asjdfjkadslfkjlsadfjlk;adsjflk;asjklfjaslkjfl;kasjld;aksfjl");

		configureAndStartPipe();

		// Ensure the JWT starts with the correct header
		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith(BASE64_HEADER_HS256));

		// Verify the JWT signature with the secret from the parameter
		assertValidTokenSignature(jwt, DUMMY_SECRET);
	}

	@Test
	void attributeAndParameterOrderShouldNotAffectPriority() throws Exception {
		// Set a shared secret (this should not take priority over the parameter)
		pipe.setSharedSecret("asjdfjkadslfkjlsadfjlk;adsjflk;asjklfjaslkjfl;kasjld;aksfjl");

		// Add a parameter with the shared secret (this should overwrite the attribute)
		pipe.addParameter(new Parameter("sharedSecret", DUMMY_SECRET));

		configureAndStartPipe();

		// Ensure the JWT starts with the correct header
		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith(BASE64_HEADER_HS256));

		// Verify the JWT signature with the secret from the parameter (priority should be given to the parameter)
		assertValidTokenSignature(jwt, DUMMY_SECRET);
	}

	@Test
	void testJwtClaimSet() throws Exception {
		// Arrange
		pipe.setSharedSecret(DUMMY_SECRET);
		pipe.setExpirationTime(60);
		pipe.addParameter(new Parameter("sub", "Smint"));
		pipe.addParameter(new Parameter("iss", "CleanBreath"));
		pipe.addParameter(new Parameter("Sugar", "Free"));

		Parameter paramFromSession = new Parameter();
		paramFromSession.setName("amt");
		paramFromSession.setSessionKey("amt");
		session.put("amt", 50);
		pipe.addParameter(paramFromSession);

		configureAndStartPipe();

		// Act
		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		JWTClaimsSet claimSet = JWT_PROCESSOR.process(jwt, null);

		// Assert
		assertEquals("Smint", claimSet.getSubject());
		assertEquals("CleanBreath", claimSet.getIssuer());
		assertEquals("Free", claimSet.getStringClaim("Sugar"));
		assertEquals(50, claimSet.getIntegerClaim("amt"));
		assertTrue(claimSet.getExpirationTime().after(new Date()));
	}

	@Test
	void dontUseSecretParameter() throws Exception {
		// Arrange
		pipe.setExpirationTime(0); //And no expiration time
		pipe.addParameter(new Parameter("sharedSecret", DUMMY_SECRET));

		pipe.addParameter(new Parameter("sub", "Smint"));
		pipe.addParameter(new Parameter("iss", "CleanBreath"));
		pipe.addParameter(new Parameter("Sugar", "Free"));

		configureAndStartPipe();

		// Act
		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		JWTClaimsSet claimSet = JWT_PROCESSOR.process(jwt, null);

		// Assert
		assertEquals("Smint", claimSet.getSubject());
		assertEquals("CleanBreath", claimSet.getIssuer());
		assertEquals("Free", claimSet.getStringClaim("Sugar"));
		assertNull(claimSet.getStringClaim("sharedSecret"));
		assertNull(claimSet.getExpirationTime());
	}

	private void assertValidTokenSignature(String jwt, String secret) throws Exception {
		JWSVerifier verifier = new MACVerifier(secret.getBytes());
		assertTrue(SignedJWT.parse(jwt).verify(verifier));
	}
}
