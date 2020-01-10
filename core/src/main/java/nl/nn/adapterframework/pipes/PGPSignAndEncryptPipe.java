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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;

public class PGPSignAndEncryptPipe extends FixedForwardPipe {
	private String sender, recipient, keyPassword, publicKeyPath, privateKeyPath;
	private KeyringConfig keyringConfig;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if(recipient == null || keyPassword == null || publicKeyPath == null || privateKeyPath == null) {
			throw new ConfigurationException("Fields [recipient, keyPassword, publicKeyPath, privateKeyPath] should be filled.");
		}

		File publicFile = new File(publicKeyPath);
		System.out.println(publicFile.getAbsolutePath());
		if (!publicFile.exists() || !publicFile.isFile())
			throw new ConfigurationException("Given public key file does not exist.");

		File privateFile = new File(privateKeyPath);
		System.out.println(privateFile.getAbsolutePath());
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
			OutputStream outputStream;
			if (sender == null) {
				outputStream = BouncyGPG
						.encryptToStream()
						.withConfig(keyringConfig)
						.withStrongAlgorithms()
						.toRecipient(recipient)
						.andDoNotSign()
						.armorAsciiOutput()
						.andWriteTo(out);
			} else {
				outputStream = BouncyGPG
						.encryptToStream()
						.withConfig(keyringConfig)
						.withStrongAlgorithms()
						.toRecipient(recipient)
						.andDoNotSign()
						.armorAsciiOutput()
						.andWriteTo(out);
			}


			Streams.pipeAll(message.asInputStream(), outputStream);
			outputStream.close();
		} catch (PGPException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new PipeRunException(this, "Encryption process failed.", e);
		}
		return new PipeRunResult(findForward("success"), out);
	}

	public void setSender(String sender) {
		this.sender = sender;
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
