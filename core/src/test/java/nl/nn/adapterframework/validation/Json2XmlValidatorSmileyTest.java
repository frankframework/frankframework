package nl.nn.adapterframework.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.xml.sax.SAXException;

import junit.framework.TestCase;
import nl.nn.adapterframework.align.Json2Xml;
import nl.nn.adapterframework.align.Xml2Json;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FilePipe;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;

public class Json2XmlValidatorSmileyTest extends TestCase {

	public String CHARSET_UTF8="UTF-8";
	
	public String charset=CHARSET_UTF8;
	
	public String jsonFile="/Align/Smileys/smiley-full.json";
	public String xmlFile="/Align/Smileys/smiley.xml";
	public String xsd="Align/Smileys/smiley.xsd";
	
	public String expectedJson="{\n  \"x\": \"😊\"\n}";
	public String expectedXml="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<x>😊</x>\n";
	public String expectedXmlNoNewline="<?xml version=\"1.0\" encoding=\"UTF-8\"?><x>😊</x>";

	public String smileyJson="\"😊\"";
	

	public String jsonToXmlViaPipe(String json) throws Exception {
		Json2XmlValidator json2xml = new Json2XmlValidator();
		json2xml.setWarn(false);
		json2xml.setSchema(xsd);
		json2xml.setRoot("x");
		json2xml.setThrowException(true);
		json2xml.registerForward(new PipeForward("success",null));
		json2xml.configure();
		json2xml.start();
		IPipeLineSession pipeLineSession = new PipeLineSessionBase();
		PipeRunResult prr = json2xml.doPipe(new Message(json),pipeLineSession);
		return prr.getResult().asString();
	}

	public String jsonToXml(String json) throws SAXException, IOException {
		return Json2Xml.translate(json, FilePipe.class.getResource("/"+xsd), true, "x", "");
	}

	public String xmlToJsonViaPipe(String xml) throws Exception {
		Json2XmlValidator json2xml = new Json2XmlValidator();
		json2xml.setWarn(false);
		json2xml.setSchema(xsd);
		json2xml.setRoot("x");
		json2xml.setOutputFormat(json2xml.FORMAT_JSON);
		json2xml.setThrowException(true);
		json2xml.registerForward(new PipeForward("success",null));
		json2xml.configure();
		json2xml.start();
		IPipeLineSession pipeLineSession = new PipeLineSessionBase();
		PipeRunResult prr = json2xml.doPipe(new Message(xml),pipeLineSession);
		return prr.getResult().asString();
	}

	public String xmlToJson(String xml) throws SAXException, IOException {
		return Xml2Json.translate(xml, FilePipe.class.getResource("/"+xsd), true, true).toString();
	}
	
	public void testReadJson() throws IOException {
		assertJsonEqual(expectedJson,getTestFile(jsonFile));
	}

	public void testReadXml() throws IOException {
		assertEquals(expectedXml,getTestFile(xmlFile));
	}
	
	
	
	public void testJson2Xml() throws SAXException, IOException  {
		String json=getTestFile(jsonFile);
		System.out.println("testJson2Xml: json ["+json+"]");
		assertJsonEqual(expectedJson,json);
		String xml=jsonToXml(json);
		System.out.println("testJson2Xml: xml ["+xml+"]");
		MatchUtils.assertXmlEquals("json2xml", expectedXmlNoNewline, xml, false);
	}

	public void testJson2XmlViaPipe() throws Exception {
		String json=getTestFile(jsonFile);
		System.out.println("testJson2XmlViaPipe: json ["+json+"]");
		assertJsonEqual(expectedJson,json);
		String xml=jsonToXmlViaPipe(json);
		System.out.println("testJson2XmlViaPipe: xml ["+xml+"]");
		assertEquals(expectedXmlNoNewline,xml);
	}


	public void testXml2Json() throws SAXException, IOException  {
		String xml=getTestFile(xmlFile);
		System.out.println("testXml2Json: xml ["+xml+"]");
		assertEquals(expectedXml,xml);
		String json=xmlToJson(xml);
		System.out.println("testXml2Json: json ["+json+"]");
		assertEquals(smileyJson,json);
	}

	public void testXml2JsonViaPipe() throws Exception {
		String xml=getTestFile(xmlFile);
		System.out.println("testReadAndJson2Xml: xml ["+xml+"]");
		assertEquals(expectedXml,xml);
		String json=xmlToJsonViaPipe(xml);
		System.out.println("testReadAndJson2Xml: json ["+json+"]");
		assertEquals(smileyJson,json);
	}

	public void assertJsonEqual(String jsonExp, String jsonAct) {
		assertEquals(removeNewlines(jsonExp),removeNewlines(jsonAct));
	}
	
	public String removeNewlines(String contents) {
		return contents.replaceAll("[\n\r]", "");
	}
	
    protected String getTestFile(String testfile) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(FilePipe.class.getResourceAsStream(testfile),charset));
        StringBuilder string = new StringBuilder();
		while (true) {
			int c=buf.read();
			if (c<0) {
				break;
			}
			string.append((char)c);
		}
        return string.toString();
    }

}
