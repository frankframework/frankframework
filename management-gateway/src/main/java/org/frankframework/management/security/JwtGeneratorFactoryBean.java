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

import java.util.Objects;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

import org.frankframework.management.switchboard.CloudAgentOutboundGateway;

public class JwtGeneratorFactoryBean implements FactoryBean<AbstractJwtKeyGenerator> {

	@Value("${management.gateway.outbound.class}")
	private String outboundClass;

	private AbstractJwtKeyGenerator jwtKeyGenerator;

	@Nonnull
	@Override
	public AbstractJwtKeyGenerator getObject() {
		if (jwtKeyGenerator == null) {
			if (Objects.equals(outboundClass, CloudAgentOutboundGateway.class.getName())) {
				jwtKeyGenerator = new KeystoreJwtKeyGenerator();
			} else {
				jwtKeyGenerator = new JwtKeyGenerator();
			}
		}
		return jwtKeyGenerator;
	}

	@Override
	public Class<?> getObjectType() {
		return (jwtKeyGenerator != null) ? jwtKeyGenerator.getClass() : AbstractJwtKeyGenerator.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
