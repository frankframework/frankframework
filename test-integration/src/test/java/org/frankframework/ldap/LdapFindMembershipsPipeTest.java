package org.frankframework.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Set;

import org.frankframework.stream.Message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.cache.EhCache;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.testutil.PropertyUtil;

public class LdapFindMembershipsPipeTest {
	private final String PROPERTY_FILE = "LdapFindMemberships.properties";

	private final String ldapProviderUrl = PropertyUtil.getProperty(PROPERTY_FILE, "ldapProviderUrl");

	private final String host = PropertyUtil.getProperty(PROPERTY_FILE, "host");
	private final int port = PropertyUtil.getProperty(PROPERTY_FILE, "port", 636);
	private final boolean useSSL = PropertyUtil.getProperty(PROPERTY_FILE, "useSSL", true);
	private final String baseDN = PropertyUtil.getProperty(PROPERTY_FILE, "baseDN");
	private final String bindDN = PropertyUtil.getProperty(PROPERTY_FILE, "bindDN");
	private final String pipeInput = PropertyUtil.getProperty(PROPERTY_FILE, "pipeInput");

	private final String bindPassword = PropertyUtil.getProperty(PROPERTY_FILE, "password");

	private LdapFindGroupMembershipsPipe pipe;

	@BeforeEach
	public void setUp() {
		pipe = new LdapFindGroupMembershipsPipe();
		pipe.addForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME,null));
		pipe.setLdapProviderURL(ldapProviderUrl);
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
	public void findMemberships() throws ConfigurationException, PipeRunException {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(false);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe(new Message(pipeInput), null);

		System.out.println("result:"+prr.getResult());
	}

	@Test
	public void findMembershipsRecursively() throws ConfigurationException, PipeRunException {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = pipe.doPipe(new Message(pipeInput), null);

		System.out.println("result:"+prr.getResult());
	}

	@Test
	public void findMembershipsRecursivelyWithCache() throws IOException, ConfigurationException, PipeRunException {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(true);

		EhCache<Set<String>> cache = new EhCache<>();
		cache.setTimeToLiveSeconds(3600);
		pipe.setCache(cache);
		pipe.configure();
		pipe.start();

		long time0=System.currentTimeMillis();
		PipeRunResult prr1 = pipe.doPipe(Message.asMessage(pipeInput), null);
		long time1=System.currentTimeMillis();

		System.out.println("result:"+prr1.getResult());

		long time2=System.currentTimeMillis();
		PipeRunResult prr2 = pipe.doPipe(Message.asMessage(pipeInput), null);
		long time3=System.currentTimeMillis();

		System.out.println("first  duration ["+(time1-time0)+"] ms");
		System.out.println("second duration ["+(time3-time2)+"] ms");

		assertEquals(prr1.getResult().asString(), prr2.getResult().asString());
	}
}
