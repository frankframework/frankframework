package org.frankframework.logging;

import static org.frankframework.logging.FrankLogConfigurationFactory.LOG4J_PROPERTY_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FrankLogConfigurationFactoryTest {

	@ParameterizedTest
	@CsvSource({
			"${ctx:security.log.level:-INFO},security.log.level",
			"${ctx:security.level.log:INFO},security.level.log:INFO",
			"${ctx:security.level},security.level",
			"${ctx::-},",
			"${ctx::-X},",
			"${ctx:d:-},d",
			"${ctx:e},e",
			"${ctx:},",
			"${ctx:,",
			"${ctx:f:-F,",
			"${ctx:fail:-F,",
			"${ctx:f,",
			"${ctx:fail,",
			"${ctx.log},",
			"${log.dir},",
	})
	void testExtractPropertyNameRegex(String expression, String expected) {
		// Act
		Matcher m = Pattern.compile(LOG4J_PROPERTY_REGEX).matcher(expression);

		// Assert
		boolean found = m.find();
		if (expected == null) {
			assertFalse(found, "Should not have found a match in expression [" + expression + "]");
			return;
		}
		assertTrue(found, "Should have found a match in expression [" + expression + "]");
		String match = m.group(1);
		assertEquals(expected, match);
	}

	@Test
	void testPopulateSubstitutions() {
		// Arrange
		String config =
				"""
				<test-xml>
				    <tag>${ctx:log.dir}</tag>
				    <tag>${ctx:security.log.level:-INFO}</tag>
				    <tag>${ctx:config.log.level:DEBUG}</tag>
				    <tag attrib="${ctx:log.maxFileSize}"/>
				</test-xml>
				""";

		Properties properties = new Properties();
		properties.setProperty("security.log.level", "${global.log.level}");
		properties.setProperty("global.log.level", "WARN");
		properties.setProperty("config.log.level", "INFO");
		properties.setProperty("log.maxFileSize", "1MB");
		properties.setProperty("log.dir", "c:\\temp");

		Map<String, String> expected = new HashMap<>();
		expected.put("log.dir", "c:/temp");
		expected.put("security.log.level", "WARN");
		expected.put("log.maxFileSize", "1MB");
		List<Map.Entry<String, String>> expectedEntries = sortMapEntries(expected);

		// Act
		Map<String, String> result = FrankLogConfigurationFactory.populateThreadContextProperties(config, properties);

		// Assert
		assertIterableEquals(expectedEntries, sortMapEntries(result));
	}

	@Nonnull
	private static List<Map.Entry<String, String>> sortMapEntries(final Map<String, String> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
	}
}
