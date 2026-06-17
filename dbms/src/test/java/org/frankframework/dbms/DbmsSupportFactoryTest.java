package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DbmsSupportFactoryTest {

	@ParameterizedTest
	@CsvSource({
			// Since a connection is required for these tests, all datasources are for in-memory H2
			"'jdbc:h2:mem:test;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;', H2",
			"'jdbc:h2:mem:test_db_mssql;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;MODE=MSSQLServer', 'Microsoft SQL Server'",
			"'jdbc:h2:mem:test_db_ora;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;MODE=Oracle', Oracle"
	})
	void getDbmsSupportFromDataSource(String dataSourceUrl, String expectedSqlDialect) {
		// Arrange
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUrl(dataSourceUrl);
		DbmsSupportFactory factory = new DbmsSupportFactory();

		// Act
		IDbmsSupport dbmsSupport = factory.getDbmsSupport(dataSource);

		// Assert
		assertEquals(Dbms.H2, dbmsSupport.getDbms());
		assertEquals(expectedSqlDialect, dbmsSupport.getTargetSqlDialect());
	}
}
