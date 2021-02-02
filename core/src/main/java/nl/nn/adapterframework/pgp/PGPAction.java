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
package nl.nn.adapterframework.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPException;

import lombok.Getter;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallback;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * This is an abstraction of general pgp actions
 * such as encryption, verification, etc.
 * to be used for {@link nl.nn.adapterframework.pipes.PGPPipe}
 *
 * @author Murat Kaan Meral
 */
public abstract class PGPAction implements IScopeProvider {
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	protected InMemoryKeyring keyringConfig;
	private String[] publicKeys;
	private String secretKey, secretPassword;

	/**
	 * A general constructor. It checks if any of the vars are null.
	 *
	 * @param publicKey      Array of path strings that point to public keys.
	 * @param secretKey      String that contains the path to the secret key.
	 * @param secretPassword String that contains the password for the secret key.
	 * @param vars           Objects that should not be null for the action to be performed.
	 * @throws ConfigurationException When any of the given vars is null.
	 */
	PGPAction(String[] publicKey, String secretKey, String secretPassword, Object... vars) throws ConfigurationException {
		verifyNotNull(vars);
		this.publicKeys = publicKey;
		this.secretPassword = secretPassword;
		this.secretKey = secretKey;
	}

	/**
	 * Generates a keyring configuration with public keys and the private key.
	 *
	 * @throws ConfigurationException When the files do not exist, or unexpected PGP exception has occurred.
	 */
	public void configure() throws ConfigurationException {
		try {
			// Create configuration
			KeyringConfigCallback callback = KeyringConfigCallbacks.withUnprotectedKeys();
			if (secretPassword != null)
				callback = KeyringConfigCallbacks.withPassword(secretPassword);

			keyringConfig = KeyringConfigs.forGpgExportedKeys(callback);

			// Add public keys
			if (publicKeys != null) {
				for (String s : publicKeys) {
					URL url = ClassUtils.getResourceURL(this, s);
					keyringConfig.addPublicKey(IOUtils.toByteArray(url.openStream()));
				}
			}

			// Add private key
			if (secretKey != null) {
				URL url = ClassUtils.getResourceURL(this, secretKey);
				keyringConfig.addSecretKey(IOUtils.toByteArray(url.openStream()));
			}
		} catch (IOException | PGPException e) {
			throw new ConfigurationException("Unknown exception has occurred.", e);
		}
	}

	/**
	 * Runs the given action (which may be any extensions of this abstract class).
	 *
	 * @param inputStream Input for the action.
	 * @param outputStream to which the encrypted/plaintext based on the action is written to.
	 * @throws Exception Any exception that can be thrown during the action.
	 */
	public abstract void run(InputStream inputStream, OutputStream outputStream) throws Exception;

	/**
	 * Verifies that given parameters are not null.
	 *
	 * @param vars Parameters to be verified.
	 * @throws ConfigurationException When one of the parameters is null.
	 */
	private void verifyNotNull(Object... vars) throws ConfigurationException {
		for (Object s : vars) {
			if (s == null)
				throw new ConfigurationException("All of the required fields should be filled for " +
						"the selected action [" + this.getClass() + "]. Please check documentation for further details.");
		}
	}
}
