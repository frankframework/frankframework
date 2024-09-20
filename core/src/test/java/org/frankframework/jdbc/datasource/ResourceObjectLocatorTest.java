package org.frankframework.jdbc.datasource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ResourceObjectLocatorTest {

	@Test
	public void validFile() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("ResourceLocator/validResources.yml");
		locator.afterPropertiesSet();

		Object obj = locator.lookup("jdbc/H2", null, Object.class);
		assertNotNull(obj);
	}

	@Test
	public void testInvalidFile() throws Exception {
		ResourceObjectLocator locator = new ResourceObjectLocator();
		locator.setResourceFile("ResourceLocator/invalidResources.yml");

		assertThrows(IllegalStateException.class, locator::afterPropertiesSet);
	}
}
