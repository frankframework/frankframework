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
		
		System.setProperty("authAliases.expansion.allowed", "alias1,alias2");
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

	@Test
	public void resolveComplexProperty() {
		String result = StringResolver.substVars("${testMultiResolve}", properties);
		assertEquals("one,two,three_value1value1,my_value2.value1,StageSpecifics_value1.value2.value1", result);
	}

	@Test
	public void resolveCyclicProperty() {
		String result = StringResolver.substVars("${cyclic}", properties);
		assertEquals("prefix ${cyclic} suffix", result);
	}

	
	@Test
	public void resolveUsername() {
		// N.B. the notation ${credential:alias1/username} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:username:alias1}", properties);
		assertEquals("username1", result);
	}

	@Test
	public void resolvePassword1() {
		// N.B. the notation ${credential:alias1/password} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:password:alias1}", properties);
		assertEquals("password1", result);
	}

	@Test
	public void resolvePassword2() {
		String result = StringResolver.substVars("${credential:alias1}", properties); // the 'credential:' prefix defaults to return the password
		assertEquals("password1", result);
	}

	@Test
	public void resolvePasswordOnlyAlias() {
		String result = StringResolver.substVars("${credential:alias2}", properties);
		assertEquals("passwordOnly", result);
	}

	@Test
	public void resolvePasswordOnlyAlias2() {
		String result = StringResolver.substVars("${credential:password:alias2}", properties);
		assertEquals("passwordOnly", result);
	}

	@Test
	public void resolvePasswordNotAllowed() {
		String result = StringResolver.substVars("${credential:password:alias3}", properties);
		assertEquals("!!not allowed to expand credential of authAlias [alias3]!!", result);
	}
}
