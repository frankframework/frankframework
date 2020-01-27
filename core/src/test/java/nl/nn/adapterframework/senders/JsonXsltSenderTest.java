package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonStructure;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class JsonXsltSenderTest extends SenderTestBase<JsonXsltSender> {

	@Override
	public JsonXsltSender createSender() {
		return new JsonXsltSender();
	}



	@Test
	public void basic() throws ConfigurationException, IOException, SenderException {
		sender.setStyleSheetName("/Xslt3/orgchart.xslt");
		sender.configure();
		sender.open();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedJson=TestFileUtils.getTestFile("/Xslt3/orgchart.json");
		Message message = new Message(input);
		PipeRunResult result = sender.sendMessage("fakecorrelationid", message, new ParameterResolutionContext(message,null), null);
		String jsonOut=(String)result.getResult();
		assertJsonEqual(null,expectedJson,jsonOut);
	}

	@Test
	public void xmlOut() throws ConfigurationException, IOException, SenderException {
		sender.setStyleSheetName("/Xslt3/orgchart.xslt");
		sender.setJsonResult(false);
		sender.configure();
		sender.open();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		Message message = new Message(input);
<<<<<<< HEAD
		String expectedXml=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		PipeRunResult result = sender.sendMessage("fakecorrelationid", message, new ParameterResolutionContext(message,null), null);
		String xmlOut=result.getResult().toString();
=======
		String expectedXml=TestFileUtils.getTestFile("/Xslt3/orgchart-noindent.xml");
		Object result = sender.sendMessage("fakecorrelationid", message, new ParameterResolutionContext(message,null), null);
		String xmlOut=result.toString();
>>>>>>> refs/heads/master
		assertEquals(expectedXml,xmlOut);
	}

	@Test
	public void testXPath() throws ConfigurationException, IOException, SenderException {
		sender.setXpathExpression("j:map/j:map/j:map[j:string[@key='department']='Security']/j:string[@key='firstname']");
		sender.setOutputType("text");
		sender.setJsonResult(false);
		sender.configure();
		sender.open();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		Message message = new Message(input);
		String expectedText="James";
		PipeRunResult result = sender.sendMessage("fakecorrelationid", message, new ParameterResolutionContext(message,null), null);
		String textOut=result.getResult().toString();
		assertEquals(expectedText,textOut);
	}
	
	public static JsonStructure string2Json(String json) {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return jsonStructure;
	}

	public void assertJsonEqual(String description, String jsonExp, String jsonAct) {
		JsonStructure jExp=string2Json(jsonExp);
		log.debug("jsonAct: ["+jsonAct+"]");
		JsonStructure jAct=string2Json(jsonAct);
		assertEquals(description,jExp.toString(),jAct.toString());
		//assertEquals(description,inputJson,jsonOut);
	}

}
