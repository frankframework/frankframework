package nl.nn.adapterframework.stream.document;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.json.JsonWriter;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class DocumentBuilderTest {

	private String expectedJson = "{\"veld1\":\"waarde1\",\"veld2\":10,\"array\":[\"elem1\",\"elem2\"],\"repField\":[\"rep1\",\"rep2\"],\"objField\":{\"o1\":\"w1\",\"o2\":10}}";
	private String expectedXml = "<root><veld1>waarde1</veld1><veld2>10</veld2><array><element>elem1</element><element>elem2</element></array><repField>rep1</repField><repField>rep2</repField><objField><o1>w1</o1><o2>10</o2></objField></root>";
	
	
	public void buildDocument(IDocumentBuilder root) throws SAXException {
		try (ObjectBuilder object = root.startObject()) {
			buildObject(object);
		}
	}

	public void buildObject(ObjectBuilder object) throws SAXException {
		object.add("veld1", "waarde1");
		object.add("veld2", 10);
		try (ArrayBuilder array = object.addField("array").startArray("element")) {
			array.addElement("elem1");
			array.addElement("elem2");
		}
		try (ArrayBuilder repeatedField = object.addRepeatedField("repField")) {
			repeatedField.addElement("rep1");
			repeatedField.addElement("rep2");
		}
		try (ObjectBuilder objectField = object.addObjectField("objField")) {
			objectField.add("o1", "w1");
			objectField.add("o2", 10);
		}
	}
	
	@Test
	public void testXmlDocumentBuilder() throws SAXException {
		
		String expected = expectedXml;
		XmlWriter writer = new XmlWriter();
		try (IDocumentBuilder root = new XmlDocumentBuilder("root", writer)) {
			buildDocument(root);
		}
		MatchUtils.assertXmlEquals(expected, writer.toString());
		assertEquals(expected, writer.toString());
	}

	@Test
	public void testJsonDocumentBuilder() throws SAXException {
		
		String expected = expectedJson;
		JsonWriter writer = new JsonWriter();
		try (IDocumentBuilder root = new JsonDocumentBuilder(writer)) {
			buildDocument(root);
		}
		//MatchUtils.assertJsonEqual("", expected, root.getRoot().toString());
		assertEquals(expected, writer.toString());
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
		try (IDocumentBuilder root = new XmlDocumentBuilder("root")) {
			buildDocument(root);
			root.close();
			assertEquals(expected, root.toString());
		}
	}

	@Test
	public void testJsonObjectDocumentBuilder() throws SAXException, StreamingException {
		
		String expected = expectedJson;
		try (ObjectBuilder root = DocumentBuilderFactory.startObjectDocument(DocumentFormat.JSON, "dummy")) {
			buildObject(root);
			root.close();
			assertEquals(expected, root.toString());
		}
	}
	
	@Test
	public void testXmlObjectDocumentBuilder() throws SAXException, StreamingException {
		
		String expected = expectedXml;
		try (ObjectBuilder root = DocumentBuilderFactory.startObjectDocument(DocumentFormat.XML, "root")) {
			buildObject(root);
			root.close();
			assertEquals(expected, root.toString());
		}
	}

}
