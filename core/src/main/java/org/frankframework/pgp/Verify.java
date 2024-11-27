/*
   Copyright 2020 WeAreFrank!

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
package org.frankframework.pgp;

import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.util.io.Streams;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildDecryptionInputStreamAPI;
import org.frankframework.configuration.ConfigurationException;

public class Verify extends AbstractPGPAction {
	private final String[] senders;

	public Verify(String[] publicKey, String secretKey, String secretPassword, String[] senders) throws ConfigurationException {
		super(publicKey, secretKey, secretPassword, null);
		if (publicKey == null || secretKey == null || secretPassword == null)
			throw new ConfigurationException("For verification the fields [publicKey, secretKey, secretPassword] have to be set.");
		this.senders = senders;
	}

	@Override
	public void run(InputStream inputStream, OutputStream outputStream) throws Exception {
		BuildDecryptionInputStreamAPI.ValidationWithKeySelectionStrategy validation = BouncyGPG
				.decryptAndVerifyStream()
				.withConfig(keyringConfig);

		try (InputStream decryptionStream = senders == null
				? validation.andValidateSomeoneSigned().fromEncryptedInputStream(inputStream)
				: validation.andRequireSignatureFromAllKeys(senders).fromEncryptedInputStream(inputStream)) {
			Streams.pipeAll(decryptionStream, outputStream);
		}
	}

}
