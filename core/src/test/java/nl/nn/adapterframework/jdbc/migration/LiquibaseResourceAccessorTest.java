package nl.nn.adapterframework.jdbc.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.List;

import org.junit.Test;

import liquibase.resource.ResourceAccessor;
import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.ClassLoaderUtils;

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
