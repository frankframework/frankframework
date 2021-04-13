package nl.nn.adapterframework.receivers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jdbc.MessageStoreListener;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.stream.Message;

@SuppressWarnings("unchecked")
public class MessageStoreListenerTest extends ListenerTestBase<MessageStoreListener> {

	@Override
	public MessageStoreListener createListener() throws Exception {
		MessageStoreListener listener = spy(new MessageStoreListener() {
			@Override
			protected Object getRawMessage(Connection conn, Map<String, Object> threadContext) throws ListenerException {
				MessageWrapper<Object> mw = new MessageWrapper<>(); //super class JdbcListener always wraps this in a MessageWrapper
				mw.setMessage(Message.asMessage(threadContext.get(STUB_RESULT_KEY)));
				mw.setId(""+threadContext.get(PipeLineSession.originalMessageIdKey));
				return mw;
			}
		});
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		doReturn("product").when(md).getDatabaseProductName();
		doReturn("version").when(md).getDatabaseProductVersion();
		Connection conn = mock(Connection.class);
		doReturn(md).when(conn).getMetaData();
		JndiDataSourceFactory factory = new JndiDataSourceFactory();
		DataSource dataSource = mock(DataSource.class);
		String dataSourceName = "myDummyDataSource";
		factory.add(dataSource, dataSourceName);
		listener.setDataSourceFactory(factory);
		doReturn(conn).when(dataSource).getConnection();
		listener.setConnectionsArePooled(false);
		listener.setDatasourceName(dataSourceName);
		return listener;
	}

	@Test
	public void basic() throws Exception {
		listener.configure();
		listener.open();

		String input = "test-message";
		Object rawMessage = getRawMessage(input);
		assertTrue(rawMessage instanceof MessageWrapper);
		assertEquals("MessageStoreListener should not manipulate the rawMessage", input, ((MessageWrapper<Object>)rawMessage).getMessage().asString());
	}

	@Test
	public void withSessionKeys() throws Exception {
		listener.setSessionKeys("sessionKey1,sessionKey2,sessionKey3");
		listener.configure();
		listener.open();

		String input = "test-message,\"value1\",value2,value3";
		Object rawMessage = getRawMessage(input);
		assertTrue(rawMessage instanceof MessageWrapper);
		MessageWrapper<Object> mw = (MessageWrapper<Object>) rawMessage;
		assertEquals("test-message", mw.getMessage().asString());

		assertEquals("value1", threadContext.get("sessionKey1"));
		assertEquals("value2", threadContext.get("sessionKey2"));
		assertEquals("value3", threadContext.get("sessionKey3"));
	}
}
