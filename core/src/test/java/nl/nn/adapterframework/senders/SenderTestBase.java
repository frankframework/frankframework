/*
   Copyright 2018-2019 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;

public abstract class SenderTestBase<S extends ISender> extends Mockito {

	protected Logger log = LogUtil.getLogger(this);
	protected S sender;
	private static TestConfiguration configuration;

	private TestConfiguration getConfiguration() {
		if(configuration == null) {
			configuration = new TestConfiguration();
		}
		return configuration;
	}

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	protected PipeLineSession session;

	public abstract S createSender() throws Exception;

	@Before
	public void setUp() throws Exception {
		session = new PipeLineSession();
		String messageId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
		String technicalCorrelationId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";
		session.put(PipeLineSession.messageIdKey, messageId);
		session.put(PipeLineSession.technicalCorrelationIdKey, technicalCorrelationId);
		sender = createSender();
		getConfiguration().autowireByType(sender);
	}

	@After
	public void tearDown() throws Exception {
		if (sender != null) {
			sender.close();
			sender = null;
		}
	}

	public Message sendMessage(String message) throws SenderException, TimeOutException {
		return sendMessage(new Message(message), session);
	}
	public Message sendMessage(Message message) throws SenderException, TimeOutException {
		return sendMessage(message, session);
	}
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
		return sender.sendMessage(message, session);
	}
}
