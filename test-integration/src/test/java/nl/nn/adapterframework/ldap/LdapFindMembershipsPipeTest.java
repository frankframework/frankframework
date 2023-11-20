package nl.nn.adapterframework.ldap;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.cache.EhCache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.PropertyUtil;

public class LdapFindMembershipsPipeTest {
	private String PROPERTY_FILE = "LdapFindMemberships.properties";

	private String ldapProviderUrl = PropertyUtil.getProperty(PROPERTY_FILE, "ldapProviderUrl");

	private String host    = PropertyUtil.getProperty(PROPERTY_FILE, "host");
	private int port       = PropertyUtil.getProperty(PROPERTY_FILE, "port", 636);
	private boolean useSSL = PropertyUtil.getProperty(PROPERTY_FILE, "useSSL", true);
	private String baseDN  = PropertyUtil.getProperty(PROPERTY_FILE, "baseDN");
	private String bindDN  = PropertyUtil.getProperty(PROPERTY_FILE, "bindDN");
	private String pipeInput  = PropertyUtil.getProperty(PROPERTY_FILE, "pipeInput");

	private String bindPassword = PropertyUtil.getProperty(PROPERTY_FILE, "password");

	private LdapFindGroupMembershipsPipe pipe;

	@Before
	public void setUp() throws ConfigurationException {
		pipe = new LdapFindGroupMembershipsPipe();
		pipe.registerForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME,null));
		pipe.setLdapProviderURL(ldapProviderUrl);
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
	public void findMemberships() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(false);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe(new Message(pipeInput), null);

		System.out.println("result:"+prr.getResult());
	}

	@Test
	public void findMembershipsRecursively() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe(new Message(pipeInput), null);

		System.out.println("result:"+prr.getResult());
	}

	@Test
	public void findMembershipsRecursivelyWithCache() {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(true);

		EhCache<Set<String>> cache = new EhCache<Set<String>>();
		cache.setTimeToLiveSeconds(3600);
		pipe.setCache(cache);
		pipe.configure();
		pipe.start();

		long time0=System.currentTimeMillis();
		PipeRunResult prr1 = pipe.doPipe(new Message(pipeInput), null);
		long time1=System.currentTimeMillis();

		System.out.println("result:"+prr1.getResult());

		long time2=System.currentTimeMillis();
		PipeRunResult prr2 = pipe.doPipe(new Message(pipeInput), null);
		long time3=System.currentTimeMillis();

		System.out.println("first  duration ["+(time1-time0)+"] ms");
		System.out.println("second duration ["+(time3-time2)+"] ms");

		assertEquals(prr1.getResult().asString(), prr2.getResult().asString());
	}
}
