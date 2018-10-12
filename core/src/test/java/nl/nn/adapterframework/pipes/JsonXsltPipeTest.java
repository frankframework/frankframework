package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonStructure;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public class JsonXsltPipeTest extends PipeTestBase<JsonXsltPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public JsonXsltPipe createPipe() {
		return new JsonXsltPipe();
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
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.configure();
		pipe.start();
		String input=getFile("/Xslt3/employees.json");
		log.debug("inputfile ["+input+"]");
		String expectedJson=getFile("/Xslt3/orgchart.json");
		PipeRunResult prr = pipe.doPipe(input,session);
		String jsonOut=(String)prr.getResult();
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
