package nl.nn.adapterframework.pipes;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.io.Streams;

import java.io.*;
import java.security.NoSuchProviderException;
import java.security.Security;

public class PGPDecryptAndVerifyPipe extends FixedForwardPipe {
	private String recipient, keyPassword, publicKeyPath, privateKeyPath;
	private String[] senders;
	private KeyringConfig keyringConfig;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if(recipient == null || keyPassword == null || publicKeyPath == null || privateKeyPath == null) {
			throw new ConfigurationException("Fields [recipient, keyPassword, publicKeyPath, privateKeyPath] should be filled.");
		}

		File publicFile = new File(publicKeyPath);
		if (!publicFile.exists() || !publicFile.isFile())
			throw new ConfigurationException("Given public key file does not exist.");

		File privateFile = new File(privateKeyPath);
		if (!privateFile.exists() || !privateFile.isFile())
			throw new ConfigurationException("Given private key file does not exist.");


		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
			Security.addProvider(new BouncyCastleProvider());

		keyringConfig = KeyringConfigs.withKeyRingsFromFiles(publicFile, privateFile,
				KeyringConfigCallbacks.withPassword(keyPassword));
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		OutputStream out = new ByteArrayOutputStream();
		Message message = new Message(input);

		try {
			InputStream inputStream;
			if (senders == null) {
				inputStream = BouncyGPG
						.decryptAndVerifyStream()
						.withConfig(keyringConfig)
						.andIgnoreSignatures()
						.fromEncryptedInputStream(message.asInputStream());
			} else {
				inputStream = BouncyGPG
						.decryptAndVerifyStream()
						.withConfig(keyringConfig)
						.andRequireSignatureFromAllKeys(senders)
						.fromEncryptedInputStream(message.asInputStream());
			}

			Streams.pipeAll(inputStream, out);
			inputStream.close();
		} catch (PGPException | NoSuchProviderException | IOException e) {
			throw new PipeRunException(this, "Encryption process failed.", e);
		}
		return new PipeRunResult(findForward("success"), out);
	}

	public void setSenders(String senders) {
		if (senders != null) {
			senders = senders.replaceAll("\\s", "");
			this.senders = senders.split(",");
		}
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public void setPublicKeyPath(String publicKeyPath) {
		this.publicKeyPath = publicKeyPath;
	}

	public void setPrivateKeyPath(String privateKeyPath) {
		this.privateKeyPath = privateKeyPath;
	}
}
