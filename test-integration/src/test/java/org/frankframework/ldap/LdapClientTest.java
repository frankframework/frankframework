package org.frankframework.ldap;

import org.frankframework.configuration.ConfigurationException;

org.frankframework.core.SenderException;

public class LdapClientTest {

	@Test
	public void createAndConfigure() throws ConfigurationException, SenderException {
		LdapClient ldapClient = new LdapClient();
		ldapClient.configure();
		ldapClient.open();
	}
}
