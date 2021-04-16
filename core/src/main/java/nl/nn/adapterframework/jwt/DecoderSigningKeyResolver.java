package nl.nn.adapterframework.jwt;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.PkiUtil;

public class DecoderSigningKeyResolver extends SigningKeyResolverAdapter {
	private @Getter @Setter String secret;
	private @Getter @Setter String authAlias;
	private @Getter @Setter URL truststoreUrl;
	private @Getter @Setter String truststoreType="pkcs12";
	private @Getter @Setter String truststoreAlias;
	private @Getter @Setter String truststorePassword;
	private @Getter @Setter String truststoreAuthAlias;
	private @Getter @Setter String trustManagerAlgorithm;
	
	private @Getter SecretKey secretKey;
	private @Getter PublicKey publicKey;
	
	public void configure() {
		if (secret!=null || authAlias!=null) {
			// set the secret key used for HMAC requests only if the secret is set;
			CredentialFactory cf = new CredentialFactory(secret, "", authAlias);
			String cfSecret = cf.getPassword();
			secretKey = Keys.hmacShaKeyFor(cfSecret.getBytes(StandardCharsets.UTF_8));
		}
		if (getTruststoreUrl()!=null) {
			try {
				Certificate certificate;
				if ("pem".equals(truststoreType)) {
					certificate = PkiUtil.getCertificateFromPem(getTruststoreUrl());
				} else {
					KeyStore keystore = PkiUtil.createKeyStore(getTruststoreUrl(), truststorePassword, truststoreType, "Keys for verifying tokens");
					certificate = keystore.getCertificate(truststoreAlias);
				}
				publicKey = certificate.getPublicKey();
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
				//throw new PipeStartException("cannot get Public Key for verification in keystore ["+keystoreUrl+"]", e);
			}
		}
	}
	
	@Override
	public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
		//inspect the header or claims, lookup and return the signing key
		// HMAC algorithms uses secrets, other algorithms use certificates
		SignatureAlgorithm algorithm = SignatureAlgorithm.forName(jwsHeader.getAlgorithm());
		
		if (algorithm.isHmac()) {
			return secretKey;
		} else if (algorithm.isRsa() || algorithm.isEllipticCurve()) {
			return publicKey;
		}
		return null;
	}
}