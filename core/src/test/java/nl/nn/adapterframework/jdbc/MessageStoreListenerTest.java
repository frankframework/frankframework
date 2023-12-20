package nl.nn.adapterframework.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doAnswer;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLWarning;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.dbms.Dbms;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.junit.DatabaseTest;
import nl.nn.adapterframework.testutil.junit.DatabaseTestEnvironment;
import nl.nn.adapterframework.testutil.junit.WithLiquibase;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

@WithLiquibase(tableName = MessageStoreListenerTest.TABLE_NAME, file = "Migrator/ChangelogBlobTests.xml")
public class MessageStoreListenerTest {

	private MessageStoreListener listener;
	protected static final String TABLE_NAME = "MSLT_TABLE";
	private boolean dropAllAfterEachTest = true;
	protected Liquibase liquibase;
	protected static Logger log = LogUtil.getLogger(MessageStoreListenerTest.class);
	private JdbcTransactionalStorage<String> storage;
	private final String slotId = "slot";
	private final String messageIdField = "MESSAGEID";

	@DatabaseTest.Parameter(0)
	private TransactionManagerType transactionManagerType;

	@DatabaseTest.Parameter(1)
	private String dataSourceName;

	private TestConfiguration getConfiguration() {
		return transactionManagerType.getConfigurationContext(dataSourceName);
	}

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2)); // tests are based on H2 syntax queries
		listener = new MessageStoreListener();
		listener = getConfiguration().createBean(MessageStoreListener.class);
		listener.setTableName(TABLE_NAME);
		listener.setMessageIdField(messageIdField);
		listener.setSlotId(slotId);
		autowire(listener);
		getConfiguration().getIbisManager();
		getConfiguration().autowireByName(listener);

		storage = getConfiguration().createBean(JdbcTransactionalStorage.class);
		storage.setTableName(TABLE_NAME);
		storage.setIdField(messageIdField);
		storage.setSlotId(slotId);
		System.setProperty("tableName", TABLE_NAME);
		autowire(storage);

		runMigrator("Migrator/ChangelogBlobTests.xml", databaseTestEnvironment);
	}

	@AfterEach
	public void teardown(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {

		if(listener != null) {
			listener.close();
		}

		if (liquibase != null) {
			if (dropAllAfterEachTest) {
				try {
					liquibase.dropAll();
				} catch (Exception e) {
					log.warn("Liquibase failed to drop all objects. Trying to rollback the changesets", e);
					liquibase.rollback(liquibase.getChangeSetStatuses(null).size(), null);
				}
			}
			liquibase.close();
			liquibase = null;
		}

		Connection connection = databaseTestEnvironment.getConnection();
		if (connection != null && !connection.isClosed()) {
			try (connection) {
				dropTableIfPresentt(databaseTestEnvironment, TABLE_NAME);
				connection.close();
			}
			connection.close();
		}
		assert connection != null;
		connection.close();
		databaseTestEnvironment.close();

//		if (failed) {
//			transactionManagerType.closeConfigurationContext();
//			databaseTestEnvironment = null;
//		}
	}

	public void dropTableIfPresentt(DatabaseTestEnvironment databaseTestEnvironment, String tableName) throws Throwable {
		if (databaseTestEnvironment.getConnection() != null && !databaseTestEnvironment.getConnection().isClosed()) {
			dropTableIfPresent(databaseTestEnvironment, tableName);
			databaseTestEnvironment.close();
		} else {
			log.warn("connection is null or closed, cannot drop table [" + tableName + "]");
			databaseTestEnvironment.close();
		}
		databaseTestEnvironment.close();
	}

	public static void dropTableIfPresent(DatabaseTestEnvironment databaseTestEnvironment, String tableName) throws Throwable {
		if (databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), tableName)) {
			JdbcUtil.executeStatement(databaseTestEnvironment.getConnection(), "DROP TABLE " + tableName);
			SQLWarning warnings = databaseTestEnvironment.getConnection().getWarnings();
			databaseTestEnvironment.close();
			if (warnings != null) {
				log.warn(JdbcUtil.warningsToString(warnings));
				databaseTestEnvironment.close();
			}
		}
		databaseTestEnvironment.close();
		assertFalse(databaseTestEnvironment.getDbmsSupport().isTablePresent(databaseTestEnvironment.getConnection(), tableName), "table [" + tableName + "] should not exist");
	}

	//IBISSTORE_CHANGESET_PATH
	protected void runMigrator(String changeLogFile, DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(databaseTestEnvironment.getConnection()));
		liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), db);
		liquibase.forceReleaseLocks();
		StringWriter out = new StringWriter(2048);
		liquibase.reportStatus(true, new Contexts(), out);
		log.info("Liquibase Database: {}, {}", liquibase.getDatabase().getDatabaseProductName(), liquibase.getDatabase().getDatabaseProductVersion());
		log.info("Liquibase Database connection: {}", liquibase.getDatabase());
		log.info("Liquibase changeset status:");
		log.info(out.toString());
		liquibase.update(new Contexts());
		databaseTestEnvironment.close();
		db.close();
	}

	protected void autowire(JdbcFacade jdbcFacade) {
		getConfiguration().autowireByName(jdbcFacade);
		jdbcFacade.setDatasourceName(dataSourceName);
	}


	public JdbcTableMessageBrowser getMessageBrowser(ProcessState state) throws JdbcException, ConfigurationException {
		JdbcTableMessageBrowser browser = Mockito.spy((JdbcTableMessageBrowser)listener.getMessageBrowser(state));
		doAnswer(arg -> {
			autowire(browser);
			return null;
		}).when(browser).copyFacadeSettings(listener);
		browser.configure();
		return browser;
	}


	@DatabaseTest
	public void testSetup() throws ConfigurationException, ListenerException {
		listener.configure();
		listener.open();
	}

	@DatabaseTest
	public void testSelectQuery() throws ConfigurationException {
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM " + TABLE_NAME + " t WHERE TYPE='M' AND (SLOTID='slot')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM " + TABLE_NAME + " t WHERE TYPE='M'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM " + TABLE_NAME + " t WHERE TYPE='M' AND (SLOTID='slot' AND (t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectConditionNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM " + TABLE_NAME + " t WHERE TYPE='M' AND ((t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

//	@DatabaseTest
//	public void testUpdateStatusQueryDone() throws ConfigurationException {
//		listener.configure();
//
//		String expected = "UPDATE "+tableName+" SET TYPE='A',MESSAGEDATE=NOW(),COMMENTS=?,EXPIRYDATE = NOW() + 30 WHERE TYPE!='A' AND MESSAGEKEY=?";
//
//		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
//	}
//
//	@DatabaseTest
//	public void testUpdateStatusQueryDoneNoMoveToMessageLog() throws ConfigurationException {
//		listener.setMoveToMessageLog(false);
//		listener.configure();
//
//		String expected = "DELETE FROM "+tableName+" WHERE MESSAGEKEY = ?";
//
//		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
//	}
//
//	@DatabaseTest
//	public void testUpdateStatusQueryError() throws ConfigurationException {
//		listener.configure();
//
//		String expected = "UPDATE "+tableName+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";
//
//		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
//	}
//
//	@DatabaseTest
//	public void testUpdateStatusQueryWithSelectCondition() throws ConfigurationException {
//		listener.setSelectCondition("1=1");
//		listener.configure();
//
//		String expected = "UPDATE "+tableName+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";
//
//		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
//	}
//
//	@DatabaseTest
//	public void testGetMessageCountQueryAvailable(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
//		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
//		listener.configure();
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
//
//		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='M' AND (SLOTID='slot'))";
//
//		assertEquals(expected, browser.getMessageCountQuery);
//	}
//
//	@DatabaseTest
//	public void testGetMessageCountQueryError(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
//		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
//		listener.configure();
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);
//
//		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='E' AND (SLOTID='slot'))";
//
//		assertEquals(expected, browser.getMessageCountQuery);
//	}
//
//	@DatabaseTest
//	public void testGetMessageCountQueryAvailableWithSelectCondition(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
//		listener.setSelectCondition("t.VARCHAR='A'");
//		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
//		listener.configure();
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
//
//		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='M' AND (SLOTID='slot' AND (t.VARCHAR='A')))";
//
//		assertEquals(expected, browser.getMessageCountQuery);
//	}
//
//	@DatabaseTest
//	public void testGetMessageCountQueryErrorSelectCondition(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
//		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
//		listener.setSelectCondition("t.VARCHAR='A'");
//		listener.configure();
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);
//
//		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='E' AND (SLOTID='slot' AND (t.VARCHAR='A')))";
//
//		assertEquals(expected, browser.getMessageCountQuery);
//	}
//
//	@DatabaseTest
//	public void testMessageBrowserContainsMessageId() throws Exception {
//		listener.configure();
//		listener.open();
//
//		String message ="fakeMessage";
//		insertARecord(message, 'M');
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
//
//		assertTrue(browser.containsMessageId("fakeMid"));
//	}
//
//	@DatabaseTest
//	public void testMessageBrowserContainsCorrelationId() throws Exception {
//		listener.configure();
//		listener.open();
//
//		String message ="fakeMessage";
//		insertARecord(message, 'M');
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
//
//		assertTrue(browser.containsCorrelationId("fakeCid"));
//	}
//
//	@DatabaseTest
//	public void testMessageBrowserBrowseMessage() throws Exception {
//		listener.configure();
//		listener.open();
//
//		String message ="fakeMessage";
//		String storageKey = insertARecord(message, 'M');
//		if (storageKey.startsWith("<id>")) {
//			storageKey = storageKey.substring(4, storageKey.length()-5);
//		}
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
//
//		RawMessageWrapper<?> ro = browser.browseMessage(storageKey);
//		assertEquals(storageKey, ro.getId());
//		Object o = ro.getRawMessage();
//		if (o instanceof MessageWrapper) {
//			MessageWrapper mw = (MessageWrapper)o;
//			assertEquals(message, mw.getMessage().asString());
//		} else {
//			assertEquals(message, o);
//		}
//	}
//
//	@DatabaseTest
//	public void testMessageBrowserIterator() throws Exception {
//		listener.configure();
//		listener.open();
//
//		String message ="fakeMessage";
//		String storageKey = insertARecord(message, 'M');
//		if (storageKey.startsWith("<id>")) {
//			storageKey = storageKey.substring(4, storageKey.length()-5);
//		}
//
//		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
//
//		IMessageBrowsingIterator iterator = browser.getIterator();
//		assertTrue(iterator.hasNext());
//
//		IMessageBrowsingIteratorItem item = iterator.next();
//		assertNotNull(item);
//
//		assertEquals(storageKey, item.getId());
//		assertEquals("fakeMid", item.getOriginalId());
//		assertEquals("fakeCid", item.getCorrelationId());
//		assertEquals("fakeComments", item.getCommentString());
//	}

	private String insertARecord(String message, char type) throws SenderException, ConfigurationException {
		storage.setType(type+"");
		storage.configure();
		storage.open();
		try {
			return storage.storeMessage("fakeMid", "fakeCid", new Date(), "fakeComments", "fakeLabel", message);
		} finally {
			storage.close();
		}
	}
}
