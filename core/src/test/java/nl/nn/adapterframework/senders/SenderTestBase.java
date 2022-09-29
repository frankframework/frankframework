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
package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.util.FilenameUtils;

public abstract class SenderTestBase<S extends ISender> extends ConfiguredTestBase {

	protected S sender;

	public abstract S createSender() throws Exception;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		session = new PipeLineSession();
		session.put(PipeLineSession.messageIdKey, testMessageId);
		session.put(PipeLineSession.correlationIdKey, testCorrelationId);
		sender = createSender();
		getConfiguration().autowireByType(sender);
	}

	@Override
	public void tearDown() throws Exception {
		if (sender != null) {
			sender.close();
			sender = null;
		}
		super.tearDown();
	}

	public Message sendMessage(String message) throws SenderException, TimeoutException {
		return sendMessage(new Message(message), session);
	}
	public Message sendMessage(Message message) throws SenderException, TimeoutException {
		return sendMessage(message, session);
	}
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return sender.sendMessage(message, session);
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
		assertTrue("unable to determine ["+sender+"] name", StringUtils.isNotEmpty(base));
		String relativeUrl = FilenameUtils.normalize("/Senders/" + base + "/" + resource, true);

		URL url = PipeTestBase.class.getResource(relativeUrl);
		assertNotNull("unable to find resource ["+resource+"] in path ["+relativeUrl+"]", url);
		return new UrlMessage(url);
	}
}
