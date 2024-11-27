package org.frankframework.extensions.esb;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;

public class EsbSoapWrapperPipeTest extends PipeTestBase<EsbSoapWrapperPipe> {

	@Override
	public EsbSoapWrapperPipe createPipe() {
		EsbSoapWrapperPipe pipe = new EsbSoapWrapperPipe();
		return pipe;
	}

	public void addParam(String name, String value) {
		Parameter param = new Parameter();
		param.setName(name);
		param.setValue(value);
		pipe.addParameter(param);
	}

	@Test
	public void testWrapCMH2InError() throws Exception {
		pipe.setOutputNamespace("http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1");
		pipe.setCmhVersion(2);
		addParam("destination", "destination-value");
		addParam("errorCode", "errorCode-value");
		addParam("errorReason", "errorReason-value");
		addParam("errorDetailCode", "errorDetailCode-value");
		addParam("errorDetailText", "errorDetailText-value");
		addParam("operation", "operation-value");
		addParam("operationVersion", "operationVersion-value");
		pipe.configure();
		pipe.start();

		String input = "<GetDocumentAndAttributes_Response><attrib>1</attrib><attrib>2</attrib></GetDocumentAndAttributes_Response>";

		PipeRunResult prr = pipe.doPipe(new Message(input), new PipeLineSession());

		String result = prr.getResult().asString();
		TestAssertions.assertXpathValueEquals("TestConfiguration", result, "/Envelope/Header/MessageHeader/From/Id");
		TestAssertions.assertXpathValueEquals("P2P.?.?.destination_value.?", result, "/Envelope/Header/MessageHeader/To/Location");
		TestAssertions.assertXpathValueEquals("operationVersion-value", result, "/Envelope/Header/MessageHeader/Service/Action/Version");
		TestAssertions.assertXpathValueEquals("ERROR", result, "/Envelope/Body/GetDocumentAndAttributes_Response/Result/Status");
		TestAssertions.assertXpathValueEquals("errorCode-value", result, "/Envelope/Body/GetDocumentAndAttributes_Response/Result/ErrorList/Error/Code");
		TestAssertions.assertXpathValueEquals("errorReason-value", result, "/Envelope/Body/GetDocumentAndAttributes_Response/Result/ErrorList/Error/Reason");
		TestAssertions.assertXpathValueEquals("errorDetailCode-value", result, "/Envelope/Body/GetDocumentAndAttributes_Response/Result/ErrorList/Error/DetailList/Detail/Code");
		TestAssertions.assertXpathValueEquals("errorDetailText-value", result, "/Envelope/Body/GetDocumentAndAttributes_Response/Result/ErrorList/Error/DetailList/Detail/Text");
	}

	@Test
	public void testWrapFindDocumentsError() throws Exception {
		String outputNamespace = "http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1";
		String rootElement = "GetDocumentAndAttributes_Response";

		String messagingLayer = "ESB";
		String businessDomain = "Archiving";
		String applicationName = "BS";
		String serviceName = "Document";
		String serviceVersion = "3";
		String operation = "FindDocuments";
		String operationVersion = "1";
		String paradigm = "Request";

		String errorCode = null;
		String errorReason = null;
		String errorDetailCode = null;
		String errorDetailText = null;

		String destination = messagingLayer + "." + businessDomain + "." + applicationName + "." + serviceName + "." + serviceVersion + "." + operation + "." + operationVersion + "." + paradigm;

		pipe.setOutputNamespace(outputNamespace);
		pipe.setCmhVersion(2);
		addParam("destination", destination);
		addParam("errorCode", errorCode);
		addParam("errorReason", errorReason);
		addParam("errorDetailCode", errorDetailCode);
		addParam("errorDetailText", errorDetailText);
		addParam("operation", null);
		addParam("operationVersion", null);
		pipe.configure();
		pipe.start();

		String expectedErrorCode = "<GetDocumentAndAttributes_Response xmlns=\"http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1\"><attrib>1</attrib><attrib>2</attrib></GetDocumentAndAttributes_Response>";
		String expectedErrorReason = expectedErrorCode.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

		String input = "<" + rootElement + "><attrib>1</attrib><attrib>2</attrib></" + rootElement + ">";

		PipeRunResult prr = pipe.doPipe(new Message(input), new PipeLineSession());

		String result = prr.getResult().asString();
		TestAssertions.assertXpathValueEquals("TestConfiguration", result, "/Envelope/Header/MessageHeader/From/Id");
		TestAssertions.assertXpathValueEquals(destination, result, "/Envelope/Header/MessageHeader/To/Location");
		TestAssertions.assertXpathValueEquals(serviceName, result, "/Envelope/Header/MessageHeader/Service/Name");
		TestAssertions.assertXpathValueEquals(paradigm, result, "/Envelope/Header/MessageHeader/Service/Action/Paradigm");
		TestAssertions.assertXpathValueEquals(operation, result, "/Envelope/Header/MessageHeader/Service/Action/Name");
		TestAssertions.assertXpathValueEquals(operationVersion, result, "/Envelope/Header/MessageHeader/Service/Action/Version");

		TestAssertions.assertXpathValueEquals("ERROR", result, "/Envelope/Body/" + rootElement + "/Result/Status");
		TestAssertions.assertXpathValueEquals(expectedErrorCode, result, "/Envelope/Body/" + rootElement + "/Result/ErrorList/Error/Code");
		TestAssertions.assertXpathValueEquals(expectedErrorReason, result, "/Envelope/Body/" + rootElement + "/Result/ErrorList/Error/Reason");
		TestAssertions.assertXpathValueEquals(expectedErrorCode, result, "/Envelope/Body/" + rootElement + "/Result/ErrorList/Error/DetailList/Detail/Code"); //not sure why the DetailCode field uses the ErrorCode...
		TestAssertions.assertXpathValueEquals(expectedErrorReason, result, "/Envelope/Body/" + rootElement + "/Result/ErrorList/Error/DetailList/Detail/Text"); //not sure why the DetailReason field uses the ErrorReason...
	}

}
