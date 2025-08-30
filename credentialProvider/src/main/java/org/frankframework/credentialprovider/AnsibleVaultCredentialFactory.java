/*
   Copyright 2021 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import net.wedjaa.ansible.vault.crypto.data.Util;
import net.wedjaa.ansible.vault.crypto.data.VaultInfo;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.StreamUtil;

/**
 * <p>CredentialFactory implementation that uses an Ansible Vault to read secrets from.</p>
 *
 * <p>Ansible Vault is a feature of Ansible that allows you to securely store and manage sensitive data, such as passwords, API keys, or other secrets, in
 * encrypted files. It is particularly useful for protecting sensitive information in automation scripts or configuration files.</p>
 *
 * <p>To set up Ansible Vault in the Framework, you need to set the following properties in {@code credentialproperties.properties}:</p>
 *
 * <pre>{@code
 * credentialFactory.class=org.frankframework.credentialprovider.AnsibleVaultCredentialFactory
 * credentialFactory.ansibleVault.vaultFile=catalina-secure-store.vault
 * credentialFactory.ansibleVault.keyFile=.secure-vault-keyfile
 * }</pre>
 *
 * <p>Note that the default values for the vault file and key file are {@code catalina-secure-store.vault} and {@code .secure-vault-keyfile} respectively.</p>
 *
 * <p>Note that the vault file and key file are read from the classpath. If you want to use a different location, you can specify the full path to the file.</p>
 *
 * @see <a href="https://docs.ansible.com/ansible/latest/vault_guide/index.html">Ansible Vault Documentation</a>
 */
public class AnsibleVaultCredentialFactory extends AbstractMapCredentialFactory {
	private static final String PROPERTY_BASE = "credentialFactory.ansibleVault";

	private static final String VAULT_PROPERTY = PROPERTY_BASE + ".vaultFile";
	private static final String VAULT_KEY_PROPERTY = PROPERTY_BASE + ".keyFile";

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

			return properties.entrySet().stream()
				.collect(Collectors.toMap(
					entry -> (String) entry.getKey(),
					entry -> (String) entry.getValue()
				));
		}
	}
}
