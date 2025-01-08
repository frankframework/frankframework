package org.frankframework.ldap;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

public class LdapClientTest {

	@Test
	public void createAndConfigure() throws ConfigurationException {
		LdapClient ldapClient = new LdapClient();
		ldapClient.configure();
		ldapClient.open();
	}
}
