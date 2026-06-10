package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class H2DbmsSupportTest {

	static Stream<Arguments> testCustomSqlModeFromUrl() {
		return Stream.of(
				arguments("jdbc:h2:mem:test_db2;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;MODE=MSSQLServer", "Microsoft SQL Server"),
				arguments("jdbc:h2:mem:test_db2;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0;MODE=Oracle", "Oracle"),
				arguments("jdbc:h2:mem:test_db2;MODE=Oracle;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0", "Oracle"),
				arguments("MODE=Oracle;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0", "Oracle"),
				arguments("mode=Oracle;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0", "Oracle"),
				arguments("jdbc:h2:mem:test_db2;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0", "H2")
		);
	}

	@Test
	void testConvertQueryOracleToH2() throws JdbcException {
		String query = "DROP SEQUENCE SEQ_IBISSTORE";
		String expected = "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE";
		String translatedQuery = (new H2DbmsSupport(null, "")).convertQuery(query, "Oracle");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testConvertQueryH2ToH2() throws JdbcException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String expected = query;
		String translatedQuery = (new H2DbmsSupport(null, "")).convertQuery(query, "H2");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testConvertMultipleQueriesOracleToH2() throws JdbcException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String expected = "--------\n  --drop--\r\n--------\nDROP SEQUENCE IF EXISTS SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String translatedQuery = (new H2DbmsSupport(null, "")).convertQuery(query, "Oracle");
		assertEquals(expected, translatedQuery);
	}

	@ParameterizedTest
	@MethodSource
	void testCustomSqlModeFromUrl(String url, String expected) {
		// Act
		H2DbmsSupport h2DbmsSupport = new H2DbmsSupport(null, url);

		// Assert
		assertEquals(expected, h2DbmsSupport.getTargetSqlDialect());
	}

	@Test
	void testCustomSqlModeInvalidValues() {
		// Act / Assert
		assertThrows(IllegalArgumentException.class, () -> new H2DbmsSupport("", "MODE=LEGACY"));
	}
}
