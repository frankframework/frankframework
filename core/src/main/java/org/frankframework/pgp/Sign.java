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
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildEncryptionOutputStreamAPI;
import org.frankframework.configuration.ConfigurationException;

public class Sign extends AbstractPGPAction {
	private final String sender;
	private final String[] recipients;

	public Sign(String[] publicKey, String secretKey, String secretPassword, String[] recipients, String sender) throws ConfigurationException {
		super(publicKey, secretKey, secretPassword, new Object[]{recipients, sender});
		this.recipients = recipients;
		this.sender = sender;
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

		try (OutputStream output = signWith.andSignWith(sender).armorAsciiOutput().andWriteTo(outputStream)) {
			Streams.pipeAll(inputStream, output);
		}
		inputStream.close();
	}

}
