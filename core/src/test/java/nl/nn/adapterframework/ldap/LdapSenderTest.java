package nl.nn.adapterframework.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;

import nl.nn.adapterframework.ldap.LdapSender.Operation;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

public class LdapSenderTest extends SenderTestBase<LdapSender> {
	InMemoryDirectoryServer inMemoryDirectoryServer = null;
	String baseDNs = "dc=ibissource,dc=org";

	@Override
	public LdapSender createSender() throws Exception {
		LDAPConnection connection = inMemoryDirectoryServer.getConnection();

		LdapSender ldapSender = new LdapSender();
		ldapSender.setLdapProviderURL("ldap://localhost:" + connection.getConnectedPort());
		return ldapSender;
	}

	@Override
	public void setUp() throws Exception {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreAttributeOrder(true);

		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDNs);
		config.setSchema(null);
		// Custom InMemoryListenerConfig because InetAddress.getLocalhost() does not always resolve to correct ip address
		InMemoryListenerConfig listenerConfig = new InMemoryListenerConfig("localhost", InetAddress.getByName("127.0.0.1"), 0, null, null, null);
		config.setListenerConfigs(listenerConfig);
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
		sender.setOperation(Operation.READ);
		sender.addParameter(ParameterBuilder.create().withName("entryName"));
		sender.setAttributesToReturn("gidNumber,mail");

		sender.configure();
		sender.open();

		String result = sendMessage("cn=LEA Administrator,ou=groups,ou=development," + baseDNs).asString();

		TestAssertions.assertEqualsIgnoreCRLF("<attributes>\n\t<attribute name=\"mail\" value=\"leaadministrator@ibissource.org\"/>\n\t<attribute name=\"gidNumber\" value=\"505\"/>\n</attributes>\n", result);
	}

	@Test
	public void readAllAttributes() throws Exception {
		sender.setOperation(Operation.READ);
		sender.addParameter(new Parameter("entryName", "cn=LEA Administrator,ou=groups,ou=development," + baseDNs));

		sender.configure();
		sender.open();

		String result = sendMessage("<dummy/>").asString();

		compareXML("Ldap/expected/read.xml", result);
	}

	@Test
	public void updateAttribute() throws Exception {
		sender.setOperation(Operation.UPDATE);
		sender.addParameter(new Parameter("entryName", "cn=LEA Administrator,ou=groups,ou=development," + baseDNs));

		sender.configure();
		sender.open();

		String result = sendMessage("<attributes><attribute name=\"mail\"><value>info@ibissource.org</value></attribute></attributes>").asString();

		assertEquals("<LdapResult>Success</LdapResult>", result);
		compareXML("Ldap/expected/updateAttribute.xml", getTree());
	}

	@Test
	public void updateNewEntry() throws Exception {
		sender.setOperation(Operation.UPDATE);
		sender.addParameter(new Parameter("entryName", "cn=LEA Administrator,ou=groups,ou=development," + baseDNs));
		sender.addParameter(new Parameter("newEntryName", "cn=LEA Administrator,ou=people,ou=development," + baseDNs));

		sender.configure();
		sender.open();

		String result = sendMessage("<dummy/>").asString();

		assertEquals("<LdapResult>Success</LdapResult>", result);
		compareXML("Ldap/expected/updateNewEntry.xml", getTree());
	}

	@Test
	public void deleteAttribute() throws Exception {
		sender.setOperation(Operation.DELETE);
		sender.addParameter(new Parameter("entryName", "cn=LEA Administrator,ou=groups,ou=development," + baseDNs));

		sender.configure();
		sender.open();

		String result = sendMessage("<attributes><attribute name=\"mail\"><value>leaadministrator@ibissource.org</value></attribute></attributes>").asString();

		assertEquals("<LdapResult>Success</LdapResult>", result);
		compareXML("Ldap/expected/delete.xml", getTree());
	}

	//Create a new sender and execute the TREE action to run a diff against that changes
	private String getTree() throws Exception {
		super.tearDown();
		super.setUp();
		sender.setOperation(Operation.GET_TREE);
		sender.addParameter(new Parameter("entryName", baseDNs));

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
