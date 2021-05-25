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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.configuration.ConfigurationUtils.ConfigurationValidator;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;

public class ConfigurationUtilsTest extends Mockito {

	private IbisContext ibisContext = spy(new IbisContext());
	private PreparedStatementMock stmt;

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
				stmt = new PreparedStatementMock(query);
				return stmt;
			}
		}).when(conn).prepareStatement(anyString());

		doReturn(fq).when(ibisContext).createBeanAutowireByName(FixedQuerySender.class);

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
		doReturn(ptm).when(ibisContext).getBean("TransactionManager", PlatformTransactionManager.class);
	}

	@Test
	public void retrieveBuildInfo() throws IOException {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(zip.openStream());

		String buildInfoName = buildInfo[0];
		assertEquals("buildInfo name does not match", "ConfigurationName", buildInfoName);

		String buildInfoVersion = buildInfo[1];
		assertEquals("buildInfo version does not match", "001_20191002-1300", buildInfoVersion);
	}

	@Test
	public void retrieveBuildInfoSC() throws IOException {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(zip.openStream());

		String buildInfoName = buildInfo[0];
		assertEquals("buildInfo name does not match", "ConfigurationName", buildInfoName);

		String buildInfoVersion = buildInfo[1];
		assertEquals("buildInfo version does not match", "123_20181002-1300", buildInfoVersion);
	}

	@Test
	public void retrieveBuildInfoCUSTOM() throws IOException {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(zip.openStream());

		String buildInfoName = buildInfo[0];
		assertEquals("buildInfo name does not match", "ConfigurationName", buildInfoName);

		String buildInfoVersion = buildInfo[1];
		assertEquals("buildInfo version does not match", "789_20171002-1300", buildInfoVersion);
	}

	@Test
	public void configurationValidator() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		ConfigurationValidator details = new ConfigurationValidator(zip.openStream());

		assertEquals("buildInfo name does not match", "ConfigurationName", details.getName());
		assertEquals("buildInfo version does not match", "001_20191002-1300", details.getVersion());
	}

	@Test(expected=ConfigurationException.class)
	public void configurationValidatorNoBuildInfoZip() throws Exception {
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/noBuildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		new ConfigurationValidator(zip.openStream());
	}

	@Test
	public void addConfigToDatabaseOld() throws Exception {
		mockDatabase();

		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull("filename cannot be determined", filename);

		boolean result = ConfigurationUtils.addConfigToDatabase(ibisContext, "fakeDataSource", false, false, "ConfigurationName", "001_20191002-1300", filename, zip.openStream(), "dummy-user");
		assertTrue("file uploaded to mock database", result);
		Map<String, Object> parameters = stmt.getNamedParameters();

		assertEquals("buildInfo name does not match", "ConfigurationName", parameters.get("NAME"));
		assertEquals("buildInfo version does not match", "001_20191002-1300", parameters.get("VERSION"));
		assertEquals("FILENAME does not match", filename, parameters.get("FILENAME"));
	}

	@Test
	public void addConfigToDatabaseNew() throws Exception {
		mockDatabase();

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		URL zip = ConfigurationUtilsTest.class.getResource("/ConfigurationUtils/buildInfoZip.jar");
		assertNotNull("BuildInfoZip not found", zip);
		String filename = FilenameUtils.getName(zip.getFile());
		assertNotNull("filename cannot be determined", filename);

		String result = ConfigurationUtils.addConfigToDatabase(ibisContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
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

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SC";
		String result = ConfigurationUtils.addConfigToDatabase(ibisContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
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

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "_SPECIAL";
		String result = ConfigurationUtils.addConfigToDatabase(ibisContext, "fakeDataSource", false, false, filename, zip.openStream(), "dummy-user");
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

		ConfigurationUtils.ADDITIONAL_PROPERTIES_FILE_SUFFIX = "";
		Map<String, String> result = ConfigurationUtils.processMultiConfigZipFile(ibisContext, "fakeDataSource", false, false, zip.openStream(), "user");
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
}
