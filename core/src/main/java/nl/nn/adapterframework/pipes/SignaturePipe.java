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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.PkiUtil;

public class SignaturePipe extends FixedForwardPipe {

	public final String ACTION_SIGN="sign";
	public final String ACTION_VERIFY="verify";
	public final String PARAMETER_SIGNATURE="signature";
	
	public final String ALGORITHM_DEFAULT = "SHA256withRSA";

	public final String[] ACTIONS= {ACTION_SIGN, ACTION_VERIFY};
	private Set<String> actions = new LinkedHashSet<String>(Arrays.asList(ACTIONS));

	private @Getter String action = ACTION_SIGN;
	private @Getter String algorithm;
	private @Getter String provider;
	private @Getter boolean signatureBase64 = true;

	private @Getter String keystore;
	private @Getter String keystoreType="pkcs12";
	private @Getter String keystoreAlias;
	private @Getter String keystoreAuthAlias;
	private @Getter String keystorePassword;
	private @Getter String keyManagerAlgorithm=null;

	private URL keystoreUrl = null;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private PipeForward failureForward; // forward used when verification fails
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (!actions.contains(getAction())) {
			throw new ConfigurationException("unknown or invalid action [" + action + "] supported actions are " + actions.toString() + "");
		}
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
		if (getAction().equals(ACTION_VERIFY)) {
			if (getParameterList().findParameter(PARAMETER_SIGNATURE)==null) {
				throw new ConfigurationException("Parameter [" + PARAMETER_SIGNATURE + "] must be specfied for action [" + action + "]");
			}
			failureForward = findForward("failure");
			if (failureForward==null)  {
				throw new ConfigurationException("Forward [failure] must be specfied for action [" + action + "]");
			}
		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		CredentialFactory credentialFactory = new CredentialFactory(getKeystoreAuthAlias(), null, getKeystorePassword());
		if (getAction().equals(ACTION_SIGN)) {
			try {
				if ("pem".equals(getKeystoreType())) {
					privateKey = PkiUtil.getPrivateKeyFromPem(keystoreUrl);
				} else {
					KeyStore keystore = PkiUtil.createKeyStore(keystoreUrl, credentialFactory.getPassword(), keystoreType, "Keys for action ["+getAction()+"]");
					KeyManager[] keymanagers = PkiUtil.createKeyManagers(keystore, credentialFactory.getPassword(), keyManagerAlgorithm);
					if (keymanagers==null || keymanagers.length==0) {
						throw new PipeStartException("No keymanager found for keystore ["+keystoreUrl+"]");
					}
					X509KeyManager keyManager = (X509KeyManager)keymanagers[0];
					privateKey = keyManager.getPrivateKey(getKeystoreAlias());
				}
				if (privateKey==null) {
					throw new PipeStartException("No Signing Key found in alias ["+getKeystoreAlias()+"] of keystore ["+keystoreUrl+"]");
				}
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | InvalidKeySpecException e) {
				throw new PipeStartException("cannot get Private Key for signing in alias ["+getKeystoreAlias()+"] of keystore ["+keystoreUrl+"]", e);
			}
		} else {
			try {
				Certificate certificate;
				if ("pem".equals(getKeystoreType())) {
					certificate = PkiUtil.getCertificateFromPem(keystoreUrl);
				} else {
					KeyStore keystore = PkiUtil.createKeyStore(keystoreUrl, credentialFactory.getPassword(), keystoreType, "Keys for action ["+getAction()+"]");
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
		}
	}


	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			Signature dsa = StringUtils.isNotEmpty(getProvider()) ? Signature.getInstance(getAlgorithm(), getProvider()) : Signature.getInstance(getAlgorithm());
			if (getAction().equals(ACTION_SIGN)) {
				dsa.initSign(privateKey);
			} else {
				dsa.initVerify(publicKey);
				message.preserve();
			}
			try (BufferedInputStream bufin = new BufferedInputStream(message.asInputStream())) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = bufin.read(buffer)) >= 0) {
					dsa.update(buffer, 0, len);
				}
			}
			if (getAction().equals(ACTION_SIGN)) {
				return new PipeRunResult(getSuccessForward(), isSignatureBase64() ? Base64.encodeBase64String(dsa.sign()):dsa.sign());
			} else {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				Message signatureMsg = Message.asMessage(pvl.getValueMap().get(PARAMETER_SIGNATURE));
				byte[] signature = isSignatureBase64() ? Base64.decodeBase64(signatureMsg.asString()):signatureMsg.asByteArray();
				
				boolean verified = dsa.verify(signature);
				PipeForward forward = verified ? getSuccessForward() : failureForward;
				
				return new PipeRunResult(forward, message);
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException | IOException | ParameterException e) {
			throw new PipeRunException(this, "Could not execute action ["+getAction()+"]", e);
		}
	}

	@IbisDoc({"1", "Action to be taken when pipe is executed. It can be one of the followed: sign (Signs the input), verify (verifies a signature)", "sign"})
	public void setAction(String action) {
		this.action = action;
	}

	@IbisDoc({"2", "The signing algorithm", ALGORITHM_DEFAULT})
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	@IbisDoc({"3", ""})
	public void setProvider(String provider) {
		this.provider = provider;
	}
	
	@IbisDoc({"4", "if true, the signature is (expected to be) base64 encoded", "true"})
	public void setSignatureBase64(boolean signatureBase64) {
		this.signatureBase64 = signatureBase64;
	}


	@IbisDoc({"10", "Keystore to obtain signing key", ""})
	public void setKeystore(String string) {
		keystore = string;
	}

	@IbisDoc({"11", "Type of keystore, can be pkcs12 or pem", "pkcs12"})
	public void setKeystoreType(String string) {
		keystoreType = string;
	}

	@IbisDoc({"12", "Alias used to obtain keystore password"})
	public void setKeystoreAuthAlias(String string) {
		keystoreAuthAlias = string;
	}

	@IbisDoc({"13", "Keystore password"})
	public void setKeystorePassword(String string) {
		keystorePassword = string;
	}

	@IbisDoc({"14", "Alias in keystore", ""})
	public void setKeystoreAlias(String string) {
		keystoreAlias = string;
	}

	@IbisDoc({"15", "", " "})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}

}
