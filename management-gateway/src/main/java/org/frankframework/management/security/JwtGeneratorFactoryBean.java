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

import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.log4j.Log4j2;

import org.frankframework.encryption.CommonsPkiUtil;


@Log4j2
public class JwtGeneratorFactoryBean implements FactoryBean<AbstractJwtKeyGenerator> {

	@Value("${client.ssl.key-store}")
	String keyStoreLocation;
	@Value("${client.ssl.key-store-password}")
	String keyStorePassword;

	private AbstractJwtKeyGenerator jwtKeyGenerator;

	@Nonnull
	@Override
	public AbstractJwtKeyGenerator getObject() {
		if (jwtKeyGenerator == null) {
			if (keyStoreLocation != null || keyStorePassword != null) {
				try {
					KeyStore keystore = CommonsPkiUtil.createKeyStore(URI.create(keyStoreLocation).toURL(), keyStorePassword, null);
					jwtKeyGenerator = new KeystoreJwtKeyGenerator(keystore);
				} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
					log.error("Something went wrong trying to load keystore from: {}", keyStoreLocation, e);
				}
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
