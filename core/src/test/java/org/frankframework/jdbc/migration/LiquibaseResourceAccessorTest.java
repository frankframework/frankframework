package org.frankframework.jdbc.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import liquibase.resource.ResourceAccessor;

import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.Resource;
import org.frankframework.util.ClassLoaderUtils;

public class LiquibaseResourceAccessorTest extends ConfiguredTestBase {

	@Test
	public void testSearch() throws Exception {
		Resource frankResource = Resource.getResource("/Migrator/DatabaseChangelog.xml");
		assertNotNull(frankResource);

		ResourceAccessor accessor = new LiquibaseResourceAccessor(frankResource);

		List<liquibase.resource.Resource> resources = accessor.search("/Migrator/DatabaseChangelog_plus_changes.xml", false);
		assertNotNull(resources);

		URI actualUri = resources.get(0).getUri();
		URI expectedURI = ClassLoaderUtils.getResourceURL("/Migrator/DatabaseChangelog_plus_changes.xml").toURI();

		assertEquals(expectedURI, actualUri);
	}
}
