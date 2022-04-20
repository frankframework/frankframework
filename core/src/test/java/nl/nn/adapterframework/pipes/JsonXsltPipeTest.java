package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonStructure;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

public class JsonXsltPipeTest extends StreamingPipeTestBase<JsonXsltPipe> {

	@Override
	public JsonXsltPipe createPipe() {
		return new JsonXsltPipe();
	}

	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.configure();
		pipe.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedJson=TestFileUtils.getTestFile("/Xslt3/orgchart.json");
		PipeRunResult prr = doPipe(pipe, input,session);
		String jsonOut=prr.getResult().asString();
		assertJsonEqual(null,expectedJson,jsonOut);
	}

	@Test
	public void xmlOut() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.setJsonResult(false);
		pipe.configure();
		pipe.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedXml=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		PipeRunResult prr = doPipe(pipe, input,session);
		String xmlOut=prr.getResult().asString();
		assertEquals(expectedXml,xmlOut);
	}

	@Test
	public void testXPath() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setXpathExpression("j:map/j:map/j:map[j:string[@key='department']='Security']/j:string[@key='firstname']");
		pipe.setOutputType(OutputType.TEXT);
		pipe.setJsonResult(false);
		pipe.configure();
		pipe.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedText="James";
		PipeRunResult prr = doPipe(pipe, input,session);
		String textOut=prr.getResult().asString();
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


	@Test
	public void parseUsingByteStream() throws Exception {
		pipe.setXpathExpression("/");
		pipe.configure();
		pipe.start();

		URL url = TestFileUtils.getTestFileURL("/Misc/minified.json");
		Message input = new Message(url.openStream());

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();
		assertEquals("onSample Konfabulator Widgetmain_window500500Images/Sun.pngsun1250250centerClick Here36boldtext1250100centersun1.opacity = (sun1.opacity / 100) * 90;", result.trim());
	}
}
