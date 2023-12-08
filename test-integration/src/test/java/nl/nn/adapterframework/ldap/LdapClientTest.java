package nl.nn.adapterframework.ldap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;

public class LdapClientTest {

	@Test
	public void createAndConfigure() throws ConfigurationException, SenderException {
		LdapClient ldapClient = new LdapClient();
		ldapClient.configure();
		ldapClient.open();
	}
}
