package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertEquals;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.AppConstants;

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

	@Test
	public void testPrepareQueryTextForWorkQueueReadingWithoutRowlock() throws JdbcException {
		AppConstants.getInstance().setProperty("dbmssupport.mssql.queuereading.rowlock", "false");

		String query = createDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);

		assertEquals("select TOP 1 * from ibisstore WITH (updlock,readpast)", query);
	}

	@Test
	public void testPrepareQueryTextForWorkQueueReadingWithRowlockProperty() throws JdbcException {
		AppConstants.getInstance().setProperty("dbmssupport.mssql.queuereading.rowlock", "true");

		String query = createDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);

		assertEquals("select TOP 1 * from ibisstore WITH (rowlock,updlock,readpast)", query);
	}

	@Test
	public void testPrepareQueryTextForWorkQueueReadingWithEmptyRowlockProperty() throws JdbcException {
		AppConstants.getInstance().setProperty("dbmssupport.mssql.queuereading.rowlock", "");

		String query = createDbmsSupport().prepareQueryTextForWorkQueueReading(1, selectQuery);

		assertEquals("select TOP 1 * from ibisstore WITH (updlock,readpast)", query);
	}
}
