/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.management.switchboard;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Enumeration;

import org.springframework.security.core.Authentication;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSetCacheRefreshEvaluator;
import com.nimbusds.jose.jwk.source.JWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import org.frankframework.management.security.JwtAuthenticationToken;

public class CloudAgentJwtVerifier extends DefaultJWTProcessor<SecurityContext> {

	public CloudAgentJwtVerifier(MtlsHelper mtlsHelper) {
		setJWSKeySelector(new LazyLoadingJwkSource(mtlsHelper));
	}

	public Authentication verify(String jwt) throws IOException {
		try {
			JWTClaimsSet claimsSet = process(jwt, null);
			return new JwtAuthenticationToken(claimsSet, jwt);
		} catch (JOSEException | ParseException | BadJOSEException e) {
			throw new IOException("unable to parse JWT", e);
		}
	}

	private static class LazyLoadingJwkSource extends JWSVerificationKeySelector<SecurityContext> {
		public LazyLoadingJwkSource(MtlsHelper mtlsHelper) {
			super(JWSAlgorithm.RS512, createKeySource(mtlsHelper));
		}

		private static JWKSource<SecurityContext> createKeySource(MtlsHelper mtlsHelper) {
			return JWKSourceBuilder.create(new LazyLoadingJwkSetSource(mtlsHelper)).cacheForever().build();
		}
	}

	private record LazyLoadingJwkSetSource(MtlsHelper mtlsHelper) implements JWKSetSource<SecurityContext> {

		@Override
		public JWKSet getJWKSet(JWKSetCacheRefreshEvaluator refreshEvaluator, long currentTime, SecurityContext context)
				throws KeySourceException {
			try {
				KeyStore ks = mtlsHelper.getKeyStore();
				Enumeration<String> aliases = ks.aliases();
				if (!aliases.hasMoreElements()) {
					throw new KeySourceException("No aliases in keystore");
				}
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
				PublicKey publicKey = cert.getPublicKey();

				if (!(publicKey instanceof java.security.interfaces.RSAPublicKey rsaPublicKey)) {
					throw new KeySourceException("Certificate is not RSA");
				}

				RSAKey jwk = new RSAKey.Builder(rsaPublicKey)
						.keyID(alias)
						.build();

				return new JWKSet(jwk);
			} catch (KeyStoreException e) {
				throw new KeySourceException("Failed to load JWK from keystore", e);
			}
		}

		@Override
		public void close() {
			// No-op
		}
	}
}
