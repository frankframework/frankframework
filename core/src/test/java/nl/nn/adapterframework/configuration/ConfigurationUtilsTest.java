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
import java.util.Hashtable;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
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
import org.xml.sax.SAXException;

import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class ConfigurationUtilsTest extends Mockito {

	private ApplicationContext applicationContext = mock(ApplicationContext.class);
	private PreparedStatementMock stmt;

	private static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	private static final String STUB4TESTTOOL_XSLT = "/xml/xsl/stub4testtool.xsl";

	private static final String STUB4TESTTOOL_DIRECTORY = "/ConfigurationUtils/stub4testtool";
	private static final String STUB4TESTTOOL_ORIGINAL_FILENAME = "original.xml";
	private static final String STUB4TESTTOOL_EXPECTED_FILENAME = "expected.xml";

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

	// Listeners
	@Test
	public void stub4testtoolApiListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ApiListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolRestListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/RestListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolWebServiceListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/WebServiceListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolDirectoryListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/DirectoryListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolJavaListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/JavaListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolJdbcQueryListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/JdbcQueryListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolJdbcTableListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/JdbcTableListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolMessageStoreListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/MessageStoreListener";
		stub4testtoolTest(directory, false);
	}

	// Senders
	@Test
	public void stub4testtoolResultSet2FileSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ResultSet2FileSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolDirectQuerySender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/DirectQuerySender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolFixedQuerySender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/FixedQuerySender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolXmlQuerySender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/XmlQuerySender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolDelaySender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/DelaySender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolEchoSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/EchoSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolIbisLocalSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/IbisLocalSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolLogSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/LogSender";
		stub4testtoolTest(directory, false);
	}
	@Test
	public void stub4testtoolParallelSenders() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ParallelSenders";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSenderSeries() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SenderSeries";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolXsltSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/XsltSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolCommandSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/CommandSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolFixedResultSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/FixedResultSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolJavascriptSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/JavascriptSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolMessageStoreSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/MessageStoreSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolReloadSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ReloadSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolZipWriterSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ZipWriterSender";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolLocalFileSystemSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/LocalFileSystemSender";
		stub4testtoolTest(directory, false);
	}

	// Pipes
	@Test
	public void stub4testtoolPutSystemDateInSession() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/PutSystemDateInSession";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolEsbSoapWrapperPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/EsbSoapWrapperPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolGetPrincipalPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/GetPrincipalPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolIsUserInRolePipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/IsUserInRolePipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolUUIDGeneratorPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/UUIDGeneratorPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolFtpFileRetrieverPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/FtpFileRetrieverPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSendTibcoMessage() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SendTibcoMessage";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolLdapFindMemberPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/LdapFindMemberPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolLdapFindGroupMembershipsPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/LdapFindGroupMembershipsPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolGenericMessageSendingPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/GenericMessageSendingPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolForEachChildElementPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ForEachChildElementPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSenderPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SenderPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSenderWrapper() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SenderWrapper";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSenderInSender() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SenderInSender";
		stub4testtoolTest(directory, false);
	}

	// Other
	@Test
	public void stub4testtoolParameterPatternNow() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ParameterPatternNow";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolJmsRealm() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/JmsRealm";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSapSystem() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SapSystem";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolListenerInPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ListenerInPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolStores() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/Stores";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolValidatorOn() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ValidatorsOn";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolValidatorOff() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ValidatorsOff";
		stub4testtoolTest(directory, true);
	}

	@Test
	public void stub4testtoolComments() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/Comments";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolMultipleReceivers() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/MultipleReceivers";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolReceiverTransactionAttribute() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ReceiverTransactionAttribute";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolFullAdapter() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/FullAdapter";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolEsbJmsListener() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/EsbJmsListener";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolDisableStub() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/DisableStub";
		stub4testtoolTest(directory, false);
	}

	private void stub4testtoolTest(String baseDirectory, boolean disableValidators) throws Exception {
		Map<String, Object> parameters = new Hashtable<String, Object>();
		parameters.put(STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, disableValidators);

		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_ORIGINAL_FILENAME);
		String stubbedConfiguration = transformConfiguration(originalConfiguration, STUB4TESTTOOL_XSLT, parameters);
		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_EXPECTED_FILENAME);

		MatchUtils.assertXmlEquals(expectedConfiguration, stubbedConfiguration);
	}

	private String transformConfiguration(String originalConfig, String xslt, Map<String, Object> parameters) throws ConfigurationException {
		URL xsltSource = ClassUtils.getResourceURL(xslt);
		if (xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + xslt + "]");
		}
		try {
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			XmlUtils.setTransformerParameters(transformer, parameters);
			// Use namespaceAware=true, otherwise for some reason the
			// transformation isn't working with a SAXSource, in system out it
			// generates:
			// jar:file: ... .jar!/xml/xsl/active.xsl; Line #34; Column #13; java.lang.NullPointerException
			return XmlUtils.transformXml(transformer, originalConfig, true);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + xslt + "]", e);
		} catch (SAXException|TransformerConfigurationException e) {
			throw new ConfigurationException("got error creating transformer from file [" + xslt + "]", e);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + xsltSource.toString() + "] from [" + xslt + "]", te);
		}
	}
}