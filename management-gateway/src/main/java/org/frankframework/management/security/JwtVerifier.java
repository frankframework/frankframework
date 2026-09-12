/*
   Copyright 2024-2026 WeAreFrank!

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
package org.frankframework.management.security;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSetCacheRefreshEvaluator;
import com.nimbusds.jose.jwk.source.JWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

public class JwtVerifier {
	private final DefaultJWTProcessor<SecurityContext> jwtProcessor  = new DefaultJWTProcessor<>();

	// Hazelcast / JWKS
	public JwtVerifier(Supplier<String> supply) {
		jwtProcessor.setJWSKeySelector(new LazyLoadingKeySelector(supply));
	}

	/**
	 * Validate the target JWT with a provided SECRET.
	 */
	public JwtVerifier(byte[] secret) {
		ImmutableSecret<SecurityContext> jwkSet = new ImmutableSecret<>(secret);
		jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.Family.HMAC_SHA, jwkSet));
	}

	// URL via RemoteJWKSet
	// URLBasedJWKSetSource
	public JwtVerifier(URL url) throws KeySourceException {
		jwtProcessor.setJWSKeySelector(JWSAlgorithmFamilyJWSKeySelector.fromJWKSetURL(url));
	}

	/**
	 * Validate and convert the given JWT to a Spring {@link Authentication Authenticaiton} object.
	 */
	public final @NonNull Authentication verify(String jwt) throws IOException {
		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(jwt, null);
		} catch (JOSEException | ParseException | BadJOSEException e) {
			throw new IOException("unable to parse JWT", e);
		}

		try {
			return new JwtAuthenticationToken(claimsSet, jwt);
		} catch (ParseException e) {
			throw new IOException("unable to create AuthenticationToken", e);
		}
	}

	private static class LazyLoadingKeySelector extends JWSVerificationKeySelector<SecurityContext> {

		public LazyLoadingKeySelector(Supplier<String> supply) {
			super(DefaultJwtKeyGenerator.JWT_DEFAULT_SIGNING_ALGORITHM, createJwkSetSource(supply));
		}

		private static JWKSource<SecurityContext> createJwkSetSource(Supplier<String> supply) {
			return JWKSourceBuilder.create(new LazyLoadingJwkSetSource(supply)).cacheForever().build();
		}
	}

	private static class LazyLoadingJwkSetSource implements JWKSetSource<SecurityContext> {
		private final Supplier<String> supply;

		public LazyLoadingJwkSetSource(Supplier<String> supply) {
			this.supply = supply;
		}

		@Override
		public JWKSet getJWKSet(JWKSetCacheRefreshEvaluator refreshEvaluator, long currentTime, SecurityContext context) throws KeySourceException {
			try {
				String jwks = supply.get();
				if (jwks == null) throw new KeySourceException("no jwks found");
				return JWKSet.parse(jwks);
			} catch (ParseException e) {
				throw new KeySourceException("parse exception", e);
			}
		}

		@Override
		public void close() {
			// Nothing to do here
		}
	}
}
