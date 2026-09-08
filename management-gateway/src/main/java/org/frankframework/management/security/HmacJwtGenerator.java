/*
 * Copyright 2026 WeAreFrank!
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

import org.jspecify.annotations.Nullable;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class HmacJwtGenerator extends AbstractJwtGenerator<OctetSequenceKey> {
	private final byte[] secret;

	public HmacJwtGenerator() {
		this(null);
	}

	public HmacJwtGenerator(byte @Nullable [] secret) {
		super(JWSAlgorithm.HS512);
		this.secret = secret;
	}


	@Override
	protected String createJwkSet(OctetSequenceKey jwk) {
		return null;
	}

	@Override
	protected OctetSequenceKey getJwk() throws JOSEException {
		return new OctetSequenceKeyGenerator(512).generate();
	}

	@Override
	protected JWSSigner getSigner(OctetSequenceKey key) throws JOSEException {
		return (secret == null) ? new MACSigner(key) : new MACSigner(secret);
	}
}
