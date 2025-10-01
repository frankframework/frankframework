package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.arjuna.ats.arjuna.common.MetaObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
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
	void testNarayanaJdbcObjectStoreFromResources() throws SQLException {
		// Arrange
		AppConstants.setGlobalProperty("transactionmanager.narayana.objectStoreType", "com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");
		AppConstants.setGlobalProperty("transactionmanager.narayana.objectStoreDatasource", "jdbc/H2-txlog");

		// Workarounds for the fact that Narayana uses a lot of JVM-global instances that get reused
		// across tests because they cannot be destroyed.
		jdbcStoreEnvironment.setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");

		try (TestAppender testAppender = TestAppender.newBuilder().build()) {

			// Act

			// Creating the configuration creates the beans and validates the object-store datasource
			TestConfiguration configuration = TransactionManagerType.NARAYANA.create(true);

			// Create a transaction to make sure that a transaction-database is indeed created.
			PlatformTransactionManager transactionManager = configuration.getBean("txManagerReal", PlatformTransactionManager.class);
			DataSource dataSource = TransactionManagerType.NARAYANA.getDataSource("H2");
			TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
			runSqlCommands(dataSource);
			transactionManager.commit(txStatus);

			// Assert

			List<String> logLines = testAppender.getLogLines();
			MatcherAssert.assertThat(logLines, CoreMatchers.hasItem(CoreMatchers.containsString("found Narayana ObjectStoreDatasource")));

			String txLogDir = AppConstants.getInstance().getProperty("transactionmanager.log.dir");
			txLogPath = Paths.get(txLogDir, "txlog_db.mv.db");
			assertTrue(Files.exists(txLogPath), "Expected to find txlog_db at " + txLogPath);
		}
	}

	private static void runSqlCommands(DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES");
		) {
			statement.execute();
		}
	}
}
