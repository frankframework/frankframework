package org.frankframework.http.authentication;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.message.BasicNameValuePair;

import com.nimbusds.jose.jwk.RSAKey;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.encryption.CorePkiUtil;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.http.AbstractHttpSession;

/**
 * Oauth private key jwt credentials implementation
 */
public class PrivateKeyJwtCredentials extends AbstractOauthAuthenticator {

	private PrivateKey privateKey;
	private PublicKey publicKey;

	public PrivateKeyJwtCredentials(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);
	}

	@Override
	public void configure() throws ConfigurationException {
		try {
			PublicKey publicKey = CorePkiUtil.getPublicKey(session);
			PrivateKey privateKey = CorePkiUtil.getPrivateKey(session);

			if (publicKey == null || privateKey == null) {
				throw new ConfigurationException("PublicKey and PrivateKey are required for PrivateKeyJwtCredentials");
			}

			this.publicKey = publicKey;
			this.privateKey = privateKey;

		} catch (EncryptionException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException {
		parameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
		parameters.add(new BasicNameValuePair("client_id", session.getClientId()));
		parameters.add(new BasicNameValuePair("client_secret", session.getClientSecret()));

		// TODO figure out key builder and bearer only token
		RSAKey key = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();

	}
}
