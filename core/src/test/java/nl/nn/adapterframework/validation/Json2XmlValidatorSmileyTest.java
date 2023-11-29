package nl.nn.adapterframework.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.align.Json2Xml;
import nl.nn.adapterframework.align.Xml2Json;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.LogUtil;

public class Json2XmlValidatorSmileyTest {
	private final Logger log = LogUtil.getLogger(this);

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
		PipeLineSession pipeLineSession = new PipeLineSession();
		PipeRunResult prr = json2xml.doPipe(new Message(json),pipeLineSession);
		return prr.getResult().asString();
	}

	public String jsonToXml(String json) throws SAXException {
		return Json2Xml.translate(json, TestFileUtils.getTestFileURL("/"+xsd), true, "x", "");
	}

	public String xmlToJsonViaPipe(String xml) throws Exception {
		Json2XmlValidator json2xml = new Json2XmlValidator();
		json2xml.setWarn(false);
		json2xml.setSchema(xsd);
		json2xml.setRoot("x");
		json2xml.setOutputFormat(DocumentFormat.JSON);
		json2xml.setThrowException(true);
		json2xml.registerForward(new PipeForward("success",null));
		json2xml.configure();
		json2xml.start();
		PipeLineSession pipeLineSession = new PipeLineSession();
		PipeRunResult prr = json2xml.doPipe(new Message(xml),pipeLineSession);
		return prr.getResult().asString();
	}

	public String xmlToJson(String xml) throws SAXException, IOException {
		return Xml2Json.translate(xml, TestFileUtils.getTestFileURL("/"+xsd), true, true).toString();
	}

	@Test
	public void testReadJson() throws IOException {
		assertJsonEqual(expectedJson, TestFileUtils.getTestFile(jsonFile));
	}

	@Test
	public void testReadXml() throws IOException {
		MatchUtils.assertXmlEquals(expectedXml, TestFileUtils.getTestFile(xmlFile));
	}

	@Test
	public void testJson2Xml() throws SAXException, IOException  {
		String json = TestFileUtils.getTestFile(jsonFile);
		log.info("testJson2Xml: json ["+json+"]");
		assertJsonEqual(expectedJson,json);
		String xml=jsonToXml(json);
		log.info("testJson2Xml: xml ["+xml+"]");
		MatchUtils.assertXmlEquals("json2xml", expectedXmlNoNewline, xml, false);
	}

	@Test
	public void testJson2XmlViaPipe() throws Exception {
		String json = TestFileUtils.getTestFile(jsonFile);
		log.info("testJson2XmlViaPipe: json ["+json+"]");
		assertJsonEqual(expectedJson,json);
		String xml=jsonToXmlViaPipe(json);
		log.info("testJson2XmlViaPipe: xml ["+xml+"]");
		assertEquals(expectedXmlNoNewline,xml);
	}

	@Test
	public void testXml2Json() throws SAXException, IOException  {
		String xml=TestFileUtils.getTestFile(xmlFile);
		log.info("testXml2Json: xml ["+xml+"]");
		String json=xmlToJson(xml);
		log.info("testXml2Json: json ["+json+"]");
		assertEquals(smileyJson,json);
	}

	@Test
	public void testXml2JsonViaPipe() throws Exception {
		String xml = TestFileUtils.getTestFile(xmlFile);
		log.info("testReadAndJson2Xml: xml ["+xml+"]");
		String json=xmlToJsonViaPipe(xml);
		log.info("testReadAndJson2Xml: json ["+json+"]");
		assertEquals(smileyJson,json);
	}

	public void assertJsonEqual(String jsonExp, String jsonAct) {
		assertEquals(removeNewlines(jsonExp),removeNewlines(jsonAct));
	}

	public String removeNewlines(String contents) {
		return contents.replaceAll("[\n\r]", "");
	}
}
