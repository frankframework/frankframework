package nl.nn.adapterframework.logging;

import static nl.nn.adapterframework.logging.IbisLoggerConfigurationFactory.LOG4J_PROPERTY_REGEX;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

class IbisLoggerConfigurationFactoryTest {

	@Test
	void testExtractPropertyNameRegex() {
		// Arrange
		String config =
				"${ctx:security.log.level:-INFO}\n" + // Should Match
				"${ctx:security.level.log:-}\n" + // Should Match
				"${ctx:security.level:x}\n" + // Should Match
				"${ctx::-}\n" + // Shouldn't Match
				"${ctx:d:-}\n" + // Should Match
				"${ctx::-X}\n" + // Shouldn't Match
				"${ctx:e}\n" + // Should Match
				"${ctx:}\n" + // Shouldn't Match
				"${ctx.log}\n" + // Shouldn't Match
				"${log.dir}\n" + // Shouldn't Match
				"${ctx:security.log}"; // Should Match

		// Act
		Matcher m = Pattern.compile(LOG4J_PROPERTY_REGEX).matcher(config);
		List<String> matches = new ArrayList<>();
		while (m.find()) {
			matches.add(m.group(1));
		}

		// Assert
		assertIterableEquals(Arrays.asList("security.log.level", "security.level.log", "security.level:x", "d", "e", "security.log"), matches);
	}

	@Test
	void testPopulateSubstitutions() {
		// Arrange
		String config =
				"<test-xml>\n" +
				"    <tag>${ctx:log.dir}</tag>\n" +
				"    <tag>${ctx:security.log.level:-INFO}</tag>\n" +
				"    <tag>${ctx:config.log.level:DEBUG}</tag>\n" + // This entry not picked up b/c not correctly specified
				"    <tag attrib=\"${ctx:log.maxFileSize}\"/>\n" +
				"</test-xml>\n";

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
		Map<String, String> result = IbisLoggerConfigurationFactory.populateThreadContextProperties(config, properties);

		// Assert
		assertIterableEquals(expectedEntries, sortMapEntries(result));
	}

	@Nonnull
	private static List<Map.Entry<String, String>> sortMapEntries(final Map<String, String> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
	}
}
