package nl.nn.adapterframework.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

/**
 * @author Peter Leeuwenburgh
 */
public class LdapSenderTest {
	InMemoryDirectoryServer inMemoryDirectoryServer = null;
	String baseDNs = "dc=ibissource,dc=org";

	@Before
	public void startLdapServer() throws LDAPException, IOException {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreAttributeOrder(true);

		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(
				baseDNs);
		config.setSchema(null);
		inMemoryDirectoryServer = new InMemoryDirectoryServer(config);

		String ldifDataFile = "Ldap/data.ldif";
		URL ldifDataUrl = ClassUtils.getResourceURL(ldifDataFile);
		if (ldifDataUrl == null) {
			throw new IOException("cannot find resource [" + ldifDataFile + "]");
		}
		inMemoryDirectoryServer.importFromLDIF(true, ldifDataUrl.getPath());
		inMemoryDirectoryServer.startListening();
	}

	@Test
	public void init() throws SAXException, IOException,
			ConfigurationException, SenderException, LDAPException, TimeOutException {
		compareXML("Ldap/expected/init.xml", getTree());
	}

	@Test
	public void updateAttribute() throws SAXException, IOException,
			ConfigurationException, SenderException, LDAPException, TimeOutException {
		String result;
		LDAPConnection connection = inMemoryDirectoryServer.getConnection();
		LdapSender ldapSender = null;
		try {
			ldapSender = new LdapSender();
			ldapSender.setLdapProviderURL("ldap://"
					+ connection.getConnectedAddress() + ":"
					+ connection.getConnectedPort());
			ldapSender.setOperation("update");
			Parameter parameter = new Parameter();
			parameter.setName("entryName");
			parameter.setValue("cn=LEA Administrator,ou=groups,ou=development,"
					+ baseDNs);
			ldapSender.addParameter(parameter);
			ldapSender.configure();
			ldapSender.open();
			result = ldapSender.sendMessage(new Message("<attributes><attribute name=\"mail\"><value>info@ibissource.org</value></attribute></attributes>"),null).asString();
		} finally {
			if (ldapSender != null) {
				ldapSender.close();
			}
			if (connection != null) {
				connection.close();
			}
		}
		assertEquals("<LdapResult>Success</LdapResult>", result);

		compareXML("Ldap/expected/updateAttribute.xml", getTree());
	}

	@Test
	public void updateNewEntry() throws SAXException, IOException, ConfigurationException, SenderException, LDAPException, TimeOutException {
		String result;
		LDAPConnection connection = inMemoryDirectoryServer.getConnection();
		LdapSender ldapSender = null;
		try {
			ldapSender = new LdapSender();
			ldapSender.setLdapProviderURL("ldap://" + connection.getConnectedAddress() + ":" + connection.getConnectedPort());
			ldapSender.setOperation("update");
			Parameter parameter = new Parameter();
			parameter.setName("entryName");
			parameter.setValue("cn=LEA Administrator,ou=groups,ou=development," + baseDNs);
			ldapSender.addParameter(parameter);
			Parameter parameter2 = new Parameter();
			parameter2.setName("newEntryName");
			parameter2.setValue("cn=LEA Administrator,ou=people,ou=development," + baseDNs);
			ldapSender.addParameter(parameter2);
			ldapSender.configure();
			ldapSender.open();
			result = ldapSender.sendMessage(new Message("<dummy/>"), null).asString();
		} finally {
			if (ldapSender != null) {
				ldapSender.close();
			}
			if (connection != null) {
				connection.close();
			}
		}
		assertEquals("<LdapResult>Success</LdapResult>", result);

		compareXML("Ldap/expected/updateNewEntry.xml", getTree());
	}

	@Test
	public void deleteAttribute() throws SAXException, IOException,
			ConfigurationException, SenderException, LDAPException, TimeOutException {
		String result;
		LDAPConnection connection = inMemoryDirectoryServer.getConnection();
		LdapSender ldapSender = null;
		try {
			ldapSender = new LdapSender();
			ldapSender.setLdapProviderURL("ldap://"
					+ connection.getConnectedAddress() + ":"
					+ connection.getConnectedPort());
			ldapSender.setOperation("delete");
			Parameter parameter = new Parameter();
			parameter.setName("entryName");
			parameter.setValue("cn=LEA Administrator,ou=groups,ou=development,"
					+ baseDNs);
			ldapSender.addParameter(parameter);
			ldapSender.configure();
			ldapSender.open();
			result = ldapSender.sendMessage(new Message("<attributes><attribute name=\"mail\"><value>leaadministrator@ibissource.org</value></attribute></attributes>"),
							null).asString();
		} finally {
			if (ldapSender != null) {
				ldapSender.close();
			}
			if (connection != null) {
				connection.close();
			}
		}
		assertEquals("<LdapResult>Success</LdapResult>", result);

		compareXML("Ldap/expected/delete.xml", getTree());
	}

	@After
	public void stopLdapServer() {
		if (inMemoryDirectoryServer != null) {
			inMemoryDirectoryServer.shutDown(true);
		}
	}

	private String getTree() throws LDAPException, ConfigurationException,
			SenderException, TimeOutException, IOException {
		LDAPConnection connection = inMemoryDirectoryServer.getConnection();
		LdapSender ldapSender = null;
		try {
			ldapSender = new LdapSender();
			ldapSender.setLdapProviderURL("ldap://"
					+ connection.getConnectedAddress() + ":"
					+ connection.getConnectedPort());
			ldapSender.setOperation("getTree");
			Parameter parameter = new Parameter();
			parameter.setName("entryName");
			parameter.setValue(baseDNs);
			ldapSender.addParameter(parameter);
			ldapSender.configure();
			ldapSender.open();
			return ldapSender.sendMessage(new Message("dummy"), null).asString();
		} finally {
			if (ldapSender != null) {
				ldapSender.close();
			}
			if (connection != null) {
				connection.close();
			}
		}
	}

	private void compareXML(String expectedFile, String result)
			throws SAXException, IOException {
		URL expectedUrl = ClassUtils.getResourceURL(expectedFile);
		if (expectedUrl == null) {
			throw new IOException("cannot find resource [" + expectedUrl + "]");
		}
		String expected = Misc.resourceToString(expectedUrl);
		Diff diff = XMLUnit.compareXML(expected, result);
		diff.overrideDifferenceListener(new DifferenceListener() {
			@Override
			public int differenceFound(Difference diff) {
				if (diff.getId() == DifferenceConstants.ATTR_VALUE_ID) {
					Attr attr = (Attr) diff.getControlNodeDetail().getNode();
					if ("value".equals(attr.getName())) {
						Node parentNode = attr.getOwnerElement();
						if ("attribute".equals(parentNode.getNodeName())) {
							Node nameAttribute = parentNode.getAttributes()
									.getNamedItem("name");
							if ("entryUUID".equals(nameAttribute
									.getTextContent())
									|| "modifyTimestamp".equals(nameAttribute
											.getTextContent())
									|| "createTimestamp".equals(nameAttribute
											.getTextContent())
									|| "userPassword".equals(nameAttribute
											.getTextContent())) {
								return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
							}
						}
					}
				}
				return RETURN_ACCEPT_DIFFERENCE;
			}

			@Override
			public void skippedComparison(Node arg0, Node arg1) {
				// TODO Auto-generated method stub

			}
		});
		assertTrue(diff.toString(), diff.identical());
	}
}
