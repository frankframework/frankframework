package nl.nn.adapterframework.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
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
import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.dbms.Dbms;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.junit.DatabaseTest;
import nl.nn.adapterframework.testutil.junit.DatabaseTestEnvironment;
import nl.nn.adapterframework.testutil.junit.WithLiquibase;
import nl.nn.adapterframework.util.LogUtil;

@WithLiquibase(tableName = MessageStoreListenerTest.tableName, file = "Migrator/JdbcTestBaseQuery.xml")
public class MessageStoreListenerTest {

	private MessageStoreListener listener;
	protected static final String tableName = "MSLT_TABLE";
	protected Liquibase liquibase;
	protected static Logger log = LogUtil.getLogger(MessageStoreListenerTest.class);
	private @Getter TestConfiguration configuration;
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
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2)); // tests are based on H2 syntax queries
		listener = new MessageStoreListener();
		listener = getConfiguration().createBean(MessageStoreListener.class);
		listener.setTableName(tableName);
		listener.setMessageIdField(messageIdField);
		listener.setSlotId(slotId);
		autowire(listener);
		getConfiguration().getIbisManager();
		getConfiguration().autowireByName(listener);

		storage = getConfiguration().createBean(JdbcTransactionalStorage.class);
		storage.setTableName(tableName);
		storage.setIdField(messageIdField);
		storage.setSlotId(slotId);
		System.setProperty("tableName", tableName);
		autowire(storage);

		runMigrator("Migrator/Ibisstore_4_unittests_changeset.xml", databaseTestEnvironment);
	}

	@AfterEach
	public void teardown(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		if(listener != null) {
			listener.close();
		}
		databaseTestEnvironment.close();
	}

	protected final Connection createNonTransactionalConnection(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
		Connection connection = databaseTestEnvironment.getConnection();
		connection.setAutoCommit(true); //Ensure this connection is NOT transactional!
		return connection;
	}

	//IBISSTORE_CHANGESET_PATH
	protected void runMigrator(String changeLogFile, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(createNonTransactionalConnection(databaseTestEnvironment)));
		liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), db);
		liquibase.forceReleaseLocks();
		StringWriter out = new StringWriter(2048);
		liquibase.reportStatus(true, new Contexts(), out);
		log.info("Liquibase Database: {}, {}", liquibase.getDatabase().getDatabaseProductName(), liquibase.getDatabase().getDatabaseProductVersion());
		log.info("Liquibase Database connection: {}", liquibase.getDatabase());
		log.info("Liquibase changeset status:");
		log.info(out.toString());
		liquibase.update(new Contexts());
	}

	protected void autowire(JdbcFacade jdbcFacade) {
		configuration.autowireByName(jdbcFacade);
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

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M' AND (SLOTID='slot')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M' AND (SLOTID='slot' AND (t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectConditionNoSlotId() throws ConfigurationException {
		listener.setSlotId(null);
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT MESSAGEKEY,MESSAGEID,CORRELATIONID,MESSAGE FROM "+tableName+" t WHERE TYPE='M' AND ((t.TVARCHAR='x'))";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testUpdateStatusQueryDone() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+tableName+" SET TYPE='A',MESSAGEDATE=NOW(),COMMENTS=?,EXPIRYDATE = NOW() + 30 WHERE TYPE!='A' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@DatabaseTest
	public void testUpdateStatusQueryDoneNoMoveToMessageLog() throws ConfigurationException {
		listener.setMoveToMessageLog(false);
		listener.configure();

		String expected = "DELETE FROM "+tableName+" WHERE MESSAGEKEY = ?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.DONE));
	}

	@DatabaseTest
	public void testUpdateStatusQueryError() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+tableName+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testUpdateStatusQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("1=1");
		listener.configure();

		String expected = "UPDATE "+tableName+" SET TYPE='E',MESSAGEDATE=NOW(),COMMENTS=? WHERE TYPE!='E' AND MESSAGEKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailable(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='M' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryError(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='E' AND (SLOTID='slot'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailableWithSelectCondition(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='M' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryErrorSelectCondition(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+tableName+" t WHERE (TYPE='E' AND (SLOTID='slot' AND (t.VARCHAR='A')))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testMessageBrowserContainsMessageId() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		insertARecord(message, 'M');

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		assertTrue(browser.containsMessageId("fakeMid"));
	}

	@DatabaseTest
	public void testMessageBrowserContainsCorrelationId() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		insertARecord(message, 'M');

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		assertTrue(browser.containsCorrelationId("fakeCid"));
	}

	@DatabaseTest
	public void testMessageBrowserBrowseMessage() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		String storageKey = insertARecord(message, 'M');
		if (storageKey.startsWith("<id>")) {
			storageKey = storageKey.substring(4, storageKey.length()-5);
		}

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		RawMessageWrapper<?> ro = browser.browseMessage(storageKey);
		assertEquals(storageKey, ro.getId());
		Object o = ro.getRawMessage();
		if (o instanceof MessageWrapper) {
			MessageWrapper mw = (MessageWrapper)o;
			assertEquals(message, mw.getMessage().asString());
		} else {
			assertEquals(message, o);
		}
	}

	@DatabaseTest
	public void testMessageBrowserIterator() throws Exception {
		listener.configure();
		listener.open();

		String message ="fakeMessage";
		String storageKey = insertARecord(message, 'M');
		if (storageKey.startsWith("<id>")) {
			storageKey = storageKey.substring(4, storageKey.length()-5);
		}

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		IMessageBrowsingIterator iterator = browser.getIterator();
		assertTrue(iterator.hasNext());

		IMessageBrowsingIteratorItem item = iterator.next();
		assertNotNull(item);

		assertEquals(storageKey, item.getId());
		assertEquals("fakeMid", item.getOriginalId());
		assertEquals("fakeCid", item.getCorrelationId());
		assertEquals("fakeComments", item.getCommentString());
	}

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
