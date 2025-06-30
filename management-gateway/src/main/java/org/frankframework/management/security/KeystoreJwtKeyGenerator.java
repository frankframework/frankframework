package org.frankframework.management.security;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import org.frankframework.management.gateway.MtlsHelper;

public class KeystoreJwtKeyGenerator extends AbstractJwtKeyGenerator {

	private final MtlsHelper mtlsHelper;

	protected KeystoreJwtKeyGenerator() {
		this.mtlsHelper = new MtlsHelper();
		try {
			KeyStore keyStore = mtlsHelper.getKeyStore();
			Enumeration<String> aliases = keyStore.aliases();
			if (!aliases.hasMoreElements()) {
				throw new IllegalStateException("Keystore contains no aliases");
			}
			String alias = aliases.nextElement();

			X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
			if (!(certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
				throw new IllegalStateException("Certificate is not RSA");
			}

			RSAPrivateKey privateKey = (RSAPrivateKey) mtlsHelper.getPrivateKey();

			RSAKey rsaKey = new RSAKey.Builder(publicKey)
					.privateKey(privateKey)
					.keyID(alias)
					.build();

			jwtHeader = new JWSHeader.Builder(JWSAlgorithm.RS512)
					.type(JOSEObjectType.JWT)
					.keyID(rsaKey.getKeyID())
					.build();

			signer = new RSASSASigner(privateKey);
			publicJwkSet = new JWKSet(rsaKey.toPublicJWK()).toString();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to init KeystoreJwtKeyGenerator from RSA keystore", e);
		}
	}

}
