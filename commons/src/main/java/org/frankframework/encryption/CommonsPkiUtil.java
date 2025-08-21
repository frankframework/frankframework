/*
   Copyright 2020, 2021, 2025 WeAreFrank!

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
package org.frankframework.encryption;

import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommonsPkiUtil {

	private CommonsPkiUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static KeyManager[] createKeyManagers(final KeyStore keystore, final String password, String algorithm) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing key manager");
		if (StringUtils.isEmpty(algorithm)) {
			log.debug("using default KeyManager algorithm [{}]", algorithm);
		} else {
			log.debug("using configured KeyManager algorithm [{}]", algorithm);
		}
		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(StringUtils.isEmpty(algorithm) ? KeyManagerFactory.getDefaultAlgorithm() : algorithm);
		kmfactory.init(keystore, password != null ? password.toCharArray() : null);
		return kmfactory.getKeyManagers();
	}

	public static TrustManager[] createTrustManagers(final KeyStore keystore, String algorithm) throws KeyStoreException, NoSuchAlgorithmException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing trust manager");
		if (StringUtils.isEmpty(algorithm)) {
			log.debug("using default TrustManager algorithm [{}]", algorithm);
		} else {
			log.debug("using configured TrustManager algorithm [{}]", algorithm);
		}
		TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(StringUtils.isEmpty(algorithm) ? TrustManagerFactory.getDefaultAlgorithm() : algorithm);
		tmFactory.init(keystore);
		return tmFactory.getTrustManagers();
	}

	public static KeyStore createKeyStore(final URL url, final String password, KeystoreType keystoreType) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		if (url == null) {
			throw new IllegalArgumentException("Keystore url may not be null");
		}
		log.info("Initializing keystore from url [{}]", url);
		KeyStore keystore = KeyStore.getInstance(keystoreType.name());
		keystore.load(url.openStream(), password != null ? password.toCharArray() : null);
		if (log.isInfoEnabled()) {
			Enumeration<String> aliases = keystore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				log.info("found alias [{}]", alias);
				Certificate trustedCert = keystore.getCertificate(alias);
				if (trustedCert instanceof X509Certificate cert) {
					log.info("  Subject DN: {}", cert.getSubjectX500Principal().getName());
					log.info("  Signature Algorithm: {}", cert.getSigAlgName());
					log.info("  Valid from: {}", cert.getNotBefore());
					log.info("  Valid until: {}", cert.getNotAfter());
					log.info("  Issuer: {}", cert.getIssuerX500Principal().getName());
				}
			}
		}
		return keystore;
	}

	private static String resolveAlias(KeyStore ks) throws KeyStoreException {
		List<String> keyAliases = new ArrayList<>();
		for (String a : Collections.list(ks.aliases())) {
			if (ks.isKeyEntry(a)) {
				keyAliases.add(a);
			}
		}
		if (keyAliases.size() != 1) {
			throw new KeyStoreException("Expected exactly one key entry, found " + keyAliases.size());
		}
		return keyAliases.get(0);
	}

	public static RSAPrivateKey getRsaPrivateKey(final KeyStore keystore) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		String effectiveAlias = resolveAlias(keystore);
		log.debug("Reading RSA private key from alias [{}]", effectiveAlias);
		Key key = keystore.getKey(effectiveAlias, new char[]{});
		if (key instanceof RSAPrivateKey rsa) {
			return rsa;
		} else {
			throw new UnrecoverableKeyException("Alias [" + effectiveAlias + "] is not an RSAPrivateKey entry");
		}
	}
}
