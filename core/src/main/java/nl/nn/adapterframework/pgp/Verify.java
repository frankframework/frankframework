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
	public void run(InputStream inputStream, OutputStream outputStream) throws Exception {
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

		Streams.pipeAll(decryptionStream, outputStream);
		inputStream.close();
	}

}
