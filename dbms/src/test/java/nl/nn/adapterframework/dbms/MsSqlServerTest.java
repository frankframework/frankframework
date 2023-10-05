package nl.nn.adapterframework.dbms;

import static org.junit.Assert.assertEquals;


import org.junit.Test;

public class MsSqlServerTest {

	public MsSqlServerDbmsSupport createDbmsSupport() {
		return new MsSqlServerDbmsSupport();
	}

	private String selectQuery = "select * from ibisstore";

	@Test
	public void testPrepareQueryTextForWorkQueueReading() throws JdbcException {
		String query = createDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);

		// dbmssupport.mssql.queuereading.rowlock=false by default now, to avoid thinking we can outsmart the mssql query optimizer
		assertEquals("select TOP 1 * from ibisstore WITH (updlock,readpast)", query);
	}
}
