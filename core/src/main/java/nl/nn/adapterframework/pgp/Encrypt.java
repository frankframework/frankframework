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

import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.util.io.Streams;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildEncryptionOutputStreamAPI;
import nl.nn.adapterframework.configuration.ConfigurationException;

public class Encrypt extends PGPAction {
	private String[] recipients;

	public Encrypt(String[] publicKey, String[] recipients) throws ConfigurationException {
		super(publicKey, null, null, recipients);

		if (publicKey == null || publicKey.length != 1)
			throw new ConfigurationException("With encryption action, there should be only one public key.");

		this.recipients = recipients;
	}

	@Override
	public void run(InputStream inputStream, OutputStream outputStream) throws Exception {
		BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To algorithmSuite =  BouncyGPG
				.encryptToStream()
				.withConfig(keyringConfig)
				.withStrongAlgorithms();

		BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To.SignWith signWith;
		if (recipients.length == 1)
			signWith = algorithmSuite.toRecipient(recipients[0]);
		else
			signWith = algorithmSuite.toRecipients(recipients);

		try (OutputStream output = signWith.andDoNotSign().armorAsciiOutput().andWriteTo(outputStream)) {
			Streams.pipeAll(inputStream, output);
		}
		inputStream.close();
	}

}
