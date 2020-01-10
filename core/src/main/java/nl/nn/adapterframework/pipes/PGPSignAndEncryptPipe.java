package nl.nn.adapterframework.pipes;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildEncryptionOutputStreamAPI;
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
	private String sender, keyPassword, publicKeyPath, privateKeyPath;
	private String[] recipients;
	private KeyringConfig keyringConfig;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (recipients == null || publicKeyPath == null) {
			throw new ConfigurationException("Fields [recipients and publicKeyPath] have to be set.");
		}

		File publicFile = new File(publicKeyPath);
		System.out.println(publicFile.getAbsolutePath());
		if (!publicFile.exists() || !publicFile.isFile())
			throw new ConfigurationException("Given public key file does not exist.");

		File privateFile;
		if(privateKeyPath != null) {
			privateFile = new File(privateKeyPath);
			System.out.println(privateFile.getAbsolutePath());
			if (!privateFile.exists() || !privateFile.isFile())
				throw new ConfigurationException("Given private key file does not exist.");

			if (keyPassword == null)
				throw new ConfigurationException("No password given for the secret key.");
		} else {
			// Required because there is bug in the library.
			// Already created an issue for it.
			// TODO: Fake file
			privateFile = new File("fake");
			keyPassword = "";
		}


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
			BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To algorithmSuite = BouncyGPG
					.encryptToStream()
					.withConfig(keyringConfig)
					.withStrongAlgorithms();

			BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To.SignWith signWith;
			if (recipients.length == 1)
				signWith = algorithmSuite.toRecipient(recipients[0]);
			else
				signWith = algorithmSuite.toRecipients(recipients);

			BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To.SignWith.Armor armor;
			if (sender == null)
				armor = signWith.andDoNotSign();
			else
				armor = signWith.andSignWith(sender);

			OutputStream outputStream = armor.armorAsciiOutput().andWriteTo(out);

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
		if (recipient != null) {
			recipient = recipient.replaceAll("\\s", "");
			recipients = recipient.split(",");
		}
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
