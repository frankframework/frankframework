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
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildEncryptionOutputStreamAPI;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.io.Streams;

import java.io.*;

public class Sign extends PGPAction {
	private String sender;
	private String[] recipients;

	public Sign(String[] publicKey, String secretKey, String secretPassword, String[] recipients, String sender) throws ConfigurationException {
		super(publicKey, secretKey, secretPassword,recipients, sender);
		this.recipients = recipients;
		this.sender = sender;
	}

	@Override
	public OutputStream run(InputStream inputStream) throws Exception {
		OutputStream output = new ByteArrayOutputStream();
		BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To algorithmSuite =  BouncyGPG
				.encryptToStream()
				.withConfig(keyringConfig)
				.withStrongAlgorithms();

		BuildEncryptionOutputStreamAPI.WithAlgorithmSuite.To.SignWith signWith;
		if (recipients.length == 1)
			signWith = algorithmSuite.toRecipient(recipients[0]);
		else
			signWith = algorithmSuite.toRecipients(recipients);

		OutputStream outputStream = signWith.andSignWith(sender).armorAsciiOutput().andWriteTo(output);
		Streams.pipeAll(inputStream, outputStream);
		outputStream.close();

		return output;
	}

}
