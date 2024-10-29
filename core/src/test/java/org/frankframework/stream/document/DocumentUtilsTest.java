package org.frankframework.stream.document;

import java.io.StringReader;
import java.io.StringWriter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.documentbuilder.DocumentUtils;
import org.frankframework.documentbuilder.Json2XmlHandler;
import org.frankframework.documentbuilder.XmlDocumentBuilder;
import org.frankframework.documentbuilder.json.JsonUtils;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.xml.XmlWriter;

public class DocumentUtilsTest {

	protected void testBuild(String json, String expected) throws Exception {
		try(JsonReader jr = Json.createReader(new StringReader(json))) {
			JsonValue jValue=jr.read();
			testBuild(jValue, expected);
		}

		XmlWriter writer = new XmlWriter();
		Json2XmlHandler j2xHandler = new Json2XmlHandler(writer, false);
		JsonUtils.parseJson(json, j2xHandler);
		MatchUtils.assertXmlEquals(expected, writer.toString());
	}

	protected void testBuild(JsonValue jValue, String expected) throws SAXException {
		StringWriter writer = new StringWriter();
		try (XmlDocumentBuilder documentBuilder = new XmlDocumentBuilder("root", writer)) {
			DocumentUtils.jsonValue2Document(jValue, documentBuilder);
		}
		MatchUtils.assertXmlEquals(expected, writer.toString());
	}

	protected JsonValue getJsonValue(String value) {
		String object = "{ \"v\":"+value+"}";
		try(JsonReader jr = Json.createReader(new StringReader(object))) {
			JsonObject jObject = (JsonObject)jr.read();
			return jObject.get("v");
		}
	}

	@Test
	public void testStringArrayDocument() throws Exception {
		testBuild("[ \"aap\", \"noot\", \"mies\" ]", "<root><item>aap</item><item>noot</item><item>mies</item></root>");
	}

	@Test
	public void testStringObjectDocument() throws Exception {
		testBuild("{ \"naam\":\"aap\" }", "<root><naam>aap</naam></root>");
	}

	@Test
	public void testStringDocument() throws SAXException {
		testBuild(getJsonValue("\"waarde\""), "<root>waarde</root>");
	}

	@Test
	public void testNumberDocument() throws SAXException {
		testBuild(getJsonValue("100"), "<root>100</root>");
	}

	@Test
	public void testDecimalDocument() throws SAXException {
		testBuild(getJsonValue("100.003"), "<root>100.003</root>");
	}

	@Test
	public void testTrueDocument() throws SAXException {
		testBuild(getJsonValue("true"), "<root>true</root>");
	}

	@Test
	public void testFalseDocument() throws SAXException {
		testBuild(getJsonValue("false"), "<root>false</root>");
	}

	@Test
	public void testNullDocument() throws SAXException {
		testBuild(getJsonValue("null"), "<root nil=\"true\"/>");
	}

	@Test
	public void testNestedObjectDocument() throws Exception {
		testBuild("{ \"items\":{ \"numeric\":1, \"chars\":\"waarde\", \"welles\":true, \"nietes\":false, \"rij\":[\"a\",2,true,false,null,{\"a\":1,\"b\":7.2},[1,2,3.9]]}}",
				"""
				<root>\
				<items>\
				<numeric>1</numeric>\
				<chars>waarde</chars>\
				<welles>true</welles>\
				<nietes>false</nietes>\
				<rij>a</rij>\
				<rij>2</rij>\
				<rij>true</rij>\
				<rij>false</rij>\
				<rij nil="true"/>\
				<rij><a>1</a><b>7.2</b></rij>\
				<rij>\
				<item>1</item>\
				<item>2</item>\
				<item>3.9</item>\
				</rij>\
				</items>\
				</root>\
				""");
	}

	@Test
	public void testSimple() throws Exception {
			testBuild("{\"a\":1,\"b\":{\"c\":2}}","<root><a>1</a><b><c>2</c></b></root>");
	}


}
