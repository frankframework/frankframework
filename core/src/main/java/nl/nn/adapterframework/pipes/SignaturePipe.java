/*
   Copyright 2020-2021 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.PkiUtil;

/**
 * 
 * @ff.parameter signature the signature to verify
 * @ff.forward failure used when verification fails
 */
public class SignaturePipe extends FixedForwardPipe {

	public final String PARAMETER_SIGNATURE="signature";
	
	public final String ALGORITHM_DEFAULT = "SHA256withRSA";


	private @Getter Action action = Action.SIGN;
	private @Getter String algorithm;
	private @Getter String provider;
	private @Getter boolean signatureBase64 = true;

	private @Getter String keystore;
	private @Getter String keystoreType="pkcs12";
	private @Getter String keystoreAuthAlias;
	private @Getter String keystorePassword;
	private @Getter String keystoreAlias;
	private @Getter String keystoreAliasAuthAlias;
	private @Getter String keystoreAliasPassword;
	private @Getter String keyManagerAlgorithm=null;

	private URL keystoreUrl = null;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private PipeForward failureForward; // forward used when verification fails
	
	private CredentialFactory keystoreCredentialFactory;
	private CredentialFactory keystoreAliasCredentialFactory;

	public enum Action {
		/** signs the input */
		SIGN,
		/** verifies a signature */
		VERIFY;
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
		keystoreUrl = ClassUtils.getResourceURL(this, getKeystore());
		if (keystoreUrl == null) {
			throw new ConfigurationException("cannot find URL for keystore resource ["+getKeystore()+"]");
		}
		log.debug("resolved keystore-URL to ["+keystoreUrl.toString()+"]");
		if (getAction() == Action.VERIFY) {
			if (getParameterList().findParameter(PARAMETER_SIGNATURE)==null) {
				throw new ConfigurationException("Parameter [" + PARAMETER_SIGNATURE + "] must be specfied for action [" + action + "]");
			}
			failureForward = findForward("failure");
			if (failureForward==null)  {
				throw new ConfigurationException("Forward [failure] must be specfied for action [" + action + "]");
			}
		}
		keystoreCredentialFactory = new CredentialFactory(getKeystoreAuthAlias(), null, getKeystorePassword());
		if (StringUtils.isNotEmpty(getKeystoreAliasAuthAlias()) || StringUtils.isNotEmpty(getKeystoreAliasPassword())) {
			keystoreAliasCredentialFactory =  new CredentialFactory(getKeystoreAliasAuthAlias(), null, getKeystoreAliasPassword());
		} else {
			keystoreAliasCredentialFactory = keystoreCredentialFactory;
		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		switch (getAction()) {
		case SIGN:
			try {
				if ("pem".equals(getKeystoreType())) {
					privateKey = PkiUtil.getPrivateKeyFromPem(keystoreUrl);
				} else {
					KeyStore keystore = PkiUtil.createKeyStore(keystoreUrl, keystoreCredentialFactory.getPassword(), keystoreType, "Keys for action ["+getAction()+"]");
					String password = keystoreAliasCredentialFactory.getPassword() != null ? keystoreAliasCredentialFactory.getPassword() : "";
					privateKey = (PrivateKey) keystore.getKey(getKeystoreAlias(), password.toCharArray());
				}
				if (privateKey==null) {
					throw new PipeStartException("No Signing Key found in alias ["+getKeystoreAlias()+"] of keystore ["+keystoreUrl+"]");
				}
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | InvalidKeySpecException e) {
				throw new PipeStartException("cannot get Private Key for signing in alias ["+getKeystoreAlias()+"] of keystore ["+keystoreUrl+"]", e);
			}
			break;
		case VERIFY:
			try {
				Certificate certificate;
				if ("pem".equals(getKeystoreType())) {
					certificate = PkiUtil.getCertificateFromPem(keystoreUrl);
				} else {
					KeyStore keystore = PkiUtil.createKeyStore(keystoreUrl, keystoreCredentialFactory.getPassword(), keystoreType, "Keys for action ["+getAction()+"]");
					TrustManager[] trustmanagers = PkiUtil.createTrustManagers(keystore, keyManagerAlgorithm);
					if (trustmanagers==null || trustmanagers.length==0) {
						throw new PipeStartException("No trustmanager for keystore ["+keystoreUrl+"]");
					}
					X509TrustManager trustManager = (X509TrustManager)trustmanagers[0];
					X509Certificate[] certificates = trustManager.getAcceptedIssuers();
					if (certificates==null || certificates.length==0) {
						throw new PipeStartException("No Verfication Key found in keystore ["+keystoreUrl+"]");
					}
					certificate = certificates[0];
				}
				publicKey = certificate.getPublicKey();
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
				throw new PipeStartException("cannot get Public Key for verification in keystore ["+keystoreUrl+"]", e);
			}
			break;
		default:
			throw new IllegalStateException("Unknown action ["+getAction()+"]");
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
					message.preserve();
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
					Message signatureMsg = Message.asMessage(pvl.getValueMap().get(PARAMETER_SIGNATURE));
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
	
	/** if true, the signature is (expected to be) base64 encoded
	 * @ff.default true
	 */
	public void setSignatureBase64(boolean signatureBase64) {
		this.signatureBase64 = signatureBase64;
	}


	/** Keystore to obtain signing key */
	public void setKeystore(String string) {
		keystore = string;
	}

	/** Type of keystore, can be pkcs12 or pem
	 * @ff.default pkcs12
	 */
	public void setKeystoreType(String string) {
		keystoreType = string;
	}

	/** Alias used to obtain keystore password */
	public void setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
	}

	/** Keystore password */
	public void setKeystorePassword(String string) {
		keystorePassword = string;
	}

	/** Alias in keystore */
	public void setKeystoreAlias(String string) {
		keystoreAlias = string;
	}

	/** Alias used to obtain keystoreAlias password 
	 * @ff default same as <code>keystoreAuthAlias</code>
	 */
	public void setKeystoreAliasAuthAlias(String string) {
		keystoreAliasAuthAlias = string;
	}

	/** KeystoreAlias password 
	 * @ff default same as <code>keystorePassword</code>
	 */
	public void setKeystoreAliasPassword(String string) {
		keystoreAliasPassword = string;
	}

	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

}
