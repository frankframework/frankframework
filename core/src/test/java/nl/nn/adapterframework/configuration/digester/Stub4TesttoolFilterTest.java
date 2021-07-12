/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration.digester;

import java.io.StringWriter;
import java.util.Properties;

import org.junit.Test;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.configuration.filters.Stub4TesttoolFilter;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class Stub4TesttoolFilterTest {
	private static final String STUB4TESTTOOL_DIRECTORY = "/ConfigurationUtils/stub4testtool";
	private static final String STUB4TESTTOOL_ORIGINAL_FILENAME = "original.xml";
	private static final String STUB4TESTTOOL_EXPECTED_FILENAME = "expected.xml";
	
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
	public void stub4testtoolFullAdapter() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/FullAdapter";
		stub4testtoolTest(directory, false);
	}
	
	public void stub4testtoolTest(String baseDirectory, boolean disableValidators) throws Exception {
		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);
		
		Properties properties = new Properties();
		properties.setProperty("stub4testtool.configuration", "true");
		properties.setProperty("validators.disabled", Boolean.toString(disableValidators));
		
		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_ORIGINAL_FILENAME);
		
		ContentHandler filter = Stub4TesttoolFilter.getStub4TesttoolContentHandler(xmlWriter, properties);
		
		XmlUtils.parseXml(originalConfiguration, filter);
		
		String actual = new String(target.toString());

		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_EXPECTED_FILENAME);
		MatchUtils.assertXmlEquals(expectedConfiguration, actual);
	}
}
