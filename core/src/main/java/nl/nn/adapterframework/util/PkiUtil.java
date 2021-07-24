/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

public class PkiUtil {
	private static Logger log = LogUtil.getLogger(MethodHandles.lookup().lookupClass());

	public static KeyManager[] createKeyManagers(final KeyStore keystore, final String password, String algorithm) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing key manager");
		if (StringUtils.isEmpty(algorithm)) {
			algorithm=KeyManagerFactory.getDefaultAlgorithm();
			log.debug("using default KeyManager algorithm ["+algorithm+"]");
		} else {
			log.debug("using configured KeyManager algorithm ["+algorithm+"]");
		}
		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(algorithm);
		kmfactory.init(keystore, password != null ? password.toCharArray(): null);
		return kmfactory.getKeyManagers(); 
	}

	public static TrustManager[] createTrustManagers(final KeyStore keystore, String algorithm) throws KeyStoreException, NoSuchAlgorithmException { 
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing trust manager");
		if (StringUtils.isEmpty(algorithm)) {
			algorithm=TrustManagerFactory.getDefaultAlgorithm();
			log.debug("using default TrustManager algorithm ["+algorithm+"]");
		} else {
			log.debug("using configured TrustManager algorithm ["+algorithm+"]");
		}
		TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(algorithm);
		tmfactory.init(keystore);
		TrustManager[] trustmanagers = tmfactory.getTrustManagers();
		return trustmanagers; 
	}

	public static KeyStore createKeyStore(final URL url, final String password, String keyStoreType, String prefix) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		if (url == null) {
			throw new IllegalArgumentException("Keystore url for "+prefix+" may not be null");
		}
		log.info("Initializing keystore for "+prefix+" from "+url.toString());
		KeyStore keystore  = KeyStore.getInstance(keyStoreType);
		keystore.load(url.openStream(), password != null ? password.toCharArray(): null);
		if (log.isInfoEnabled()) {
			Enumeration<String> aliases = keystore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = (String)aliases.nextElement();
				log.info(prefix+" alias [" + alias + "]:");
				Certificate trustedcert = keystore.getCertificate(alias);
				if (trustedcert != null && trustedcert instanceof X509Certificate) {
					X509Certificate cert = (X509Certificate)trustedcert;
					log.info("  Subject DN: " + cert.getSubjectDN());
					log.info("  Signature Algorithm: " + cert.getSigAlgName());
					log.info("  Valid from: " + cert.getNotBefore() );
					log.info("  Valid until: " + cert.getNotAfter());
					log.info("  Issuer: " + cert.getIssuerDN());
				}
			}
		}
		return keystore;
	}

	public static byte[] loadPEM(URL resource) throws IOException {
		InputStream in = resource.openStream();
		String pem = StreamUtil.streamToString(in, null, "ISO_8859_1");
		Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
		String encoded = parse.matcher(pem).replaceFirst("$1");
		return Base64.decodeBase64(encoded);
	}
	
	public static PrivateKey getPrivateKeyFromPem(URL resource) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		KeyFactory kf = KeyFactory.getInstance("RSA");
		byte[] pkcs8EncodedKeySpec = loadPEM(resource);
		return kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKeySpec));
	}

	public static Certificate getCertificateFromPem(URL resource) throws CertificateException, IOException {
		CertificateFactory fact = CertificateFactory.getInstance("X.509");
		return fact.generateCertificate(resource.openStream());
	}
}
