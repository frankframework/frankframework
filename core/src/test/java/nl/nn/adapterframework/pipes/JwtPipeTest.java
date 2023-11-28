package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;

public class JwtPipeTest extends PipeTestBase<JwtPipe> {
	private static final String DUMMY_SECRET = "PotatoSecretMustBeAtLeast265Bits";
	private static final String DUMMY_INPUT = "InputMessage";

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
	public void noSecret() {
		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(ex.getMessage(), Matchers.containsString("must either provide a [sharedSecret] (alias) or parameter"));
	}

	@Test
	public void secretTooShortShouldThrow() {
		pipe.setSharedSecret("Potato");
		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(ex.getMessage(), Matchers.containsString("must be at least 256 bits"));
	}

	@Test
	public void secretTooShortShouldBePadded() throws Exception {
		pipe.setJwtAllowWeakSecrets(true);
		pipe.setSharedSecret("Potato");
		configureAndStartPipe();
		String jwt1 = doPipe(DUMMY_INPUT).getResult().asString();

		pipe.setSharedSecret("Potato\0\0\0\0");
		configureAndStartPipe();
		String jwt2 = doPipe(DUMMY_INPUT).getResult().asString();
		assertEquals(jwt1, jwt2);
	}

	@Test
	public void secretPaddedIsTheSame() throws Exception {
		// Run with secret, 32 chars long (OK)
		pipe.setSharedSecret(DUMMY_SECRET);
		configureAndStartPipe();
		String jwt1 = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt1, Matchers.startsWith("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."));

		// Run with padded secret, 34 chars long (OK)
		pipe.setSharedSecret(DUMMY_SECRET + "\0\0");
		configureAndStartPipe();
		String jwt2 = doPipe(DUMMY_INPUT).getResult().asString();

		// Assert
		assertThat(jwt2, Matchers.startsWith("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."));
		assertEquals(jwt1, jwt2);
	}

	@Test
	public void secretAsAttribute() throws Exception {
		pipe.setSharedSecret(DUMMY_SECRET);
		configureAndStartPipe();

		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."));
	}

	@Test
	public void secretAsParameter() throws Exception {
		pipe.addParameter(new Parameter("sharedSecret", DUMMY_SECRET));
		configureAndStartPipe();

		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."));
	}

	@Test
	public void attributeAndParameter() throws Exception {
		pipe.setSharedSecret("asjdfjkadslfkjlsadfjlk;adsjflk;asjklfjaslkjfl;kasjld;aksfjl");
		pipe.addParameter(new Parameter("sharedSecret", DUMMY_SECRET)); //Should overwrite the attribute
		configureAndStartPipe();

		String jwt = doPipe(DUMMY_INPUT).getResult().asString();
		assertThat(jwt, Matchers.startsWith("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."));
	}

	@Test
	public void testJwtClaimSet() throws Exception {
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
	public void dontUseSecretParameter() throws Exception {
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
}
