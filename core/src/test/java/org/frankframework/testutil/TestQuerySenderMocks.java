package org.frankframework.testutil;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.testutil.mock.FixedQuerySenderMock;
import org.frankframework.util.SpringUtils;

/**
 * Test mocked/stubbed ResultSets
 *
 * @See {@link TestConfiguration#mockQuery(String, ResultSet)}
 * @See {@link FixedQuerySenderMock}
 *
 * @author Niels Meijer
 */
public class TestQuerySenderMocks {

	private TestConfiguration configuration;

	@Test
	public void testIfMockedResultSetWorks() throws Exception {
		configuration = new TestConfiguration();

		ResultSet mockedResultSet = FixedQuerySenderMock.ResultSetBuilder.create().build();
		configuration.mockQuery("SELECT COUNT(*) FROM FAKE_DATABASE_TABLE", mockedResultSet);

		FixedQuerySender qs = SpringUtils.createBean(configuration, FixedQuerySender.class);
		qs.setQuery("SELECT COUNT(*) FROM FAKE_DATABASE_TABLE");
		qs.configure();
		qs.start();
		try (Connection conn = qs.getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement("SELECT JOBNAME,JOBGROUP,ADAPTER,RECEIVER,CRON,EXECUTIONINTERVAL,MESSAGE,LOCKER,LOCK_KEY FROM IBISSCHEDULES")) {
				try (ResultSet rs = stmt.executeQuery()) {
					assertTrue(rs == mockedResultSet);
				}
			}
		}
	}
}
