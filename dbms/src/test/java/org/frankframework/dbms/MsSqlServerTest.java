package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MsSqlServerTest {

	private final String selectQuery = "select * from ibisstore";

	public MsSqlServerDbmsSupport createDbmsSupport() {
		return new MsSqlServerDbmsSupport();
	}

	@Test
	public void testPrepareQueryTextForWorkQueueReading() throws JdbcException {
		String query = createDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);

		// dbmssupport.mssql.queuereading.rowlock=false by default now, to avoid thinking we can outsmart the mssql query optimizer
		assertEquals("select TOP 1 * from ibisstore WITH (updlock,readpast)", query);
	}
}
