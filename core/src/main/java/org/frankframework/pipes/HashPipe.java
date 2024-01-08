/*
   Copyright 2018, 2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StreamUtil;

/**
 * Pipe that hashes the input message.
 *
 * @author	Niels Meijer
 */
@ElementType(ElementTypes.TRANSLATOR)
public class HashPipe extends FixedForwardPipe {

	private @Getter String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private @Getter String secret = null;
	private @Getter String authAlias = null;

	private @Getter HashAlgorithm algorithm = HashAlgorithm.HmacSHA256;
	private @Getter HashEncoding hashEncoding = HashEncoding.Base64;

	public enum HashAlgorithm {
		HmacMD5, HmacSHA1, HmacSHA256, HmacSHA384, HmacSHA512;
	}
	public enum HashEncoding {
		Base64, Hex;
	}

	@Override
	public PipeRunResult doPipe (Message message, PipeLineSession session) throws PipeRunException {
		String authAlias = getAuthAlias();
		String secret = getSecret();
		try {
			ParameterList parameterList = getParameterList();
			ParameterValueList pvl = parameterList==null ? null : parameterList.getValues(message, session);
			if(pvl != null) { // at this location, it would never be useful that the parameterValue defaults to inputMessage
				ParameterValue authAliasParamValue = pvl.get("authAlias");
				if (authAliasParamValue != null) {
					authAlias = authAliasParamValue.asStringValue();
				}
				ParameterValue secretParamValue = pvl.get("secret");
				if (secretParamValue != null) {
					secret = secretParamValue.asStringValue();
				}
			}
		}
		catch (Exception e) {
			throw new PipeRunException(this, "exception extracting authAlias", e);
		}

		CredentialFactory accessTokenCf = new CredentialFactory(authAlias, "", secret);
		String cfSecret = accessTokenCf.getPassword();

		if(cfSecret == null || cfSecret.isEmpty())
			throw new PipeRunException(this, "empty secret, unable to hash");

		try {
			Mac mac = Mac.getInstance(getAlgorithm().name());

			SecretKeySpec secretkey = new SecretKeySpec(cfSecret.getBytes(getCharset()), "algorithm");
			mac.init(secretkey);

			// if we need to preserve this, use: mac.update(message.asByteArray());
			try (InputStream inputStream = message.asInputStream()) {
				byte[] byteArray = new byte[1024];
				int readLength;
				while ((readLength = inputStream.read(byteArray)) != -1) {
					mac.update(byteArray, 0, readLength);
				}
			}

			String hash = "";
			switch (getHashEncoding()) {
				case Base64:
					hash = Base64.encodeBase64String(mac.doFinal());
					break;
				case Hex:
					hash = Hex.encodeHexString(mac.doFinal());
					break;

				default: // Should never happen, as a ConfigurationException is thrown during configuration if another method is tried
					throw new PipeRunException(this, "error determining hashEncoding");
			}

			return new PipeRunResult(getSuccessForward(), hash);
		}
		catch (IOException e) {
			throw new PipeRunException(this, "error reading input", e);
		}
		catch (IllegalStateException | InvalidKeyException | NoSuchAlgorithmException e) {
			throw new PipeRunException(this, "error creating hash", e);
		}
	}

	/**
	 * Hash Algorithm to use
	 * @ff.default HmacSHA256
	 */
	public void setAlgorithm(HashAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	@Deprecated
	@ConfigurationWarning("attribute encoding has been replaced with attribute charset, default has changed from ISO8859_1 to UTF-8")
	public void setEncoding(String encoding) {
		setCharset(encoding);
	}
	/**
	 * Character set to use for converting the secret from String to bytes
	 * @ff.default UTF-8
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * Method to use for converting the hash from bytes to String
	 * @ff.default Base64
	 */
	public void setHashEncoding(HashEncoding hashEncoding) {
		this.hashEncoding = hashEncoding;
	}
	@Deprecated
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
