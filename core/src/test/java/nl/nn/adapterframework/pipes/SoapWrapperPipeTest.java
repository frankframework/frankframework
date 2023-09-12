package nl.nn.adapterframework.pipes;

import javax.xml.soap.SOAPConstants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.IWrapperPipe.Direction;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.processors.InputOutputPipeProcessor;
import nl.nn.adapterframework.soap.SoapVersion;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;

public class SoapWrapperPipeTest<P extends SoapWrapperPipe> extends PipeTestBase<P> {

	private final String TARGET_NAMESPACE="urn:fakenamespace";

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
		pipe.configure();
		pipe.start();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testUnwrapRemoveNamespaces() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setRemoveOutputNamespaces(true);
		pipe.configure();
		pipe.start();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<root>\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testUnwrapSwitchRoot() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setRoot("OtherRoot");
		pipe.configure();
		pipe.start();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<OtherRoot xmlns=\""+TARGET_NAMESPACE+"\">"
				+"<attrib>1</attrib>"
				+"<attrib>2</attrib>"
				+"</OtherRoot>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testUnwrapRemoveNamespacesAndSwitchRoot() throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.setRemoveOutputNamespaces(true);
		pipe.setRoot("OtherRoot");
		pipe.configure();
		pipe.start();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<OtherRoot>"
				+"<attrib>1</attrib>"
				+"<attrib>2</attrib>"
				+"</OtherRoot>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}


	@Test
	public void testWrap() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.configure();
		pipe.start();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoapVersionSoap12() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapVersion(SoapVersion.SOAP12);
		pipe.configure();
		pipe.start();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoapVersionNone() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapVersion(SoapVersion.NONE);
		pipe.configure();
		pipe.start();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoap11ViaSessionKey() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapNamespaceSessionKey("soapNamespace");
		pipe.configure();
		pipe.start();

		session.put("soapNamespace", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoap12ViaSessionKey() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapNamespaceSessionKey("soapNamespace");
		pipe.configure();
		pipe.start();

		session.put("soapNamespace", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapSoapVersionDefaultViaSessionKey() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setSoapNamespaceSessionKey("soapNamespace");
		pipe.configure();
		pipe.start();

		session.put("soapNamespace","");

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testWrapChangeRoot() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setRoot("OtherRoot");
		pipe.configure();
		pipe.start();

		String input = "<root>\n<attrib>1</attrib>\n<attrib>2</attrib>\n</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<OtherRoot xmlns=\""+TARGET_NAMESPACE+"\">"
				+"<attrib>1</attrib>"
				+"<attrib>2</attrib>"
				+"</OtherRoot></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	public void testUnwrapConditional(boolean expectUnwrap) throws Exception {
		pipe.setDirection(Direction.UNWRAP);
		pipe.configure();
		pipe.start();

		String input = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";

		InputOutputPipeProcessor ioProcessor = new InputOutputPipeProcessor();
		CorePipeProcessor coreProcessor = new CorePipeProcessor();

		ioProcessor.setPipeProcessor(coreProcessor);
		PipeRunResult prr = ioProcessor.processPipe(pipeline, pipe, new Message(input), session);

		String actual = prr.getResult().asString();

		if (expectUnwrap) {
			TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
		} else {
			Assertions.assertEquals(input, actual);
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
		String expected = "<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";

		PipeRunResult prr = doPipe(pipe, input,new PipeLineSession());

		String actual = prr.getResult().asString();

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

}
