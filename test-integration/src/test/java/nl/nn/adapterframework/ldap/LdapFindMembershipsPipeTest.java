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

public class LdapFindMembershipsPipeTest {

//	private String host="ADDSRO.D-DIREP.BIZ";
//	private int port=636;
//	private boolean useSSL=true;
//	private String baseDN="DC=D-DIREP,DC=BIZ";
//	private String bindDN="UID=xxx,ou=DI-IUF-EP,ou=Services,dc=d-direp,dc=biz";
//	private String bindPassword="xxxxxxxxxxxxxxxxxxxxxxx";

	private String ldapProviderUrl="ldaps://insim.biz";
//	private String host="insim.biz";
//	private int port=636;
//	private boolean useSSL=true;
	private String baseDN="OU=Tenants,DC=INSIM,DC=BIZ";
	private String bindDN="cn=xx00xx,OU=Users,OU=PRD,OU=AB,OU=Tenants,DC=INSIM,DC=BIZ";
	private String bindPassword="xxxxxxxxxxxxxxxxx";
	
	private LdapFindGroupMembershipsPipe pipe;
	
	@Before
	public void setUp() throws ConfigurationException {
		pipe = new LdapFindGroupMembershipsPipe();
		pipe.registerForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME,null));
		pipe.setLdapProviderURL(ldapProviderUrl);
//		pipe.setHost(host);
//		pipe.setPort(port);
//		pipe.setUseSsl(useSSL);
		pipe.setUserName(bindDN);
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
		
		String input="CN=ck08pu,OU=Users,OU=PRD,OU=AB,OU=Tenants,DC=INSIM,DC=BIZ";
		
		PipeRunResult prr = pipe.doPipe(new Message(input), null);
		
		System.out.println("result:"+prr.getResult());
		
		
	}
	
	@Test
	public void findMembershipsRecursively() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(true);
		pipe.configure();
		pipe.start();
		
		String input="CN=ck08pu,OU=Users,OU=PRD,OU=AB,OU=Tenants,DC=INSIM,DC=BIZ";
		
		PipeRunResult prr = pipe.doPipe(new Message(input), null);
		
		System.out.println("result:"+prr.getResult());
		
		
	}

	@Test
	public void findMembershipsRecursivelyWithCache() throws Exception {
		pipe.setBaseDN(baseDN);
		pipe.setRecursiveSearch(true);
		
		EhCache<Set<String>> cache = new EhCache<Set<String>>();
		cache.setTimeToLiveSeconds(3600);
		pipe.setCache(cache);
		pipe.configure();
		pipe.start();
		
		String input="CN=ni83nz,OU=Users,OU=PRD,OU=AB,OU=Tenants,DC=INSIM,DC=BIZ";
		
		long time0=System.currentTimeMillis();
		PipeRunResult prr1 = pipe.doPipe(new Message(input), null);
		long time1=System.currentTimeMillis();
		
		System.out.println("result:"+prr1.getResult());
		
		long time2=System.currentTimeMillis();
		PipeRunResult prr2 = pipe.doPipe(new Message(input), null);
		long time3=System.currentTimeMillis();
		
		System.out.println("first  duration ["+(time1-time0)+"] ms");
		System.out.println("second duration ["+(time3-time2)+"] ms");
		
		assertEquals(prr1.getResult().asString(), prr2.getResult().asString());
		
	}
}
