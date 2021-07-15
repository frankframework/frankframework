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

public class LdapFindMemberPipeTest {

//	private String host="ADDSRO.D-DIREP.BIZ";
//	private int port=636;
//	private boolean useSSL=true;
//	private String baseDN="DC=D-DIREP,DC=BIZ";
//	private String bindDN="UID=xxx,ou=DI-IUF-EP,ou=Services,dc=d-direp,dc=biz";
//	private String bindPassword="xxxxxxxxxxxxxxxxxxxxxxx";

	private String host="insim.biz";
	private int port=636;
	private boolean useSSL=true;
	private String baseDN="OU=Tenants,DC=INSIM,DC=BIZ";
	private String bindDN="cn=xx00xx,OU=Users,OU=PRD,OU=AB,OU=Tenants,DC=INSIM,DC=BIZ";
	private String bindPassword="xxxxxxxxxxxxxxxxx";
	
	private LdapFindMemberPipe pipe;
	
	@Before
	public void setUp() throws ConfigurationException {
		pipe = new LdapFindMemberPipe();
		pipe.registerForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, null));
		pipe.setHost(host);
		pipe.setPort(port);
		pipe.setUseSsl(useSSL);
		pipe.setUserName(bindDN);
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
		pipe.setDnFind("CN=ni83nz,OU=Users,OU=PRD,OU=AB,OU=Tenants,DC=INSIM,DC=BIZ");
		pipe.setRecursiveSearch(true);
		pipe.configure();
		pipe.start();
		
		PipeLineSession session = new PipeLineSession();
		String input = bindDN;
		
		PipeRunResult prr = pipe.doPipe(new Message(input), session);
		
		
	}
	
}
