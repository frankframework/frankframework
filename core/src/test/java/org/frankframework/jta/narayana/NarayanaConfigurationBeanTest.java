package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import com.arjuna.ats.arjuna.common.MetaObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.AppConstants;

@Log4j2
@Isolated
class NarayanaConfigurationBeanTest {

	private Path txLogPath;
	private MetaObjectStoreEnvironmentBean jdbcStoreEnvironment;
	private String objectStoreType;

	@BeforeEach
	void setUp() {
		// Make sure that we don't re-use an existing TestConfiguration
		TransactionManagerType.NARAYANA.closeConfigurationContext();
		AppConstants.removeInstance();

		// Workarounds for the fact that Narayana uses a lot of JVM-global instances that get reused
		// across tests because they cannot be destroyed.
		jdbcStoreEnvironment = BeanPopulator.getDefaultInstance(MetaObjectStoreEnvironmentBean.class);
		objectStoreType = jdbcStoreEnvironment.getObjectStoreType();
	}

	@AfterEach
	void tearDown() {
		// Workarounds for the fact that Narayana uses a lot of JVM-global instances that get reused
		// across tests because they cannot be destroyed.
		jdbcStoreEnvironment.setObjectStoreType(objectStoreType);
		jdbcStoreEnvironment.setJdbcDataSource(null);

		AppConstants.clearGlobalProperty("transactionmanager.narayana.objectStoreType");
		AppConstants.clearGlobalProperty("transactionmanager.narayana.objectStoreDatasource");
		TransactionManagerType.NARAYANA.closeConfigurationContext();
		AppConstants.removeInstance();

		if (txLogPath != null) {
			try {
				Files.deleteIfExists(txLogPath);
			} catch (IOException e) {
				log.warn(() -> "Error deleting txlog_db.mv.db at " + txLogPath, e);
			}
		}
	}

	@Test
	void testConfigureNarayanaJdbcObjectStoreFromResources() {
		// This test does not test that the ObjectStore is actually used, because that doesn't work
		// if Narayana has already created different object stores.

		// Arrange
		AppConstants.setGlobalProperty("transactionmanager.narayana.objectStoreType", "com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");
		AppConstants.setGlobalProperty("transactionmanager.narayana.objectStoreDatasource", "jdbc/H2-txlog");

		// Workarounds for the fact that Narayana uses a lot of JVM-global instances that get reused
		// across tests because they cannot be destroyed.
		jdbcStoreEnvironment.setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");

		try (TestAppender testAppender = TestAppender.newBuilder().build()) {

			// Act

			// Creating the configuration creates the beans and validates the object-store datasource
			TransactionManagerType.NARAYANA.create(true);

			// Assert
			List<String> logLines = testAppender.getLogLines();
			MatcherAssert.assertThat(logLines, CoreMatchers.hasItem(CoreMatchers.containsString("found Narayana ObjectStoreDatasource")));
			MatcherAssert.assertThat(logLines, CoreMatchers.hasItem(CoreMatchers.containsString("jdbc/H2-txlog")));
		}
	}

	@Test
	@Disabled("This test only works in a clean JVM when not other tests have executed that create a Narayana instance")
	void testUseNarayanaJdbcObjectStoreFromResources() {
		// Arrange
		AppConstants.setGlobalProperty("transactionmanager.narayana.objectStoreType", "com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");
		AppConstants.setGlobalProperty("transactionmanager.narayana.objectStoreDatasource", "jdbc/H2-txlog");

		// Workarounds for the fact that Narayana uses a lot of JVM-global instances that get reused
		// across tests because they cannot be destroyed.
		jdbcStoreEnvironment.setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");

		try (TestAppender testAppender = TestAppender.newBuilder().build()) {

			// Act

			// Creating the configuration creates the beans and validates the object-store datasource
			TransactionManagerType.NARAYANA.create(true);

			// Assert
			List<String> logLines = testAppender.getLogLines();
			MatcherAssert.assertThat(logLines, CoreMatchers.hasItem(CoreMatchers.containsString("found Narayana ObjectStoreDatasource")));

			// If Narayana has already been instantiated before with the file-based recovery store it will still use that and no DB file will be craeted,
			// so this test fails unless executed in a clean JVM.
			String userDir = AppConstants.getInstance().getProperty("user.dir");
			String txLogDir = AppConstants.getInstance().getProperty("transactionmanager.log.dir");
			txLogPath = Paths.get(userDir, txLogDir, "txlog_db.mv.db");
			assertTrue(Files.exists(txLogPath), "Expected to find txlog_db at " + txLogPath);
		}
	}
}
