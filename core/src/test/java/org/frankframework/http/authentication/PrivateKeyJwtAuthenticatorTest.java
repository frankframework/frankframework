/*
   Copyright 2024 - 2026 WeAreFrank!

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
package org.frankframework.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.http.HttpSender;
import org.frankframework.util.StreamUtil;

class PrivateKeyJwtAuthenticatorTest {

	private static final String CLIENT_ID = "fakeClientId";
	private static final String TOKEN_ENDPOINT = "https://token-dummy";

	private HttpSender httpSender;

	@BeforeEach
	void setup() {
		httpSender = new HttpSender();
		httpSender.setUrl("https://dummy");
		httpSender.setTokenEndpoint(TOKEN_ENDPOINT);
		httpSender.setOauthAuthenticationMethod(AbstractHttpSession.OauthAuthenticationMethod.PRIVATE_KEY_JWT);
		httpSender.setClientId(CLIENT_ID);

		httpSender.setKeystore("/Signature/saml-keystore.p12");
		httpSender.setKeystorePassword("geheim");
		httpSender.setKeystoreAlias("myalias");
		httpSender.setKeystoreAliasPassword("geheim");
	}

	@Test
	void testPrivateKeyJwtRequestContainsRequiredParameters() throws Exception {
		httpSender.configure();
		httpSender.start();

		PrivateKeyJwtAuthenticator authenticator = (PrivateKeyJwtAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.PRIVATE_KEY_JWT.newAuthenticator(httpSender);
		authenticator.configure();

		HttpEntityEnclosingRequestBase request = authenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

		assertEquals("POST", request.getMethod());

		String body = StreamUtil.streamToString(request.getEntity().getContent(), "\n", "UTF-8");
		String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

		assertTrue(decodedBody.contains("grant_type=client_credentials"), "Body must contain grant_type=client_credentials");
		assertTrue(decodedBody.contains("client_id=" + CLIENT_ID), "Body must contain client_id");
		assertTrue(decodedBody.contains("client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer"), "Body must contain client_assertion_type");
		assertTrue(decodedBody.contains("client_assertion="), "Body must contain client_assertion");
	}

	@Test
	void testPrivateKeyJwtAssertionIsValidJwt() throws Exception {
		httpSender.configure();
		httpSender.start();

		PrivateKeyJwtAuthenticator authenticator = (PrivateKeyJwtAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.PRIVATE_KEY_JWT.newAuthenticator(httpSender);
		authenticator.configure();

		HttpEntityEnclosingRequestBase request = authenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

		String body = StreamUtil.streamToString(request.getEntity().getContent(), "\n", "UTF-8");
		String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

		String jwtToken = extractParam(decodedBody, "client_assertion");
		assertNotNull(jwtToken, "client_assertion must be present");

		SignedJWT signedJWT = SignedJWT.parse(jwtToken);
		JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

		assertEquals(CLIENT_ID, claims.getIssuer(), "JWT issuer must be clientId");
		assertEquals(CLIENT_ID, claims.getSubject(), "JWT subject must be clientId");
		assertTrue(claims.getAudience().contains(TOKEN_ENDPOINT), "JWT audience must contain token endpoint");
		assertNotNull(claims.getJWTID(), "JWT must have a jti");
		assertNotNull(claims.getIssueTime(), "JWT must have an iat");
		assertNotNull(claims.getExpirationTime(), "JWT must have an exp");
		assertTrue(claims.getExpirationTime().after(claims.getIssueTime()), "exp must be after iat");
	}

	@Test
	void testPrivateKeyJwtWithScope() throws Exception {
		httpSender.setScope("email,profile");
		httpSender.configure();
		httpSender.start();

		PrivateKeyJwtAuthenticator authenticator = (PrivateKeyJwtAuthenticator) AbstractHttpSession.OauthAuthenticationMethod.PRIVATE_KEY_JWT.newAuthenticator(httpSender);
		authenticator.configure();

		HttpEntityEnclosingRequestBase request = authenticator.createRequest(httpSender.getDomainAwareCredentials(), new ArrayList<>());

		String body = StreamUtil.streamToString(request.getEntity().getContent(), "\n", "UTF-8");
		String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

		assertTrue(decodedBody.contains("scope=email profile"), "Body must contain scope with spaces instead of commas");
	}

	@Test
	void testConfigureFailsWithoutClientId() {
		httpSender.setClientId(null);

		assertThrows(ConfigurationException.class, () -> httpSender.configure(),
				"Expected ConfigurationException when clientId is missing");
	}

	@Test
	void testConfigureFailsWithoutPrivateKey() {
		HttpSender noKeySender = new HttpSender();
		noKeySender.setUrl("https://dummy");
		noKeySender.setTokenEndpoint(TOKEN_ENDPOINT);
		noKeySender.setOauthAuthenticationMethod(AbstractHttpSession.OauthAuthenticationMethod.PRIVATE_KEY_JWT);
		noKeySender.setClientId(CLIENT_ID);
		// No keystore configured — private key loading must fail

		assertThrows(ConfigurationException.class, noKeySender::configure,
				"Expected ConfigurationException when private key is missing");
	}

	private String extractParam(String body, String paramName) {
		for (String part : body.split("&")) {
			if (part.startsWith(paramName + "=")) {
				return part.substring(paramName.length() + 1);
			}
		}
		return null;
	}
}
