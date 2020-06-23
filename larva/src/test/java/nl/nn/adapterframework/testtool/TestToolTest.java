package nl.nn.adapterframework.testtool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Properties;

import org.junit.Test;

public class TestToolTest {

    @Test
	public void mapDecodeUnzipContentBetweenKeysBasedOnIdentifier() {
        String propertyName = "decodeUnzipContentBetweenKeys";

        Properties scenario = new Properties();
        String key1 = "<field name='zip'>";
        String key2 = "</field'>";
        String replaceNewlines = "true";
        scenario.setProperty(propertyName + ".identifier.key1", key1);
        scenario.setProperty(propertyName + ".identifier.key2", key2);
        scenario.setProperty(propertyName + ".identifier.replaceNewlines", replaceNewlines);

        HashMap<String, HashMap<String, String>> result = TestTool.mapPropertiesByIdentifier(propertyName, scenario);

        assertEquals(result.size(), 1);
        HashMap<String, String> identifier = result.get("identifier");
        assertNotNull(identifier);

        assertEquals(identifier.get("key1"), key1);
        assertEquals(identifier.get("key2"), key2);
        assertEquals(identifier.get("replaceNewlines"), replaceNewlines);
	}
    
}