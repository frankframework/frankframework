package nl.nn.credentialprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import net.wedjaa.ansible.vault.crypto.VaultHandler;

public class AnsibleVaultCredentialFactoryTest {

	public String ANSIBLE_VAULT_FILE="/credentials-vault.txt";
	public String ANSIBLE_VAULT_KEY_FILE="/credentials-vault-key.txt";

	public String ANSIBLE_VAULT_PASSWORD="GEHEIM";
	
	String vaultFile;
	String keyFile;
	
	AnsibleVaultCredentialFactory credentialFactory;
	
	@Before
	public void setup() {
		String vaultUrl = this.getClass().getResource(ANSIBLE_VAULT_FILE).toExternalForm();
		vaultFile =  Paths.get(vaultUrl.substring(vaultUrl.indexOf(":/")+2)).toString();
		assumeTrue(Files.exists(Paths.get(vaultFile)));

		String keyUrl = this.getClass().getResource(ANSIBLE_VAULT_KEY_FILE).toExternalForm();
		keyFile =  Paths.get(keyUrl.substring(keyUrl.indexOf(":/")+2)).toString();
		assumeTrue(Files.exists(Paths.get(keyFile)));
		
		System.setProperty("credentialFactory.ansibleVault.vaultFile", vaultFile);
		System.setProperty("credentialFactory.ansibleVault.keyFile", keyFile);
		
		credentialFactory = new AnsibleVaultCredentialFactory();
	}
	
	@Test
	// run this to obtain a fresh ansibile vault
	public void setupVault() throws IOException {
		Properties aliases = new Properties();
		aliases.put("noUsername/password","password from alias");
		aliases.put("straight/username","username from alias");
		aliases.put("straight/password","password from alias");
		aliases.put("singleValue","Plain Credential");
		
		ByteArrayOutputStream credentialData = new ByteArrayOutputStream();
		aliases.store(credentialData, "test data for Ansible Vault");

		System.out.println("Vault data before encryption:\n"+new String(credentialData.toByteArray()));
		
		byte[] encryptedVault = VaultHandler.encrypt(credentialData.toByteArray(), ANSIBLE_VAULT_PASSWORD);
		
		System.out.println("Ansible Vault:\n"+new String(encryptedVault));

	}

	@Test
	public void testNoAlias() {
		
		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(username, mc.getUsername());
		assertEquals(password, mc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		
		String alias = "straight";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		
		String alias = "noUsername";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = username;
		String expectedPassword = "password from alias";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}

	@Test
	public void testPlainCredential() {
		
		String alias = "singleValue";
		String username = null;
		String password = "fakePassword";
		String expectedUsername = null;
		String expectedPassword = "Plain Credential";
		
		ICredentials mc = credentialFactory.getCredentials(alias, username, password);
		
		assertEquals(expectedUsername, mc.getUsername());
		assertEquals(expectedPassword, mc.getPassword());
	}
	
	
}
