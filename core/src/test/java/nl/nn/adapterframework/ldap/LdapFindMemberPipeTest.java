/*
   Copyright 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import org.junit.Test;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public class LdapFindMemberPipeTest extends LdapQueryPipeTestBase<LdapFindMemberPipe> {
	private String foundForwardName = "success";
	private String notFoundForwardName = "notFound";
	
	private String dnL1Group="ou=L1Group,ou=groups,ou=development,dc=ibissource,dc=org";
	private String dnL4Group="ou=L4Group,ou=groups,ou=development,dc=ibissource,dc=org";
	private String dnL4User="cn=L4user,ou=people,ou=development,dc=ibissource,dc=org";
	private String dnNotExistingUser="cn=NonUser,ou=people,ou=development,dc=ibissource,dc=org";
	
	@Override
	public LdapFindMemberPipe createPipe() {
		return new LdapFindMemberPipe();
	}

	@Override
	public void setup() throws ConfigurationException {
		super.setup();
		pipe.registerForward(new PipeForward("notFound",null));
	}
	
	@Test
	public void recursiveSuccessFind() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setRecursiveSearch(true);
		pipe.setDnFind(dnL4User);
		pipe.setDnSearchIn(dnL1Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,"dummy", session);
		PipeForward forward=prr.getPipeForward();
		
		assertNotNull(forward);
		
		String actualForwardName=forward.getName();
		assertEquals(foundForwardName,actualForwardName);
	}
	
	@Test
	public void nonRecursiveSuccessFind() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setRecursiveSearch(false);
		pipe.setDnFind(dnL4User);
		pipe.setDnSearchIn(dnL4Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,"dummy", session);
		PipeForward forward=prr.getPipeForward();
		
		assertNotNull(forward);
		
		String actualForwardName=forward.getName();
		assertEquals(foundForwardName,actualForwardName);
	}
	
	@Test
	public void recursiveFailedFind() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setRecursiveSearch(true);
		pipe.setDnFind(dnNotExistingUser);
		pipe.setDnSearchIn(dnL1Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,"dummy", session);
		PipeForward forward=prr.getPipeForward();
		
		assertNotNull(forward);
		
		String actualForwardName=forward.getName();
		assertEquals(notFoundForwardName,actualForwardName);
	}
	
	@Test
	public void nonRecursiveFailedFind() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setRecursiveSearch(false);
		pipe.setDnFind(dnL4User); //L4user is not a direct member of L1group
		pipe.setDnSearchIn(dnL1Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,"dummy", session);
		PipeForward forward=prr.getPipeForward();
		
		assertNotNull(forward);
		
		String actualForwardName=forward.getName();
		assertEquals(notFoundForwardName,actualForwardName);
	}
}