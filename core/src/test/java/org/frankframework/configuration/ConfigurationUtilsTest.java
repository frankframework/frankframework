package org.frankframework.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import org.frankframework.configuration.classloaders.DatabaseClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;
import org.frankframework.testutil.mock.PreparedStatementMock;
import org.frankframework.util.AppConstants;

public class ConfigurationUtilsTest extends Mockito {

	private final ApplicationContext applicationContext = mock(ApplicationContext.class);
	private PreparedStatementMock stmt;

	@BeforeAll
	public static void setUp() throws Exception {
		AppConstants.removeInstance();
		AppConstants.getInstance().setProperty("configurations.configuration2.parentConfig", "configuration4");
		AppConstants.getInstance().setProperty("configurations.configuration3.parentConfig", "configuration4");
		AppConstants.getInstance().setProperty("configurations.Config.parentConfig", "ClassLoader");

		URL url = TestFileUtils.getTestFileURL("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(url);
		File directory = new File(url.toURI());
		AppConstants.getInstance().setProperty("configurations.directory", directory.getCanonicalPath());
	}

	@AfterAll
	public static void tearDown() {
		AppConstants.removeInstance();
	}

	private void mockDatabase() throws Exception {
		// Mock a FixedQuerySender
		FixedQuerySender fq = mock(FixedQuerySender.class);
		doReturn(new GenericDbmsSupport()).when(fq).getDbmsSupport();

		Connection conn = mock(Connection.class);
		doReturn(conn).when(fq).getConnection();

		//Override prepareStatement(String query) and return a mock to validate the parameters
		doAnswer(new Answer<PreparedStatementMock>() {
			@Override
			public PreparedStatementMock answer(InvocationOnMock invocation) throws Throwable {
				String query = (String) invocation.getArguments()[0];
				stmt = PreparedStatementMock.newInstance(query);
				return stmt;
			}
		}).when(conn).prepareStatement(anyString());

		//Mock applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		doReturn(beanFactory).when(applicationContext).getAutowireCapableBeanFactory();
		doReturn(fq).when(beanFactory).createBean(FixedQuerySender.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

		// STUB a TransactionManager
		PlatformTransactionManager ptm = new PlatformTransactionManager() {

			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				return mock(TransactionStatus.class);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				// STUB
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				// STUB
			}
		};
		doReturn(ptm).when(applicationContext).getBean("txManager", PlatformTransactionManager.class);
	}

	@Test
	public void addConfigToDatabaseOld() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull(filename, "filename cannot be determined");

		boolean result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, "ConfigurationName", "001_20191002-1300", filename, zip.openStream(), "dummy-user");
		assertTrue(result, "file uploaded to mock database");
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("ConfigurationName", parameters.get("NAME"), "buildInfo name does not match");
		assertEquals("001_20191002-1300", parameters.get("VERSION"), "buildInfo version does not match");
		assertEquals(filename, parameters.get("FILENAME"), "FILENAME does not match");
	}

	@Test
	public void addConfigToDatabaseNew() throws Exception {
		mockDatabase();

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull(filename, "filename cannot be determined");

		String result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
		assertNotNull(result, "file uploaded to mock database");
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("ConfigurationName", parameters.get("NAME"), "buildInfo name does not match");
		assertEquals("001_20191002-1300", parameters.get("VERSION"), "buildInfo version does not match");
		assertEquals(filename, parameters.get("FILENAME"), "FILENAME does not match");
	}

	@Test
	public void addConfigToDatabaseNewBuildInfoSC() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull(filename, "filename cannot be determined");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		String result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
		assertNotNull(result, "file uploaded to mock database");
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("ConfigurationName", parameters.get("NAME"), "buildInfo name does not match");
		assertEquals("123_20181002-1300", parameters.get("VERSION"), "buildInfo version does not match");
		assertEquals(filename, parameters.get("FILENAME"), "FILENAME does not match");
	}

	@Test
	public void addConfigToDatabaseNewBuildInfoSPECIAL() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull(zip, "BuildInfoZip not found");
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull(filename, "filename cannot be determined");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		String result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
		assertNotNull(result, "file uploaded to mock database");
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("ConfigurationName", parameters.get("NAME"), "buildInfo name does not match");
		assertEquals("789_20171002-1300", parameters.get("VERSION"), "buildInfo version does not match");
		assertEquals(filename, parameters.get("FILENAME"), "FILENAME does not match");
	}

	@Test
	public void processMultiConfigZipFile() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/multiConfig.zip");
		assertNotNull(zip, "multiConfig.zip not found");

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		Map<String, String> result = ConfigurationUtils.processMultiConfigZipFile(applicationContext, "fakeDataSource", false, false, zip.openStream(), "user");
		assertNotEquals(0, result.size(), "file uploaded to mock database");
		assertEquals("{ConfigurationName: 001_20191002-1300=loaded, ConfigurationName: 002_20191002-1400=loaded, noBuildInfoZip.jar=no [BuildInfo.properties] present in configuration}",result.toString());

		Map<String, Object> parameters = stmt.getNamedParameters(); //Test the 2nd file, because the 3rd result fails
		assertEquals("ConfigurationName", parameters.get("NAME"), "buildInfo name does not match");
		assertEquals("002_20191002-1400", parameters.get("VERSION"), "buildInfo version does not match");
		assertEquals("buildInfoZip2.jar", parameters.get("FILENAME"), "FILENAME does not match");

		//Make sure ACTIVECONFIG, AUTORELOAD and RUSER are passed through properly
		assertEquals("FALSE", parameters.get("ACTIVECONFIG"), "ACTIVECONFIG does not match");
		assertEquals("FALSE", parameters.get("AUTORELOAD"), "AUTORELOAD does not match");
		assertEquals("user", parameters.get("RUSER"), "RUSER does not match");

		//This field is pretty obsolete, check if it's been set
		assertNotNull(parameters.get("FILENAME"), "FILENAME not set");
	}

	@Test
	public void retrieveConfigNamesFromDatabaseTest() throws Exception {
		TestConfiguration applicationContext = new TestConfiguration();
		ResultSetBuilder builder = ResultSetBuilder.create()
				.setValue("config1")
				.addRow().setValue("config2")
				.addRow().setValue("config3")
				.addRow().setValue("config4")
				.addRow().setValue("config5");
		applicationContext.mockQuery("SELECT COUNT(*) FROM IBISCONFIG", builder.build());
		List<String> configs = ConfigurationUtils.retrieveConfigNamesFromDatabase(applicationContext);

		assertThat(configs, IsIterableContainingInOrder.contains("config1", "config2", "config3", "config4", "config5")); //checks order!
	}

	@Test
	public void retrieveAllConfigNamesTestWithDB() throws Exception {
		TestConfiguration applicationContext = new TestConfiguration();
		ResultSetBuilder builder = ResultSetBuilder.create()
				.setValue("configuration1")
				.addRow().setValue("configuration2")
				.addRow().setValue("configuration3")
				.addRow().setValue("configuration4")
				.addRow().setValue("configuration5");
		applicationContext.mockQuery("SELECT COUNT(*) FROM IBISCONFIG", builder.build());
		Map<String, Class<? extends IConfigurationClassLoader>> configs = ConfigurationUtils.retrieveAllConfigNames(applicationContext, false, true);

		assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "configuration1", "configuration4", "configuration2", "configuration3", "configuration5")); //checks order!

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DatabaseClassLoader.class, configs.get("configuration1"));
		assertEquals(DatabaseClassLoader.class, configs.get("configuration2"));
	}

	@Test
	public void retrieveAllConfigNamesTestWithFS() throws Exception {
		TestConfiguration applicationContext = new TestConfiguration();
		ResultSetBuilder builder = ResultSetBuilder.create()
				.setValue("configuration1")
				.addRow().setValue("configuration2")
				.addRow().setValue("configuration3")
				.addRow().setValue("configuration4")
				.addRow().setValue("configuration5");
		applicationContext.mockQuery("SELECT COUNT(*) FROM IBISCONFIG", builder.build());
		Map<String, Class<? extends IConfigurationClassLoader>> configs = ConfigurationUtils.retrieveAllConfigNames(applicationContext, true, true);

		assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "ClassLoader", "Config", "configuration1", "configuration4", "configuration2", "configuration3", "configuration5")); //checks order!

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DirectoryClassLoader.class, configs.get("ClassLoader"));
		assertEquals(DirectoryClassLoader.class, configs.get("Config"));
		assertEquals(DatabaseClassLoader.class, configs.get("configuration1"));
		assertEquals(DatabaseClassLoader.class, configs.get("configuration2"));
	}

	@Test
	public void testConfigurationDirectoryAutoLoadInvalidClassName() throws Exception {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType("not-a-ClassLoader");
			assertEquals(DirectoryClassLoader.class, clazz);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("FATAL - invalid classloader type provided for [configurations.directory.classLoaderType] value [not-a-ClassLoader]"));
		}
	}

	@Test
	public void testConfigurationDirectoryAutoLoadIncompatibleClassType() throws Exception {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType(WebAppClassLoader.class.getSimpleName());
			assertEquals(DirectoryClassLoader.class, clazz);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("FATAL - incompatible classloader type provided for [configurations.directory.classLoaderType] value [WebAppClassLoader]"));
		}
	}

	@Test
	public void testConfigurationDirectoryAutoLoadDefaultClassName() throws Exception {
		Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType("DirectoryClassLoader");
		assertEquals(DirectoryClassLoader.class, clazz);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ScanningDirectoryClassLoader", "org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader"}) // Tests both simple and canonical names
	public void testConfigurationDirectoryAutoLoadScanningDirectoryClassName(String name) throws Exception {
		Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType(name);
		assertEquals(ScanningDirectoryClassLoader.class, clazz);
	}
}
