package nl.nn.adapterframework.jdbc;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

public abstract class JdbcSenderTestBase<S extends JdbcSenderBase<?>> extends JdbcTestBase {

	protected Logger log = LogUtil.getLogger(this);
	protected S sender;

	@Mock
	protected PipeLineSession session;

	public abstract S createSender() throws Exception;

	@Before
	public void setUp() throws Exception {
		session = new PipeLineSession();
		session.put(PipeLineSession.MESSAGE_ID_KEY, ConfiguredTestBase.testMessageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, ConfiguredTestBase.testCorrelationId);
		sender = createSender();
		autowire(sender);
	}

	@After
	public void tearDown() throws Exception {
		if (sender != null) {
			sender.close();
			sender = null;
		}
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
}
