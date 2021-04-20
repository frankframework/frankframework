package nl.nn.adapterframework.jwt;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.apache.commons.lang3.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.JWTPipe.Direction;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.PkiUtil;

public class JWTKeyResolver extends SigningKeyResolverAdapter {
	
	private @Getter Direction direction = Direction.ENCODE;
	private @Getter SignatureAlgorithm algorithm;
	
	private @Getter @Setter String secret;
	private @Getter @Setter String authAlias;
	
	private @Getter @Setter String keystore;
	private @Getter @Setter String keystoreType="pkcs12";
	private @Getter @Setter String keystoreAlias;
	private @Getter @Setter String keystorePassword;
	private @Getter @Setter String keystoreAuthAlias;
	private @Getter @Setter String keyManagerAlgorithm;
	
	private @Getter URL keystoreUrl;
	
	private @Getter SecretKey secretKey;
	private @Getter PrivateKey privateKey;
	private @Getter PublicKey publicKey;
	
	public void configure(IScopeProvider scopeProvider) throws ConfigurationException {
		if (StringUtils.isNotEmpty(getKeystore())) {
			keystoreUrl = ClassUtils.getResourceURL(scopeProvider, getKeystore());
			if (keystoreUrl == null) {
				throw new ConfigurationException("cannot find URL for resource ["+getKeystore()+"]");
			}
		}
		
		if (StringUtils.isEmpty(getSecret()) && StringUtils.isEmpty(getAuthAlias()) && StringUtils.isEmpty(getKeystore())) {
			throw new ConfigurationException("has neither secret nor authAlias nor keystore set");
		}
		
		if (getAlgorithm()!= null && getDirection()==Direction.ENCODE ) {
			if (getAlgorithm().isHmac() && (StringUtils.isEmpty(getSecret()) || StringUtils.isEmpty(getAuthAlias()))) {
				throw new ConfigurationException("has HMAC-algorithm [" + getAlgorithm().getValue() + "] but neither secret nor authAlias set");
			} else if ((getAlgorithm().isRsa() || getAlgorithm().isEllipticCurve()) && StringUtils.isEmpty(getKeystore())) {
				throw new ConfigurationException("has RSA/EC-algorithm [" + getAlgorithm().getValue() + "] but no keystore set");
			}
		}
	}
	
	public void start() throws PipeStartException {
		if (getSecret()!=null || getAuthAlias()!=null) {
			// set the secret key used for HMAC requests only if the secret is set;
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), "", getSecret());
			String cfSecret = cf.getPassword();
			secretKey = Keys.hmacShaKeyFor(cfSecret.getBytes(StandardCharsets.UTF_8));
		}
		if (getKeystoreUrl()!=null) {
			if (getDirection() == Direction.DECODE) {
				try {
					Certificate certificate;
					if ("pem".equals(getKeystoreType())) {
						certificate = PkiUtil.getCertificateFromPem(getKeystoreUrl());
					} else {
						CredentialFactory cf = new CredentialFactory(getKeystoreAuthAlias(), "", getKeystorePassword());
						KeyStore keystore = PkiUtil.createKeyStore(getKeystoreUrl(), cf.getPassword(), getKeystoreType(), "Keys for verifying tokens");
						certificate = keystore.getCertificate(getKeystoreAlias());
					}
					publicKey = certificate.getPublicKey();
				} catch (KeyStoreException  | NoSuchAlgorithmException | CertificateException | IOException e) {
					throw new PipeStartException("cannot get Public Key for verification in truststore ["+getKeystoreUrl()+"]", e);
				}
			} else {
				try {
					if ("pem".equals(getKeystoreType())) {
						privateKey = PkiUtil.getPrivateKeyFromPem(getKeystoreUrl());
					} else {
						CredentialFactory cf = new CredentialFactory(getKeystoreAuthAlias(), "", getKeystorePassword());
						KeyStore keystore = PkiUtil.createKeyStore(getKeystoreUrl(), cf.getPassword(), getKeystoreType(), "Keys for signing tokens");
						KeyManager[] keymanagers = PkiUtil.createKeyManagers(keystore, cf.getPassword(), getKeyManagerAlgorithm());
						if (keymanagers==null || keymanagers.length==0) {
							throw new PipeStartException("No keymanager found for keystore ["+getKeystoreUrl()+"]");
						}
						X509KeyManager keyManager = (X509KeyManager)keymanagers[0];
						privateKey = keyManager.getPrivateKey(getKeystoreAlias());
					}
				} catch (KeyStoreException  | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | InvalidKeySpecException e) {
					throw new PipeStartException("cannot get Private Key for signing in keystore ["+keystoreUrl+"]", e);
				}
			}
		}
	}
	
	@Override
	public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
		//inspect the header or claims, lookup and return the signing key
		// HMAC algorithms uses secrets, other algorithms use certificates
		SignatureAlgorithm algorithm = SignatureAlgorithm.forName(jwsHeader.getAlgorithm());
		
		if (algorithm.isHmac()) {
			return getSecretKey();
		} else if (algorithm.isRsa() || algorithm.isEllipticCurve()) {
			return getPublicKey();
		}
		return null;
	}
	
	public Key getSigningKey() {
		if (getPrivateKey() != null) {
			return getPrivateKey();
		} else if (getSecretKey() != null) {
			return getSecretKey();
		}
		return null;
	}
	
	public void setDirection(String direction) {
		this.direction = Misc.parse(Direction.class, direction);
	}
	
	public void setAlgorithm(String algorithm) {
		this.algorithm = Misc.parse(SignatureAlgorithm.class, algorithm);
	}
}