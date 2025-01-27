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
package org.frankframework.pgp;

import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.util.io.Streams;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;

import org.frankframework.configuration.ConfigurationException;

public class Decrypt extends AbstractPGPAction {

	public Decrypt(String secretKey, String secretPassword) throws ConfigurationException {
		super(null, secretKey, secretPassword, null);
		if(secretKey == null || secretPassword == null)
			throw new ConfigurationException("For decryption, fields [secretKey, secretPassword] have to be set.");
	}

	@Override
	public void run(InputStream inputStream, OutputStream outputStream) throws Exception {
		InputStream decryptionStream = BouncyGPG
				.decryptAndVerifyStream()
				.withConfig(keyringConfig)
				.andIgnoreSignatures()
				.fromEncryptedInputStream(inputStream);

		Streams.pipeAll(decryptionStream, outputStream);
		inputStream.close();
	}

}
