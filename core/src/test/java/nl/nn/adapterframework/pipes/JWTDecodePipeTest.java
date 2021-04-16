package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.Test;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class JWTDecodePipeTest extends PipeTestBase<JWTDecodePipe> {

	// Automatically create a key for the test
	private final static String SECRET = "TESTKEYTHATISLONGENOUGHTOPASSVALIDATION1234";
	private final static String ISSUER = "JWTEncodePipeTest";
	private final static String SUBJECT = "UnitTest";
	private final static String AUDIENCE = "Framework";
	private final static String JTI = "1234";
	
	private String failureForwardName = "failure";
	private String expiredForwardName = "expired";
	private String prematureForwardName = "premature";
	
	private String decodedToken = "{aud=Framework, jti=1234, iss=JWTEncodePipeTest, sub=UnitTest}";
	
	@Override
	public JWTDecodePipe createPipe() {
		JWTDecodePipe jwtDecodePipe = new JWTDecodePipe();
		jwtDecodePipe.registerForward(new PipeForward(failureForwardName, null));
		jwtDecodePipe.registerForward(new PipeForward(expiredForwardName, null));
		jwtDecodePipe.registerForward(new PipeForward(prematureForwardName, null));
		jwtDecodePipe.setSecret(SECRET);
		return jwtDecodePipe;
	}

	@Test
	public void testDecode() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(createToken(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testIssuerMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setIssuer("WrongIssuer");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createToken(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testAudienceMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setAudience("WrongAudience");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createToken(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testJTIMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setJTI("WrongJTI");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createToken(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testSubjectMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setSubject("WrongSubject");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createToken(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testNotBeforeInFuture() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		configureAndStartPipe();
		
		Calendar notBefore = Calendar.getInstance();
		notBefore.add(Calendar.MINUTE, 5);
		
		PipeRunResult prr = doPipe(new Message(createToken(null, notBefore.getTime())));

		assertEquals("premature", prr.getPipeForward().getName());
	}
	
	@Test
	public void testExpirationInPast() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		configureAndStartPipe();
		
		Calendar expiration = Calendar.getInstance();
		expiration.add(Calendar.MINUTE, -5);
		
		PipeRunResult prr = doPipe(new Message(createToken(expiration.getTime(), null)));

		assertEquals("expired", prr.getPipeForward().getName());
	}
	
	@Test
	public void testWrongSecret() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		exception.expect(PipeRunException.class);
		pipe.setSecret("ThisIsNotTheCorrectSecretAndItIsLongEnough");
		configureAndStartPipe();

		doPipe(new Message(createToken(null, null)));
	}
	
	private String createToken(Date expiration, Date notBefore) {
		JwtBuilder jwtBuilder = Jwts.builder().setAudience(AUDIENCE).setId(JTI).setIssuer(ISSUER).setSubject(SUBJECT);
		if (expiration != null) {
			jwtBuilder.setExpiration(expiration);
		}
		if (notBefore != null) {
			jwtBuilder.setNotBefore(notBefore);
		}
		
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		
		return jwtBuilder.signWith(key).compact();
	}

}
