/*
   Copyright 2020-2022, 2025 WeAreFrank!

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.encryption.AuthSSLContextFactory;
import org.frankframework.encryption.CorePkiUtil;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 *
 * @ff.parameter signature the signature to verify
 */
@Forward(name = "failure", description = "verification has failed")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class SignaturePipe extends FixedForwardPipe implements HasKeystore {

	public static final String PARAMETER_SIGNATURE="signature";
	public static final String ALGORITHM_DEFAULT = "SHA256withRSA";


	private @Getter Action action = Action.SIGN;
	private @Getter String algorithm;
	private @Getter String provider;
	private @Getter boolean signatureBase64 = true;

	private @Getter String keystore;
	private @Getter KeystoreType keystoreType=KeystoreType.PKCS12;
	private @Getter String keystoreAuthAlias;
	private @Getter String keystorePassword;
	private @Getter String keystoreAlias;
	private @Getter String keystoreAliasAuthAlias;
	private @Getter String keystoreAliasPassword;
	private @Getter String keyManagerAlgorithm=null;

	private PrivateKey privateKey;
	private PublicKey publicKey;
	private PipeForward failureForward; // forward used when verification fails


	public enum Action {
		/** signs the input */
		SIGN,
		/** verifies a signature */
		VERIFY
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getAlgorithm())) {
			setAlgorithm(ALGORITHM_DEFAULT);
		}
		if (StringUtils.isEmpty(getKeystore())) {
			throw new ConfigurationException("keystore must be specified");
		}

		AuthSSLContextFactory.verifyKeystoreConfiguration(this, null);
		if (getAction() == Action.VERIFY) {
			if (!getParameterList().hasParameter(PARAMETER_SIGNATURE)) {
				throw new ConfigurationException("Parameter [" + PARAMETER_SIGNATURE + "] must be specfied for action [" + action + "]");
			}
			failureForward = findForward("failure");
			if (failureForward==null)  {
				throw new ConfigurationException("Forward [failure] must be specified for action [" + action + "]");
			}
		}
	}

	@Override
	public void start() {
		super.start();
		switch (getAction()) {
			case SIGN:
				try {
					privateKey = CorePkiUtil.getPrivateKey(this);
				} catch (EncryptionException e) {
					throw new LifecycleException("unable to get private key for action [" + getAction() + "]", e);
				}
				break;
			case VERIFY:
				try {
					publicKey = CorePkiUtil.getPublicKey(CorePkiUtil.keyStoreAsTrustStore(this));
				} catch (EncryptionException e) {
					throw new LifecycleException("unable to get public key for action [" + getAction() + "]", e);
				}
				break;
			default:
				throw new IllegalStateException("Unknown action [" + getAction() + "]");
		}
	}


	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			Signature dsa = StringUtils.isNotEmpty(getProvider()) ? Signature.getInstance(getAlgorithm(), getProvider()) : Signature.getInstance(getAlgorithm());
			switch (getAction()) {
				case SIGN:
					dsa.initSign(privateKey);
					break;
				case VERIFY:
					dsa.initVerify(publicKey);
					break;
				default:
					throw new IllegalStateException("Unknown action ["+getAction()+"]");
			}
			try (BufferedInputStream bufin = new BufferedInputStream(message.asInputStream())) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = bufin.read(buffer)) >= 0) {
					dsa.update(buffer, 0, len);
				}
			}
			switch (getAction()) {
				case SIGN:
					return new PipeRunResult(getSuccessForward(), isSignatureBase64() ? Base64.encodeBase64String(dsa.sign()):dsa.sign());
				case VERIFY:
					ParameterValueList pvl = getParameterList().getValues(message, session);
					Message signatureMsg = pvl.get(PARAMETER_SIGNATURE).asMessage();
					byte[] signature = isSignatureBase64() ? Base64.decodeBase64(signatureMsg.asString()):signatureMsg.asByteArray();

					boolean verified = dsa.verify(signature);
					PipeForward forward = verified ? getSuccessForward() : failureForward;

					return new PipeRunResult(forward, message);
				default:
					throw new IllegalStateException("Unknown action ["+getAction()+"]");
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException | IOException | ParameterException e) {
			throw new PipeRunException(this, "Could not execute action ["+getAction()+"]", e);
		}
	}

	/**
	 * Action to be taken when pipe is executed.
	 * @ff.default SIGN
	 */
	public void setAction(Action action) {
		this.action = action;
	}

	/**
	 * The signing algorithm
	 * @ff.default ALGORITHM_DEFAULT
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/** Cryptography provider */
	public void setProvider(String provider) {
		this.provider = provider;
	}

	/** If {@code true}, the signature is (expected to be) base64 encoded
	 * @ff.default true
	 */
	public void setSignatureBase64(boolean signatureBase64) {
		this.signatureBase64 = signatureBase64;
	}


	/** Keystore to obtain signing key
	 * @ff.mandatory
	 */
	@Override
	public void setKeystore(String string) {
		keystore = string;
	}

	/** Type of keystore, can be pkcs12 or pem
	 * @ff.default pkcs12
	 */
	@Override
	public void setKeystoreType(KeystoreType value) {
		keystoreType = value;
	}

	/** Alias used to obtain keystore password */
	@Override
	public void setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
	}

	/** Keystore password */
	@Override
	public void setKeystorePassword(String string) {
		keystorePassword = string;
	}

	/** Alias in keystore */
	@Override
	public void setKeystoreAlias(String string) {
		keystoreAlias = string;
	}

	/** Alias used to obtain keystoreAlias password
	 * @ff default same as {@code keystoreAuthAlias}
	 */
	@Override
	public void setKeystoreAliasAuthAlias(String string) {
		keystoreAliasAuthAlias = string;
	}

	/** KeystoreAlias password
	 * @ff default same as <code>keystorePassword</code>
	 */
	@Override
	public void setKeystoreAliasPassword(String string) {
		keystoreAliasPassword = string;
	}

	@Override
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

}
