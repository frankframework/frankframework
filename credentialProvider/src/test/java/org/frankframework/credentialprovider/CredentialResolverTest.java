package org.frankframework.credentialprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.util.StringResolver;

class CredentialResolverTest {

	Properties properties;

	@BeforeEach
	void setUp() throws Exception {
		properties = new Properties();
		URL propertiesURL = getClass().getResource("/StringResolver.properties");
		assertNotNull(propertiesURL, "properties file [StringResolver.properties] not found!");

		InputStream propsStream = propertiesURL.openStream();
		properties.load(propsStream);
		assertFalse(properties.isEmpty(), "did not find any properties!");

		System.setProperty("authAliases.expansion.allowed", "${allowedAliases}");
	}

	/**
	 *  Make sure to clean up the system properties after the tests
	 */
	@AfterAll
	public static void tearDown() {
		System.clearProperty("authAliases.expansion.allowed");
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
	public void resolvePassword1AndHide() {
		// N.B. the notation ${credential:alias1/password} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:password:alias1}", properties, null, Collections.emptySet());
		assertEquals("*********", result);

		result = StringResolver.substVars("${credential:password:alias1}", properties, null, Collections.emptySet(), true);
		assertEquals("${credential:password:alias1:-*********}", result);
	}

	@Test
	public void resolvePassword2AndHide() {
		String result = StringResolver.substVars("${credential:alias1}", properties, null, Collections.emptySet()); // the 'credential:' prefix defaults to return the password
		assertEquals("*********", result);

		result = StringResolver.substVars("${credential:alias1}", properties, null, Collections.emptySet(), true);
		assertEquals("${credential:alias1:-*********}", result);
	}

	@Test
	public void resolvePasswordOnlyAliasAndHide() {
		String result = StringResolver.substVars("${credential:alias2}", properties, null, Collections.emptySet());
		assertEquals("************", result);

		result = StringResolver.substVars("${credential:alias2}", properties, null, Collections.emptySet(), true);
		assertEquals("${credential:alias2:-************}", result);
	}

	@Test
	public void resolvePasswordOnlyAlias2AndHide() {
		String result = StringResolver.substVars("${credential:password:alias2}", properties, null, Collections.emptySet());
		assertEquals("************", result);

		result = StringResolver.substVars("${credential:password:alias2}", properties, null, Collections.emptySet(), true);
		assertEquals("${credential:password:alias2:-************}", result);
	}

	@Test
	public void resolveUsernameAndHide() {
		// N.B. the notation ${credential:alias1/username} will work too, for some implementations of CredentialProvider, but not for all!
		String result = StringResolver.substVars("${credential:username:alias1}", properties, null, Collections.emptySet());
		assertEquals("*********", result);

		result = StringResolver.substVars("${credential:username:alias1}", properties, null, Collections.emptySet(), true);
		assertEquals("${credential:username:alias1:-*********}", result);
	}
}
