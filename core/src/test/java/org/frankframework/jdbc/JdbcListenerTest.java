/*
   Copyright 2025 WeAreFrank!

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

class JdbcListenerTest {

	private JdbcListener<String> listener;

	@BeforeEach
	void setUp() throws Exception {
		Receiver<String> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);

		listener = spy(new JdbcListener<>());
		listener.setReceiver(receiver);
		listener.setMessageFieldType(JdbcListener.MessageFieldType.STRING);
		listener.setKeyField("TKEY");
		listener.setMessageField("FM");
		listener.setMessageIdField("MID");
		listener.setAdditionalFields("F1, F2");

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
