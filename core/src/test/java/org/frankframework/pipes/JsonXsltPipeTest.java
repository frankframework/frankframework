package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.TransformerPool.OutputType;

public class JsonXsltPipeTest extends PipeTestBase<JsonXsltPipe> {

	@Override
	public JsonXsltPipe createPipe() {
		return new JsonXsltPipe();
	}

	@Test
	public void basic() throws ConfigurationException, IOException, PipeRunException {
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
	public void xmlOut() throws ConfigurationException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.setJsonResult(false);
		pipe.configure();
		pipe.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedXml=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		PipeRunResult prr = doPipe(pipe, input,session);
		String xmlOut=prr.getResult().asString();
		assertEquals(expectedXml, xmlOut);
	}

	@Test
	public void testXPath() throws ConfigurationException, IOException, PipeRunException {
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
		assertEquals(expectedText, textOut);
	}


	public static JsonStructure string2Json(String json) {
		return Json.createReader(new StringReader(json)).read();
	}

	public void assertJsonEqual(String description, String jsonExp, String jsonAct) {
		JsonStructure jExp=string2Json(jsonExp);
		log.debug("jsonAct: ["+jsonAct+"]");
		JsonStructure jAct=string2Json(jsonAct);
		assertEquals(jExp.toString(), jAct.toString(), description);
	}


	@Test
	public void parseUsingByteStream() throws Exception {
		pipe.setXpathExpression("/");
		pipe.configure();
		pipe.start();

		URL url = TestFileUtils.getTestFileURL("/Misc/minified.json");
		Message input = new Message(url.openStream());

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertEquals("onSample Konfabulator Widgetmain_window500500Images/Sun.pngsun1250250centerClick Here36boldtext1250100centersun1.opacity = (sun1.opacity / 100) * 90;", result.trim());
	}
}
