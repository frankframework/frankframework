package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.soap.SOAPConstants;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IWrapperPipe.Direction;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.processors.InputOutputPipeProcessor;
import nl.nn.adapterframework.soap.SoapVersion;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;

public class SoapWrapperPipeTest<P extends SoapWrapperPipe> extends PipeTestBase<P> {

	private static final String TARGET_NAMESPACE = "urn:fakenamespace";
	private static final String DEFAULT_BODY_END = "<root xmlns=\"" + TARGET_NAMESPACE + "\">\n<attrib>1</attrib>\n<attrib>2</attrib>\n"
			+ "</root></soapenv:Body></soapenv:Envelope>";

	private final String messageBody = "<soapenv:Body><FindDocuments_Response xmlns=\"http://api.nn.nl/FindDocuments\">"
			+ "<Result xmlns=\"http://nn.nl/XSD/Generic/MessageHeader/1\"><Status>OK</Status></Result>"
			+ "</FindDocuments_Response></soapenv:Body></soapenv:Envelope>";
	private final String soapMessageSoap11 = "<soapenv:Envelope xmlns:soapenv=\"" + SoapVersion.SOAP11.namespace + "\">" + messageBody;
	private final String soapMessageSoap12 = "<soapenv:Envelope xmlns:soapenv=\"" + SoapVersion.SOAP12.namespace + "\">" + messageBody;

	@Override
	public P createPipe() {
		return (P) new SoapWrapperPipe();
	}

	public void addParam(String name, String value) {
		pipe.addParameter(new Parameter(name, value));
	}

	@Test
	public void testUnwrap() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		configureAndStartPipe();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+ DEFAULT_BODY_END;
		String expected = "<root xmlns=\"" + TARGET_NAMESPACE + "\">\n"
				+ "<attrib>1</attrib>\n"
				+ "<attrib>2</attrib>\n"
				+ "</root>";

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testShouldKeepPipeSoapConfigurationSoap11VersionWhileWrapping() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.registerForward(new PipeForward("pipe2", "READY"));
		pipe.setRemoveOutputNamespaces(true);
		configureAndStartPipe();

		P wrapPipeSoap = createWrapperSoapPipe(SoapVersion.SOAP11, true);

		// Arrange
		String inputSoap12 = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeLineSession pipeLineSession = new PipeLineSession();
		// Act
		PipeRunResult prr = doPipe(pipe, inputSoap12, pipeLineSession);
		PipeRunResult pipeRunResult = doPipe(wrapPipeSoap, prr.getResult(), pipeLineSession);

		// Assert
		String actual = pipeRunResult.getResult().asString();
		assertNotNull(actual);
		assertTrue(actual.contains(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE));
		pipeLineSession.close();
		wrapPipeSoap.stop();
	}

	@Test
	public void testShouldKeepPipeSoapConfigurationSoap12VersionWhileWrapping() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.registerForward(new PipeForward("pipe2", "READY"));
		pipe.setRemoveOutputNamespaces(true);
		pipe.configure();
		pipe.start();

		P wrapPipeSoap = createWrapperSoapPipe(SoapVersion.SOAP12, true);

		// Arrange
		String inputSoap11 = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeLineSession pipeLineSession = new PipeLineSession();

		// Act
		PipeRunResult prr = doPipe(pipe, inputSoap11, pipeLineSession);
		PipeRunResult pipeRunResult = doPipe(wrapPipeSoap, prr.getResult(), pipeLineSession);

		// Assert
		String actual = pipeRunResult.getResult().asString();
		assertNotNull(actual);
		assertTrue(actual.contains(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE));
		pipeLineSession.close();
		wrapPipeSoap.stop();
	}

	@Test
	public void testKeepSessionKeyHeaderContent() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setStoreResultInSessionKey("originalMessage_unwrapped");
		pipe.setRemoveOutputNamespaces(true);
		pipe.setSoapHeaderSessionKey("key");
		configureAndStartPipe();

		// Arrange
		String inputSoap11 = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\">" +
				"<soapenv:Header><trans>234</trans></soapenv:Header>" +
				"<soapenv:Body>" + DEFAULT_BODY_END;
		PipeLineSession pipeLineSession = new PipeLineSession();

		// Act
		doPipe(pipe, inputSoap11, pipeLineSession);
		pipeLineSession.close();

		// Assert
		assertTrue(pipeLineSession.get("key").toString().contains(">234</trans>"));

		// Arrange 2
		String inputSoap12 = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\">" +
				"<soapenv:Header><trans>234</trans></soapenv:Header>" +
				"<soapenv:Body>" + DEFAULT_BODY_END;
		pipeLineSession = new PipeLineSession();

		// Act 2
		doPipe(pipe, inputSoap12, pipeLineSession);
		pipeLineSession.close();

		// Assert 2
		assertTrue(pipeLineSession.get("key").toString().contains(">234</trans>"));
	}

	@Test
	public void testUnwrapRemoveNamespaces() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setRemoveOutputNamespaces(true);
		configureAndStartPipe();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;
		String expected = "<root>\n"
				+ "<attrib>1</attrib>\n"
				+ "<attrib>2</attrib>\n"
				+ "</root>";

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testUnwrapSwitchRoot() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setRoot("OtherRoot");
		configureAndStartPipe();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;
		String expected = "<OtherRoot xmlns=\"" + TARGET_NAMESPACE + "\">"
				+ "<attrib>1</attrib>"
				+ "<attrib>2</attrib>"
				+ "</OtherRoot>";

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testUnwrapRemoveNamespacesAndSwitchRoot() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setRemoveOutputNamespaces(true);
		pipe.setRoot("OtherRoot");
		configureAndStartPipe();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;
		String expected = "<OtherRoot>"
				+ "<attrib>1</attrib>"
				+ "<attrib>2</attrib>"
				+ "</OtherRoot>";

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}


	@Test
	public void testWrap() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		configureAndStartPipe();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoapVersionSoap12() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapVersion(SoapVersion.SOAP12);
		configureAndStartPipe();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoapVersionNone() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapVersion(SoapVersion.NONE);
		configureAndStartPipe();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<root xmlns=\"" + TARGET_NAMESPACE + "\">\n"
				+ "<attrib>1</attrib>\n"
				+ "<attrib>2</attrib>\n"
				+ "</root>";

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoap11ViaSessionKey() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapNamespaceSessionKey("soapNamespace");
		configureAndStartPipe();

		session.put("soapNamespace", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeRunResult prr = doPipe(pipe, input, session);

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoap12ViaSessionKey() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapNamespaceSessionKey("soapNamespace");
		configureAndStartPipe();

		session.put("soapNamespace", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeRunResult prr = doPipe(pipe, input, session);

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoapVersionDefaultViaSessionKey() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapNamespaceSessionKey("soapNamespace");
		configureAndStartPipe();

		session.put("soapNamespace", "");

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeRunResult prr = doPipe(pipe, input, session);

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapChangeRoot() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setRoot("OtherRoot");
		configureAndStartPipe();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+ "<OtherRoot xmlns=\"" + TARGET_NAMESPACE + "\">"
				+ "<attrib>1</attrib>"
				+ "<attrib>2</attrib>"
				+ "</OtherRoot></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	public void testUnwrapConditional(boolean expectUnwrap) throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		configureAndStartPipe();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;
		String expected = "<root xmlns=\"" + TARGET_NAMESPACE + "\">\n"
				+ "<attrib>1</attrib>\n"
				+ "<attrib>2</attrib>\n"
				+ "</root>";

		InputOutputPipeProcessor ioProcessor = new InputOutputPipeProcessor();
		CorePipeProcessor coreProcessor = new CorePipeProcessor();

		ioProcessor.setPipeProcessor(coreProcessor);
		PipeRunResult prr = ioProcessor.processPipe(pipeline, pipe, new Message(input), session);

		String actual = prr.getResult().asString();

		if (expectUnwrap) {
			TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
		} else {
			assertEquals(input, actual);
		}
	}

	@Test
	public void testUnwrapConditionalOnlyIf() throws Exception {
		pipe.setOnlyIfSessionKey("onlyIfKey");
		session.put("onlyIfKey", "dummy");
		testUnwrapConditional(true);
	}

	@Test
	public void testUnwrapConditionalOnlyIfSkip() throws Exception {
		pipe.setOnlyIfSessionKey("onlyIfKey");
		testUnwrapConditional(false);
	}

	@Test
	public void testUnwrapConditionalOnlyIfValueEqual() throws Exception {
		pipe.setOnlyIfSessionKey("onlyIfKey");
		pipe.setOnlyIfValue("onlyIfTargetValue");
		session.put("onlyIfKey", "onlyIfTargetValue");
		testUnwrapConditional(true);
	}

	@Test
	public void testUnwrapConditionalOnlyIfSkipValueNotEqual() throws Exception {
		pipe.setOnlyIfSessionKey("onlyIfKey");
		pipe.setOnlyIfValue("onlyIfTargetValue");
		session.put("onlyIfKey", "otherValue");
		testUnwrapConditional(false);
	}

	@Test
	public void testUnwrapConditionalOnlyIfSkipValueNoValue() throws Exception {
		pipe.setOnlyIfSessionKey("onlyIfKey");
		pipe.setOnlyIfValue("onlyIfTargetValue");
		testUnwrapConditional(false);
	}

	@Test
	public void testUnwrapConditionalUnless() throws Exception {
		pipe.setUnlessSessionKey("unlessKey");
		session.put("unlessKey", "dummy");
		testUnwrapConditional(false);
	}

	@Test
	public void testUnwrapConditionalUnlessSkip() throws Exception {
		pipe.setUnlessSessionKey("unlessKey");
		testUnwrapConditional(true);
	}

	@Test
	public void testUnwrapConditionalUnlessValueEqual() throws Exception {
		pipe.setUnlessSessionKey("unlessKey");
		pipe.setOnlyIfValue("unlessTargetValue");
		session.put("unlessKey", "unlessTargetValue");
		testUnwrapConditional(false);
	}

	@Test
	public void testUnwrapConditionalUnlessSkipValueNotEqual() throws Exception {
		pipe.setUnlessSessionKey("unlessKey");
		pipe.setOnlyIfValue("unlessTargetValue");
		session.put("unlessKey", "otherValue");
		testUnwrapConditional(false);
	}

	@Test
	public void testUnwrapConditionalUnlessSkipValueNoValue() throws Exception {
		pipe.setUnlessSessionKey("unlessKey");
		pipe.setOnlyIfValue("unlessTargetValue");
		testUnwrapConditional(true);
	}

	@Test
	public void testWrapSoap11() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.configure();
		pipe.setSoapVersion(SoapVersion.SOAP11);
		pipe.start();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>" + DEFAULT_BODY_END;

		PipeRunResult prr = doPipe(pipe, input, new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	void shouldUseConfiguredPipelineSoapVersion11() throws Exception {
		// Arrange: Simulate soapOutputWrapper having soap version 1.1 (expecting soap 1.1 namespace in the output)
		setupUnwrapSoapPipe(SoapVersion.AUTO, true);
		P outputWrapper = createWrapperSoapPipe(SoapVersion.SOAP11, true);

		// Act: message through unwrap & wrap pipes
		PipeRunResult pipeRunResult1 = doPipe(pipe, Message.asMessage(soapMessageSoap12), session);
		PipeRunResult pipeRunResult2 = doPipe(outputWrapper, pipeRunResult1.getResult(), session);

		// Assert: message is determined as SOAP 1.2
		assertEquals(SoapVersion.SOAP12, session.get(SoapWrapper.SOAP_VERSION_SESSION_KEY));

		// Assert: but the output is determined as SOAP 1.1 instead, as configured
		String result = pipeRunResult2.getResult().asString();
		assert result != null;
		assertTrue(result.contains(SoapVersion.SOAP11.namespace), "result should have the same namespace as configured to be 1.1 namespace");
		outputWrapper.stop();
	}

	@Test
	void shouldUseConfiguredPipelineSoapVersion12() throws Exception {
		// Arrange: Simulate soapOutputWrapper having soap version 1.2 (expecting soap 1.2 namespace in the output)
		setupUnwrapSoapPipe(SoapVersion.AUTO, true);
		P outputWrapper = createWrapperSoapPipe(SoapVersion.SOAP12, true);
		session = new PipeLineSession();

		// Act: message SOAP11 through unwrap & wrap pipes
		PipeRunResult pipeRunResult1 = doPipe(pipe, Message.asMessage(soapMessageSoap11), session);
		PipeRunResult pipeRunResult2 = doPipe(outputWrapper, pipeRunResult1.getResult(), session);

		// Assert: but the output is determined as SOAP 1.2 instead, as configured
		String result = pipeRunResult2.getResult().asString();
		assert result != null;
		assertTrue(result.contains(SoapVersion.SOAP12.namespace), "result should have the same namespace as configured to be 1.2 namespace");
		outputWrapper.stop();
	}

	@Test
	void shouldUseConfiguredPipelineSoapVersionAuto() throws Exception {
		// Arrange: Wrapper pipe SoapVersion not set. Should go to AUTO.
		setupUnwrapSoapPipe(SoapVersion.AUTO, true);
		P outputWrapper = createWrapperSoapPipe(null, true);
		session = new PipeLineSession();

		// Act: message SOAP12 through unwrap & wrap pipes
		PipeRunResult pipeRunResult1 = doPipe(pipe, Message.asMessage(soapMessageSoap12), session);
		PipeRunResult pipeRunResult2 = doPipe(outputWrapper, pipeRunResult1.getResult(), session);

		// Assert: but the output is determined as SOAP 1.2 instead, as configured
		String result = pipeRunResult2.getResult().asString();
		assert result != null;
		assertTrue(result.contains(SoapVersion.SOAP12.namespace), "result should have the same namespace as configured to be 1.2 namespace");
		outputWrapper.stop();
	}

	@Test
	void shouldUnwrapEvenThoughSessionHasDifferentSoapVersionStored() throws Exception {
		// Scenario:
		// <inputWrapper direction="UNWRAP" storeResultInSessionKey="originalMessage"/> Note: input=1.2, stored version in session
		// <inputWrapper soapVersion="1.1"> Note: output=1.1
		// <outputWrapper direction="UNWRAP" removeOutputNamespaces="true" soapVersion="1.1"/> Note: input=1.1, should be unwrapped fine with 1.1

		// Arrange: inputWrapper 1 - unwrap
		setupUnwrapSoapPipe(SoapVersion.AUTO, false);
		pipe.setDirection(Direction.UNWRAP);
		pipe.setStoreResultInSessionKey("originalMessage");
		pipe.configure();
		pipe.start();

		// Arrange: inputWrapper 2 - wrap
		P outputWrapper = createPipe();
		autowireByType(outputWrapper);
		outputWrapper.registerForward(new PipeForward("success", PipeLine.OUTPUT_WRAPPER_NAME + "2"));
		outputWrapper.setName(PipeLine.OUTPUT_WRAPPER_NAME + "1");
		outputWrapper.setSoapVersion(SoapVersion.SOAP11);
		pipeline.addPipe(outputWrapper);
		outputWrapper.configure();
		outputWrapper.start();

		// Arrange: outputWrapper 2 - unwrap
		P outputWrapper2 = createWrapperSoapPipe(SoapVersion.SOAP11, false);
		outputWrapper2.setName(PipeLine.OUTPUT_WRAPPER_NAME + "2");
		outputWrapper2.setDirection(Direction.UNWRAP);
		outputWrapper2.setRemoveOutputNamespaces(true);
		outputWrapper2.registerForward(new PipeForward("success", "READY"));
		outputWrapper2.configure();
		outputWrapper2.start();
		pipeline.addPipe(outputWrapper2);

		session = new PipeLineSession();

		// Act: message SOAP12 through pipes
		PipeRunResult pipeRunResult1 = doPipe(pipe, Message.asMessage(soapMessageSoap12), session);
		PipeRunResult pipeRunResult2 = doPipe(outputWrapper, pipeRunResult1.getResult(), session);
		PipeRunResult pipeRunResult3 = doPipe(outputWrapper2, pipeRunResult2.getResult(), session);

		// Assert: but the output is without SOAP namespaces, as it is unwrapped.
		String result = pipeRunResult3.getResult().asString();
		assertNotNull(result);
		assertFalse(result.contains(SoapVersion.SOAP11.namespace), "result should be unwrapped for sure");
		assertFalse(result.contains(SoapVersion.SOAP12.namespace), "result should be unwrapped for sure");
		assertTrue(result.contains("FindDocuments_Response"), "result should contain original message, without namespaces");
		outputWrapper.stop();
	}

	private P createWrapperSoapPipe(final SoapVersion soapVersion, boolean started) throws ConfigurationException, PipeStartException {
		P wrapPipeSoap = createPipe();
		wrapPipeSoap.setDirection(Direction.WRAP);
		autowireByType(wrapPipeSoap);
		wrapPipeSoap.registerForward(new PipeForward("success", "READY"));
		wrapPipeSoap.setName(PipeLine.OUTPUT_WRAPPER_NAME);
		wrapPipeSoap.setSoapVersion(soapVersion);
		pipeline.addPipe(wrapPipeSoap);
		if (started) {
			wrapPipeSoap.configure();
			wrapPipeSoap.start();
		}
		return wrapPipeSoap;
	}

	private void setupUnwrapSoapPipe(final SoapVersion soapVersion, boolean started) throws ConfigurationException, PipeStartException {
		// Arrange - Simulate soapInputWrapper unwrapping the input and creating soapNamespace sessionKey using the namespace from the input
		pipe.setDirection(Direction.UNWRAP);
		pipe.setSoapVersion(soapVersion);
		pipe.setName(PipeLine.INPUT_WRAPPER_NAME);
		if (started) {
			pipe.configure();
			pipe.start();
		}
	}

}