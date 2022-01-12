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
package nl.nn.adapterframework.extensions.akamai;

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

public class NetStorageSenderTest extends HttpSenderTestBase<NetStorageSender> {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		AppConstants.getInstance().setProperty("http.headers.messageid", false);
	}

	@Override
	public NetStorageSender createSender() {
		return spy(new NetStorageSender() {
			@Override
			public Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
				return new Message( getResponseBodyAsString(responseHandler, true) );
			}
		});
	}

	@Override
	public NetStorageSender getSender() throws Exception {
		NetStorageSender sender = super.getSender(false);
		sender.setAction("du");
		sender.setCpCode("cpCode123");
		sender.setNonce("myNonce");
		sender.setAccessToken(null); //As long as this is NULL, X-Akamai-ACS-Auth-Sign will be NULL
		return sender;
	}

	@Test
	public void testContentType() throws Throwable {
		NetStorageSender sender = getSender();
		sender.configure();
		assertNull("no content-type should be present", sender.getFullContentType());
	}

	@Test
	public void duAction() throws Throwable {
		NetStorageSender sender = getSender();
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("duAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void duActionWithRootDir() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setRootDir("/my/special/"); //Start and end with a slash!
		Message input = new Message("path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("duAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}