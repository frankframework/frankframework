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
import io.jsonwebtoken.security.WeakKeyException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class JWTEncodePipeTest extends PipeTestBase<JWTEncodePipe> {

	// Automatically create a key for the test
	private final static String SECRET = "TESTKEYTHATISLONGENOUGHTOPASSVALIDATION1234";
	private final static String ISSUER = "JWTEncodePipeTest";
	private final static String SUBJECT = "UnitTest";
	private final static String AUDIENCE = "Framework";
	private final static String JTI = "1234";
	
	private final static String TYPE_TIMESTAMP = "datetime";
	private final static String PARAM_ISSUEDAT = "issuedAt";
	private final static String PARAM_NOTBEFORE = "notBefore";
	private final static String PARAM_EXPIRATION = "expiration";
	
	@Override
	public JWTEncodePipe createPipe() {
		JWTEncodePipe jwtEncodePipe = new JWTEncodePipe();
		jwtEncodePipe.setSecret(SECRET);
		jwtEncodePipe.setIssuedAtNow(false); // Set to false to prevent drifting test cases
		return jwtEncodePipe;
	}

	@Test
	public void testEncode() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJKV1RFbmNvZGVQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9.g81w22zb9XKO5bLAjspVSsKjgaglc_OfjdIpi8eCEI4", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodeParams() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		// The following should be overwritten by the fixed parameter values
		pipe.setIssuedAtNow(true); 
		pipe.setNotBeforeOffset(10);
		pipe.setExpirationOffset(20);
		
		Parameter issuedAtParameter = new Parameter();
		issuedAtParameter.setName(PARAM_ISSUEDAT);
		issuedAtParameter.setValue("2021-01-01 00:00:00");
		issuedAtParameter.setType(TYPE_TIMESTAMP);
		Parameter notBeforeParameter = new Parameter();
		notBeforeParameter.setName("notBefore");
		notBeforeParameter.setValue("2020-01-01 00:00:00");
		notBeforeParameter.setType(TYPE_TIMESTAMP);
		Parameter expirationParameter = new Parameter();
		expirationParameter.setName(PARAM_EXPIRATION);
		expirationParameter.setValue("2022-01-01 00:00:00");
		expirationParameter.setType(TYPE_TIMESTAMP);
		pipe.addParameter(issuedAtParameter);
		pipe.addParameter(notBeforeParameter);
		pipe.addParameter(expirationParameter);
		
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJKV1RFbmNvZGVQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCIsImlhdCI6MTYwOTQ1NTYwMCwiZXhwIjoxNjQwOTkxNjAwLCJuYmYiOjE1Nzc4MzMyMDB9.coRoVx3QaNjLkqu1H3xD6Kd2u55PHbq_h-R8rIllR-M", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testInvalidSecret() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		exception.expect(WeakKeyException.class);
		pipe.setSecret("InvalidSecret");
		configureAndStartPipe();

		doPipe(new Message(""));
	}
}
