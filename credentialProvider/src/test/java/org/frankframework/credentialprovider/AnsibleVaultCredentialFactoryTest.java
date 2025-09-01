package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.wedjaa.ansible.vault.crypto.VaultHandler;

public class AnsibleVaultCredentialFactoryTest {

	public String ANSIBLE_VAULT_FILE="/credentials-vault.txt";
	public String ANSIBLE_VAULT_KEY_FILE="/credentials-vault-key.txt";

	public String ANSIBLE_VAULT_PASSWORD="GEHE\\uIM";

	private AnsibleVaultCredentialFactory credentialFactory;

	@BeforeEach
	public void setup() throws IOException {
		String vaultUrl = this.getClass().getResource(ANSIBLE_VAULT_FILE).toExternalForm();
		String vaultFile =  Paths.get(vaultUrl.substring(vaultUrl.indexOf(":/")+2)).toString();
		assumeTrue(Files.exists(Paths.get(vaultFile)));

		String keyUrl = this.getClass().getResource(ANSIBLE_VAULT_KEY_FILE).toExternalForm();
		String keyFile =  Paths.get(keyUrl.substring(keyUrl.indexOf(":/")+2)).toString();
		assumeTrue(Files.exists(Paths.get(keyFile)));

		System.setProperty("credentialFactory.ansibleVault.vaultFile", vaultFile);
		System.setProperty("credentialFactory.ansibleVault.keyFile", keyFile);

		credentialFactory = new AnsibleVaultCredentialFactory();
		credentialFactory.initialize();
	}

	/**
	 *  Make sure to clean up the system properties after the test
	 */
	@AfterAll
	static void tearDown() {
		System.clearProperty("credentialFactory.ansibleVault.vaultFile");
		System.clearProperty("credentialFactory.ansibleVault.keyFile");
	}

	public void setupVault(Properties aliases, String title) throws IOException {
		ByteArrayOutputStream credentialData = new ByteArrayOutputStream();
		aliases.store(credentialData, title);

		String vaultData = credentialData.toString(StandardCharsets.US_ASCII);

// re-enable the line below to generate a vault which contains single backslashes, if you want to test with that.
//		vaultData = vaultData.replace("\\\\", "\\");
		System.out.println("Vault data before encryption:\n"+ vaultData);

		byte[] encryptedVault = VaultHandler.encrypt(vaultData.getBytes(StandardCharsets.US_ASCII), ANSIBLE_VAULT_PASSWORD);

		System.out.println("Ansible Vault:\n"+new String(encryptedVault));

	}

	//@Test
	// run this to obtain a fresh ansible vault
	public void testSetupVault() throws IOException {
		Properties aliases = new Properties();
		aliases.put("noUsername/password","password from alias");
		aliases.put("straight/username","\\username from alias");
		aliases.put("straight/password","passw\\urd from alias");
		aliases.put("singleValue","Plain Credential");
		setupVault(aliases, "test data for Ansible Vault");
	}

	public void testCreateVaultFromProperties(String resource, String title) throws IOException {
		URL urlin = getClass().getResource(resource+".properties");
		Properties properties = new Properties();
		properties.load(urlin.openStream());

		properties.forEach((k,v) -> System.out.println(k+": "+v));
		setupVault(properties, "");
	}

	//@Test
	public void testCreateVaultFromProperties() throws IOException {
		testCreateVaultFromProperties("","");
	}


	public void testReadVault(String file, String password) throws IOException {
		FileInputStream fis = new FileInputStream(file);

		ByteArrayOutputStream credentialData = new ByteArrayOutputStream();
		VaultHandler.decrypt(fis, credentialData, password);

		System.out.println("Decrypted Vault data:\n" + credentialData);
	}


	public void decryptFile(String file, String password) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file+".txt")) {
			try (FileInputStream fis = new FileInputStream(file+".vault")) {
				VaultHandler.decrypt(fis, fos, password);
			}
		}
	}

	public void encryptFile(String file, String password) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file+".vault")) {
			try (FileInputStream fis = new FileInputStream(file+".txt")) {
				VaultHandler.encrypt(fis, fos, password);
			}
		}
	}

	@Test
	public void testPlainAlias() {
		CredentialAlias alias = CredentialAlias.parse("straight");

		String expectedUsername = "\\username from alias";
		String expectedPassword = "passw\\urd from alias";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		CredentialAlias alias = CredentialAlias.parse("noUsername");
		String expectedPassword = "password from alias";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertNull(mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testUnknownAlias() {
		CredentialAlias alias = CredentialAlias.parse("fakeAlias");

		ICredentials mc = credentialFactory.getCredentials(alias);
		assertThrows(NoSuchElementException.class, mc::getUsername);
		assertThrows(NoSuchElementException.class, mc::getPassword);
	}


	@Test
	public void testPlainCredential() {
		CredentialAlias alias = CredentialAlias.parse("singleValue");

		String expectedUsername = null;
		String expectedPassword = "Plain Credential";

		ICredentials mc = credentialFactory.getCredentials(alias);

		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testGetAliases() {
		Collection<String> aliases = credentialFactory.getConfiguredAliases();
		assertEquals("[straight, singleValue, noUsername]", aliases.toString());
	}

}
