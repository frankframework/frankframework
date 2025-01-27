/*
   Copyright 2021 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package org.frankframework.credentialprovider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.wedjaa.ansible.vault.crypto.data.Util;
import net.wedjaa.ansible.vault.crypto.data.VaultInfo;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.StreamUtil;

public class AnsibleVaultCredentialFactory extends AbstractMapCredentialFactory {

	public static final String PROPERTY_BASE = "credentialFactory.ansibleVault";

	public static final String VAULT_PROPERTY = PROPERTY_BASE + ".vaultFile";
	public static final String VAULT_KEY_PROPERTY = PROPERTY_BASE + ".keyFile";

	private static final String DEFAULT_VAULT_FILE = "catalina-secure-store.vault";
	private static final String DEFAULT_VAULT_KEY_FILE = ".secure-vault-keyfile";

	@Override
	public String getPropertyBase() {
		return PROPERTY_BASE;
	}

	@Override
	protected Map<String, String> getCredentialMap(CredentialConstants appConstants) throws IOException {
		try (InputStream vaultStream = getInputStream(appConstants, VAULT_PROPERTY, DEFAULT_VAULT_FILE, "Ansible Vault");
			InputStream keyStream = getInputStream(appConstants, VAULT_KEY_PROPERTY, DEFAULT_VAULT_KEY_FILE, "Ansible Vault Key")) {

			String vaultKey = StreamUtil.streamToString(keyStream).trim();
			String encrypted = StreamUtil.streamToString(vaultStream);

			VaultInfo vaultInfo = Util.getVaultInfo(encrypted);
			if (!vaultInfo.isEncryptedVault()) {
				throw new IOException("File is not an Ansible Encrypted Vault");
			}

			if (!vaultInfo.isValidVault()) {
				throw new IOException("The vault is not a format we can handle - check the cypher.");
			}

			byte[] decryptedData = vaultInfo.getCypher().decrypt(Util.getVaultData(encrypted), vaultKey);

			Properties properties = new Properties();
			try (Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(new ByteArrayInputStream(decryptedData))) {
				properties.load(reader);
			}

			Map<String, String> result = new HashMap<>();
			properties.forEach((k, v) -> result.put((String) k, (String) v));
			return result;
		}
	}
}
