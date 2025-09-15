package org.frankframework.jdbc.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class ResourceObjectLocatorTest {

	@Test
	public void validFile() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("ResourceLocator/validResources.yml");
		locator.afterPropertiesSet();

		Object obj = locator.lookup("jdbc/H2", null, Object.class);
		assertNotNull(obj);
		JdbcDataSource jdbcDataSource = assertInstanceOf(JdbcDataSource.class, obj);
		assertEquals("fake+user", jdbcDataSource.getUser());
		assertEquals("fake_pwd", jdbcDataSource.getPassword());
	}

	@Test
	public void testInstanceNameLc() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();

		locator.setResourceFile("ResourceLocator/validResources.yml");
		locator.afterPropertiesSet();

		Object obj = locator.lookup("jdbc/testconfiguration", null, Object.class);
		assertNotNull(obj);
	}

	@Test
	public void testInvalidFile() {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("ResourceLocator/invalidResources.yml");

		IllegalStateException e = assertThrows(IllegalStateException.class, locator::afterPropertiesSet);
		assertTrue(e.getMessage().contains("[ResourceLocator/invalidResources.yml]"));
	}

	@Test
	public void testNonExistingPrefix() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("ResourceLocator/validResources.yml");
		locator.afterPropertiesSet();

		Object obj = locator.lookup("idonotexist/qwerty", null, Object.class);
		assertNull(obj);
	}

	@Test
	public void testNonInitializedLocator() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("blaat");
		locator.afterPropertiesSet();

		Object obj = locator.lookup("idonotexist/qwerty", null, Object.class);
		assertNull(obj);
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void validEmptyLookup(String lookupString) throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("ResourceLocator/validResources.yml");
		locator.afterPropertiesSet();

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> locator.lookup(lookupString, null, Object.class));
		assertTrue(e.getMessage().contains("invalid resource"));
	}
}
