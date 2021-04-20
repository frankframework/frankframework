package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.Test;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.PkiUtil;

public class JWTPipeTest extends PipeTestBase<JWTPipe> {

	// Automatically create a key for the test
	private final static String SECRET = "TESTKEYTHATISLONGENOUGHTOPASSVALIDATION1234";
	private final static String ISSUER = "JWTPipeTest";
	private final static String SUBJECT = "UnitTest";
	private final static String AUDIENCE = "Framework";
	private final static String JTI = "1234";
	
	private final static String TYPE_TIMESTAMP = "datetime";
	private final static String PARAM_ISSUEDAT = "issuedAt";
	private final static String PARAM_NOTBEFORE = "notBefore";
	private final static String PARAM_EXPIRATION = "expiration";
	
	private final static String PARAM_ISSUER = "issuer";
	private final static String PARAM_SUBJECT = "subject";
	private final static String PARAM_AUDIENCE = "audience";
	private final static String PARAM_JTI = "jti";
	
	private String failureForwardName = "failure";
	private String expiredForwardName = "expired";
	private String prematureForwardName = "premature";
	
	private String decodedToken = "{aud=Framework, jti=1234, iss=JWTPipeTest, sub=UnitTest, CustomClaim1=CustomClaim1Value, CustomClaim2=CustomClaim2Value}";
	
	@Override
	public JWTPipe createPipe() {
		JWTPipe jwtPipe = new JWTPipe();
		jwtPipe.setSecret(SECRET);
		jwtPipe.setIssuedAtNow(false); // Set to false to prevent drifting test cases
		jwtPipe.registerForward(new PipeForward(failureForwardName, null));
		jwtPipe.registerForward(new PipeForward(expiredForwardName, null));
		jwtPipe.registerForward(new PipeForward(prematureForwardName, null));
		return jwtPipe;
	}

	@Test
	public void testEncode() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9.IizVZV_opGPInFVPaCs8LoHxve5o7kzF93yMzOdxt80", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodeKeystore() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		pipe.setKeystore("/JWT/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9.U1VsMoITf5kUEHtzfgJTyRWEDZ2gjtTuQI3DVRrJcpden2pjCsAWwl4VOr6McmQkcndZj0GPvN4w3NkJR712ltlsIXw1zMm67vuFY0_id7TP2zIJh3jMkKrTuSPE-SBXZyVnIq22Q54R1VMnOTjO6spbrbYowIzyyeAC7U1RzyB3aKxTgeYJS6auLBaiR3-SWoXs_hBnbIIgYT7AC2e76ICpMlFPQS_e2bcqe1B-yz69se8ZlJgwWK-YhqHMoOCA9oQy3t_cObQI0KSzg7cYDkkQ17cWF3SoyTSTs6Cek_Y97Z17lJX2RVBayPc2uI_oWWuaIUbukxAOIUkgpgtf6g", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodePEM() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		pipe.setKeystore("/JWT/privateKey.key");
		pipe.setKeystoreType("pem");
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9.U1VsMoITf5kUEHtzfgJTyRWEDZ2gjtTuQI3DVRrJcpden2pjCsAWwl4VOr6McmQkcndZj0GPvN4w3NkJR712ltlsIXw1zMm67vuFY0_id7TP2zIJh3jMkKrTuSPE-SBXZyVnIq22Q54R1VMnOTjO6spbrbYowIzyyeAC7U1RzyB3aKxTgeYJS6auLBaiR3-SWoXs_hBnbIIgYT7AC2e76ICpMlFPQS_e2bcqe1B-yz69se8ZlJgwWK-YhqHMoOCA9oQy3t_cObQI0KSzg7cYDkkQ17cWF3SoyTSTs6Cek_Y97Z17lJX2RVBayPc2uI_oWWuaIUbukxAOIUkgpgtf6g", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodeTimeParams() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
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
		notBeforeParameter.setName(PARAM_NOTBEFORE);
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

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCIsImlhdCI6MTYwOTQ1NTYwMCwiZXhwIjoxNjQwOTkxNjAwLCJuYmYiOjE1Nzc4MzMyMDB9.JOJZK3oqbQg3TLNUgH-CE-3BSrDLxDMhd5xDs6l0Oc0", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodeParams() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		
		Parameter issuerParameter = new Parameter();
		issuerParameter.setName(PARAM_ISSUER);
		issuerParameter.setValue("OtherJWTPipeTest");
		Parameter subjectParameter = new Parameter();
		subjectParameter.setName(PARAM_SUBJECT);
		subjectParameter.setValue("OtherSubject");
		Parameter audienceParameter = new Parameter();
		audienceParameter.setName(PARAM_AUDIENCE);
		audienceParameter.setValue("OTHERAudience");
		Parameter jtiParameter = new Parameter();
		jtiParameter.setName(PARAM_JTI);
		jtiParameter.setValue("OTHERJTI");
		pipe.addParameter(issuerParameter);
		pipe.addParameter(subjectParameter);
		pipe.addParameter(audienceParameter);
		pipe.addParameter(jtiParameter);
		
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPdGhlckpXVFBpcGVUZXN0Iiwic3ViIjoiT3RoZXJTdWJqZWN0IiwiYXVkIjoiT1RIRVJBdWRpZW5jZSIsImp0aSI6Ik9USEVSSlRJIn0.HcjImu_ZFodmlNZOAyDvbKVcT1qXJgHO9_Ef-PzWn9k", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodeCustomClaims() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		pipe.setCustomClaimsParams("CustomClaim1,CustomClaim2");
		Parameter customClaim1 = new Parameter();
		customClaim1.setName("CustomClaim1");
		customClaim1.setValue("CustomClaim1Value");
		Parameter customClaim2 = new Parameter();
		customClaim2.setName("CustomClaim2");
		customClaim2.setValue("CustomClaim1Value");
		pipe.addParameter(customClaim1);
		pipe.addParameter(customClaim2);
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCIsIkN1c3RvbUNsYWltMSI6IkN1c3RvbUNsYWltMVZhbHVlIiwiQ3VzdG9tQ2xhaW0yIjoiQ3VzdG9tQ2xhaW0xVmFsdWUifQ.ML2cPQjKMtjwyiZlkAfvlY8qY4o1MIJ54Rq7M_aZ7mk", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEncodeNonExistingCustomClaims() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("encode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);
		pipe.setCustomClaimsParams("CustomClaim");
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(""));

		assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJKV1RQaXBlVGVzdCIsInN1YiI6IlVuaXRUZXN0IiwiYXVkIjoiRnJhbWV3b3JrIiwianRpIjoiMTIzNCJ9.IizVZV_opGPInFVPaCs8LoHxve5o7kzF93yMzOdxt80", prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testInvalidSecret() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		exception.expect(WeakKeyException.class);
		pipe.setDirection("encode");
		pipe.setSecret("InvalidSecret");
		configureAndStartPipe();

		doPipe(new Message(""));
	}
	
	@Test
	public void testDecode() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		pipe.setIssuer(ISSUER);
		pipe.setSubject(SUBJECT);
		pipe.setAudience(AUDIENCE);
		pipe.setJTI(JTI);		
		pipe.setCustomClaimsParams("CustomClaim1,CustomClaim2");
		Parameter customClaim1 = new Parameter();
		customClaim1.setName("CustomClaim1");
		customClaim1.setValue("CustomClaim1Value");
		Parameter customClaim2 = new Parameter();
		customClaim2.setName("CustomClaim2");
		customClaim2.setValue("CustomClaim2Value");
		pipe.addParameter(customClaim1);
		pipe.addParameter(customClaim2);
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testDecodeKeystore() throws ConfigurationException, PipeStartException, PipeRunException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		pipe.setDirection("decode");
		pipe.setKeystore("/JWT/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(createTokenWithPK(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testDecodePEM() throws ConfigurationException, PipeStartException, PipeRunException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		pipe.setDirection("decode");
		pipe.setKeystore("/JWT/certificate.crt");
		pipe.setKeystoreType("pem");
		configureAndStartPipe();
				
		PipeRunResult prr = doPipe(new Message(createTokenWithPK(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testIssuerMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		pipe.setIssuer("WrongIssuer");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testAudienceMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		pipe.setAudience("WrongAudience");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testJTIMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		pipe.setJTI("WrongJTI");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testSubjectMismatch() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		pipe.setSubject("WrongSubject");
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(null, null)));

		assertEquals(decodedToken, prr.getResult().asString());
		assertEquals("failure", prr.getPipeForward().getName());
	}
	
	@Test
	public void testNotBeforeInFuture() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		configureAndStartPipe();
		
		Calendar notBefore = Calendar.getInstance();
		notBefore.add(Calendar.MINUTE, 5);
		
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(null, notBefore.getTime())));

		assertEquals("premature", prr.getPipeForward().getName());
	}
	
	@Test
	public void testExpirationInPast() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		pipe.setDirection("decode");
		configureAndStartPipe();
		
		Calendar expiration = Calendar.getInstance();
		expiration.add(Calendar.MINUTE, -5);
		
		PipeRunResult prr = doPipe(new Message(createTokenWithSecret(expiration.getTime(), null)));

		assertEquals("expired", prr.getPipeForward().getName());
	}
	
	@Test
	public void testWrongSecret() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		exception.expect(PipeRunException.class);
		pipe.setDirection("decode");
		pipe.setSecret("ThisIsNotTheCorrectSecretAndItIsLongEnough");
		configureAndStartPipe();

		doPipe(new Message(createTokenWithSecret(null, null)));
	}
	
	@Test
	public void testConfigurationFailureNoSecrets() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("has neither secret nor authAlias nor keystore set");
		pipe.setDirection("encode");
		pipe.setSecret(null);
		configureAndStartPipe();
	}
	
	
	@Test
	public void testConfigurationFailureHMACNoSecret() throws ConfigurationException, PipeStartException, PipeRunException, IOException {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("has HMAC-algorithm [HS256] but neither secret nor authAlias set");
		pipe.setDirection("encode");
		pipe.setAlgorithm("HS256");
		configureAndStartPipe();
	}
	
	private String createTokenWithSecret(Date expiration, Date notBefore) {
		JwtBuilder jwtBuilder = Jwts.builder().setAudience(AUDIENCE).setId(JTI).setIssuer(ISSUER).setSubject(SUBJECT).claim("CustomClaim1", "CustomClaim1Value").claim("CustomClaim2", "CustomClaim2Value");
		if (expiration != null) {
			jwtBuilder.setExpiration(expiration);
		}
		if (notBefore != null) {
			jwtBuilder.setNotBefore(notBefore);
		}
		
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		
		return jwtBuilder.signWith(key).compact();
	}	
	
	private String createTokenWithPK(Date expiration, Date notBefore) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
		JwtBuilder jwtBuilder = Jwts.builder().setAudience(AUDIENCE).setId(JTI).setIssuer(ISSUER).setSubject(SUBJECT).claim("CustomClaim1", "CustomClaim1Value").claim("CustomClaim2", "CustomClaim2Value");
		if (expiration != null) {
			jwtBuilder.setExpiration(expiration);
		}
		if (notBefore != null) {
			jwtBuilder.setNotBefore(notBefore);
		}
		
		PrivateKey key = PkiUtil.getPrivateKeyFromPem(TestFileUtils.getTestFileURL("/JWT/privateKey.key"));
		
		return jwtBuilder.signWith(key).compact();
	}
}
