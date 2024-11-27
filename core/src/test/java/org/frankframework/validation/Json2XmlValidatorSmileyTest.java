package org.frankframework.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.align.Json2Xml;
import org.frankframework.align.Xml2Json;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.LogUtil;

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
		json2xml.addForward(new PipeForward("success",null));
		json2xml.configure();
		json2xml.start();
		PipeLineSession pipeLineSession = new PipeLineSession();
		PipeRunResult prr = json2xml.doPipe(new Message(json),pipeLineSession);
		return prr.getResult().asString();
	}

	public String jsonToXml(String json) throws SAXException {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return Json2Xml.translate(jsonStructure, TestFileUtils.getTestFileURL("/"+xsd), true, "x", "");
	}

	public String xmlToJsonViaPipe(String xml) throws Exception {
		Json2XmlValidator json2xml = new Json2XmlValidator();
		json2xml.setWarn(false);
		json2xml.setSchema(xsd);
		json2xml.setRoot("x");
		json2xml.setOutputFormat(DocumentFormat.JSON);
		json2xml.setThrowException(true);
		json2xml.addForward(new PipeForward("success",null));
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
