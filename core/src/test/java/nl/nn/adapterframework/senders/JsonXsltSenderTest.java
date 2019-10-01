package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonStructure;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class JsonXsltSenderTest extends SenderTestBase<JsonXsltSender> {

	@Mock
	private IPipeLineSession session;

	@Override
	public JsonXsltSender createSender() {
		return new JsonXsltSender();
	}


//	@Test
//	public void testGetInput() throws ConfigurationException, PipeStartException, IOException, PipeRunException, DomBuilderException, TransformerException {
//		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
//		pipe.configure();
//		pipe.start();
//		String input=getFile("/Xslt3/employees.json");
//		log.debug("inputfile ["+input+"]");
//		
//		ParameterResolutionContext prc = pipe.getInput(input, session);
//		prc.getInputSource();
//		TransformerFactory factory = XmlUtils.getTransformerFactory(false);
//		Transformer transformer = factory.newTransformer();
//		
//		String result = XmlUtils.transformXml(transformer, prc.getInputSource());
//		
//		String expected=getFile("/Xslt3/employees.xml");
//		assertEquals(expected,result);
//	}

	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, SenderException {
		sender.setStyleSheetName("/Xslt3/orgchart.xslt");
		sender.configure();
		sender.open();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedJson=TestFileUtils.getTestFile("/Xslt3/orgchart.json");
		Message message = new Message(input);
		Object result = sender.sendMessage("fakecorrelationid", message, new ParameterResolutionContext(message,null), null);
		String jsonOut=result.toString();
		assertJsonEqual(null,expectedJson,jsonOut);
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
