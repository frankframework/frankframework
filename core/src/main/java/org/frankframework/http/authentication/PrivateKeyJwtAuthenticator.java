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

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.jspecify.annotations.NonNull;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.encryption.CorePkiUtil;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.http.AbstractHttpSession;

/**
 * OAuth 2.0 authenticator using the {@code private_key_jwt} client authentication method, as defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc7523#section-2.2">RFC 7523</a>. Also see the <a href="https://docs.spring.io/spring-security/reference/reactive/oauth2/client/client-authentication.html#oauth2-client-authentication-jwt-bearer-private-key-jwt">
 *     Spring docuemntation</a> on this topic.
 *
 * <p>A signed JWT is created using the configured private key and sent to the token endpoint as the
 * {@code client_assertion} parameter. Both RSA and EC keys are supported.
 *
 * <p>You can use this in your configuration like this:
 *
 * <pre>{@code
 *   <HttpSender url="..." tokenEndpoint="..."
 *     oauthAuthenticationMethod="PRIVATE_KEY_JWT"
 *     clientId="my-client-id">
 *     <Keystore keystoreResource="/path/to/keystore.p12"
 *       password="..."
 *       alias="my-key-alias"
 *       aliasPassword="..." />
 *   </HttpSender>
 * }</pre></p>
 *
 * <p>Please note, that with this setup you cannot have a separate key/certificate to be used for SSL. This will be addressed in the future to be able to have
 * different certificates used for SSL and private key authentication</p>
 *
 * @see org.frankframework.encryption.KeystoreConfiguration
 */
public class PrivateKeyJwtAuthenticator extends AbstractOauthAuthenticator {

	private static final String JWT_BEARER_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

	/**
	 * JWT expiry duration in milliseconds (5 minutes).
	 */
	private static final long JWT_EXPIRY_MS = 5 * 60 * 1000L;

	private PrivateKey privateKey;

	/**
	 * Will be determined based on the type of the private key (RSA or EC) and used to sign the JWT assertion.
	 */
	private JWSAlgorithm algorithm;

	public PrivateKeyJwtAuthenticator(AbstractHttpSession abstractHttpSession) throws HttpAuthenticationException {
		super(abstractHttpSession);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isBlank(clientId)) {
			throw new ConfigurationException("clientId is required for PrivateKeyJwtAuthenticator");
		}

		if (StringUtils.isBlank(session.getKeystore())) {
			throw new ConfigurationException("A keystore with a private key must be configured for PrivateKeyJwtAuthenticator");
		}

		try {
			privateKey = CorePkiUtil.getPrivateKey(session);
		} catch (EncryptionException e) {
			throw new ConfigurationException("Cannot load private key for PrivateKeyJwtAuthenticator", e);
		}

		if (privateKey instanceof RSAPrivateKey) {
			algorithm = JWSAlgorithm.RS256;
		} else if (privateKey instanceof ECPrivateKey) {
			algorithm = JWSAlgorithm.ES256;
		} else {
			throw new ConfigurationException("Unsupported private key type for PrivateKeyJwtAuthenticator provided: Only RSA and EC keys are supported.");
		}
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException {
		parameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
		parameters.add(new BasicNameValuePair("client_id", clientId));
		parameters.add(new BasicNameValuePair("client_assertion_type", JWT_BEARER_ASSERTION_TYPE));

		if (session.getScope() != null) {
			parameters.add(new BasicNameValuePair("scope", session.getScope().replace(',', ' ')));
		}

		try {
			parameters.add(new BasicNameValuePair("client_assertion", createJwtAssertion()));
		} catch (JOSEException e) {
			throw new HttpAuthenticationException("failed to create JWT assertion for private_key_jwt", e);
		}

		return createPostRequestWithForm(authorizationEndpoint, parameters);
	}

	private String createJwtAssertion() throws JOSEException {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + JWT_EXPIRY_MS);

		// These values are set according to the RFC 7523 specification for private_key_jwt client authentication:
		// * iss - must be the clientId
		// * sub - must be the clientId
		// * aud - must identify the authorization server (which is the token endpoint)
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.issuer(clientId)
				.subject(clientId)
				.audience(session.getTokenEndpoint())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(now)
				.expirationTime(expiry)
				.build();

		JWSHeader header = new JWSHeader.Builder(algorithm).build();
		return getSignedJWT(header, claimsSet).serialize();
	}

	private @NonNull SignedJWT getSignedJWT(JWSHeader header, JWTClaimsSet claimsSet) throws JOSEException {
		SignedJWT signedJWT = new SignedJWT(header, claimsSet);

		if (privateKey instanceof RSAPrivateKey rsaKey) {
			signedJWT.sign(new RSASSASigner(rsaKey));
		} else {
			signedJWT.sign(new ECDSASigner((ECPrivateKey) privateKey));
		}

		return signedJWT;
	}
}
