package nl.nn.adapterframework.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import nl.nn.adapterframework.stream.Message;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.TestFileUtils;

import static org.junit.jupiter.api.Assertions.*;

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
	public void resolveFromEnvironment() {
		// Arrange
		String envVarName = System.getenv().containsKey("Path") ? "Path" : "PATH";
		String substString = "${" + envVarName + "}";

		// Act
		String result = StringResolver.substVars(substString, null);

		// Assert
		assertNotNull(result);
		assertFalse(StringUtils.isBlank(result));
		assertTrue(result.contains(File.pathSeparator));

		// Act
		result = StringResolver.substVars(substString, null, true);

		// Assert
		assertTrue(result.startsWith("${" + envVarName + ":-"));
		assertTrue(result.contains(File.pathSeparator));
	}

	@Test
	public void resolveSimple() {
		String result = StringResolver.substVars("blalblalab ${key1}", properties);
		assertEquals("blalblalab value1", result);

		result = StringResolver.substVars("blalblalab ${key1}", properties, true);
		assertEquals("blalblalab ${key1:-value1}", result);
	}

	@Test
	public void resolveSimpleFromProps2() {
		// Arrange
		Map<Object, Object> emptyMap = Collections.emptyMap();

		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", emptyMap, properties);

		// Assert
		assertEquals("blalblalab value1", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", emptyMap, properties, true);

		// Assert
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
	public void resolveNonExistentWithProps2() {
		String result = StringResolver.substVars("blalblalab ${key7}", null, properties);
		assertEquals("blalblalab ", result);

		result = StringResolver.substVars("blalblalab ${key7}", null, properties, true);
		assertEquals("blalblalab ${key7:-}", result);
	}

	@Test
	public void resolveNonExistentWithDefaultWithProps2() {
		String result = StringResolver.substVars("blalblalab ${dir}", null, properties);
		assertEquals("blalblalab d:/temporary/dir", result);

		result = StringResolver.substVars("blalblalab ${dir}",null, properties, true);
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
	public void resolveWithDefaultFromProps2() {
		// Arrange
		Map<Object, Object> emptyMap = Collections.emptyMap();

		// Act
		String result = StringResolver.substVars("blalblalab ${dir2}", emptyMap, properties);

		// Assert
		assertEquals("blalblalab c:/temp/dir", result);

		// Act
		result = StringResolver.substVars("blalblalab ${dir2}", emptyMap, properties, true);

		// Assert
		assertEquals("blalblalab ${dir2:-${log.dir:-c:/temp}/dir}", result);
	}

	@Test
	public void resolveResultIsMessage() {
		// Arrange
		Message message = Message.asMessage("My Message");
		Map<String, Object> propsMap = Collections.singletonMap("msg1", message);

		// Act
		String result = StringResolver.substVars("blalblalab ${msg1}", propsMap);

		// Assert
		assertEquals("blalblalab My Message", result);

		// Act
		result = StringResolver.substVars("blalblalab ${msg1}", propsMap, true);

		// Assert
		assertEquals("blalblalab ${msg1:-My Message}", result);
	}

	@Test
	public void resolveResultIsMessageWithProps2() {
		// Arrange
		Message message = Message.asMessage("My Message");
		Map<String, Object> propsMap = Collections.singletonMap("msg1", message);

		// Act
		String result = StringResolver.substVars("blalblalab ${msg1}", properties, propsMap);

		// Assert
		assertEquals("blalblalab My Message", result);

		// Act
		result = StringResolver.substVars("blalblalab ${msg1}", properties, propsMap, true);

		// Assert
		assertEquals("blalblalab ${msg1:-My Message}", result);
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

	@Test
	public void resolveSimpleHideProperty() {
		// Arrange
		List<String> propsToHide = Collections.singletonList("key1");

		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", properties, null, propsToHide );

		// Assert
		assertEquals("blalblalab ******", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", properties, null, propsToHide, true);

		// Assert
		assertEquals("blalblalab ${key1:-******}", result);
	}

	@Test
	public void resolveUsernameAndHide() {
		// N.B. the notation ${credential:alias1/username} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:username:alias1}", properties, null, Collections.emptyList());
		assertEquals("*********", result);

		result = StringResolver.substVars("${credential:username:alias1}", properties, null, Collections.emptyList(), true);
		assertEquals("${credential:username:alias1:-*********}", result);
	}

	@Test
	public void resolvePassword1AndHide() {
		// N.B. the notation ${credential:alias1/password} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:password:alias1}", properties, null, Collections.emptyList());
		assertEquals("*********", result);

		result = StringResolver.substVars("${credential:password:alias1}", properties, null, Collections.emptyList(), true);
		assertEquals("${credential:password:alias1:-*********}", result);
	}

	@Test
	public void resolvePassword2AndHide() {
		String result = StringResolver.substVars("${credential:alias1}", properties, null, Collections.emptyList()); // the 'credential:' prefix defaults to return the password
		assertEquals("*********", result);

		result = StringResolver.substVars("${credential:alias1}", properties, null, Collections.emptyList(), true);
		assertEquals("${credential:alias1:-*********}", result);
	}

	@Test
	public void resolvePasswordOnlyAliasAndHide() {
		String result = StringResolver.substVars("${credential:alias2}", properties, null, Collections.emptyList());
		assertEquals("************", result);

		result = StringResolver.substVars("${credential:alias2}", properties, null, Collections.emptyList(), true);
		assertEquals("${credential:alias2:-************}", result);
	}

	@Test
	public void resolvePasswordOnlyAlias2AndHide() {
		String result = StringResolver.substVars("${credential:password:alias2}", properties, null, Collections.emptyList());
		assertEquals("************", result);

		result = StringResolver.substVars("${credential:password:alias2}", properties, null, Collections.emptyList(), true);
		assertEquals("${credential:password:alias2:-************}", result);
	}
}
