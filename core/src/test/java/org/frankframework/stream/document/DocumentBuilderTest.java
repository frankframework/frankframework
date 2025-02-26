package org.frankframework.stream.document;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.documentbuilder.ArrayBuilder;
import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.DocumentUtils;
import org.frankframework.documentbuilder.IDocumentBuilder;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.documentbuilder.JsonDocumentBuilder;
import org.frankframework.documentbuilder.ObjectBuilder;
import org.frankframework.documentbuilder.XmlDocumentBuilder;
import org.frankframework.documentbuilder.json.JsonWriter;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.xml.XmlWriter;

public class DocumentBuilderTest {

	private final String expectedJson = "{\"attr1\":\"alpha quote[\\\"]\",\"attr2\":1.2,\"attr3\":true,\"attr4\":\"a b  c d e f   g\",\"veld1\":\"waarde1 quote[\\\"]\",\"veld2\":10,\"array\":[\"elem1\",\"elem2\"],\"repField\":[\"rep1\",\"rep2\"],\"objField\":{\"o1\":\"w1\",\"o2\":10}}";
	private final String expectedXml = "<root attr1=\"alpha quote[&quot;]\" attr2=\"1.2\" attr3=\"true\" attr4=\"a b  c d e f   g\"><veld1>waarde1 quote[\"]</veld1><veld2>10</veld2><array><element>elem1</element><element>elem2</element></array><repField>rep1</repField><repField>rep2</repField><objField><o1>w1</o1><o2>10</o2></objField></root>";
	private final String expectedXmlPref = "<root pref_attr1=\"pref:alpha quote[&quot;]\" pref_attr2=\"1.2\" pref_attr3=\"true\" pref_attr4=\"a b  c d e f   g\"><pref_veld1>pref:waarde1 quote[\"]</pref_veld1><pref_veld2>10</pref_veld2><pref_array><pref_element>pref:elem1</pref_element><pref_element>pref:elem2</pref_element></pref_array><pref_repField>pref:rep1</pref_repField><pref_repField>pref:rep2</pref_repField><pref_objField><pref_o1>pref:w1</pref_o1><pref_o2>10</pref_o2></pref_objField></root>";

	public void buildDocument(IDocumentBuilder root) throws SAXException {
		try (ObjectBuilder object = root.startObject()) {
			buildObject(object);
		}
	}

	public void buildObject(ObjectBuilder object) throws SAXException {
		buildObject(object, "");
	}

	public String getExpectedXml(String elementPrefix, String valuePrefix) {
		return expectedXmlPref.replace("pref_", elementPrefix).replace("pref:", valuePrefix);
	}

	public void buildObject(ObjectBuilder object, String prefix) throws SAXException {
		object.addAttribute(prefix+"attr1", prefix+"alpha quote[\"]");
		object.addAttribute(prefix+"attr2", 1.2);
		object.addAttribute(prefix+"attr3", true);
		object.addAttribute(prefix+"attr4", "a b  c\td\re\nf\r\n\t\ng");
		object.add(prefix+"veld1", prefix+"waarde1 quote[\"]");
		object.add(prefix+"veld2", 10);
		try (INodeBuilder node=object.addField(prefix+"array")) {
			try (ArrayBuilder array = node.startArray(prefix+"element")) {
				array.addElement(prefix+"elem1");
				array.addElement(prefix+"elem2");
			}
		}
		try (ArrayBuilder repeatedField = object.addRepeatedField(prefix+"repField")) {
			repeatedField.addElement(prefix+"rep1");
			repeatedField.addElement(prefix+"rep2");
		}
		try (ObjectBuilder objectField = object.addObjectField(prefix+"objField")) {
			objectField.add(prefix+"o1", prefix+"w1");
			objectField.add(prefix+"o2", 10);
		}
	}

	@Test
	public void testXmlDocumentBuilder() throws SAXException {
		String expected = expectedXml;
		XmlWriter writer = new XmlWriter();
		try (IDocumentBuilder root = new XmlDocumentBuilder("root", writer, false)) {
			buildDocument(root);
		}
		MatchUtils.assertXmlEquals(expected, writer.toString());
		assertEquals(expected, writer.toString());
	}

	@Test
	public void testJsonDocumentBuilder() throws SAXException {
		String expected = expectedJson;
		StringWriter result = new StringWriter();
		JsonWriter writer = new JsonWriter(result, true);
		try (IDocumentBuilder root = new JsonDocumentBuilder(writer)) {
			buildDocument(root);
		}
		//MatchUtils.assertJsonEqual("", expected, root.getRoot().toString());
		assertEquals(expected, result.toString());
	}

	@Test
	public void testJsonDocumentBuilderDefault() throws SAXException {
		String expected = expectedJson;
		try (IDocumentBuilder root = new JsonDocumentBuilder()) {
			buildDocument(root);
			root.close();
			assertEquals(expected, root.toString());
		}
	}

	@Test
	public void testXmlDocumentBuilderDefault() throws SAXException {
		String expected = expectedXml;
		XmlWriter writer = new XmlWriter();
		try (IDocumentBuilder root = new XmlDocumentBuilder("root", writer, true)) {
			buildDocument(root);
		}
		MatchUtils.assertXmlEquals(expected, writer.toString());
	}

	@Test
	public void testJsonObjectDocumentBuilder() throws SAXException {
		String expected = expectedJson;
		try (ObjectBuilder root = DocumentBuilderFactory.startDocument(DocumentFormat.JSON, "dummy", new StringWriter()).asObjectBuilder()) {
			buildObject(root);
			root.close();
			assertEquals(expected, root.toString());
		}
	}

	@Test
	public void testXmlObjectDocumentBuilder() throws SAXException {
		String expected = expectedXml;
		try (ObjectBuilder root = DocumentBuilderFactory.startDocument(DocumentFormat.XML, "root", new StringWriter()).asObjectBuilder()) {
			buildObject(root);
			root.close();
			MatchUtils.assertXmlEquals(expected, root.toString());
		}
	}


	@Test
	public void testXmlDocumentBuilderWithStrangeCharPrefix() throws SAXException {
		String prefix = "pr#x @y*:";
		String expectedElementPrefix="pr_x__y__";
		String expectedValuePrefix=prefix;
		String expected = getExpectedXml(expectedElementPrefix, expectedValuePrefix);
		XmlWriter writer = new XmlWriter();
		try (IDocumentBuilder root = new XmlDocumentBuilder("root", writer, false)) {
			try (ObjectBuilder object = root.startObject()) {
				buildObject(object, prefix);
			}
		}
		MatchUtils.assertXmlEquals(expected, writer.toString());
		assertEquals(expected, writer.toString());
	}


	@Test
	public void Issue4106ContentAfterDuplicateArray() throws Exception {
		String input="{ \"a\": [ 1 ], \"b\": [ 2 ], \"c\": \"cc\" }";
		String expected = """
								<root>\
								<a>1</a>\
								<b>2</b>\
								<c>cc</c>\
								</root>\
								""";
		try(JsonReader jr = Json.createReader(new StringReader(input))) {
			JsonValue jValue=null;
			jValue = jr.read();
			StringWriter writer = new StringWriter();
			try (XmlDocumentBuilder documentBuilder = new XmlDocumentBuilder("root", writer)) {
				DocumentUtils.jsonValue2Document(jValue, documentBuilder);
			}
			MatchUtils.assertXmlEquals(expected, writer.toString());
		}
	}


	@Test
	public void jsonToJsonObject() throws Exception {
		String input="{\"a\":\"aa\"}";
		try(JsonReader jr = Json.createReader(new StringReader(input))) {
			JsonValue jValue=null;
			jValue = jr.read();
			StringWriter writer = new StringWriter();
			try (JsonDocumentBuilder documentBuilder = new JsonDocumentBuilder(writer)) {
				DocumentUtils.jsonValue2Document(jValue, documentBuilder);
			}
			assertEquals(input, writer.toString());
		}
	}

	@Test
	public void jsonToJsonArray() throws Exception {
		String input="[\"a\",\"b\"]";
		try(JsonReader jr = Json.createReader(new StringReader(input))) {
			JsonValue jValue=null;
			jValue = jr.read();
			StringWriter writer = new StringWriter();
			try (JsonDocumentBuilder documentBuilder = new JsonDocumentBuilder(writer)) {
				DocumentUtils.jsonValue2Document(jValue, documentBuilder);
			}
			assertEquals(input, writer.toString());
		}
	}
}
