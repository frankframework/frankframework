package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;

public class ClassLoaderManagerTest extends Mockito {

	private static final IbisContext ibisContext = spy(new IbisContext());
	private ClassLoaderManager manager;

	private static final String BASE_DIR = "/ClassLoader";
	private static final String JAR_FILE = BASE_DIR+ "/zip/classLoader-test.zip";

	private static final String CONFIG_0_NAME = "config0";
	private static final String CONFIG_1_NAME = "config1";
	private static final String CONFIG_2_NAME = "config2";
	private static final String CONFIG_3_NAME = "config3";
	private static final String CONFIG_5_NAME = "config5";
	private static final String CONFIG_6_NAME = "config6";

	// declarations for parameterized tests
	private String type = null;
	private boolean skip = false;
	private String configurationName;
	private AppConstants appConstants;

	public static List<Arguments> data() {
		return Arrays.asList(new Arguments[] {
				Arguments.of("", CONFIG_0_NAME),
				Arguments.of("WebAppClassLoader", CONFIG_1_NAME),
				Arguments.of("DirectoryClassLoader", CONFIG_2_NAME),
				Arguments.of("JarFileClassLoader", CONFIG_3_NAME),
				Arguments.of("DatabaseClassLoader", CONFIG_5_NAME),
				Arguments.of("DummyClassLoader", CONFIG_6_NAME),
				Arguments.of("org.frankframework.configuration.classloaders.WebAppClassLoader", CONFIG_1_NAME),
				Arguments.of("org.frankframework.configuration.classloaders.DirectoryClassLoader", CONFIG_2_NAME),
				Arguments.of("org.frankframework.configuration.classloaders.JarFileClassLoader", CONFIG_3_NAME),
				Arguments.of("org.frankframework.configuration.classloaders.DatabaseClassLoader", CONFIG_5_NAME),
				Arguments.of("org.frankframework.configuration.classloaders.DummyClassLoader", CONFIG_6_NAME),
				Arguments.of("org.frankframework.configuration.classloaders.DefaultClassLoader", "tralla")
		});
	}

	@BeforeAll
	public static void before() throws Exception {
		mockDatabase();
	}

	public void configureClassloader(String type, String configurationName) {
		if(type == null || "DummyClassLoader".equals(type))
			skip = true;
		else if(type.isEmpty()) // If empty string, it's a WebAppClassLoader
			type = "WebAppClassLoader";

		this.type = type;
		this.configurationName = configurationName;

		if(type.endsWith("DirectoryClassLoader")) {
			String directory = JunitTestClassLoaderWrapper.getTestClassesLocation()+"ClassLoader/DirectoryClassLoaderRoot/";
			setLocalProperty("configurations."+configurationName+".directory", directory);
			setLocalProperty("configurations."+configurationName+".basePath", ".");
		}

		if(type.endsWith("JarFileClassLoader")) {
			URL file = this.getClass().getResource(JAR_FILE);
			setLocalProperty("configurations."+configurationName+".jar", file.getFile());
		}
	}

	@BeforeEach
	public void setUp() {
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
		String configurationsNames = "";
		for(Arguments a: data()) {
			Object[] o = a.get();
			configurationsNames += o[1]+",";
			if(o[0] != null) {
				String value = "" + (String) o[0];
				setLocalProperty("configurations."+o[1]+".classLoaderType", value);
			}
		}
		setLocalProperty("configurations.names", configurationsNames);

		manager = new ClassLoaderManager(ibisContext);
	}

	private static void mockDatabase() throws Exception {
		// Mock a FixedQuerySender
		JmsRealm jmsRealm = spy(new JmsRealm());
		jmsRealm.setDatasourceName("fake");
		jmsRealm.setRealmName("myRealm");
		JmsRealmFactory.getInstance().addJmsRealm(jmsRealm);
		FixedQuerySender fq = mock(FixedQuerySender.class);
		doReturn(new GenericDbmsSupport()).when(fq).getDbmsSupport();

		Connection conn = mock(Connection.class);
		doReturn(conn).when(fq).getConnection();
		PreparedStatement stmt = mock(PreparedStatement.class);
		doReturn(stmt).when(conn).prepareStatement(anyString());
		ResultSet rs = mock(ResultSet.class);
		doReturn(true).when(rs).next();
		doReturn("dummy").when(rs).getString(anyInt());
		URL file = ClassLoaderManager.class.getResource(JAR_FILE);
		doReturn(StreamUtil.streamToBytes(file.openStream())).when(rs).getBytes(anyInt());
		doReturn(rs).when(stmt).executeQuery();

		// Mock applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		IbisManager ibisManager = mock(IbisManager.class);
		doReturn(ibisManager).when(ibisContext).getIbisManager();
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(applicationContext).when(ibisManager).getApplicationContext();
		doReturn(beanFactory).when(applicationContext).getAutowireCapableBeanFactory();
		doReturn(fq).when(beanFactory).createBean(FixedQuerySender.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	private ClassLoader getClassLoader() throws Exception {
		return getClassLoader(configurationName);
	}

	private ClassLoader getClassLoader(String testConfiguration) throws Exception {
		ClassLoader config = manager.get(testConfiguration);
		if(config instanceof AbstractClassLoader base) {
			base.setBasePath(".");
		}
		return config;
	}

	@ParameterizedTest
	@MethodSource("data")
	public void properClassLoaderType(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();

		// In case the FQDN has been used, strip it
		String name = type;
		if(type.indexOf(".") > 0)
			name = type.substring(type.lastIndexOf(".")+1);
		assertEquals(name, config.getClass().getSimpleName());
	}

	@ParameterizedTest
	@MethodSource("data")
	public void retrieveTestFileNotInClassLoader(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource("test1.xml");

		MatchUtils.assertTestFileEquals("/test1.xml", resource);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void retrieveTestFileInClassLoaderRoot(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		if(skip) return; // This ClassLoader can't actually retrieve files...

		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource("ClassLoaderTestFile.xml");

		MatchUtils.assertTestFileEquals("/ClassLoaderTestFile.xml", resource);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void retrieveTestFileInSubFolder(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		if(skip) return; // This ClassLoader can't actually retrieve files...

		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource(BASE_DIR.substring(1)+"/ClassLoaderTestFile.xml");

		MatchUtils.assertTestFileEquals(BASE_DIR+"/ClassLoaderTestFile.xml", resource);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void retrieveNonExistingTestFile(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource("dummy-test-file.xml");

		assertNull(resource);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testInheritanceMakeSureFileIsFoundInBothParentAndChild(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		if(skip) return; // This ClassLoader can't actually retrieve files...

		String testConfiguration = "myNewClassLoader";
		setLocalProperty("configurations."+testConfiguration+".classLoaderType", "DirectoryClassLoader");
		setLocalProperty("configurations."+testConfiguration+".basePath", ".");
		setLocalProperty("configurations."+configurationName+".parentConfig", testConfiguration);
		String directory = JunitTestClassLoaderWrapper.getTestClassesLocation()+"ClassLoader/";
		setLocalProperty("configurations."+testConfiguration+".directory", directory);
		setLocalProperty("configurations.names", appConstants.get("configurations.names") + ","+testConfiguration);

		String testFile = "fileOnlyOnLocalClassPath.txt";
		ClassLoader parentClassloader = getClassLoader(testConfiguration);

		assertEquals("DirectoryClassLoader", parentClassloader.getClass().getSimpleName());
		URL parentResource = parentClassloader.getResource(testFile);
		assertNotNull(parentResource);

		ClassLoader config = getClassLoader();
		URL resource = config.getResource(testFile);

		assertNotNull(resource);
		appConstants.remove("configurations."+configurationName+".parentConfig");
	}

	@ParameterizedTest
	@MethodSource("data")
	public void reloadString(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config1 = manager.get(configurationName);

		manager.reload(configurationName);

		// Make sure the manager returns the same classloader after reloading it
		ClassLoader config2 = manager.get(configurationName);
		assertEquals(config1.toString(), config2.toString());
	}

	@ParameterizedTest
	@MethodSource("data")
	public void reloadClassLoader(String classLoader, String configurationName) throws Exception {
		configureClassloader(classLoader, configurationName);
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config1 = manager.get(configurationName);

		manager.reload(config1);

		// Make sure the manager returns the same classloader after reloading it
		ClassLoader config2 = manager.get(configurationName);
		assertEquals(config1.toString(), config2.toString());
	}

	/**This method makes sure the property is only set in the local AppConstants instance.
	 * When you use the default put/setProperty it stores the property in the additionalProperties and
	 * upon creation of a new AppConstants instance it will set all the previously set properties
	 */
	@SuppressWarnings("deprecation")
	private void setLocalProperty(String key, String value) {
		appConstants.put(key, (Object) value);
	}
}
