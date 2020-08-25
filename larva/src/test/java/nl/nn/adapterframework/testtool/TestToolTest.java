package nl.nn.adapterframework.testtool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Properties;

import org.junit.Test;

public class TestToolTest {

    @Test
	public void decodeUnzipContentBetweenKeysFromIgnoreMap() {
        String propertyName = "decodeUnzipContentBetweenKeys";

        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        String replaceNewlines = "true";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);
        scenario.setProperty(propertyName + ".identifier.replaceNewlines", replaceNewlines);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
        assertEquals(identifier.get("replaceNewlines"), replaceNewlines);
    }
    
    @Test
    public void canonicaliseFilePathContentBetweenKeysFromIgnoreMap() {
        String propertyName = "canonicaliseFilePathContentBetweenKeys";

        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void replaceRegularExpressionKeysFromIgnoreMap() {
        String propertyName = "replaceRegularExpressionKeys";

        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void ignoreContentBetweenKeysFromIgnoreMap() {
        String propertyName = "ignoreContentBetweenKeys";
        
        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void ignoreKeysAndContentBetweenKeysFromIgnoreMap() {
        String propertyName = "ignoreKeysAndContentBetweenKeys";
        
        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void removeKeysAndContentBetweenKeysFromIgnoreMap() {
        String propertyName = "removeKeysAndContentBetweenKeys";
        
        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void replaceKeyFromIgnoreMap() {
        String propertyName = "replaceKey";
        
        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void replaceEverywhereKeyFromIgnoreMap() {
        String propertyName = "replaceEverywhereKey";
        
        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void formatDecimalContentBetweenKeysFromIgnoreMap() {
        String propertyName = "formatDecimalContentBetweenKeys";
        
        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
    }

    @Test
    public void ignoreRegularExpressionKeyFromIgnoreMap() {
        String propertyName = "ignoreRegularExpressionKey";
        
        Properties scenario = new Properties();
        String key = "abc*";
        scenario.setProperty(propertyName + ".identifier.key", key);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key"), key);
    }

    @Test
    public void removeRegularExpressionKeyFromIgnoreMap() {
        String propertyName = "removeRegularExpressionKey";
        
        Properties scenario = new Properties();
        String key = "abc*";
        scenario.setProperty(propertyName + ".identifier.key", key);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key"), key);
    }

    @Test
    public void ignoreContentBeforeKeyFromIgnoreMap() {
        String propertyName = "ignoreContentBeforeKey";
        
        Properties scenario = new Properties();
        String key = "abc*";
        scenario.setProperty(propertyName + ".identifier.key", key);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key"), key);
    }

    @Test
    public void ignoreContentAfterKeyFromIgnoreMap() {
        String propertyName = "ignoreContentAfterKey";
        
        Properties scenario = new Properties();
        String key = "abc*";
        scenario.setProperty(propertyName + ".identifier.key", key);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key"), key);
    }

    @Test
	public void ignoreCurrentTimeBetweenKeysFromIgnoreMap() {
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

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
        assertEquals(identifier.get("pattern"), pattern);
        assertEquals(identifier.get("margin"), margin);
        assertEquals(identifier.get("errorMessageOnRemainingString"), errorMessageOnRemainingString);
    }

    @Test
    public void removeKeyFromIgnoreMap() {
        String propertyName = "removeKey";
        
        Properties scenario = new Properties();
        String key = "<field name='zip'>";
        scenario.setProperty(propertyName + ".identifier.key", key);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key"), key);
    }

    @Test
    public void removeKeyWithoutKeyFromIgnoreMap() {
        String propertyName = "removeKey";
        
        Properties scenario = new Properties();
        String value = "<field name='zip'>";
        scenario.setProperty(propertyName + ".identifier", value);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("value"), value);
    }

    @Test
    public void ignoreKeyFromIgnoreMap() {
        String propertyName = "ignoreKey";
        
        Properties scenario = new Properties();
        String key = "<field name='zip'>";
        scenario.setProperty(propertyName + ".identifier.key", key);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key"), key);
    }

    @Test
    public void ignoreKeyWithoutKeyFromIgnoreMap() {
        String propertyName = "ignoreKey";
        
        Properties scenario = new Properties();
        String value = "<field name='zip'>";
        scenario.setProperty(propertyName + ".identifier", value);

        HashMap<String, HashMap<String, HashMap<String, String>>> result = TestTool.mapPropertiesToIgnores(scenario);
        assertNotNull(result);
        assertEquals(result.size(), 1);

        HashMap<String, HashMap<String, String>> ignore = result.get(propertyName);
        assertNotNull(ignore);
        assertEquals(ignore.size(), 1);

        HashMap<String, String> identifier = ignore.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("value"), value);
    }
    
}