package org.frankframework.jdbc.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

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
	public void propertySubstitutionDataSource() throws Exception {
		System.setProperty("substitution.dummyURL", "jdbc:mock:mem:test");
		System.setProperty("substitution.type", "org.frankframework.jdbc.datasource.MockDataSource");
		System.setProperty("substitution.myDsAuthAlias", "alias1");
		System.setProperty("substitution.defaultUsername", "ignoreMe1");
		System.setProperty("substitution.defaultPassword", "ignoreMe2");

		try {
			ResourceObjectLocator locator = new ResourceObjectLocator();
			locator.setResourceFile("ResourceLocator/validResources.yml");
			locator.afterPropertiesSet();

			Object obj = locator.lookup("jdbc/substitution", null, Object.class);
			assertNotNull(obj);
			MockDataSource jdbcDataSource = assertInstanceOf(MockDataSource.class, obj);
			assertEquals("username1", jdbcDataSource.getUser());
			assertEquals("password1", jdbcDataSource.getPassword());
		} finally {
			System.clearProperty("substitution.dummyURL");
			System.clearProperty("substitution.type");
			System.clearProperty("substitution.myDsAuthAlias");
			System.clearProperty("substitution.defaultUsername");
			System.clearProperty("substitution.defaultPassword");
		}
	}

	@Test
	public void propertySubstitutionDriver() throws Exception {
		System.setProperty("substitution.dummyURL", "jdbc:postgresql:mock:mem:test");
		System.setProperty("substitution.type", "org.postgresql.Driver");
		System.setProperty("substitution.myDsAuthAlias", "alias1");
		System.setProperty("substitution.defaultUsername", "ignoreMe1");
		System.setProperty("substitution.defaultPassword", "ignoreMe2");

		try {
			ResourceObjectLocator locator = new ResourceObjectLocator();
			locator.setResourceFile("ResourceLocator/validResources.yml");
			locator.afterPropertiesSet();

			Object obj = locator.lookup("jdbc/substitution", null, DataSource.class);
			assertNotNull(obj);
			DriverManagerDataSource jdbcDataSource = assertInstanceOf(DriverManagerDataSource.class, obj);
			assertEquals("username1", jdbcDataSource.getUsername());
			assertEquals("password1", jdbcDataSource.getPassword());

		} finally {
			System.clearProperty("substitution.dummyURL");
			System.clearProperty("substitution.type");
			System.clearProperty("substitution.myDsAuthAlias");
			System.clearProperty("substitution.defaultUsername");
			System.clearProperty("substitution.defaultPassword");
		}
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
