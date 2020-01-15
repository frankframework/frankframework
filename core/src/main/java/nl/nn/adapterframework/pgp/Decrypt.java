package nl.nn.adapterframework.pgp;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import nl.nn.adapterframework.configuration.ConfigurationException;
import org.bouncycastle.util.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Decrypt extends PGPAction{

	public Decrypt(String secretKey, String secretPassword) throws ConfigurationException {
		super(null, secretKey, secretPassword);
		if(secretKey == null || secretPassword == null)
			throw new ConfigurationException("For decryption, fields [secretKey, secretPassword] have to be set.");
	}

	@Override
	public OutputStream run(InputStream inputStream) throws Exception {
		OutputStream output = new ByteArrayOutputStream();
		InputStream decryptionStream = BouncyGPG
				.decryptAndVerifyStream()
				.withConfig(keyringConfig)
				.andIgnoreSignatures()
				.fromEncryptedInputStream(inputStream);

		Streams.pipeAll(decryptionStream, output);
		inputStream.close();
		return output;
	}

}
