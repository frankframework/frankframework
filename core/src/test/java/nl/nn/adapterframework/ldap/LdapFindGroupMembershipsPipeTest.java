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
package nl.nn.adapterframework.ldap;

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
import nl.nn.adapterframework.ldap.LdapFindGroupMembershipsPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

public class LdapFindGroupMembershipsPipeTest extends LdapQueryPipeTestBase<LdapFindGroupMembershipsPipe> {
	private String foundForwardName = "success";

	private String dnL4User="cn=L4user,ou=people,ou=development,dc=ibissource,dc=org";

	@Override
	public LdapFindGroupMembershipsPipe createPipe() {
		return new LdapFindGroupMembershipsPipe();
	}

	@Override
	public void setup() throws ConfigurationException {
		super.setup();
		pipe.registerForward(new PipeForward("notFound",null));
	}

	@Test
	public void recursiveFind() throws ConfigurationException, PipeStartException, IOException, PipeRunException, SAXException {
		pipe.setRecursiveSearch(true);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,new Message(dnL4User), session);
		PipeForward forward=prr.getPipeForward();

		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(foundForwardName,actualForwardName);

		compareXML("Ldap/FindGroupMembershipsPipe/recursive.xml", prr.getResult().asString());
	}

	@Test
	public void nonRecursiveFind() throws ConfigurationException, PipeStartException, IOException, PipeRunException, SAXException {
		pipe.setRecursiveSearch(false);
		configurePipe();
		pipe.start();

		PipeRunResult prr = doPipe(pipe,new Message(dnL4User), session);
		PipeForward forward=prr.getPipeForward();

		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(foundForwardName,actualForwardName);

		compareXML("Ldap/FindGroupMembershipsPipe/nonRecursive.xml", prr.getResult().asString());
	}

	private void compareXML(String expectedFile, String result)
			throws SAXException, IOException {
		URL expectedUrl = ClassUtils.getResourceURL(expectedFile);
		if (expectedUrl == null) {
			throw new IOException("cannot find resource [" + expectedUrl + "]");
		}
		String expected = Misc.resourceToString(expectedUrl);
		Diff diff = XMLUnit.compareXML(expected, result);

		assertTrue(diff.toString(), diff.identical());
	}
} 