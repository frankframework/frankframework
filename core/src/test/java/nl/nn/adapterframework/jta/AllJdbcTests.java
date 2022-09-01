package nl.nn.adapterframework.jta;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import nl.nn.adapterframework.jdbc.FixedQuerySenderTest;
import nl.nn.adapterframework.jdbc.JdbcTableListenerTest;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorageTest;
import nl.nn.adapterframework.jdbc.MessageStoreListenerTest;
import nl.nn.adapterframework.jdbc.ResultSetIteratingPipeTest;
import nl.nn.adapterframework.jdbc.TransactionManagerTest;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportTest;
import nl.nn.adapterframework.jdbc.dbms.TestBlobs;
import nl.nn.adapterframework.jdbc.migration.MigratorTest;
import nl.nn.adapterframework.scheduler.CleanupDatabaseJobTest;
import nl.nn.adapterframework.util.LockerTest;
import nl.nn.adapterframework.util.MessageBrowsingFilterTest;

@RunWith(Suite.class)
@SuiteClasses({ 
	CleanupDatabaseJobTest.class,
	LockerTest.class,
	FixedQuerySenderTest.class,
	JdbcTableListenerTest.class,
	JdbcTransactionalStorageTest.class,
	MessageStoreListenerTest.class,
	MessageBrowsingFilterTest.class,
	ResultSetIteratingPipeTest.class,
	TransactionManagerTest.class,
	DbmsSupportTest.class,
	TestBlobs.class,
	MigratorTest.class,
	DbmsSupportTest.class,
	StatusRecordingTransactionManagerTest.class, 
	StatusRecordingTransactionManagerImplementationTest.class, 
	TransactionConnectorTest.class 
})
//@Ignore("to avoid duplicate run during build")
public class AllJdbcTests {

}
