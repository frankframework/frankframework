/*
   Copyright 2024 WeAreFrank!

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
import java.text.ParseException;
import java.util.function.Supplier;

import org.springframework.security.core.Authentication;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSetCacheRefreshEvaluator;
import com.nimbusds.jose.jwk.source.JWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

public class JwtVerifier extends DefaultJWTProcessor<SecurityContext> {

	public JwtVerifier(Supplier<String> supply) {
		setJWSKeySelector(new LazyLoadingJwkSource(supply));
	}

	public Authentication verify(String jwt) throws IOException {
		JWTClaimsSet claimsSet;
		try {
			claimsSet = process(jwt, null);
		} catch (JOSEException | ParseException | BadJOSEException e) {
			throw new IOException("unable to parse JWT", e);
		}

		try {
			return new JwtAuthenticationToken(claimsSet, jwt);
		} catch (ParseException e) {
			throw new IOException("unable to create AuthenticationToken", e);
		}
	}

	private static class LazyLoadingJwkSource extends JWSVerificationKeySelector<SecurityContext> {

		public LazyLoadingJwkSource(Supplier<String> supply) {
			super(JwtKeyGenerator.JWT_DEFAULT_SIGNING_ALGORITHM, createKeySource(supply));
		}

		private static JWKSource<SecurityContext> createKeySource(Supplier<String> supply) {
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
		public void close() throws IOException {
			// TODO Auto-generated method stub
			
		}
	}
}
