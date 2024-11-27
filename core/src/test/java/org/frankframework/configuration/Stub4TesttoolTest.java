package org.frankframework.configuration;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.XmlUtils;

public class Stub4TesttoolTest {

	private static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	private static final String STUB4TESTTOOL_XSLT = "/xml/xsl/stub4testtool.xsl";

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
	public void stub4testtoolForEachChildElementPipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ForEachChildElementPipe";
		stub4testtoolTest(directory, false);
	}

	@Test
	public void stub4testtoolSamba2Pipe() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/Samba2Pipe";
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
		Map<String, Object> parameters = new Hashtable<>();
		parameters.put(STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, disableValidators);

		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_ORIGINAL_FILENAME);
		String stubbedConfiguration = transformConfiguration(originalConfiguration, STUB4TESTTOOL_XSLT, parameters);
		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_EXPECTED_FILENAME);

		MatchUtils.assertXmlEquals(expectedConfiguration, stubbedConfiguration);
	}

	private String transformConfiguration(String originalConfig, String xslt, Map<String, Object> parameters) throws ConfigurationException {
		URL xsltSource = ClassLoaderUtils.getResourceURL(xslt);
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
			throw new ConfigurationException("got error transforming resource [" + xsltSource + "] from [" + xslt + "]", te);
		}
	}
}
