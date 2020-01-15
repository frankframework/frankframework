package nl.nn.adapterframework.pgp;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildDecryptionInputStreamAPI;
import nl.nn.adapterframework.configuration.ConfigurationException;
import org.bouncycastle.util.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Verify extends PGPAction {
	private String[] senders;

	public Verify(String[] publicKey, String secretKey, String secretPassword, String[] senders) throws ConfigurationException {
		super(publicKey, secretKey, secretPassword);
		if (publicKey == null || secretKey == null || secretPassword == null)
			throw new ConfigurationException("For verification the fields [publicKey, secretKey, secretPassword] have to be set.");
		this.senders = senders;
	}

	@Override
	public OutputStream run(InputStream inputStream) throws Exception {
		OutputStream output = new ByteArrayOutputStream();
		BuildDecryptionInputStreamAPI.ValidationWithKeySelectionStrategy validation = BouncyGPG
				.decryptAndVerifyStream()
				.withConfig(keyringConfig);

		InputStream decryptionStream;
		if(senders == null) {
			decryptionStream = validation
					.andValidateSomeoneSigned()
					.fromEncryptedInputStream(inputStream);
		} else {
			decryptionStream = validation
					.andRequireSignatureFromAllKeys(senders)
					.fromEncryptedInputStream(inputStream);
		}

		Streams.pipeAll(decryptionStream, output);
		inputStream.close();
		return output;
	}

}
