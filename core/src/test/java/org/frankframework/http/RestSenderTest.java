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
package org.frankframework.http;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.stream.Message;

public class RestSenderTest extends HttpSenderTestBase<RestSender> {

	@Override
	public RestSender createSender() {
		return spy(new RestSender());
	}

	@Test
	void relativeUrl() throws Throwable {
		RestSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!

		sender.setMethodType(HttpMethod.GET);
		sender.setUrl("relative/path");

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertThat(e.getMessage(), Matchers.endsWith("must use an absolute url starting with http(s)://"));
	}

	@Test
	void simpleMockedHttpGet() throws Throwable {
		RestSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.GET);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString().replace("&#xD;", "\r");
		assertEqualsIgnoreCRLF(getFile("simpleMockedRestGet.txt"), result.trim());
	}

	@Test
	void simpleMockedHttpPost() throws Throwable {
		RestSender sender = getSender(false); //Cannot add headers (aka parameters) for this test!
		Message input = new Message("hallo this is my message");

		PipeLineSession pls = new PipeLineSession(session);

		sender.setMethodType(HttpMethod.POST); //should handle both upper and lowercase methodtypes :)

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, pls).asString().replace("&#xD;", "\r");
		assertEqualsIgnoreCRLF(getFile("simpleMockedRestPost.txt"), result.trim());
	}
}
