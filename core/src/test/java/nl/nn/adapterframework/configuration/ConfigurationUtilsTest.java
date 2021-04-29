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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.configuration.ConfigurationUtils.ConfigurationValidator;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

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

	public void testCanonicalize1(String configFile) throws Exception {
		String configurationSource = TestFileUtils.getTestFile(configFile);

		String expected = configurationSource;
		String actual = ConfigurationUtils.getCanonicalizedConfiguration(configurationSource);
		
		MatchUtils.assertXmlEquals(configFile, expected, actual);
	}

	public void testCanonicalize2(String configFile) throws Exception {
		String configurationSource = TestFileUtils.getTestFile(configFile);

		String expected = configurationSource;
		String actual = ConfigurationUtils.getCanonicalizedConfiguration2(configurationSource, new XmlErrorHandler());
		
		MatchUtils.assertXmlEquals(configFile, expected, actual);
	}

	@Test
	public void testCanonicalize1a() throws Exception {
		testCanonicalize1("/IAF_Util/ConfigurationManageDatabase.xml");
	}

	@Test
	public void testCanonicalize2a() throws Exception {
		testCanonicalize2("/IAF_Util/ConfigurationManageDatabase.xml");
	}

	private class XmlErrorHandler implements ErrorHandler  {

		@Override
		public void warning(SAXParseException exception) throws SAXParseException {
			System.out.println("Warning at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void error(SAXParseException exception) throws SAXParseException {
			System.out.println("Error at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void fatalError(SAXParseException exception) throws SAXParseException {
			System.out.println("FatalError at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
	}
}
