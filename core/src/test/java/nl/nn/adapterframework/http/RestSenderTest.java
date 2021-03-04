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
package nl.nn.adapterframework.http;

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.stream.Message;

public class RestSenderTest extends HttpSenderTestBase<RestSender> {

	@Override
	public RestSender createSender() {
		return spy(new RestSender());
	}

	@Test
	public void relativeUrl() throws Throwable {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("must use an absolute url starting with http(s)://");

		RestSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!

		sender.setMethodType("GET");
		sender.setUrl("relative/path");

		sender.configure();
	}

	@Test
	public void simpleMockedHttpGet() throws Throwable {
		RestSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		IPipeLineSession pls = new PipeLineSessionBase(session);

		sender.setMethodType("GET");

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString().replaceAll("&#xD;", "\r");
		assertEqualsIgnoreCRLF(getFile("simpleMockedRestGet.txt"), result.trim());
	}

	@Test
	public void simpleMockedHttpPost() throws Throwable {
		RestSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo this is my message");

		IPipeLineSession pls = new PipeLineSessionBase(session);

		sender.setMethodType("post"); //should handle both upper and lowercase methodtypes :)

		sender.configure();
		sender.open();

		String result = sender.sendMessage(input, pls).asString().replaceAll("&#xD;", "\r");
		assertEqualsIgnoreCRLF(getFile("simpleMockedRestPost.txt"), result.trim());
	}
}