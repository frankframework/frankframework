/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.soap;

import java.io.IOException;
import java.security.KeyStore;

import jakarta.xml.soap.SOAPException;

import org.apache.wss4j.common.ext.WSSecurityException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.encryption.CorePkiUtil;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;

public class CryptoSoapValidator extends SoapValidator implements HasKeystore {
	private CredentialFactory certificateCf;
	private KeyStore keystore;

	private @Getter KeystoreConfiguration keystoreConfiguration;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		certificateCf = new CredentialFactory(getKeystoreAliasAuthAlias(), null, getKeystoreAliasPassword());
		try {
			keystore = CorePkiUtil.createKeyStore(keystoreConfiguration);
		} catch (EncryptionException e) {
			throw new ConfigurationException("unable to open keystore", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		if (responseMode) {
			return super.doPipe(input, session, true, messageRoot);
		}

		try {
			Message result = SoapUtils.verifyMessage(input, keystore, getKeystoreAlias(), certificateCf.getPassword(), false);
			Message result2 = SoapUtils.decryptMessage(result, keystore, getKeystoreAlias(), certificateCf.getPassword(), true);
			return super.doPipe(result2, session, false, messageRoot);
		} catch (SOAPException | IOException | WSSecurityException e) {
			throw new PipeRunException(this, "blabla", e);
		}
	}

	@Override
	public void setKeystoreConfiguration(KeystoreConfiguration keystoreConfiguration) {
		this.keystoreConfiguration = keystoreConfiguration;
	}

}
