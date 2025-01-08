package org.frankframework.ldap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.PropertyUtil;

public class LdapFindMemberPipeTest {
	private final String PROPERTY_FILE = "LdapFindMemberPipe.properties";

	private final String host = PropertyUtil.getProperty(PROPERTY_FILE, "host");
	private final int port = PropertyUtil.getProperty(PROPERTY_FILE, "port", 636);
	private final boolean useSSL = PropertyUtil.getProperty(PROPERTY_FILE, "useSSL", true);
	private final String baseDN = PropertyUtil.getProperty(PROPERTY_FILE, "baseDN");
	private final String bindDN = PropertyUtil.getProperty(PROPERTY_FILE, "bindDN");
	private final String findDN = PropertyUtil.getProperty(PROPERTY_FILE, "findDN");

	private final String bindPassword = PropertyUtil.getProperty(PROPERTY_FILE, "password");

	private LdapFindMemberPipe pipe;

	@BeforeEach
	public void setUp() {
		pipe = new LdapFindMemberPipe();
		pipe.addForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, null));
		pipe.setHost(host);
		pipe.setPort(port);
		pipe.setUseSsl(useSSL);
		pipe.setUsername(bindDN);
		pipe.setPassword(bindPassword);
	}

	@Test
	public void createAndConfigure() throws ConfigurationException {
		pipe.configure();
		pipe.start();
	}

	@Test
	public void findMember() throws ConfigurationException, PipeRunException {
		pipe.setDnSearchIn(baseDN);
		pipe.setDnFind(findDN);
		pipe.setRecursiveSearch(true);
		pipe.configure();
		pipe.start();

		PipeLineSession session = new PipeLineSession();

		PipeRunResult prr = pipe.doPipe(new Message(bindDN), session);
	}

}
