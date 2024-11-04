/*
   Copyright 2018, 2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.hash.Algorithm;
import org.frankframework.pipes.hash.HashGenerator;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StreamUtil;

/**
 *
 * This pipe can be used to generate a hash for the given message using an algorithm. With this, you can prove the integrity of the message.
 * If you use one of the Mac-based algorithms (starting with 'Hmac'), you need a secret as well. A Mac algorithm uses a secret, combined with the algorithm
 * to create a 'hash' of a message. Only sources which have this secret are able to generate the same hash for the given message.
 * With this, you can prove integrity and authenticity of a message.
 * <p>
 *
 * @see Algorithm
 * @author Niels Meijer
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class HashPipe extends FixedForwardPipe {

	private @Getter String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter String secret = null;
	private @Getter String authAlias = null;

	private @Getter Algorithm algorithm;
	private @Getter HashEncoding hashEncoding;

	public enum HashEncoding {
		Base64, Hex
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getAlgorithm() == null) {
			setAlgorithm(Algorithm.HmacSHA256);
		}

		if (getHashEncoding() == null) {
			setHashEncoding(HashEncoding.Base64);
		}

		super.configure();

		if (algorithm.isSecretRequired() && (secret == null && !getParameterList().hasParameter("secret"))) {
			throw new ConfigurationException("When using a (h)mac based Algorithm, using a secret is mandatory");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			HashGenerator hashGenerator = HashGenerator.getInstance(algorithm, getSecretKeySpec(message, session));

			// if we need to preserve this, use: mac.update(message.asByteArray());
			try (InputStream inputStream = message.asInputStream()) {
				byte[] byteArray = new byte[1024];
				int readLength;
				while ((readLength = inputStream.read(byteArray)) != -1) {
					hashGenerator.update(byteArray, 0, readLength);
				}
			}

			String hash = hashGenerator.getResult(hashEncoding);

			return new PipeRunResult(getSuccessForward(), hash);
		} catch (IOException e) {
			throw new PipeRunException(this, "error reading input", e);
		} catch (IllegalStateException | InvalidKeyException | NoSuchAlgorithmException e) {
			throw new PipeRunException(this, "error creating hash", e);
		}
	}

	private SecretKeySpec getSecretKeySpec(Message message, PipeLineSession session) throws PipeRunException, UnsupportedEncodingException {
		if (!algorithm.isSecretRequired()) {
			return null;
		}

		try {
			ParameterList parameterList = getParameterList();
			ParameterValueList pvl = parameterList == null ? null : parameterList.getValues(message, session);
			if (pvl != null) { // at this location, it would never be useful that the parameterValue defaults to inputMessage
				ParameterValue authAliasParamValue = pvl.get("authAlias");
				if (authAliasParamValue != null) {
					authAlias = authAliasParamValue.asStringValue();
				}
				ParameterValue secretParamValue = pvl.get("secret");
				if (secretParamValue != null) {
					secret = secretParamValue.asStringValue();
				}
			}
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting authAlias", e);
		}

		CredentialFactory accessTokenCf = new CredentialFactory(authAlias, "", secret);
		String cfSecret = accessTokenCf.getPassword();

		return new SecretKeySpec(cfSecret.getBytes(getCharset()), "algorithm");
	}

	/**
	 * Hash Algorithm to use
	 *
	 * @ff.default HmacSHA256
	 */
	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	@Deprecated(forRemoval = true, since = "7.6.0")
	@ConfigurationWarning("attribute encoding has been replaced with attribute charset, default has changed from ISO8859_1 to UTF-8")
	public void setEncoding(String encoding) {
		setCharset(encoding);
	}

	/**
	 * Character set to use for converting the secret from String to bytes
	 *
	 * @ff.default UTF-8
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * Method to use for converting the hash from bytes to String
	 *
	 * @ff.default Base64
	 */
	public void setHashEncoding(HashEncoding hashEncoding) {
		this.hashEncoding = hashEncoding;
	}

	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("use attribute hashEncoding instead")
	public void setBinaryToTextEncoding(HashEncoding hashEncoding) {
		setHashEncoding(hashEncoding);
	}

	/** The secret to hash with. Only used if no parameter secret is configured. The secret is only used when there is no authAlias specified, by attribute or parameter */
	public void setSecret(String secret) {
		this.secret = secret;
	}

	/** authAlias to retrieve the secret from (password field). Only used if no parameter authAlias is configured */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
}
