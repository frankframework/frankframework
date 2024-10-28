package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.junit.jupiter.api.Test;

import org.frankframework.core.SenderResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.TransformerPool.OutputType;

public class JsonXsltSenderTest extends SenderTestBase<JsonXsltSender> {

	@Override
	public JsonXsltSender createSender() {
		return new JsonXsltSender();
	}

	@Test
	public void basic() throws Exception {
		sender.setStyleSheetName("/Xslt3/orgchart.xslt");
		sender.configure();
		sender.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedJson=TestFileUtils.getTestFile("/Xslt3/orgchart.json");
		Message message = new Message(input);
		SenderResult senderResult = sender.sendMessage(message, session);
		String jsonOut=senderResult.getResult().asString();
		assertJsonEqual(expectedJson,jsonOut, null);
	}

	@Test
	public void xmlOut() throws Exception {
		sender.setStyleSheetName("/Xslt3/orgchart.xslt");
		sender.setJsonResult(false);
		sender.configure();
		sender.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		Message message = new Message(input);
		String expectedXml=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		SenderResult senderResult = sender.sendMessage(message, session);
		String xmlOut=senderResult.getResult().asString();
		assertEquals(expectedXml,xmlOut);
	}

	@Test
	public void testXPath() throws Exception {
		sender.setXpathExpression("j:map/j:map/j:map[j:string[@key='department']='Security']/j:string[@key='firstname']");
		sender.setOutputType(OutputType.TEXT);
		sender.setJsonResult(false);
		sender.configure();
		sender.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		Message message = new Message(input);
		String expectedText="James";
		SenderResult senderResult = sender.sendMessage(message, session);
		String textOut=senderResult.getResult().asString();
		assertEquals(expectedText,textOut);
	}

	public static JsonStructure string2Json(String json) {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return jsonStructure;
	}

	public void assertJsonEqual(String jsonExp, String jsonAct, String description) {
		JsonStructure jExp=string2Json(jsonExp);
		log.debug("jsonAct: ["+jsonAct+"]");
		JsonStructure jAct=string2Json(jsonAct);
		assertEquals(jExp.toString(), jAct.toString(), description);
		//assertEquals(description,inputJson,jsonOut);
	}

}
