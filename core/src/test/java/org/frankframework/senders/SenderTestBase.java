/*
   Copyright 2018-2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;

public abstract class SenderTestBase<S extends ISender> extends ConfiguredTestBase {

	protected S sender;

	public abstract S createSender() throws Exception;

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		session = new PipeLineSession();
		session.put(PipeLineSession.MESSAGE_ID_KEY, testMessageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, testCorrelationId);
		sender = createSender();
		getConfiguration().autowireByType(sender);
	}

	@AfterEach
	@Override
	public void tearDown() {
		try {
			if (sender != null) {
				sender.stop();
			}
		} catch (LifecycleException e) {
			log.warn("Error closing Sender", e);
		}
		sender = null;
		super.tearDown();
	}

	public Message sendMessage(String message) throws SenderException, TimeoutException {
		return sendMessage(new Message(message), session);
	}
	public Message sendMessage(Message message) throws SenderException, TimeoutException {
		return sendMessage(message, session);
	}
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return sender.sendMessageOrThrow(message, session);
	}

	/**
	 * Retrieves a file from the test-classpath, with the senders' classname as basepath.
	 */
	protected Message getResource(String resource) {
		String base = sender.getClass().getSimpleName();
		if(StringUtils.isEmpty(base)) {
			Class<?> superClass = sender.getClass().getSuperclass();
			if(superClass != null) {
				base = superClass.getSimpleName();
			}
		}
		assertTrue(StringUtils.isNotEmpty(base), "unable to determine ["+sender+"] name");
		String relativeUrl = FilenameUtils.normalize("/Senders/" + base + "/" + resource, true);

		URL url = PipeTestBase.class.getResource(relativeUrl);
		assertNotNull(url, "unable to find resource ["+resource+"] in path ["+relativeUrl+"]");
		return new UrlMessage(url);
	}

	@Test
	public void testIfToStringWorks() {
		assertNotNull(sender.toString()); //And no NPE
	}
}
