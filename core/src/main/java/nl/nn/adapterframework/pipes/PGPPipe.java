/*
   Copyright 2020 Integration Partners

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

import java.io.OutputStream;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pgp.Decrypt;
import nl.nn.adapterframework.pgp.Encrypt;
import nl.nn.adapterframework.pgp.PGPAction;
import nl.nn.adapterframework.pgp.Sign;
import nl.nn.adapterframework.pgp.Verify;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingPipe;

/**
 * <p>Performs various PGP (Pretty Good Privacy) actions such as Encrypt, Sign, Decrypt, Verify.</p>
 * <p>To use this pipe action parameter has to be set to one of the actions above.</p>
 * <p>
 * <br/><strong>Encrypt:</strong>
 * <p>
 * Requires the publicKey to be set to recipients public key,
 * and recipients to be set to recipients email addresses.
 * </p>
 * <p>
 * <br/><strong>Sign:</strong>
 * <p>
 * On top of the requirements for <i>Encrypt</i> action,
 * signing requires senders to bet set for user's email;
 * and secretKey & secretPassword to be set to private key's path and it's password
 * (password is optional, if private key does not have protection).
 * </p>
 * <p>
 * <br/><strong>Decrypt:</strong>
 * <p>
 * Requires secretKey and secretPassword to bet set to private key's path and it's password.
 * Just like signing, password is not required, if private key does not have protection.
 * </p>
 * <p>
 * <br/><strong>Verify:</strong>
 * <p>
 * On top of the requirements for <i>Decrypt</i> action,
 * verification expects list of senders' email's and corresponding public keys.
 * However, sender emails does not have to be set, and in that case,
 * this pipe will only validate that someone signed the input.
 * </p>
 * <p>
 * <br/>
 * <p>
 * <strong>Note:</strong> When secret key is required in any of the actions,
 * the related public key should also be included in public keys.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> For fields that require multiple input
 * (such as recipients, senders and publicKey -in certain actions-)
 * you can seperate multiple values with ";" (semicolon).
 * </p>
 */
public class PGPPipe extends StreamingPipe {
	/**
	 * Action to be taken by pipe.
	 * Available Actions:
	 * <ul>
	 *     <li>Encrypt: Encrypts the given input</li>
	 *     <li>Sign: Encrypts and then signs the given input</li>
	 *     <li>Decrypt: Decrypts the given input</li>
	 *     <li>Verify: Decrypts and verifies the given input</li>
	 * </ul>
	 */
	private String action;
	/**
	 * Emails of the recipients
	 */
	private String[] recipients;
	/**
	 * Emails of the senders. This will be used to verify that all the senders have signed the given message.
	 * If not set, and the action is verify; this pipe will validate that at least one person has signed.
	 *
	 * For signing action, it needs to be set to the email that was used to generate the private key
	 * that is being used for this process.
	 */
	private String[] verificationAddresses;
	/**
	 * Path to the private key. It will be used when signing or decrypting.
	 */
	private String secretKey;
	/**
	 * Password for the private key.
	 */
	private String secretPassword;
	/**
	 * Path to the recipient's public key. It will be used for encryption and verification.
	 */
	private String[] publicKeys;

	/**
	 * This is the {@link PGPAction} object that executes the desired action.
	 */
	private PGPAction pgpAction;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (action == null)
			throw new ConfigurationException("Action can not be null!");

		if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null)
			Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);

		Security.addProvider(new BouncyCastleProvider());

		switch (action.toLowerCase()) {
			case "encrypt":
				pgpAction = new Encrypt(publicKeys, recipients);
				break;
			case "decrypt":
				pgpAction = new Decrypt(secretKey, secretPassword);
				break;
			case "sign":
				if(verificationAddresses == null || verificationAddresses.length == 0)
					throw new ConfigurationException("During signing action, senders has to be set.");
				pgpAction = new Sign(publicKeys, secretKey, secretPassword, recipients, verificationAddresses[0]);
				break;
			case "verify":
				pgpAction = new Verify(publicKeys, secretKey, secretPassword, verificationAddresses);
				break;
			default:
				throw new ConfigurationException("Unknown action. Action has to be set to one of [Encrypt, Decrypt, Sign, Verify]");
		}
		pgpAction.configure();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try (MessageOutputStream target=getTargetStream(session)) {
			try (OutputStream out = target.asStream()) {
				pgpAction.run(message.asInputStream(), out);
			}
			return target.getPipeRunResult();
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception was thrown during PGPPipe execution.", e);
		}
	}

	@IbisDoc({"Action to be taken when pipe is executed. It can be one of the followed: Encrypt (encrypts the input), Sign (Encrypts and Signs the input), Decrypt (Decrypts the input), Verify (Decrypts and verifies the input)"})
	public void setAction(String action) {
		this.action = action;
	}

	@IbisDoc({"Recipients to be used during encryption stage. If multiple, separate with ';' (semicolon)"})
	public void setRecipients(String recipients) {
		this.recipients = split(recipients);
	}

	@IbisDoc({"Emails of the senders. This will be used to verify that all the senders have signed the given message. " +
			"If not set, and the action is verify; this pipe will validate that at least one person has signed. " +
			"For signing action, it needs to be set to the email that was used to generate the private key " +
			"that is being used for this process."})
	public void setVerificationAddresses(String verificationAddresses) {
		this.verificationAddresses = split(verificationAddresses);
	}

	@IbisDoc({"Path to the private key. It will be used when signing or decrypting."})
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	@IbisDoc({"Password for the private key."})
	public void setSecretPassword(String secretPassword) {
		this.secretPassword = secretPassword;
	}

	@IbisDoc({"Path to the recipient's public key. It will be used for encryption and verification."})
	public void setPublicKeys(String publicKeys) {
		this.publicKeys = split(publicKeys);
	}

	/**
	 * Removes the spaces near semicolons, and then splits the string with semicolons.
	 *
	 * @param str String to be split.
	 * @return Array of strings that were split from the original string.
	 */
	private String[] split(String str) {
		if (str == null)
			return null;

		str = str.replaceAll(";\\s", ";");
		str = str.replaceAll("\\s;", ";");
		return str.split(";");
	}
}
