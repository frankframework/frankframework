package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.TestFileUtils;

public class StringResolverTest {

	Properties properties;

	@BeforeEach
	public void setUp() throws Exception {
		properties = new Properties();
		URL propertiesURL = TestFileUtils.getTestFileURL("/StringResolver.properties");
		assertNotNull(propertiesURL, "properties file [StringResolver.properties] not found!");

		InputStream propsStream = propertiesURL.openStream();
		properties.load(propsStream);
		assertTrue(properties.size() > 0, "did not find any properties!");
		
		System.setProperty("authAliases.expansion.allowed", "alias1,alias2");
	}

	@Test
	public void resolveSimple() {
		String result = StringResolver.substVars("blalblalab ${key1}", properties);
		assertEquals("blalblalab value1", result);

		result = StringResolver.substVars("blalblalab ${key1}", properties, true);
		assertEquals("blalblalab ${key1:-value1}", result);
	}

	@Test
	public void resolveNonExistent() {
		String result = StringResolver.substVars("blalblalab ${key7}", properties);
		assertEquals("blalblalab ", result);

		result = StringResolver.substVars("blalblalab ${key7}", properties, true);
		assertEquals("blalblalab ${key7:-}", result);
	}

	@Test
	public void resolveNonExistentWithDefault() {
		String result = StringResolver.substVars("blalblalab ${dir}", properties);
		assertEquals("blalblalab d:/temporary/dir", result);

		result = StringResolver.substVars("blalblalab ${dir}", properties, true);
		assertEquals("blalblalab ${dir:-${path:-d:/temporary}/dir}", result);
	}

	@Test
	public void resolveWithDefault() {
		String result = StringResolver.substVars("blalblalab ${dir2}", properties);
		assertEquals("blalblalab c:/temp/dir", result);

		result = StringResolver.substVars("blalblalab ${dir2}", properties, true);
		assertEquals("blalblalab ${dir2:-${log.dir:-c:/temp}/dir}", result);
	}

	@Test
	public void resolveRecursively() {
		String result = StringResolver.substVars("blalblalab ${key4}", properties);
		assertEquals("blalblalab value1.value2.value1", result);

		result = StringResolver.substVars("blalblalab ${key4}", properties, true);
		assertEquals("blalblalab ${key4:-${key1:-value1}.${key3:-value2.${key1:-value1}}}", result);
	}

	@Test
	public void resolveRecursivelyAvoidStackOverflow() {
		String result = StringResolver.substVars("blalblalab ${key5}", properties);
		assertEquals("blalblalab ${key5}", result);

		result = StringResolver.substVars("blalblalab ${key5}", properties, true);
		assertEquals("blalblalab ${key5:-${key5}}", result);
	}

	@Test
	public void resolveComplexRecursively() {
		String result = StringResolver.substVars("blalblalab ${key1_${key2}}", properties);
		assertEquals("blalblalab value101", result);

		result = StringResolver.substVars("blalblalab ${key1_${key2}}", properties, true);
		assertEquals("blalblalab ${key1_${key2:-${key1:-value1}}:-value101}", result);

	}
	
	@Test
	public void resolveMFHNested() {
		String result = StringResolver.substVars("${mfh}", properties, false);
		assertEquals("c:/temp/testdata/mfh", result);

		result = StringResolver.substVars("${mfh}", properties, true);
		assertEquals("${mfh:-${testdata:-${log.dir:-c:/temp}/testdata}/mfh}", result);

	}

	@Test
	public void resolveComplexProperty() {
		String result = StringResolver.substVars("${testMultiResolve}", properties);
		assertEquals("one,two,three_value1value1,my_value2.value1,StageSpecifics_value1.value2.value1", result);

		result = StringResolver.substVars("${testMultiResolve}", properties, true);
		assertEquals("${testMultiResolve:-one,two,three_${key1:-value1}${key2:-${key1:-value1}},my_${key3:-value2.${key1:-value1}},StageSpecifics_${key4:-${key1:-value1}.${key3:-value2.${key1:-value1}}}}", result);
	}

	@Test
	public void resolveCyclicProperty() {
		String result = StringResolver.substVars("${cyclic}", properties);
		assertEquals("prefix ${cyclic} suffix", result);

		result = StringResolver.substVars("${cyclic}", properties, true);
		assertEquals("${cyclic:-prefix ${cyclic} suffix}", result);
	}

	
	@Test
	public void resolveUsername() {
		// N.B. the notation ${credential:alias1/username} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:username:alias1}", properties);
		assertEquals("username1", result);

		result = StringResolver.substVars("${credential:username:alias1}", properties, true);
		assertEquals("${credential:username:alias1:-username1}", result);
	}

	@Test
	public void resolvePassword1() {
		// N.B. the notation ${credential:alias1/password} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:password:alias1}", properties);
		assertEquals("password1", result);

		result = StringResolver.substVars("${credential:password:alias1}", properties, true);
		assertEquals("${credential:password:alias1:-password1}", result);
	}

	@Test
	public void resolvePassword2() {
		String result = StringResolver.substVars("${credential:alias1}", properties); // the 'credential:' prefix defaults to return the password
		assertEquals("password1", result);

		result = StringResolver.substVars("${credential:alias1}", properties, true);
		assertEquals("${credential:alias1:-password1}", result);
	}

	@Test
	public void resolvePasswordOnlyAlias() {
		String result = StringResolver.substVars("${credential:alias2}", properties);
		assertEquals("passwordOnly", result);

		result = StringResolver.substVars("${credential:alias2}", properties, true);
		assertEquals("${credential:alias2:-passwordOnly}", result);
	}

	@Test
	public void resolvePasswordOnlyAlias2() {
		String result = StringResolver.substVars("${credential:password:alias2}", properties);
		assertEquals("passwordOnly", result);

		result = StringResolver.substVars("${credential:password:alias2}", properties, true);
		assertEquals("${credential:password:alias2:-passwordOnly}", result);
	}

	@Test
	public void resolvePasswordNotAllowed() {
		String result = StringResolver.substVars("${credential:password:alias3}", properties);
		assertEquals("!!not allowed to expand credential of authAlias [alias3]!!", result);

		result = StringResolver.substVars("${credential:password:alias3}", properties, true);
		assertEquals("${credential:password:alias3:-!!not allowed to expand credential of authAlias [alias3]!!}", result);
	}
}
