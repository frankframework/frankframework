/*
   Copyright 2021 WeAreFrank!
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
package test.java.nl.nn.adapterframework.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.ldap.LdapIsMemberOfPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

public class LdapIsMemberOfPipeTest extends LdapQueryPipeTestBase<LdapIsMemberOfPipe> {
	private String thenForwardName = "then";
	private String elseForwardName = "else";
	
	private String dnL1Group="ou=L1Group,ou=groups,ou=development,dc=ibissource,dc=org";
	private String dnL4Group="ou=L4Group,ou=groups,ou=development,dc=ibissource,dc=org";
	private String dnL4User="cn=L4user,ou=people,ou=development,dc=ibissource,dc=org";

	@Override
	public LdapIsMemberOfPipe createPipe() {
		return new LdapIsMemberOfPipe();
	}

	@Override
	public void setup() throws ConfigurationException {
		super.setup();
		pipe.registerForward(new PipeForward(thenForwardName,null));
		pipe.registerForward(new PipeForward(elseForwardName,null));
	}

	@Test
	public void recursiveIsMemberOf() throws ConfigurationException, PipeStartException, IOException, PipeRunException, SAXException {
		pipe.setRecursiveSearch(true);
		pipe.setMemberOfDN(dnL1Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,new Message(dnL4User), session);
		PipeForward forward=prr.getPipeForward();

		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(thenForwardName,actualForwardName);
		
		assertTrue(dnL4User.equals(prr.getResult().asString()));
	}

	@Test
	public void nonRecursiveIsMemberOf() throws ConfigurationException, PipeStartException, IOException, PipeRunException, SAXException {
		pipe.setRecursiveSearch(false);
		pipe.setMemberOfDN(dnL4Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,new Message(dnL4User), session);
		PipeForward forward=prr.getPipeForward();

		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(thenForwardName,actualForwardName);

		assertTrue(dnL4User.equals(prr.getResult().asString()));
	}
	

	@Test
	public void isNotMemberOf() throws ConfigurationException, PipeStartException, IOException, PipeRunException, SAXException {
		pipe.setRecursiveSearch(false);
		pipe.setMemberOfDN(dnL1Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,new Message(dnL4User), session);
		PipeForward forward=prr.getPipeForward();

		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(elseForwardName,actualForwardName);
		
		assertTrue(dnL4User.equals(prr.getResult().asString()));
	}

	@Test
	public void nonRecursiveIsRecursiveMemberOf() throws ConfigurationException, PipeStartException, IOException, PipeRunException, SAXException {
		pipe.setRecursiveSearch(false);
		pipe.setMemberOfDN(dnL1Group);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,new Message(dnL4User), session);
		PipeForward forward=prr.getPipeForward();

		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(elseForwardName,actualForwardName);

		assertTrue(dnL4User.equals(prr.getResult().asString()));
	}
} 