package nl.nn.credentialprovider.rolemapping;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.CatalinaBaseConfigurationSource;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.unittest.TesterContext;
import org.apache.tomcat.util.file.ConfigFileLoader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import nl.nn.credentialprovider.RoleToGroupMappingJndiRealm;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = { @CreateTransport(protocol = "LDAP", address = "localhost") })
@CreateDS(name = "myDS", allowAnonAccess = false, partitions = {
		@CreatePartition(name = "test", suffix = "dc=myorg,dc=com") })
@ApplyLdifFiles({ "users.ldif" })
public class RoleGroupMapperTest extends AbstractLdapTestUnit {

	private static final Log log = LogFactory.getLog(RoleGroupMapperTest.class);

	private RoleToGroupMappingJndiRealm setupRoleToGroupMappingJndiRealm(Context context, String pathname) {
		RoleToGroupMappingJndiRealm realm = new RoleToGroupMappingJndiRealm();
		int port = getLdapServer().getPort();

		realm.setConnectionURL("ldap://localhost:" + port);
		realm.setConnectionName("cn=LdapTester1,ou=Users,dc=myorg,dc=com");
		realm.setConnectionPassword("12345");

		realm.setUserBase("ou=Users,dc=myorg,dc=com");
		realm.setUserSearch("(CN={0})");
		realm.setUserRoleName("memberOf");
		realm.setUserSubtree(true);

		realm.setRoleName("memberOf");

		if (context != null) {
			realm.setContainer(context);
		} else {
			realm.setContainer(new TesterContext());
		}

		if (pathname != null) {
			realm.setPathname(pathname);
		}

		return realm;
	}

	@BeforeClass
	public static void setup() {
		String loggings = ClassLoader.getSystemResource("logging.properties").getPath();

		System.setProperty("java.util.logging.config.file", loggings);

		TomcatURLStreamHandlerFactory.getInstance();
		System.setProperty("catalina.base", "");
		ConfigFileLoader
				.setSource(new CatalinaBaseConfigurationSource(new File(System.getProperty("catalina.base")), null));
	}

	@Test
	public void testEmptyRoleToGroupMappingJndiRealm() throws LifecycleException {

		RoleToGroupMappingJndiRealm realm = setupRoleToGroupMappingJndiRealm(null, null);
		realm.start();
	}

	@Test(expected = LifecycleException.class)
	public void testNoExistingResource() throws LifecycleException {

		RoleToGroupMappingJndiRealm realm = setupRoleToGroupMappingJndiRealm(null, "classpath:conf/tomcat-role-group-mapping1.xml");

		realm.start();
	}

	@Test
	public void testGetNestedGroups() throws LifecycleException {

		TesterContext context = new TesterContext();
		RoleToGroupMappingJndiRealm realm = setupRoleToGroupMappingJndiRealm(context, "classpath:conf/tomcat-role-group-mapping.xml");
		realm.start();

		List<String> roles = realm.getRoles("LdapTester1");

		assertThat(roles, contains(
				"cn=UserGroup1,ou=Groups,dc=myorg,dc=com",
				"cn=ApplGroup1,ou=Groups,dc=myorg,dc=com",
				"cn=ApplSubGroup1,ou=Groups,dc=myorg,dc=com"
			));

	}

	@Test
	public void testGetRoleMapping() throws LifecycleException {

		TesterContext context = new TesterContext();
		RoleToGroupMappingJndiRealm realm = setupRoleToGroupMappingJndiRealm(context, "classpath:conf/tomcat-role-group-mapping.xml");
		realm.start();

		// this mapping is used to find the required group for a role, apparently
		Map<String,String> roleToGroupMapping = context.getRoleMapping();

		log.info("Configured roleMappings: " + roleToGroupMapping);

		assertEquals("director", roleToGroupMapping.get("Admin"));
		assertEquals("director2", roleToGroupMapping.get("Admin2"));
		assertEquals("cn=UserGroup1,ou=Groups,dc=myorg,dc=com", roleToGroupMapping.get("PowerUser"));

	}

}
