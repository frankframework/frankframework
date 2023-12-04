package nl.nn.adapterframework.ldap;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.PropertyUtil;

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

	@Before
	public void setUp() throws ConfigurationException {
		pipe = new LdapFindMemberPipe();
		pipe.registerForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, null));
		pipe.setHost(host);
		pipe.setPort(port);
		pipe.setUseSsl(useSSL);
		pipe.setUsername(bindDN);
		pipe.setPassword(bindPassword);
	}

	@Test
	public void createAndConfigure() throws ConfigurationException, PipeStartException {
		pipe.configure();
		pipe.start();
	}

	@Test
	public void findMember() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setDnSearchIn(baseDN);
		pipe.setDnFind(findDN);
		pipe.setRecursiveSearch(true);
		pipe.configure();
		pipe.start();

		PipeLineSession session = new PipeLineSession();
		String input = bindDN;

		PipeRunResult prr = pipe.doPipe(new Message(input), session);

	}

}
