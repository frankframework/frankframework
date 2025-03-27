/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.receivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.jdbc.JdbcListener;
import org.frankframework.jdbc.MessageStoreListener;
import org.frankframework.jdbc.datasource.DataSourceFactory;
import org.frankframework.stream.Message;

@SuppressWarnings("unchecked")
public class MessageStoreListenerTest extends ListenerTestBase<Serializable, MessageStoreListener> {

	@Override
	public MessageStoreListener createListener() throws Exception {
		MessageStoreListener listener = spy(new MessageStoreListener() {
			@Override
			protected RawMessageWrapper<Serializable> getRawMessage(Connection conn, Map<String, Object> threadContext) {
				RawMessageWrapper<Serializable> result;
				Serializable o = (Serializable) threadContext.get(STUB_RESULT_KEY);
				if (o instanceof MessageWrapper<?>) {
					result = (RawMessageWrapper<Serializable>) o;
				} else {
					result = new RawMessageWrapper<>(o, String.valueOf(threadContext.get(PipeLineSession.MESSAGE_ID_KEY)), null);
				}
				if (!getAdditionalFieldsList().isEmpty()) {
					Map<String, String> additionalFields = getAdditionalFieldsList().stream()
							.collect(Collectors.toMap(Function.identity(), Function.identity()));
					result.getContext().put(JdbcListener.ADDITIONAL_QUERY_FIELDS_KEY, additionalFields);
				}
				return result;
			}
		});
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		doReturn("product").when(md).getDatabaseProductName();
		doReturn("version").when(md).getDatabaseProductVersion();
		Connection conn = mock(Connection.class);
		doReturn(md).when(conn).getMetaData();
		DataSourceFactory factory = new DataSourceFactory();
		DataSource dataSource = mock(DataSource.class);
		String dataSourceName = "myDummyDataSource";
		factory.add(dataSource, dataSourceName);
		listener.setDataSourceFactory(factory);
		doReturn(conn).when(dataSource).getConnection();
		listener.setConnectionsArePooled(false);
		listener.setDatasourceName(dataSourceName);
		doReturn(new GenericDbmsSupport()).when(listener).getDbmsSupport();

		Receiver<Serializable> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);
		listener.setReceiver(receiver);

		return listener;
	}

	@Test
	void basic() throws Exception {
		listener.configure();
		listener.start();

		String input = "test-message";
		RawMessageWrapper<Serializable> rawMessage = getRawMessage(input);
		assertEquals(input, rawMessage.getRawMessage().toString(), "MessageStoreListener should not manipulate the rawMessage");

		Message message = listener.extractMessage(rawMessage, session);
		assertFalse(session.containsKey(JdbcListener.ADDITIONAL_QUERY_FIELDS_KEY));
		assertEquals(input, message.asString());
	}

	@Test
	void withAdditionalFields() throws Exception {
		listener.setAdditionalFields("timestamp");
		listener.configure();
		listener.start();

		String input = "test-message";
		RawMessageWrapper<Serializable> rawMessage = getRawMessage(input);
		assertTrue(rawMessage.getContext().containsKey(JdbcListener.ADDITIONAL_QUERY_FIELDS_KEY), "RawMessage Context should contain additional fields");
		assertEquals(input, rawMessage.getRawMessage().toString(), "MessageStoreListener should not manipulate the rawMessage");

		Message message = listener.extractMessage(rawMessage, session);
		assertEquals(input, message.asString());
		assertFalse(session.containsKey(JdbcListener.ADDITIONAL_QUERY_FIELDS_KEY));
		assertTrue(session.containsKey("timestamp"));
		assertEquals("timestamp", session.get("timestamp"));

	}

	@Test
	void withSessionKeysLegacyCsvFormat() throws Exception {
		listener.setSessionKeys("sessionKey1,sessionKey2,sessionKey3");
		listener.configure();
		listener.start();

		String input = "test-message,\"value1\",value2,value3";
		Message inputMessage = Message.asMessage(input);
		MessageWrapper<Serializable> wrapper = new MessageWrapper<>(inputMessage, null, null);
		wrapper.getContext().put("sessionKey1", "value1");
		wrapper.getContext().put("sessionKey2", "value2");
		wrapper.getContext().put("sessionKey3", "value3");

		RawMessageWrapper<Serializable> rawMessage = getRawMessage(wrapper);
		Message message = listener.extractMessage(rawMessage, session);
		assertEquals(input, message.asString());

		assertEquals("value1", session.get("sessionKey1"));
		assertEquals("value2", session.get("sessionKey2"));
		assertEquals("value3", session.get("sessionKey3"));
	}

	@Test
	void withSessionKeysBinaryFormat() throws Exception {
		listener.setSessionKeys("sessionKey1,sessionKey2,sessionKey3");
		listener.configure();
		listener.start();

		String input = "test-message,\"value1\",value2,value3";

		RawMessageWrapper<Serializable> rawMessage = getRawMessage(input);
		Message message = listener.extractMessage(rawMessage, session);
		assertEquals("test-message", message.asString());

		assertEquals("value1", session.get("sessionKey1"));
		assertEquals("value2", session.get("sessionKey2"));
		assertEquals("value3", session.get("sessionKey3"));
	}
}
