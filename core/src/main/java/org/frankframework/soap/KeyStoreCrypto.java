/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.soap;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;

import org.apache.wss4j.common.crypto.CryptoBase;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AppConstants;

/**
 * A WSS4J Crypto implementation based on a Java KeyStore.
 * <br />
 * WSS4J does not come with a method to use a KeyStore.
 * There is a property based WSS4J Merlin implementation, but that does not suit the
 * configuration based approach we use in the Frank!Framework.
 *
 * This way the {@code keystore} element can be used to provided a certificate.
 *
 * NOTE: When no TrustStore is provided the default (CACERTS) is used.
 *
 * The KeyStore is used for signing and encrypting SOAP Messages, the TrustStore is used to validate the CA Chains.
 * The default should suffice but it's configurable for those who have their own Certificate Authority.
 *
 * @author Niels Meijer
 */
@Log4j2
@SuppressWarnings("java:S1141") // Suppress 'Try-catch blocks should not be nested'.
public class KeyStoreCrypto extends CryptoBase {
	@SuppressWarnings("java:S2068") // Suppress 'Hard-coded credentials are security-sensitive'
	private static final String CA_CERTS_PASSWORD = AppConstants.getInstance().getProperty("cacerts.password");
	private static final String CA_CERTS_PATH = AppConstants.getInstance().getProperty("cacerts.location");

	protected KeyStore keystore;
	protected KeyStore truststore;

	public KeyStoreCrypto(KeyStore keystore) {
		this(keystore, getDefaultTruststore());
	}

	private static KeyStore getDefaultTruststore() {
		try (InputStream cacertsIs = Files.newInputStream(Paths.get(CA_CERTS_PATH))) {
			KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
			truststore.load(cacertsIs, CA_CERTS_PASSWORD.toCharArray());

			return truststore;
		} catch (Exception e) {
			log.warn("CA certs could not be loaded", e);
		}

		return null;
	}

	public KeyStoreCrypto(KeyStore keystore, KeyStore truststore) {
		if (keystore == null) {
			throw new IllegalStateException("The keystore is null");
		}

		this.keystore = keystore;
		this.truststore = truststore;
	}

	/**
	 * Singleton certificate factory for this Crypto instance.
	 * <p/>
	 *
	 * @return Returns a <code>CertificateFactory</code> to construct
	 *         X509 certificates
	 * @throws WSSecurityException
	 */
	@Override
	public CertificateFactory getCertificateFactory() throws WSSecurityException {
		if (certificateFactory != null) {
			return certificateFactory;
		}

		String provider = getCryptoProvider();
		String keyStoreProvider = null;
		if (keystore != null) {
			keyStoreProvider = keystore.getProvider().getName();
		}

		try {
			if (provider == null || provider.length() == 0) {
				if (keyStoreProvider != null && keyStoreProvider.length() != 0) {
					try {
						certificateFactory = CertificateFactory.getInstance("X.509", mapKeystoreProviderToCertProvider(keyStoreProvider));
					} catch (Exception ex) {
						log.debug("The keystore provider '" + keyStoreProvider + "' does not support X.509 because \"" + ex
								.getMessage() + "\". The JVM default provider will be tried out next", ex);
						// Ignore, we'll just use the default since they didn't specify one.
						// Hopefully that will work for them.
					}
				}
				if (certificateFactory == null) {
					certificateFactory = CertificateFactory.getInstance("X.509");
				}
			} else {
				certificateFactory = CertificateFactory.getInstance("X.509", provider);
			}
		} catch (CertificateException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "unsupportedCertType");
		} catch (NoSuchProviderException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "noSecProvider");
		}

		return certificateFactory;
	}

	private String mapKeystoreProviderToCertProvider(String s) {
		if ("SunJSSE".equals(s)) {
			return "SUN";
		}
		return s;
	}

	/**
	 * Retrieves the identifier name of the default certificate. This should be the certificate
	 * that is used for signature and encryption. This identifier corresponds to the certificate
	 * that should be used whenever KeyInfo is not present in a signed or an encrypted
	 * message. May return null. The identifier is implementation specific, e.g. it could be the
	 * KeyStore alias.
	 *
	 * @return name of the default X509 certificate.
	 */
	@Override
	public String getDefaultX509Identifier() throws WSSecurityException {
		if (super.getDefaultX509Identifier() != null) {
			return super.getDefaultX509Identifier();
		}

		if (keystore != null) {
			try {
				Enumeration<String> as = keystore.aliases();
				if (as.hasMoreElements()) {
					String alias = as.nextElement();
					if (!as.hasMoreElements()) {
						setDefaultX509Identifier(alias);
						return alias;
					}
				}
			} catch (KeyStoreException ex) {
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "keystore");
			}
		}
		return null;
	}

	//
	// Keystore-specific Crypto functionality methods
	//

	/**
	 * Get an X509Certificate (chain) corresponding to the CryptoType argument. The supported
	 * types are as follows:
	 *
	 * TYPE.ISSUER_SERIAL - A certificate (chain) is located by the issuer name and serial number
	 * TYPE.THUMBPRINT_SHA1 - A certificate (chain) is located by the SHA1 of the (root) cert
	 * TYPE.SKI_BYTES - A certificate (chain) is located by the SKI bytes of the (root) cert
	 * TYPE.SUBJECT_DN - A certificate (chain) is located by the Subject DN of the (root) cert
	 * TYPE.ALIAS - A certificate (chain) is located by an alias, which for this implementation
	 * means an alias of the keystore or truststore.
	 */
	@Override
	public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
		if (cryptoType == null) {
			return new X509Certificate[0];
		}

		return switch (cryptoType.getType()) {
			case ISSUER_SERIAL -> getX509Certificates(cryptoType.getIssuer(), cryptoType.getSerial());
			case THUMBPRINT_SHA1 -> getX509Certificates(cryptoType.getBytes());
			case SKI_BYTES -> getX509CertificatesSKI(cryptoType.getBytes());
			case SUBJECT_DN -> getX509CertificatesSubjectDN(cryptoType.getSubjectDN());
			case ALIAS -> getX509Certificates(cryptoType.getAlias());
			case ENDPOINT -> new X509Certificate[0];
		};
	}

	/**
	 * Get the implementation-specific identifier corresponding to the cert parameter. In this
	 * case, the identifier corresponds to a KeyStore alias.
	 * @param cert The X509Certificate for which to search for an identifier
	 * @return the identifier corresponding to the cert parameter
	 * @throws WSSecurityException
	 */
	@Override
	public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
		String identifier = null;

		if (keystore != null) {
			identifier = getIdentifier(cert, keystore);
		}

		if (identifier == null && truststore != null) {
			identifier = getIdentifier(cert, truststore);
		}

		return identifier;
	}

	/**
	 * Gets the private key corresponding to the certificate.
	 *
	 * @param certificate The X509Certificate corresponding to the private key
	 * @param callbackHandler The callbackHandler needed to get the password
	 * @return The private key
	 */
	@Override
	public PrivateKey getPrivateKey(X509Certificate certificate, CallbackHandler callbackHandler) throws WSSecurityException {
		if (keystore == null) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { "The keystore is null" });
		}
		if (callbackHandler == null) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { "The CallbackHandler is null" });
		}

		String identifier = getIdentifier(certificate, keystore);
		if (identifier == null) {
			try {
				String msg = "Cannot find key for certificate";
				String logMsg = createKeyStoreErrorMessage(keystore);
				log.error(msg + logMsg);
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { msg });
			} catch (KeyStoreException ex) {
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "noPrivateKey", new Object[] { ex.getMessage() });
			}
		}
		String password = getPassword(identifier, callbackHandler);
		return getPrivateKey(identifier, password);
	}

	/**
	 * Gets the private key corresponding to the given PublicKey.
	 *
	 * @param publicKey The PublicKey corresponding to the private key
	 * @param callbackHandler The callbackHandler needed to get the password
	 * @return The private key
	 */
	@Override
	public PrivateKey getPrivateKey(PublicKey publicKey, CallbackHandler callbackHandler) throws WSSecurityException {
		if (keystore == null) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { "The keystore is null" });
		}
		if (callbackHandler == null) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { "The CallbackHandler is null" });
		}

		String identifier = getIdentifier(publicKey, keystore);
		if (identifier == null) {
			try {
				String msg = "Cannot find key for corresponding public key";
				String logMsg = createKeyStoreErrorMessage(keystore);
				log.error(msg + logMsg);
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { msg });
			} catch (KeyStoreException ex) {
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "noPrivateKey", new Object[] { ex.getMessage() });
			}
		}
		String password = getPassword(identifier, callbackHandler);
		return getPrivateKey(identifier, password);
	}

	/**
	 * Gets the private key corresponding to the identifier.
	 *
	 * @param identifier The implementation-specific identifier corresponding to the key
	 * @param password The password needed to get the key
	 * @return The private key
	 */
	@Override
	public PrivateKey getPrivateKey(String identifier, String password) throws WSSecurityException {
		try {
			if (identifier == null || !keystore.isKeyEntry(identifier)) {
				String msg = "Cannot find key for alias: [" + identifier + "]";
				String logMsg = createKeyStoreErrorMessage(keystore);
				log.error(msg + logMsg);
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { msg });
			}

			Key keyTmp = keystore.getKey(identifier, password == null ? new char[] {} : password.toCharArray());
			if (!(keyTmp instanceof PrivateKey)) {
				String msg = "Key is not a private key, alias: [" + identifier + "]";
				String logMsg = createKeyStoreErrorMessage(keystore);
				log.error(msg + logMsg);
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty", new Object[] { msg });
			}

			return (PrivateKey) keyTmp;
		} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException ex) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "noPrivateKey", new Object[] { ex.getMessage() });
		}
	}

	/**
	 * Evaluate whether a given certificate chain should be trusted.
	 *
	 * @param certs Certificate chain to validate
	 * @param enableRevocation whether to enable CRL verification or not
	 * @param subjectCertConstraints A set of constraints on the Subject DN of the certificates
	 *
	 * @throws WSSecurityException if the certificate chain is invalid
	 */
	private void verifyTrust(X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints) throws WSSecurityException {
		//
		// FIRST step - Search the keystore for the transmitted certificate
		//
		if (certs.length == 1 && !enableRevocation) {
			String issuerString = certs[0].getIssuerX500Principal().getName();
			BigInteger issuerSerial = certs[0].getSerialNumber();

			CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
			cryptoType.setIssuerSerial(issuerString, issuerSerial);
			X509Certificate[] foundCerts = getX509Certificates(cryptoType);

			//
			// If a certificate has been found, the certificates must be compared
			// to ensure against phony DNs (compare encoded form including signature)
			//
			if (foundCerts != null && foundCerts.length > 0 && foundCerts[0] != null && foundCerts[0].equals(certs[0])) {
				try {
					certs[0].checkValidity();
				} catch (CertificateExpiredException | CertificateNotYetValidException e) {
					throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, e, "invalidCert");
				}
				log.debug("Direct trust for certificate with {}", certs[0].getSubjectX500Principal().getName());
				return;
			}
		}

		//
		// SECOND step - Search for the issuer cert (chain) of the transmitted certificate in the
		// keystore or the truststore
		//
		List<Certificate[]> foundIssuingCertChains = null;
		String issuerString = certs[0].getIssuerX500Principal().getName();
		if (certs.length == 1) {

			Object subject = convertSubjectToPrincipal(issuerString);

			if (keystore != null) {
				log.debug("Searching keystore [{}] for cert with Subject {}", keystore, subject);
				foundIssuingCertChains = getCertificates(subject, keystore);
			}

			// If we can't find the issuer in the keystore then look at the truststore
			if ((foundIssuingCertChains == null || foundIssuingCertChains.isEmpty()) && truststore != null) {
				log.debug("Searching truststore [{}] for cert with Subject {}", truststore, subject);
				foundIssuingCertChains = getCertificates(subject, truststore);
			}

			if (foundIssuingCertChains == null || foundIssuingCertChains.isEmpty() || foundIssuingCertChains.get(0).length < 1) {
				String subjectString = certs[0].getSubjectX500Principal().getName();
				log.debug("No certs found in keystore for issuer {} of certificate for {}", issuerString, subjectString);
				throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "certpath", new Object[] { "No trusted certs found" });
			}
		}

		//
		// THIRD step
		// Check the certificate trust path for the issuer cert chain
		//
		log.debug("Preparing to validate certificate path for issuer {}", issuerString);

		try {
			Set<TrustAnchor> set = new HashSet<>();
			if (truststore != null) {
				addTrustAnchors(set, truststore);
			}

			if (keystore != null) {
				addTrustAnchors(set, keystore);
			}

			// Verify the trust path using the above settings
			String provider = getCryptoProvider();
			CertPathValidator validator = null;
			if (provider == null || provider.length() == 0) {
				validator = CertPathValidator.getInstance("PKIX");
			} else {
				validator = CertPathValidator.getInstance("PKIX", provider);
			}

			PKIXParameters param = new PKIXParameters(set);

			// Generate cert path
			if (foundIssuingCertChains != null && !foundIssuingCertChains.isEmpty()) {
				java.security.cert.CertPathValidatorException validatorException = null;
				// Try each potential issuing cert path for a match
				for (Certificate[] foundCertChain : foundIssuingCertChains) {
					X509Certificate[] x509certs = new X509Certificate[foundCertChain.length + 1];
					x509certs[0] = certs[0];
					System.arraycopy(foundCertChain, 0, x509certs, 1, foundCertChain.length);

					List<X509Certificate> certList = Arrays.asList(x509certs);
					CertPath path = getCertificateFactory().generateCertPath(certList);

					try {
						validator.validate(path, param);
						// We have a valid cert path at this point so break
						validatorException = null;
						break;
					} catch (java.security.cert.CertPathValidatorException e) {
						validatorException = e;
					}
				}

				if (validatorException != null) {
					throw validatorException;
				}
			} else {
				List<X509Certificate> certList = Arrays.asList(certs);
				CertPath path = getCertificateFactory().generateCertPath(certList);

				validator.validate(path, param);
			}
		} catch (NoSuchProviderException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException
				| java.security.cert.CertPathValidatorException | KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "certpath");
		}

		// Finally check Cert Constraints
		if (!matchesSubjectDnPattern(certs[0], subjectCertConstraints)) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
		}
	}

	@Override
	public void verifyTrust(X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
		verifyTrust(certs, enableRevocation, subjectCertConstraints);

		if (!matchesIssuerDnPattern(certs[0], issuerCertConstraints)) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
		}
	}

	/**
	 * Evaluate whether a given public key should be trusted.
	 *
	 * @param publicKey The PublicKey to be evaluated
	 * @throws WSSecurityException if the PublicKey is invalid
	 */
	@Override
	public void verifyTrust(PublicKey publicKey) throws WSSecurityException {
		//
		// If the public key is null, do not trust the signature
		//
		if (publicKey == null) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
		}

		//
		// Search the keystore for the transmitted public key (direct trust). If not found
		// then search the truststore for the transmitted public key (direct trust)
		//
		if (!findPublicKeyInKeyStore(publicKey, keystore, false) && !findPublicKeyInKeyStore(publicKey, truststore, true)) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
		}
	}

	/**
	 * Get an X509 Certificate (chain) according to a given serial number and issuer string.
	 *
	 * @param issuer The Issuer String
	 * @param serialNumber The serial number of the certificate
	 * @return an X509 Certificate (chain) corresponding to the found certificate(s)
	 * @throws WSSecurityException
	 */
	private X509Certificate[] getX509Certificates(String issuer, BigInteger serialNumber) throws WSSecurityException {
		//
		// Convert the subject DN to a java X500Principal object first. This is to ensure
		// interop with a DN constructed from .NET, where e.g. it uses "S" instead of "ST".
		// Then convert it to a BouncyCastle X509Name, which will order the attributes of
		// the DN in a particular way (see WSS-168). If the conversion to an X500Principal
		// object fails (e.g. if the DN contains "E" instead of "EMAILADDRESS"), then fall
		// back on a direct conversion to a BC X509Name
		//
		Object issuerName = null;
		try {
			X500Principal issuerRDN = new X500Principal(issuer);
			issuerName = createBCX509Name(issuerRDN.getName());
		} catch (IllegalArgumentException ex) {
			issuerName = createBCX509Name(issuer);
		}
		Certificate[] certs = null;
		if (keystore != null) {
			log.debug("Searching keystore [{}] for cert with issuer {} and serial {}", keystore, issuerName, serialNumber);
			certs = getCertificates(issuerName, serialNumber, keystore);
		}

		// If we can't find the issuer in the keystore then look at the truststore
		if ((certs == null || certs.length == 0) && truststore != null) {
			log.debug("Searching keystore [{}] for cert with issuer {} and serial {}", truststore, issuerName, serialNumber);
			certs = getCertificates(issuerName, serialNumber, truststore);
		}

		if (certs == null || certs.length == 0) {
			return new X509Certificate[0];
		}

		return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
	}

	/**
	 * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore
	 * @param issuerRDN either an X500Principal or a BouncyCastle X509Name instance.
	 * @param store The KeyStore
	 * @return an X509 Certificate (chain)
	 * @throws WSSecurityException
	 */
	private Certificate[] getCertificates(Object issuerRDN, BigInteger serialNumber, KeyStore store) throws WSSecurityException {
		log.debug("Searching {} for cert with issuer {} and serial {}", issuerRDN, serialNumber);
		try {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				Certificate[] certs = store.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate cert = store.getCertificate(alias);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}

				if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate x509cert) {
					log.debug("Keystore alias {} has issuer {} and serial {}", alias, x509cert.getIssuerX500Principal().getName(), x509cert.getSerialNumber());
					if (x509cert.getSerialNumber().compareTo(serialNumber) == 0) {
						Object certName = createBCX509Name(x509cert.getIssuerX500Principal().getName());
						if (certName.equals(issuerRDN)) {
							log.debug("Issuer Serial match found using keystore alias {}", alias);
							return certs;
						}
					}
				}
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}

		log.debug("No issuer serial match found in {}", store);
		return new Certificate[] {};
	}

	/**
	 * Get an X509 Certificate (chain) according to a given Thumbprint.
	 *
	 * @param thumbprint The SHA1 thumbprint info bytes
	 * @return the X509 Certificate (chain) that was found (can be null)
	 * @throws WSSecurityException if problems during keystore handling or wrong certificate
	 */
	private X509Certificate[] getX509Certificates(byte[] thumbprint) throws WSSecurityException {
		MessageDigest sha = null;

		try {
			sha = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "decoding.general");
		}

		Certificate[] certs = null;
		if (keystore != null) {
			log.debug("Searching keystore [{}] for cert using a SHA-1 thumbprint", keystore);
			certs = getCertificates(thumbprint, keystore, sha);
		}

		// If we can't find the issuer in the keystore then look at the truststore
		if ((certs == null || certs.length == 0) && truststore != null) {
			log.debug("Searching truststore [{}] for cert using a SHA-1 thumbprint", truststore);
			certs = getCertificates(thumbprint, truststore, sha);
		}

		if (certs == null || certs.length == 0) {
			return new X509Certificate[0];
		}

		return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
	}

	/**
	 * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore
	 * @param thumbprint
	 * @param store The KeyStore
	 * @return an X509 Certificate (chain)
	 * @throws WSSecurityException
	 */
	private Certificate[] getCertificates(byte[] thumbprint, KeyStore store, MessageDigest sha) throws WSSecurityException {
		try {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				Certificate[] certs = store.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate cert = store.getCertificate(alias);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}

				if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate x509cert) {
					try {
						sha.update(x509cert.getEncoded());
					} catch (CertificateEncodingException ex) {
						throw new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, ex, "encodeError");
					}
					byte[] data = sha.digest();

					if (Arrays.equals(data, thumbprint)) {
						log.debug("Thumbprint match found using keystore alias {}", alias);
						return certs;
					}
				}
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}

		log.debug("No thumbprint match found in {}", store);
		return new Certificate[] {};
	}

	/**
	 * Get an X509 Certificate (chain) according to a given SubjectKeyIdentifier.
	 *
	 * @param skiBytes The SKI bytes
	 * @return the X509 certificate (chain) that was found (can be null)
	 */
	private X509Certificate[] getX509CertificatesSKI(byte[] skiBytes) throws WSSecurityException {
		Certificate[] certs = null;
		if (keystore != null) {
			log.debug("Searching keystore for cert using Subject Key Identifier bytes");
			certs = getCertificates(skiBytes, keystore);
		}

		// If we can't find the issuer in the keystore then look at the truststore
		if ((certs == null || certs.length == 0) && truststore != null) {
			log.debug("Searching truststore for cert using Subject Key Identifier bytes");
			certs = getCertificates(skiBytes, truststore);
		}

		if (certs == null || certs.length == 0) {
			return new X509Certificate[0];
		}

		return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
	}

	/**
	 * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore
	 * @param skiBytes
	 * @param store The KeyStore
	 * @return an X509 Certificate (chain)
	 * @throws WSSecurityException
	 */
	private Certificate[] getCertificates(byte[] skiBytes, KeyStore store) throws WSSecurityException {
		log.debug("Searching {} for cert using Subject Key Identifier bytes", store);
		try {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				Certificate[] certs = store.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate cert = store.getCertificate(alias);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}

				if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate x509cert) {
					byte[] data = getSKIBytesFromCert(x509cert);
					if (data.length == skiBytes.length && Arrays.equals(data, skiBytes)) {
						log.debug("SKI match found using keystore alias {}", alias);
						return certs;
					}
				}
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}

		log.debug("No SKI match found in {}", store);
		return new Certificate[] {};
	}

	/**
	 * Get an X509 Certificate (chain) according to a given DN of the subject of the certificate
	 *
	 * @param subjectDN The DN of subject to look for
	 * @return An X509 Certificate (chain) with the same DN as given in the parameters
	 * @throws WSSecurityException
	 */
	private X509Certificate[] getX509CertificatesSubjectDN(String subjectDN) throws WSSecurityException {
		Object subject = convertSubjectToPrincipal(subjectDN);

		List<Certificate[]> certs = null;
		if (keystore != null) {
			certs = getCertificates(subject, keystore);
			log.debug("Searching keystore [{}] for cert with Subject {}", keystore, subject);
		}

		// If we can't find the issuer in the keystore then look at the truststore
		if ((certs == null || certs.isEmpty()) && truststore != null) {
			certs = getCertificates(subject, truststore);
			log.debug("Searching truststore [{}] for cert with Subject {}", truststore, subject);
		}

		if (certs == null || certs.isEmpty()) {
			return new X509Certificate[0];
		}

		// We just choose the first entry
		return Arrays.copyOf(certs.get(0), certs.get(0).length, X509Certificate[].class);
	}

	private Object convertSubjectToPrincipal(String subjectDN) {
		//
		// Convert the subject DN to a java X500Principal object first. This is to ensure
		// interop with a DN constructed from .NET, where e.g. it uses "S" instead of "ST".
		// Then convert it to a BouncyCastle X509Name, which will order the attributes of
		// the DN in a particular way (see WSS-168). If the conversion to an X500Principal
		// object fails (e.g. if the DN contains "E" instead of "EMAILADDRESS"), then fall
		// back on a direct conversion to a BC X509Name
		//
		try {
			X500Principal subjectRDN = new X500Principal(subjectDN);
			return createBCX509Name(subjectRDN.getName());
		} catch (IllegalArgumentException ex) {
			return createBCX509Name(subjectDN);
		}
	}

	/**
	 * Get an X509 Certificate (chain) that correspond to the identifier. For this implementation,
	 * the identifier corresponds to the KeyStore alias.
	 *
	 * @param identifier The identifier that corresponds to the returned certs
	 * @return an X509 Certificate (chain) that corresponds to the identifier
	 */
	private X509Certificate[] getX509Certificates(String identifier) throws WSSecurityException {
		if (identifier == null) {
			return new X509Certificate[0];
		}
		Certificate[] certs = null;
		try {
			if (keystore != null) {
				// There's a chance that there can only be a set of trust stores
				certs = keystore.getCertificateChain(identifier);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate cert = keystore.getCertificate(identifier);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}
			}

			if (certs == null && truststore != null) {
				// Now look into the trust stores
				certs = truststore.getCertificateChain(identifier);
				if (certs == null) {
					Certificate cert = truststore.getCertificate(identifier);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}
			}

			if (certs == null) {
				return new X509Certificate[0];
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}

		return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
	}

	/**
	 * Find the Public Key in a keystore.
	 */
	private boolean findPublicKeyInKeyStore(PublicKey publicKey, KeyStore keyStoreToSearch, boolean truststore) {
		if (keyStoreToSearch == null) {
			return false;
		}
		String keyStoreName = truststore ? "truststore" : "keystore";
		log.debug("Searching {} for public key {}", keyStoreName, publicKey);
		try {
			for (Enumeration<String> e = keyStoreToSearch.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				Certificate[] certs = keyStoreToSearch.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate cert = keyStoreToSearch.getCertificate(alias);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}

				if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate crt && publicKey.equals(crt.getPublicKey())) {
					log.debug("PublicKey match found using keystore alias {}", alias);
					return true;
				}
			}
		} catch (KeyStoreException e) {
			return false;
		}

		log.debug("No PublicKey match found in {}", keyStoreName);
		return false;
	}

	/**
	 * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore. If multiple
	 * certs match the Subject DN, then multiple cert chains are returned.
	 * @param subjectRDN either an X500Principal or a BouncyCastle X509Name instance.
	 * @param store The KeyStore
	 * @return an X509 Certificate (chain)
	 * @throws WSSecurityException
	 */
	private List<Certificate[]> getCertificates(Object subjectRDN, KeyStore store) throws WSSecurityException {
		log.debug("Searching {} for cert with Subject {}", store, subjectRDN);
		List<Certificate[]> foundCerts = new ArrayList<>();
		try {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				Certificate[] certs = store.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate cert = store.getCertificate(alias);
					if (cert != null) {
						certs = new Certificate[] { cert };
					}
				}
				if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate crt) {
					X500Principal foundRDN = crt.getSubjectX500Principal();
					Object certName = createBCX509Name(foundRDN.getName());

					if (subjectRDN.equals(certName)) {
						log.debug("Subject certificate match found using keystore alias {}", alias);
						foundCerts.add(certs);
					}
				}
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}

		if (foundCerts.isEmpty()) {
			log.debug("No Subject match found in {}", store);
		}
		return foundCerts;
	}

	private static String createKeyStoreErrorMessage(KeyStore keystore) throws KeyStoreException {
		Enumeration<String> aliases = keystore.aliases();
		StringBuilder sb = new StringBuilder(keystore.size() * 7);
		boolean firstAlias = true;
		while (aliases.hasMoreElements()) {
			if (!firstAlias) {
				sb.append(", ");
			}
			sb.append(aliases.nextElement());
			firstAlias = false;
		}

		return " in keystore of type [%s] from provider [%s] with size [%d] and aliasses [%s]".formatted(
				keystore.getType(), keystore.getProvider(), keystore.size(),  sb.toString() );
	}

	/**
	 * Adds {@code TrustAnchor}s found in the provided key store to the set.
	 * <p>
	 * When the Trust Anchors are constructed, the value of the
	 * {@link #CRYPTO_CERT_PROVIDER_HANDLES_NAME_CONSTRAINTS} property will be checked.
	 * If it has been set to {@code true}, then {@code NameConstraint}s will be added
	 * to their Trust Anchors; if unset or set to false, the Name Constraints
	 * will be nulled out on their Trust Anchors.
	 *
	 * The default Sun PKIX Path Validator does not support Name Constraints on
	 * Trust Anchors and will throw an InvalidAlgorithmParameterException if they
	 * are provided. Other implementations may also be unsafe.
	 *
	 * @param set       the set to which to add the {@code TrustAnchor}s
	 * @param keyStore  the store to search for {@code X509Certificate}s
	 */
	protected void addTrustAnchors(Set<TrustAnchor> set, KeyStore keyStore) throws KeyStoreException {
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
			if (cert != null) {
				TrustAnchor anchor = new TrustAnchor(cert, null);
				set.add(anchor);
			}
		}
	}

	/**
	 * Get an implementation-specific identifier that corresponds to the X509Certificate. In
	 * this case, the identifier is the KeyStore alias.
	 * @param cert The X509Certificate corresponding to the returned identifier
	 * @param store The KeyStore to search
	 * @return An implementation-specific identifier that corresponds to the X509Certificate
	 */
	private String getIdentifier(X509Certificate cert, KeyStore store) throws WSSecurityException {
		try {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();

				Certificate[] certs = store.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate retrievedCert = store.getCertificate(alias);
					if (retrievedCert != null) {
						certs = new Certificate[] { retrievedCert };
					}
				}

				if (certs != null && certs.length > 0 && certs[0].equals(cert)) {
					return alias;
				}
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}
		return null;
	}

	private String getIdentifier(PublicKey publicKey, KeyStore store) throws WSSecurityException {
		try {
			for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();

				Certificate[] certs = store.getCertificateChain(alias);
				if (certs == null || certs.length == 0) {
					// no cert chain, so lets check if getCertificate gives us a result.
					Certificate retrievedCert = store.getCertificate(alias);
					if (retrievedCert != null) {
						certs = new Certificate[] { retrievedCert };
					}
				}

				if (certs != null && certs.length > 0 && certs[0].getPublicKey().equals(publicKey)) {
					return alias;
				}
			}
		} catch (KeyStoreException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "keystore");
		}
		return null;
	}

	/**
	 * Get a password from the CallbackHandler
	 * @param identifier The identifier to give to the Callback
	 * @param cb The CallbackHandler
	 * @return The password retrieved from the CallbackHandler
	 * @throws WSSecurityException
	 */
	private String getPassword(String identifier, CallbackHandler cb) throws WSSecurityException {
		WSPasswordCallback pwCb = new WSPasswordCallback(identifier, WSPasswordCallback.DECRYPT);
		try {
			Callback[] callbacks = new Callback[] { pwCb };
			cb.handle(callbacks);
		} catch (IOException | UnsupportedCallbackException e) {
			throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "noPassword", new Object[] { identifier });
		}

		return pwCb.getPassword();
	}

}
