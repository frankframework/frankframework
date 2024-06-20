/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.jwt;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class JwtValidator<C extends SecurityContext> {

	private @Getter int connectTimeout=2000;
	private @Getter int readTimeout=2000;

	private @Getter ConfigurableJWTProcessor<C> jwtProcessor = null;

	public JwtValidator() {
		jwtProcessor = new DefaultJWTProcessor<>();
	}

	public void init(String jwksUrl, String requiredIssuer) throws ParseException, IOException {
		JWKSource<C> keySource = getKeySource(new URL(jwksUrl));

		// The expected JWS algorithm of the access tokens (agreed out-of-band)
		JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;

		// Configure the JWT processor with a key selector to feed matching public
		// RSA keys sourced from the JWK set URL
		JWSKeySelector<C> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);

		// Set up a JWT processor to parse the tokens and then check their signature
		// and validity time window (bounded by the "iat", "nbf" and "exp" claims)
		if (StringUtils.isNotEmpty(requiredIssuer)) {
			DefaultJWTClaimsVerifier<C> verifier=new DefaultJWTClaimsVerifier<>() {

				@Override
				public void verify(JWTClaimsSet claimsSet, C context) throws BadJWTException {
					super.verify(claimsSet, context);
					String issuer=claimsSet.getIssuer();
					if (!requiredIssuer.equals(issuer)) {
						throw new BadJWTException("illegal issuer ["+issuer+"], must be ["+requiredIssuer+"]");
					}
				}
			};
			getJwtProcessor().setJWTClaimsSetVerifier(verifier);
		}

		getJwtProcessor().setJWSKeySelector(keySelector);

	}

	protected JWKSource<C> getKeySource(URL jwksURL) throws IOException, ParseException {
		JWKSource<C> keySource = null;
		if("file".equals(jwksURL.getProtocol()) || "jar".equals(jwksURL.getProtocol())) {
			JWKSet set = JWKSet.load(jwksURL.openStream());
			keySource = new ImmutableJWKSet<>(set);
			return keySource;
		} else {
			// The public RSA keys to validate the signatures will be sourced from the
			// OAuth 2.0 server's JWK set, published at a well-known URL. The RemoteJWKSet
			// object caches the retrieved keys to speed up subsequent look-ups and can
			// also gracefully handle key-rollover
			ResourceRetriever retriever = new DefaultResourceRetriever(getConnectTimeout(), getReadTimeout());
			//JWKSource<C> keySource = new RemoteJWKSet<C>(new URL(jwksURL),retriever);
			// Implemented Seam for Dependency Injection of JWKSource for unit testing
			keySource = new RemoteJWKSet<>(jwksURL, retriever);
		}

		return keySource;
	}

	protected C createSecurityContext(String idToken) {
		return null;  // optional context parameter, not required here
	}

	public Map<String, Object> validateJWT(String idToken) throws ParseException, BadJOSEException, JOSEException {
		// Process the token
		C ctx = createSecurityContext(idToken);
		JWTClaimsSet claimsSet = getJwtProcessor().process(idToken, ctx);
		return claimsSet.toJSONObject();
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

}
