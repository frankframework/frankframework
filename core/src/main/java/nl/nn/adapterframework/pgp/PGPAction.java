package nl.nn.adapterframework.pgp;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallback;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import nl.nn.adapterframework.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is an abstraction of general pgp actions
 * such as encryption, verification, etc.
 * to be used for {@link nl.nn.adapterframework.pipes.PGPPipe}
 *
 * @author Murat Kaan Meral
 */
public abstract class PGPAction {

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
					keyringConfig.addPublicKey(IOUtils.toByteArray(new FileInputStream(s)));
				}
			}
			// Add private key
			if (secretKey != null)
				keyringConfig.addSecretKey(IOUtils.toByteArray(new FileInputStream(secretKey)));

		} catch (IOException | PGPException e) {
			throw new ConfigurationException("Unknown exception has occurred.", e);
		}
	}

	/**
	 * Runs the given action (which may be any extensions of this abstract class).
	 *
	 * @param inputStream Input for the action.
	 * @return OutputStream that contains the encrypted/plaintext based on the action.
	 * @throws Exception Any exception that can be thrown during the action.
	 */
	public abstract OutputStream run(InputStream inputStream) throws Exception;

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
