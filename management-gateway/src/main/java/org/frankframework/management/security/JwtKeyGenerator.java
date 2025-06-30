/*
 * Copyright 2025 WeAreFrank!
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

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.Environment;

@Log4j2
public class JwtKeyGenerator extends AbstractJwtKeyGenerator {

	public static final Curve JWT_DEFAULT_CURVE = Curve.P_384;
	public static final JWSAlgorithm JWT_DEFAULT_SIGNING_ALGORITHM = JWSAlgorithm.ES384;

	public JwtKeyGenerator() {
		try {
			ECKey key = new ECKeyGenerator(JWT_DEFAULT_CURVE).keyIDFromThumbprint(true).generate();

			String version = Environment.getModuleVersion("iaf-management-gateway");
			log.info("Initializing GeneratedJwtKeyGenerator version [{}]", version);

			jwtHeader = new JWSHeader.Builder(JWT_DEFAULT_SIGNING_ALGORITHM)
					.type(JOSEObjectType.JWT)
					.customParam("version", version)
					.keyID(key.getKeyID()).build();

			signer = new ECDSASigner(key.toECPrivateKey(), JWT_DEFAULT_CURVE);
			publicJwkSet = new JWKSet(key.toPublicJWK()).toString();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to generate JWT key", e);
		}
	}
}
