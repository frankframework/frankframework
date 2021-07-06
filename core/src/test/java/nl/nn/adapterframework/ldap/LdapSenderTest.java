package nl.nn.adapterframework.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

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

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

public class LdapSenderTest extends SenderTestBase<LdapSender> {
	InMemoryDirectoryServer inMemoryDirectoryServer = null;
	String baseDNs = "dc=ibissource,dc=org";

	@Override
	public LdapSender createSender() throws Exception {
		LDAPConnection connection = null;
		try {
			connection = inMemoryDirectoryServer.getConnection();
		} catch (LDAPException e) {
			if(!TestAssertions.isTestRunningOnGitHub()) {
				fail(e.getMessage());
			}
		}

		assumeNotNull(connection);
		LdapSender ldapSender = new LdapSender();
		ldapSender.setLdapProviderURL("ldap://localhost:" + connection.getConnectedPort());
		return ldapSender;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreAttributeOrder(true);

		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDNs);
		config.setSchema(null);
		inMemoryDirectoryServer = new InMemoryDirectoryServer(config);

		String ldifDataFile = "Ldap/data.ldif";
		URL ldifDataUrl = ClassUtils.getResourceURL(ldifDataFile);
		if (ldifDataUrl == null) {
			fail("cannot find resource [" + ldifDataFile + "]");
		}
		inMemoryDirectoryServer.importFromLDIF(true, ldifDataUrl.getPath());
		inMemoryDirectoryServer.startListening();
		super.setUp();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		if(inMemoryDirectoryServer != null) {
			inMemoryDirectoryServer.shutDown(true);
		}
		super.tearDown();
	}

	@Test
	public void init() throws Exception {
		compareXML("Ldap/expected/init.xml", getTree());
	}

	@Test
	public void readTwoAttributes() throws Exception {
		sender.setOperation("read");
		Parameter parameter = new Parameter();
		parameter.setName("entryName");
		sender.addParameter(parameter);
		sender.setAttributesToReturn("gidNumber,mail");

		sender.configure();
		sender.open();

		String result = sendMessage("cn=LEA Administrator,ou=groups,ou=development," + baseDNs).asString();

		TestAssertions.assertEqualsIgnoreCRLF("<attributes>\n  <attribute name=\"mail\" value=\"leaadministrator@ibissource.org\" />\n  <attribute name=\"gidNumber\" value=\"505\" />\n</attributes>\n", result);
	}

	@Test
	public void readAllAttributes() throws Exception {
		sender.setOperation("read");
		Parameter parameter = new Parameter();
		parameter.setName("entryName");
		parameter.setValue("cn=LEA Administrator,ou=groups,ou=development," + baseDNs);
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		String result = sendMessage("<dummy/>").asString();

		compareXML("Ldap/expected/read.xml", result);
	}

	@Test
	public void updateAttribute() throws Exception {
		sender.setOperation("update");
		Parameter parameter = new Parameter();
		parameter.setName("entryName");
		parameter.setValue("cn=LEA Administrator,ou=groups,ou=development," + baseDNs);
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		String result = sendMessage("<attributes><attribute name=\"mail\"><value>info@ibissource.org</value></attribute></attributes>").asString();

		assertEquals("<LdapResult>Success</LdapResult>", result);
		compareXML("Ldap/expected/updateAttribute.xml", getTree());
	}

	@Test
	public void updateNewEntry() throws Exception {
		sender.setOperation("update");
		Parameter parameter = new Parameter();
		parameter.setName("entryName");
		parameter.setValue("cn=LEA Administrator,ou=groups,ou=development," + baseDNs);
		sender.addParameter(parameter);
		Parameter parameter2 = new Parameter();
		parameter2.setName("newEntryName");
		parameter2.setValue("cn=LEA Administrator,ou=people,ou=development," + baseDNs);
		sender.addParameter(parameter2);

		sender.configure();
		sender.open();

		String result = sendMessage("<dummy/>").asString();

		assertEquals("<LdapResult>Success</LdapResult>", result);
		compareXML("Ldap/expected/updateNewEntry.xml", getTree());
	}

	@Test
	public void deleteAttribute() throws Exception {
		sender.setOperation("delete");
		Parameter parameter = new Parameter();
		parameter.setName("entryName");
		parameter.setValue("cn=LEA Administrator,ou=groups,ou=development," + baseDNs);
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		String result = sendMessage("<attributes><attribute name=\"mail\"><value>leaadministrator@ibissource.org</value></attribute></attributes>").asString();

		assertEquals("<LdapResult>Success</LdapResult>", result);
		compareXML("Ldap/expected/delete.xml", getTree());
	}

	private String getTree() throws Exception {
		sender.setOperation("getTree");
		Parameter parameter = new Parameter();
		parameter.setName("entryName");
		parameter.setValue(baseDNs);
		sender.addParameter(parameter);

		sender.configure();
		sender.open();

		return sendMessage("dummy").asString();
	}

	private void compareXML(String expectedFile, String result) throws SAXException, IOException {
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
