package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class H2DbmsSupportTest {

	static Stream<Arguments> testCustomSqlModeFromMap() {
		return Stream.of(
				arguments("MSSQLServer", "Microsoft SQL Server"),
				arguments("Oracle", "Oracle"),
				arguments("", "H2"),
				arguments("LEGACY", "H2")
		);
	}

	@Test
	void testConvertQueryOracleToH2() throws JdbcException {
		String query = "DROP SEQUENCE SEQ_IBISSTORE";
		String expected = "DROP SEQUENCE IF EXISTS SEQ_IBISSTORE";
		String translatedQuery = (new H2DbmsSupport(null, Map.of())).convertQuery(query, "Oracle");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testConvertQueryH2ToH2() throws JdbcException {
		String query = "SELECT COUNT(*) FROM IBISSTORE";
		String expected = query;
		String translatedQuery = (new H2DbmsSupport(null, Map.of())).convertQuery(query, "H2");
		assertEquals(expected, translatedQuery);
	}

	@Test
	void testConvertMultipleQueriesOracleToH2() throws JdbcException {
		String query = "--------\n  --drop--\r\n--------\nDROP SEQUENCE SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String expected = "--------\n  --drop--\r\n--------\nDROP SEQUENCE IF EXISTS SEQ_IBISSTORE;\nselect count(*) from ibisstore;";
		String translatedQuery = (new H2DbmsSupport(null, Map.of())).convertQuery(query, "Oracle");
		assertEquals(expected, translatedQuery);
	}

	@ParameterizedTest
	@MethodSource
	void testCustomSqlModeFromMap(String mode, String expected) {
		// Act
		H2DbmsSupport h2DbmsSupport = new H2DbmsSupport(null, Map.of("MODE", mode));

		// Assert
		assertEquals(expected, h2DbmsSupport.getTargetSqlDialect());
	}
}
