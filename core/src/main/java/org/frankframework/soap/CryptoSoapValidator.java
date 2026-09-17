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
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import jakarta.xml.soap.SOAPException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wss4j.common.ext.WSSecurityException;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

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

/**
 * Validator that can sign, encrypt, verify and decrypt SOAP messages.
 * At least one operation is required, you may combine `sign,encrypt` and `verify,decrypt` operations.
 * <br />
 * This validator extends the {@link SoapValidator} and can be used to validate SOAP messages with additional security features.
 * If you wish to use it without validating the soap message, set the attribute {@code allowPlainXml} to true.
 *
 * @ff.info It is not possible to use this validator as a Mixed input-output validator.
 */
public class CryptoSoapValidator extends SoapValidator implements HasKeystore {
	private CredentialFactory certificateCf;
	private KeyStore keystore;

	/**
	 * Add the certificate to the signed or encrypted message.
	 */
	private @Getter @Setter boolean includeCertificateInMessage = true;

	/**
	 * Add a restriction on when the message must be read / verified by.
	 * If this time has exceeded, the message is rejected.
	 */
	private @Getter @Setter int ttl = 300;

	/**
	 * Remove the `WSSE:Security` part from the SOAP:Header. Should only be used when `VERIFY` or `DECRYPT` are used, otherwise the message will be invalid.
	 */
	private @Getter @Setter boolean removeSecurityHeader = true;

	/**
	 * {@code xenc:EncryptionMethod} encryption algorithm to use for encrypting the symmetric key.
	 */
	private @Getter @Setter SoapUtils.KeyEncryptionAlgorithm keyEncryptionAlgorithm = SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP;

	/**
	 * {@code xenc:EncryptionMethod} encryption algorithm to use for encrypting the SOAP (body) data.
	 */
	private @Getter @Setter SoapUtils.DataEncryptionAlgorithm dataEncryptionAlgorithm = SoapUtils.DataEncryptionAlgorithm.AES_256;

	/**
	 * {@code wsse:KeyIdentifier} type to detect which certificate to use to verify the SOAP message.
	 */
	private @Getter @Setter SoapUtils.KeyIdentifierType keyIdentifier = SoapUtils.KeyIdentifierType.X509_KEY_IDENTIFIER;

	/**
	 * Which {@code ds:DigestMethod} hash algorithm to use for signing the SOAP message.
	 */
	private @Getter @Setter SoapUtils.DigestAlgorithm digestAlgorithm = SoapUtils.DigestAlgorithm.SHA256;

	/**
	 * Which {@code ds:SignatureMethod} signature algorithm to use for signing the SOAP message.
	 */
	private @Getter @Setter SoapUtils.SignatureAlgorithm signatureAlgorithm = SoapUtils.SignatureAlgorithm.RSA_SHA256;

	private @Getter KeystoreConfiguration keystoreConfiguration = createKeystoreConfiguration();
	private SecretKey symmetricKey;
	private List<Operation> operations = List.of();

	public enum Operation {
		SIGN, ENCRYPT, VERIFY, DECRYPT
	}

	@Override
	public void configure() throws ConfigurationException {
		if (operations.isEmpty()) {
			throw new ConfigurationException("no operations specified");
		}
		if (isConfiguredForMixedValidation() || getResponseRootValidations() != null) {
			throw new ConfigurationException("this validator does not support input/output processing in '1' element, configure an OutputValidator explicitly");
		}
		if (keystoreConfiguration.getResource() == null) {
			throw new ConfigurationException("element [Keystore] must be specified");
		}
		if (StringUtils.isBlank(getKeystoreAlias())) {
			throw new ConfigurationException("attribute [keystoreAlias] (the name of the certificate) must be specified");
		}

		super.configure();

		certificateCf = new CredentialFactory(getKeystoreAliasAuthAlias(), null, getKeystoreAliasPassword());
		keystore = createKeyStore(keystoreConfiguration);

		if (operations.contains(Operation.ENCRYPT)) {
			try {
				KeyGenerator keyGen = KeyGenerator.getInstance("AES");
				keyGen.init(256);
				symmetricKey = keyGen.generateKey();
			} catch (NoSuchAlgorithmException e) {
				throw new ConfigurationException("unable to generate symmetric key", e);
			}
		}
	}

	/**
	 * Create a keystore based on the given FF! Keystore Element.
	 * Protected so it can be overridden in tests.
	 */
	protected KeyStore createKeyStore(KeystoreConfiguration keystoreConfiguration) throws ConfigurationException {
		try {
			return CorePkiUtil.createKeyStore(keystoreConfiguration);
		} catch (EncryptionException e) {
			throw new ConfigurationException("unable to open keystore", e);
		}
	}

	@SneakyThrows
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) {
		Message result = input;
		try {
			if (operations.contains(Operation.VERIFY)) {
				boolean removeSH = this.removeSecurityHeader && !operations.contains(Operation.DECRYPT);
				result = SoapUtils.verifyMessage(result, keystore, getKeystoreAlias(), certificateCf.getPassword(), removeSH);
			}
			if (operations.contains(Operation.DECRYPT)) {
				result = SoapUtils.decryptMessage(result, keystore, getKeystoreAlias(), certificateCf.getPassword(), removeSecurityHeader);
			}

			PipeRunResult pipeRunResult = super.doPipe(result, session, responseMode, messageRoot);

			Message validationResult = pipeRunResult.getResult();
			if (operations.contains(Operation.ENCRYPT)) {
				validationResult = SoapUtils.encryptMessage(validationResult, keystore, getKeystoreAlias(), symmetricKey, includeCertificateInMessage, keyIdentifier, digestAlgorithm, keyEncryptionAlgorithm, dataEncryptionAlgorithm, ttl);
			}
			if (operations.contains(Operation.SIGN)) {
				validationResult = SoapUtils.signMessage(validationResult, keystore, getKeystoreAlias(), certificateCf.getPassword(), includeCertificateInMessage, keyIdentifier, digestAlgorithm, signatureAlgorithm, ttl);
			}
			pipeRunResult.setResult(validationResult);

			return pipeRunResult;
		} catch (SOAPException | IOException | WSSecurityException e) {
			throw new PipeRunException(this, "unable to process SOAP crypto operations: " + operations, e);
		}
	}

	/**
	 * Which operations must be executed.
	 * <p>It is not possible to use this validator as a Mixed input-output validator.</p>
	 */
	public void setOperations(Operation... operations) {
		this.operations = List.of(operations);
	}

	@Override
	public void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration) {
		this.keystoreConfiguration = keystoreConfiguration;
	}
}
