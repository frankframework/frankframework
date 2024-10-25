package org.frankframework.credentialprovider.rolemapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.CatalinaBaseConfigurationSource;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.unittest.TesterContext;
import org.apache.tomcat.util.file.ConfigFileLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;

import org.frankframework.credentialprovider.RoleToGroupMappingJndiRealm;
import org.frankframework.util.ClassUtils;

public class RoleGroupMapperTest {

	private static InMemoryDirectoryServer inMemoryDirectoryServer = null;
	private static final String baseDNs = "dc=myorg,dc=com";
	private static final Log log = LogFactory.getLog(RoleGroupMapperTest.class);

	private RoleToGroupMappingJndiRealm setupRoleToGroupMappingJndiRealm(Context context, String pathname) {
		RoleToGroupMappingJndiRealm realm = new RoleToGroupMappingJndiRealm();
		int port = inMemoryDirectoryServer.getListenPort();

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

	@BeforeAll
	public static void setup() throws Exception {
		String loggings = ClassLoader.getSystemResource("logging.properties").getPath();

		System.setProperty("java.util.logging.config.file", loggings);

		TomcatURLStreamHandlerFactory.getInstance();
		System.setProperty("catalina.base", "");
		ConfigFileLoader.setSource(new CatalinaBaseConfigurationSource(new File(System.getProperty("catalina.base")), null));


		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDNs);
		config.setSchema(null);
		inMemoryDirectoryServer = new InMemoryDirectoryServer(config);

		String ldifDataFile = "users.ldif";
		URL ldifDataUrl = ClassUtils.getResourceURL(ldifDataFile);
		if (ldifDataUrl == null) {
			fail("cannot find resource [" + ldifDataFile + "]");
		}
		inMemoryDirectoryServer.importFromLDIF(true, ldifDataUrl.getPath());
		inMemoryDirectoryServer.startListening();
	}

	@AfterAll
	public static void tearDown() {
		if(inMemoryDirectoryServer != null) {
			inMemoryDirectoryServer.shutDown(true);
		}
	}

	@Test
	public void testEmptyRoleToGroupMappingJndiRealm() throws LifecycleException {

		RoleToGroupMappingJndiRealm realm = setupRoleToGroupMappingJndiRealm(null, null);
		realm.start();
	}

	@Test
	public void testNoExistingResource() {

		RoleToGroupMappingJndiRealm realm = setupRoleToGroupMappingJndiRealm(null, "classpath:conf/tomcat-role-group-mapping1.xml");

		assertThrows(LifecycleException.class, realm::start);
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
				"cn=ApplSubGroup1,ou=Groups,dc=myorg,dc=com",
				"cn=ApplSubGroup2,ou=Groups,dc=myorg,dc=com",
				"cn=ApplSubSubGroup1,ou=SubGroups,ou=Groups,dc=myorg,dc=com",
				"AllAuthenticated"
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
