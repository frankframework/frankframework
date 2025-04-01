package org.frankframework.jdbc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.dbms.H2DbmsSupport;
import org.frankframework.receivers.Receiver;

@SuppressWarnings("removal")
class JdbcQueryListenerTest {

	private JdbcQueryListener listener;

	@BeforeEach
	void setUp() throws Exception {
		Receiver<?> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);

		listener = spy(new JdbcQueryListener());
		listener.setReceiver(receiver);
		listener.setMessageFieldType(JdbcListener.MessageFieldType.STRING);
		listener.setKeyField("TKEY");
		listener.setMessageField("FM");
		listener.setMessageIdField("MID");
		listener.setAdditionalFields("F1, F2");
		listener.setUpdateStatusToProcessedQuery("UPDATE TEST_TABLE SET STATUS = 'PROCESSED' WHERE TKEY = ?");

		DataSource ds = mock(DataSource.class);
		doReturn(ds).when(listener).getDatasource();
		H2DbmsSupport dbmsSupport = new H2DbmsSupport("2.3.232 (2024-08-11)");
		doReturn(dbmsSupport).when(listener).getDbmsSupport();
	}

	@Test
	void testSelectQueryContainsAdditionalFields() {
		listener.setSelectQuery("SELECT TKEY, FM, MID, F1, F2 FROM TEST_TABLE");

		assertDoesNotThrow(listener::configure);
	}

	@Test
	void testSelectQueryDoesNotContainAdditionalFields() {
		listener.setSelectQuery("SELECT TKEY, FM, MID, EF1, F12 FROM TEST_TABLE");

		ConfigurationException exception = assertThrows(ConfigurationException.class, listener::configure);
		assertThat(exception.getMessage(), containsString("[F1, F2]"));
	}
}
