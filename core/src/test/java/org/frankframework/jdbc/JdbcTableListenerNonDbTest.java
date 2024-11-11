package org.frankframework.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ConfigurationWarnings;

class JdbcTableListenerNonDbTest {

	private JdbcTableListener<String> jdbcTableListener;
	private ConfigurationWarnings warnings;

	@BeforeEach
	void setUp() {
		jdbcTableListener = new JdbcTableListener<>();

		warnings = new ConfigurationWarnings();
		ApplicationContext mockContext = mock();
		when(mockContext.getBean("configurationWarnings", ConfigurationWarnings.class)).thenReturn(warnings);
		jdbcTableListener.setApplicationContext(mockContext);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"A_FIELD IS NULL",
			"F3 = A_FIELD",
			"F = TRUE AND A_FIELD IS NULL"
	})
	public void testVerifyFieldNotInQueryMatch(String queryText) {
		// Act
		jdbcTableListener.verifyFieldNotInQuery("A_FIELD", queryText);

		// Assert
		assertFalse(warnings.isEmpty());
		assertThat(warnings.getWarnings(), hasItem(containsString("may not reference the timestampField or commentField. Found: [A_FIELD]")));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"A_FIELD_2 IS NULL",
			"F3 = B_A_FIELD",
			"F = TRUE AND A_FIELD_2 IS NULL"
	})
	public void testVerifyFieldNotInQueryNoMatch(String queryText) {
		// Act
		jdbcTableListener.verifyFieldNotInQuery("A_FIELD", queryText);

		// Assert
		assertTrue(warnings.isEmpty());
	}
}
