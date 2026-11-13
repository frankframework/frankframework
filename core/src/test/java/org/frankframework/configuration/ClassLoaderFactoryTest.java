package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;

import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;

public class ClassLoaderFactoryTest extends Mockito {

	private static final IbisContext ibisContext = spy(new IbisContext());
	private ClassLoaderFactory factory;

	private static final String BASE_DIR = "/ClassLoader";
	private static final String JAR_FILE = BASE_DIR+ "/zip/classLoader-test.zip";

	private static final String CONFIG_0_NAME = "config0";
	private static final String CONFIG_1_NAME = "config1";
	private static final String CONFIG_2_NAME = "config2";
	private static final String CONFIG_3_NAME = "config3";
	private static final String CONFIG_5_NAME = "config5";
	private static final String CONFIG_6_NAME = "config6";
	private static final String CONFIG_7_NAME = "config7";

	// declarations for parameterized tests
	private String type = null;
	private boolean skip = false;
	private String configurationName;
	private AppConstants appConstants;

	public static List<Arguments> data() {
		return Arrays.asList(
				arguments("", CONFIG_0_NAME),
				arguments("WebAppClassLoader", CONFIG_1_NAME),
				arguments("DirectoryClassLoader", CONFIG_2_NAME),
				arguments("JarFileClassLoader", CONFIG_3_NAME),
				arguments("DatabaseClassLoader", CONFIG_5_NAME),
				arguments("DummyClassLoader", CONFIG_6_NAME),
				arguments("ScanningDirectoryClassLoader", CONFIG_7_NAME),
				arguments("org.frankframework.configuration.classloaders.WebAppClassLoader", CONFIG_1_NAME),
				arguments("org.frankframework.configuration.classloaders.DirectoryClassLoader", CONFIG_2_NAME),
				arguments("org.frankframework.configuration.classloaders.JarFileClassLoader", CONFIG_3_NAME),
				arguments("org.frankframework.configuration.classloaders.DatabaseClassLoader", CONFIG_5_NAME),
				arguments("org.frankframework.configuration.classloaders.DummyClassLoader", CONFIG_6_NAME),
				arguments("org.frankframework.configuration.classloaders.DefaultClassLoader", "tralla")
		);
	}

	@BeforeAll
	public static void before() throws Exception {
		mockApplicationContext();
	}

	@AfterAll
	public static void teardown() {
		AppConstants.removeInstance();
	}

	public void configureClassloader(String type, String configurationName) {
		if (type == null || "DummyClassLoader".equals(type))
			skip = true;
		else if(type.isEmpty()) // If empty string, it's a WebAppClassLoader
			type = "WebAppClassLoader";

		this.type = type;
		this.configurationName = configurationName;

		if (type.endsWith("DirectoryClassLoader")) {
			String directory = JunitTestClassLoaderWrapper.getTestClassesLocation()+"ClassLoader/DirectoryClassLoaderRoot/";
			setLocalProperty("configurations."+configurationName+".directory", directory);
			setLocalProperty("configurations."+configurationName+".basePath", ".");
		}

		if (type.endsWith("JarFileClassLoader")) {
			URL file = this.getClass().getResource(JAR_FILE);
			setLocalProperty("configurations."+configurationName+".jar", file.getFile());
		}
	}

	@BeforeEach
	public void setUp() {
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
		StringBuilder configurationsNames = new StringBuilder();
		for(Arguments a: data()) {
			Object[] o = a.get();
			configurationsNames.append(o[1]).append(",");
			if(o[0] != null) {
				String value = "" + o[0];
				setLocalProperty("configurations."+o[1]+".classLoaderType", value);
			}
		}
		setLocalProperty("configurations.names", configurationsNames.toString());

		factory = new ClassLoaderFactory();
		factory.setIbisContext(ibisContext);
		factory.setApplicationContext(ibisContext.getApplicationContext());
	}

	private static void mockApplicationContext() throws Exception {
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
		URL file = ClassLoaderFactory.class.getResource(JAR_FILE);
		doReturn(StreamUtil.streamToBytes(file.openStream())).when(rs).getBytes(anyInt());
		doReturn(rs).when(stmt).executeQuery();

		// Mock applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		IbisManager ibisManager = mock(IbisManager.class);
		doReturn(ibisManager).when(ibisContext).getIbisManager();
		AbstractApplicationContext applicationContext = mock();
		doReturn(applicationContext).when(ibisManager).getApplicationContext();
		doReturn(applicationContext).when(ibisContext).getApplicationContext();
		doReturn(beanFactory).when(applicationContext).getAutowireCapableBeanFactory();
		doReturn(fq).when(beanFactory).createBean(FixedQuerySender.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		doAnswer(invocation -> {
			IConfigurationClassLoader classLoader = invocation.getArgument(0);
			classLoader.setConfigurationName("");
			if (classLoader instanceof ApplicationContextAware applicationContextAware) {
				applicationContextAware.setApplicationContext(applicationContext);
			}
			if (classLoader instanceof ScanningDirectoryClassLoader sdcl) {
				sdcl.setIbisManager(ibisManager);
			}
			classLoader.afterPropertiesSet();
			return classLoader;
		}).when(beanFactory).initializeBean(isA(IConfigurationClassLoader.class), anyString());
	}

	private ClassLoader getClassLoader() throws Exception {
		return getClassLoader(configurationName);
	}

	private ClassLoader getClassLoader(String testConfiguration) throws Exception {
		return getClassLoader(testConfiguration, null);
	}

	private ClassLoader getClassLoader(String testConfiguration, ClassLoader parent) throws Exception {
		ClassLoader config = factory.createClassLoader(testConfiguration, null, parent);
		if (config instanceof AbstractClassLoader base) {
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

		ClassLoader config = getClassLoader(configurationName, parentClassloader);
		URL resource = config.getResource(testFile);

		assertNotNull(resource);
		appConstants.remove("configurations."+configurationName+".parentConfig");
	}

	/**This method makes sure the property is only set in the local AppConstants instance.
	 * When you use the default put/setProperty it stores the property in the additionalProperties and
	 * upon creation of a new AppConstants instance it will set all the previously set properties
	 */
	@SuppressWarnings("deprecation")
	private void setLocalProperty(String key, String value) {
		appConstants.put(key, value);
	}
}
