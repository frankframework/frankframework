package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.testutil.TestFileUtils;

public class StringResolverTest {

	Properties properties;

	@Before
	public void setUp() throws Exception {
		properties = new Properties();
		URL propertiesURL = TestFileUtils.getTestFileURL("/StringResolver.properties");
		assertNotNull("properties file [StringResolver.properties] not found!", propertiesURL);

		InputStream propsStream = propertiesURL.openStream();
		properties.load(propsStream);
		assertTrue("did not find any properties!", properties.size() > 0);
	}

	@Test
	public void resolveSimple() {
		String result = StringResolver.substVars("blalblalab ${key1}", properties);
		assertEquals("blalblalab value1", result);
	}

	@Test
	public void resolveRecursively() {
		String result = StringResolver.substVars("blalblalab ${key4}", properties);
		assertEquals("blalblalab value1.value2.value1", result);
	}

	@Test
	public void resolveRecursivelyAvoidStackOverflow() {
		String result = StringResolver.substVars("blalblalab ${key5}", properties);
		assertEquals("blalblalab ${key5}", result);
	}

	@Test
	public void resolveComplexRecursively() {
		String result = StringResolver.substVars("blalblalab ${key1_${key2}}", properties);
		assertEquals("blalblalab value101", result);
	}
}
