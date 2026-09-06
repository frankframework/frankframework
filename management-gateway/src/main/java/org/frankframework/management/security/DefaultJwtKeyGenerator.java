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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DefaultJwtKeyGenerator extends AbstractJwtGenerator<ECKey> {
	private static final Curve JWT_DEFAULT_CURVE = Curve.P_384;

	public DefaultJwtKeyGenerator() {
		super();
	}

	@Override
	protected ECKey getJwk() throws JOSEException {
		return new ECKeyGenerator(JWT_DEFAULT_CURVE).keyIDFromThumbprint(true).generate();
	}

	@Override
	protected JWSSigner getSigner(ECKey key) throws JOSEException {
		return new ECDSASigner(key.toECPrivateKey(), JWT_DEFAULT_CURVE);
	}
}
