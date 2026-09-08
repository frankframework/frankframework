/*
 * Copyright 2025-2026 WeAreFrank!
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.frankframework.management.security;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;

import org.frankframework.encryption.CommonsPkiUtil;

public class KeystoreJwtGenerator extends AbstractJwtGenerator<RSAKey> {

	private final KeyStore keyStore;

	protected KeystoreJwtGenerator(KeyStore keyStore) {
		super(JWSAlgorithm.RS512);
		this.keyStore = keyStore;
	}

	@Override
	protected RSAKey getJwk() throws GeneralSecurityException, JOSEException {
		Enumeration<String> aliases = keyStore.aliases();
		if (!aliases.hasMoreElements()) {
			throw new IllegalStateException("Keystore contains no aliases");
		}
		String alias = aliases.nextElement();

		X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
		if (!(certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
			throw new IllegalStateException("Certificate is not RSA");
		}

		RSAPrivateKey privateKey = CommonsPkiUtil.getRsaPrivateKey(keyStore);
		return new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(alias)
				.build();
	}

	@Override
	protected JWSSigner getSigner(RSAKey key) throws JOSEException {
		return new RSASSASigner(key.toRSAPrivateKey());
	}
}
