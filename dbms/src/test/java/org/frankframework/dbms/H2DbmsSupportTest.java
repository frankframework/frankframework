package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class H2DbmsSupportTest {

	@Test
	void testConvertQueryOracleToH2() throws JdbcException {
		String query = "DROP SEQUENCE SEQ_IBISSTORE";
		String expected = "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE";
		String translatedQuery = (new H2DbmsSupport(null, null)).convertQuery(query, "Oracle");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testConvertQueryH2ToH2() throws JdbcException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String expected = query;
		String translatedQuery = (new H2DbmsSupport(null, null)).convertQuery(query, "H2");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testConvertMultipleQueriesOracleToH2() throws JdbcException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String expected = "--------\n  --drop--\r\n--------\nDROP SEQUENCE IF EXISTS SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String translatedQuery = (new H2DbmsSupport(null, null)).convertQuery(query, "Oracle");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testCannotCreateWithoutParams() {
		assertThrows(IllegalStateException.class, H2DbmsSupport::new);
	}

	@ParameterizedTest
	@CsvSource({
			// Since a connection is required for these tests, all datasources are for in-memory H2
			"'jdbc:h2:mem:test;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;', H2",
			"'jdbc:h2:mem:test_db_mssql;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;MODE=MSSQLServer', 'Microsoft SQL Server'",
			"'jdbc:h2:mem:test_db_ora;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;MODE=Oracle', Oracle"
	})
	void testCreateInstanceWithConnection(String dataSourceUrl, String expectedSqlDialect) throws SQLException {
		// Arrange
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUrl(dataSourceUrl);
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();

			// Act
			IDbmsSupport dbmsSupport = new H2DbmsSupport(metaData.getDatabaseProductVersion(), connection);

			// Assert
			assertEquals(Dbms.H2, dbmsSupport.getDbms());
			assertEquals(expectedSqlDialect, dbmsSupport.getTargetSqlDialect());
		}
	}
}
