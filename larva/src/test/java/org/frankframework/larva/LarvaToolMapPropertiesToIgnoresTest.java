package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class LarvaToolMapPropertiesToIgnoresTest {
	@Test
	void decodeUnzipContentBetweenKeysFromIgnoreMap() {
		String propertyName = "decodeUnzipContentBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		String replaceNewlines = "true";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);
		scenario.setProperty(propertyName + ".identifier.replaceNewlines", replaceNewlines);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
		assertEquals(replaceNewlines, identifier.get("replaceNewlines"));
	}

	@Test
	void canonicaliseFilePathContentBetweenKeysFromIgnoreMap() {
		String propertyName = "canonicaliseFilePathContentBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void replaceRegularExpressionKeysFromIgnoreMap() {
		String propertyName = "replaceRegularExpressionKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void ignoreContentBetweenKeysFromIgnoreMap() {
		String propertyName = "ignoreContentBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void ignoreKeysAndContentBetweenKeysFromIgnoreMap() {
		String propertyName = "ignoreKeysAndContentBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void removeKeysAndContentBetweenKeysFromIgnoreMap() {
		String propertyName = "removeKeysAndContentBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void replaceKeyFromIgnoreMap() {
		String propertyName = "replaceKey";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void replaceEverywhereKeyFromIgnoreMap() {
		String propertyName = "replaceEverywhereKey";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void formatDecimalContentBetweenKeysFromIgnoreMap() {
		String propertyName = "formatDecimalContentBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
	}

	@Test
	void ignoreRegularExpressionKeyFromIgnoreMap() {
		String propertyName = "ignoreRegularExpressionKey";

		Properties scenario = new Properties();
		String key = "abc*";
		scenario.setProperty(propertyName + ".identifier.key", key);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key, identifier.get("key"));
	}

	@Test
	void removeRegularExpressionKeyFromIgnoreMap() {
		String propertyName = "removeRegularExpressionKey";

		Properties scenario = new Properties();
		String key = "abc*";
		scenario.setProperty(propertyName + ".identifier.key", key);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key, identifier.get("key"));
	}

	@Test
	void ignoreContentBeforeKeyFromIgnoreMap() {
		String propertyName = "ignoreContentBeforeKey";

		Properties scenario = new Properties();
		String key = "abc*";
		scenario.setProperty(propertyName + ".identifier.key", key);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key, identifier.get("key"));
	}

	@Test
	void ignoreContentAfterKeyFromIgnoreMap() {
		String propertyName = "ignoreContentAfterKey";

		Properties scenario = new Properties();
		String key = "abc*";
		scenario.setProperty(propertyName + ".identifier.key", key);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key, identifier.get("key"));
	}

	@Test
	void ignoreCurrentTimeBetweenKeysFromIgnoreMap() {
		String propertyName = "ignoreCurrentTimeBetweenKeys";

		Properties scenario = new Properties();
		String key1 = "<field name='zip'>";
		String key2 = "</field'>";
		String pattern = "YYYY-MM-DD";
		String margin = "0";
		String errorMessageOnRemainingString = "false";
		scenario.setProperty(propertyName + ".identifier.key1", key1);
		scenario.setProperty(propertyName + ".identifier.key2", key2);
		scenario.setProperty(propertyName + ".identifier.pattern", pattern);
		scenario.setProperty(propertyName + ".identifier.margin", margin);
		scenario.setProperty(propertyName + ".identifier.errorMessageOnRemainingString", errorMessageOnRemainingString);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key1, identifier.get("key1"));
		assertEquals(key2, identifier.get("key2"));
		assertEquals(pattern, identifier.get("pattern"));
		assertEquals(margin, identifier.get("margin"));
		assertEquals(errorMessageOnRemainingString, identifier.get("errorMessageOnRemainingString"));
	}

	@Test
	void removeKeyFromIgnoreMap() {
		String propertyName = "removeKey";

		Properties scenario = new Properties();
		String key = "<field name='zip'>";
		scenario.setProperty(propertyName + ".identifier.key", key);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key, identifier.get("key"));
	}

	@Test
	void removeKeyWithoutKeyFromIgnoreMap() {
		String propertyName = "removeKey";

		Properties scenario = new Properties();
		String value = "<field name='zip'>";
		scenario.setProperty(propertyName + ".identifier", value);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(value, identifier.get("value"));
	}

	@Test
	void ignoreKeyFromIgnoreMap() {
		String propertyName = "ignoreKey";

		Properties scenario = new Properties();
		String key = "<field name='zip'>";
		scenario.setProperty(propertyName + ".identifier.key", key);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(key, identifier.get("key"));
	}

	@Test
	void ignoreKeyWithoutKeyFromIgnoreMap() {
		String propertyName = "ignoreKey";

		Properties scenario = new Properties();
		String value = "<field name='zip'>";
		scenario.setProperty(propertyName + ".identifier", value);

		HashMap<String, HashMap<String, HashMap<String, String>>> result = LarvaTool.mapPropertiesToIgnores(scenario);
		assertNotNull(result);
		assertEquals(1, result.size());

		HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
		assertNotNull(ignore);
		assertEquals(1, ignore.size());

		HashMap<String, String> identifier = ignore.get("identifier");
		assertNotNull(identifier);

		assertEquals(value, identifier.get("value"));
	}

}
