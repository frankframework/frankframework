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
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import jakarta.xml.soap.SOAPException;

import org.apache.wss4j.common.ext.WSSecurityException;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.encryption.CorePkiUtil;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;

public class CryptoSoapValidator extends SoapValidator implements HasKeystore {
	private CredentialFactory certificateCf;
	private KeyStore keystore;

	private @Getter @Setter SoapUtils.KeyEncryptionAlgorithm keyEncryptionAlgorithm = SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP;
	private @Getter @Setter SoapUtils.DataEncryptionAlgorithm dataEncryptionAlgorithm = SoapUtils.DataEncryptionAlgorithm.AES_256;
	private @Getter @Setter boolean includeCertificateInMessage = true;
	private @Getter @Setter SoapUtils.KeyIdentifierType keyIdentifier = SoapUtils.KeyIdentifierType.X509_KEY_IDENTIFIER;
	private @Getter @Setter int ttl = 300;
	private @Getter @Setter SoapUtils.DigestAlgorithm digestAlgorithm = SoapUtils.DigestAlgorithm.SHA256;
	private @Getter @Setter SoapUtils.SignatureAlgorithm signatureAlgorithm = SoapUtils.SignatureAlgorithm.RSA_SHA256;

	private @Getter KeystoreConfiguration keystoreConfiguration;
	private SecretKey symmetricKey;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		certificateCf = new CredentialFactory(getKeystoreAliasAuthAlias(), null, getKeystoreAliasPassword());
		try {
			keystore = CorePkiUtil.createKeyStore(keystoreConfiguration);
		} catch (EncryptionException e) {
			throw new ConfigurationException("unable to open keystore", e);
		}

		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256);
			symmetricKey = keyGen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException("unable to generate symmetric key", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		if (responseMode) {
			try {
				PipeRunResult pipeRunResult = super.doPipe(input, session, true, messageRoot);

				Message validated = pipeRunResult.getResult();
				Message encrypted = SoapUtils.encryptMessage(validated, keystore, getKeystoreAlias(), symmetricKey, includeCertificateInMessage, keyIdentifier, digestAlgorithm, keyEncryptionAlgorithm, dataEncryptionAlgorithm, ttl);
				Message signed = SoapUtils.signMessage(encrypted, keystore, getKeystoreAlias(), certificateCf.getPassword(), includeCertificateInMessage, keyIdentifier, digestAlgorithm, signatureAlgorithm, ttl);

				pipeRunResult.setResult(signed);
				return pipeRunResult;
			} catch (SOAPException | IOException | WSSecurityException e) {
				throw new PipeRunException(this, "unable to verify and decrypt SOAP message", e);
			}
		}

		try {
			Message result = SoapUtils.verifyMessage(input, keystore, getKeystoreAlias(), certificateCf.getPassword(), false);
			Message result2 = SoapUtils.decryptMessage(result, keystore, getKeystoreAlias(), certificateCf.getPassword(), true);
			return super.doPipe(result2, session, false, messageRoot);
		} catch (SOAPException | IOException | WSSecurityException e) {
			throw new PipeRunException(this, "unable to verify and decrypt SOAP message", e);
		}
	}

	@Override
	public void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration) {
		this.keystoreConfiguration = keystoreConfiguration;
	}
}
