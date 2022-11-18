/*
   Copyright 2019-2020 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;
import nl.nn.adapterframework.testutil.mock.PreparedStatementMock;
import nl.nn.adapterframework.util.AppConstants;

public class ConfigurationUtilsTest extends Mockito {

	private ApplicationContext applicationContext = mock(ApplicationContext.class);
	private PreparedStatementMock stmt;

	@BeforeClass
	public static void setUp() {
		AppConstants.getInstance().setProperty("configurations.database.autoLoad", true);
		AppConstants.getInstance().setProperty("configurations.directory.autoLoad", true);
		AppConstants.getInstance().setProperty("configurations.config2.parentConfig", "config4");
		AppConstants.getInstance().setProperty("configurations.config3.parentConfig", "config4");
	}

	@AfterClass
	public static void tearDown() {
		AppConstants.getInstance().setProperty("configurations.database.autoLoad", false);
		AppConstants.getInstance().setProperty("configurations.directory.autoLoad", false);
		AppConstants.getInstance().remove("configurations.config2.parentConfig");
		AppConstants.getInstance().remove("configurations.config3.parentConfig");
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
		assertNotNull("BuildInfoZip not found", zip);
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull("filename cannot be determined", filename);

		boolean result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, "ConfigurationName", "001_20191002-1300", filename, zip.openStream(), "dummy-user");
		assertTrue("file uploaded to mock database", result);
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("buildInfo name does not match", "ConfigurationName", parameters.get("NAME"));
		assertEquals("buildInfo version does not match", "001_20191002-1300", parameters.get("VERSION"));
		assertEquals("FILENAME does not match", filename, parameters.get("FILENAME"));
	}

	@Test
	public void addConfigToDatabaseNew() throws Exception {
		mockDatabase();

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull("filename cannot be determined", filename);

		String result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
		assertNotNull("file uploaded to mock database", result);
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("buildInfo name does not match", "ConfigurationName", parameters.get("NAME"));
		assertEquals("buildInfo version does not match", "001_20191002-1300", parameters.get("VERSION"));
		assertEquals("FILENAME does not match", filename, parameters.get("FILENAME"));
	}

	@Test
	public void addConfigToDatabaseNewBuildInfoSC() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull("filename cannot be determined", filename);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		String result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
		assertNotNull("file uploaded to mock database", result);
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("buildInfo name does not match", "ConfigurationName", parameters.get("NAME"));
		assertEquals("buildInfo version does not match", "123_20181002-1300", parameters.get("VERSION"));
		assertEquals("FILENAME does not match", filename, parameters.get("FILENAME"));
	}

	@Test
	public void addConfigToDatabaseNewBuildInfoSPECIAL() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull("filename cannot be determined", filename);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		String result = ConfigurationUtils.addConfigToDatabase(applicationContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
		assertNotNull("file uploaded to mock database", result);
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("buildInfo name does not match", "ConfigurationName", parameters.get("NAME"));
		assertEquals("buildInfo version does not match", "789_20171002-1300", parameters.get("VERSION"));
		assertEquals("FILENAME does not match", filename, parameters.get("FILENAME"));
	}

	@Test
	public void processMultiConfigZipFile() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/multiConfig.zip");
		assertNotNull("multiConfig.zip not found", zip);

		BuildInfoValidator.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		Map<String, String> result = ConfigurationUtils.processMultiConfigZipFile(applicationContext, "fakeDataSource", false, false, zip.openStream(), "user");
		assertNotEquals("file uploaded to mock database", 0, result.size());
		assertEquals("{ConfigurationName: 001_20191002-1300=loaded, ConfigurationName: 002_20191002-1400=loaded, noBuildInfoZip.jar=no [BuildInfo.properties] present in configuration}",result.toString());

		Map<String, Object> parameters = stmt.getNamedParameters(); //Test the 2nd file, because the 3rd result fails
		assertEquals("buildInfo name does not match", "ConfigurationName", parameters.get("NAME"));
		assertEquals("buildInfo version does not match", "002_20191002-1400", parameters.get("VERSION"));
		assertEquals("FILENAME does not match", "buildInfoZip2.jar", parameters.get("FILENAME"));

		//Make sure ACTIVECONFIG, AUTORELOAD and RUSER are passed through properly
		assertEquals("ACTIVECONFIG does not match", "FALSE", parameters.get("ACTIVECONFIG"));
		assertEquals("AUTORELOAD does not match", "FALSE", parameters.get("AUTORELOAD"));
		assertEquals("RUSER does not match", "user", parameters.get("RUSER"));

		//This field is pretty obsolete, check if it's been set
		assertNotNull("FILENAME not set", parameters.get("FILENAME"));
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

		MatcherAssert.assertThat(configs, IsIterableContainingInOrder.contains("config1", "config2", "config3", "config4", "config5")); //checks order!
	}

	@Test
	public void retrieveAllConfigNamesTest() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(url);
		File directory = new File(url.toURI());
		AppConstants.getInstance().setProperty("configurations.directory", directory.getCanonicalPath());

		TestConfiguration applicationContext = new TestConfiguration();
		ResultSetBuilder builder = ResultSetBuilder.create()
				.setValue("config1")
				.addRow().setValue("config2")
				.addRow().setValue("config3")
				.addRow().setValue("config4")
				.addRow().setValue("config5");
		applicationContext.mockQuery("SELECT COUNT(*) FROM IBISCONFIG", builder.build());
		Map<String, Class<? extends IConfigurationClassLoader>> configs = ConfigurationUtils.retrieveAllConfigNames(applicationContext);
		MatcherAssert.assertThat(configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "ClassLoader", "Config", "config1", "config4", "config2", "config3", "config5")); //checks order!

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DirectoryClassLoader.class, configs.get("ClassLoader"));
		assertEquals(DirectoryClassLoader.class, configs.get("Config"));
		assertEquals(DatabaseClassLoader.class, configs.get("config1"));
		assertEquals(DatabaseClassLoader.class, configs.get("config2"));
	}
}