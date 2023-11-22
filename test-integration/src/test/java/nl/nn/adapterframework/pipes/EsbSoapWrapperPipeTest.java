package nl.nn.adapterframework.pipes;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.XmlEncodingUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class EsbSoapWrapperPipeTest extends SoapWrapperPipeTest<EsbSoapWrapperPipe> {

	@Override
	public EsbSoapWrapperPipe createPipe() {
		EsbSoapWrapperPipe pipe = new EsbSoapWrapperPipe();
		return pipe;
	}

	
	@Override
	public void addParam(String name, String value) {
		Parameter param = new Parameter();
		param.setName(name);
		param.setValue(value);
		pipe.addParameter(param);
	}
	
	
	@Test
	public void testWrapCMH2() {
		pipe.setOutputNamespace("http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1");
		pipe.setCmhVersion(2);
		addParam("destination","destination-value");
		addParam("errorCode","errorCode-value");
		addParam("errorReason","errorReason-value");
		addParam("errorDetailCode","errorDetailCode-value");
		addParam("errorDetailText","errorDetailText-value");
		addParam("operation","operation-value");
		addParam("operationVersion","operationVersion-value");
		pipe.configure();
		pipe.start();
		
		String input = "<GetDocumentAndAttributes_Response><attrib>1</attrib><attrib>2</attrib></GetDocumentAndAttributes_Response>";
		
		PipeRunResult prr = pipe.doPipe(new Message(input),new PipeLineSession());
		
		String result=prr.getResult().asString();
		System.out.println("result ["+result+"]");
		TestAssertions.assertXpathValueEquals("test", 					result, "/Envelope/Header/MessageHeader/From/Id");
//		TestAssertions.assertXpathValueEquals("test", 					result, "/Envelope/Header/MessageHeader/Service/Name");
//		TestAssertions.assertXpathValueEquals("operationVersion-value", result, "**/Envelope/Header/MessageHeader/Service/Version");
		TestAssertions.assertXpathValueEquals("OK", 					result, "/Envelope/Body/MessageHeader/Result/Status");
	}

	@Test
	public void testWrapFindDocumentsOK() {
		
		String outputNamespace="http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1";
		String rootElement="GetDocumentAndAttributes_Response";
		
		String messagingLayer="ESB";
		String businessDomain="Archiving";
		String applicationName="BS";
		String serviceName="Document";
		String serviceVersion="3";
		String operation="FindDocuments";
		String operationVersion="1";
		String paradigm="Request";
		
		String errorCode=null;
		String errorReason=null;
		String errorDetailCode=null;
		String errorDetailText=null;
		
		
		String destination=messagingLayer+"."+businessDomain+"."+applicationName+"."+serviceName+"."+serviceVersion+"."+operation+"."+operationVersion+"."+paradigm;
		
		pipe.setOutputNamespace(outputNamespace);
		pipe.setCmhVersion(2);
		addParam("destination",destination);
		addParam("errorCode",errorCode);
		addParam("errorReason",errorReason);
		addParam("errorDetailCode",errorDetailCode);
		addParam("errorDetailText",errorDetailText);
		addParam("operation",null);
		addParam("operationVersion",null);
		pipe.configure();
		pipe.start();
		
		String input = "<"+rootElement+"><attrib>1</attrib><attrib>2</attrib></"+rootElement+">";
		
		PipeRunResult prr = pipe.doPipe(new Message(input),new PipeLineSession());
		
		String result=prr.getResult().asString();
		System.out.println("result ["+result+"]");
		TestAssertions.assertXpathValueEquals("test", 			result, "/Envelope/Header/MessageHeader/From/Id");
		TestAssertions.assertXpathValueEquals(destination, 		result, "/Envelope/Header/MessageHeader/To/Location");
		TestAssertions.assertXpathValueEquals(serviceName, 		result, "/Envelope/Header/MessageHeader/Service/Name");
		TestAssertions.assertXpathValueEquals(paradigm, 		result, "/Envelope/Header/MessageHeader/Service/Action/Paradigm");
		TestAssertions.assertXpathValueEquals(operation, 		result, "/Envelope/Header/MessageHeader/Service/Action/Name");
		TestAssertions.assertXpathValueEquals(operationVersion, result, "/Envelope/Header/MessageHeader/Service/Action/Version");

		TestAssertions.assertXpathValueEquals("ERROR", 			result, "/Envelope/Body/"+rootElement+"/Result/Status");
		TestAssertions.assertXpathValueEquals(errorCode, 		result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/Code");
		TestAssertions.assertXpathValueEquals(errorReason, 		result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/Reason");
		TestAssertions.assertXpathValueEquals(errorDetailCode, 	result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/DetailList/Detail/Code");
		TestAssertions.assertXpathValueEquals(XmlEncodingUtils.encodeChars(errorDetailText), 	result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/DetailList/Detail/Text");
	}

	@Test
	public void testWrapFindDocumentsWithError() {
		
		String outputNamespace="http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1";
		String rootElement="GetDocumentAndAttributes_Response";
		
		String messagingLayer="ESB";
		String businessDomain="Archiving";
		String applicationName="BS";
		String serviceName="Document";
		String serviceVersion="3";
		String operation="FindDocuments";
		String operationVersion="1";
		String paradigm="Request";
		
		String errorCode="errorCode-value";
		String errorReason="errorReason-value";
		String errorDetailCode="errorDetailCode-value";
		String errorDetailText="<reasons>errorDetailText-value</reasons>";
		
		
		String destination=messagingLayer+"."+businessDomain+"."+applicationName+"."+serviceName+"."+serviceVersion+"."+operation+"."+operationVersion+"."+paradigm;
		
		pipe.setOutputNamespace(outputNamespace);
		pipe.setCmhVersion(2);
		addParam("destination",destination);
		addParam("errorCode",errorCode);
		addParam("errorReason",errorReason);
		addParam("errorDetailCode",errorDetailCode);
		addParam("errorDetailText",errorDetailText);
		addParam("operation",operation);
		addParam("operationVersion",operationVersion);
		pipe.configure();
		pipe.start();
		
		String input = "<"+rootElement+"><attrib>1</attrib><attrib>2</attrib></"+rootElement+">";
		
		PipeRunResult prr = pipe.doPipe(new Message(input),new PipeLineSession());
		
		String result=prr.getResult().asString();
		System.out.println("result ["+result+"]");
		TestAssertions.assertXpathValueEquals("test", 			result, "/Envelope/Header/MessageHeader/From/Id");
		TestAssertions.assertXpathValueEquals(destination, 		result, "/Envelope/Header/MessageHeader/To/Location");
		TestAssertions.assertXpathValueEquals(serviceName, 		result, "/Envelope/Header/MessageHeader/Service/Name");
		TestAssertions.assertXpathValueEquals(paradigm, 		result, "/Envelope/Header/MessageHeader/Service/Action/Paradigm");
		TestAssertions.assertXpathValueEquals(operation, 		result, "/Envelope/Header/MessageHeader/Service/Action/Name");
		TestAssertions.assertXpathValueEquals(operationVersion, result, "/Envelope/Header/MessageHeader/Service/Action/Version");

		TestAssertions.assertXpathValueEquals("ERROR", 			result, "/Envelope/Body/"+rootElement+"/Result/Status");
		TestAssertions.assertXpathValueEquals(errorCode, 		result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/Code");
		TestAssertions.assertXpathValueEquals(errorReason, 		result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/Reason");
		TestAssertions.assertXpathValueEquals(errorDetailCode, 	result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/DetailList/Detail/Code");
		TestAssertions.assertXpathValueEquals(XmlEncodingUtils.encodeChars(errorDetailText), 	result, "/Envelope/Body/"+rootElement+"/Result/ErrorList/Error/DetailList/Detail/Text");
	}

	@Test
	public void testWithError() {
		pipe.setOutputNamespace("http://nn.nl/XSD/Archiving/Document/3/GetDocumentAndAttributes/1");
		pipe.setCmhVersion(2);
		addParam("destination","destination-value");
		addParam("errorCode","errorCode-value");
		addParam("errorReason","errorReason-value");
		addParam("errorDetailCode","errorDetailCode-value");
		addParam("errorDetailText","errorDetailText-value");
		addParam("operation","operation-value");
		addParam("operationVersion","operationVersion-value");
		pipe.configure();
		pipe.start();
		
		String input = "<GetDocumentAndAttributes_Response><attrib>1</attrib><attrib>2</attrib></GetDocumentAndAttributes_Response>";
		
		PipeRunResult prr = pipe.doPipe(new Message(input),new PipeLineSession());
		
		String result=prr.getResult().asString();
		System.out.println("result ["+result+"]");
	}


//	@Override
//	@Test
//	@Ignore("Must incorporate CMH in test")
//	public void testBasicUnWrap() throws Exception {
//		// TODO Auto-generated method stub
//		super.testBasicWrap();
//	}
//
//
//	@Override
//	@Test
//	@Ignore("Must incorporate CMH in test")
//	public void testBasicWrapChangeRoot() throws Exception {
//		// TODO Auto-generated method stub
//		super.testBasicWrapChangeRoot();
//	}
//	
}
