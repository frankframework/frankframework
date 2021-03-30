package nl.nn.adapterframework.stream.document;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.stream.json.JsonWriter;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class DocumentBuilderTest {

	String expectedJson = "{\"veld1\":\"waarde1\",\"veld2\":10,\"array\":[\"elem1\",\"elem2\"],\"repField\":[\"rep1\",\"rep2\"],\"objField\":{\"o1\":\"w1\",\"o2\":10}}";
	String expectedXml = "<root><veld1>waarde1</veld1><veld2>10</veld2><array><element>elem1</element><element>elem2</element></array><repField>rep1</repField><repField>rep2</repField><objField><o1>w1</o1><o2>10</o2></objField></root>";
	
	
	public void buildDocument(INodeBuilder root) throws DocumentException {
		try (ObjectBuilder object = root.startObject()) {
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
	}
	
	@Test
	public void testXmlDocumentBuilder() throws DocumentException {
		
		String expected = expectedXml;
		XmlWriter writer = new XmlWriter();
		try (INodeBuilder root = new XmlNodeBuilder("root", writer)) {
			buildDocument(root);
		}
		MatchUtils.assertXmlEquals(expected, writer.toString());
		assertEquals(expected, writer.toString());
	}

	@Test
	public void testJsonStructureDocumentBuilder() throws DocumentException {
		
		String expected = expectedJson;
		try (JsonStructureNodeBuilder root = new JsonStructureNodeBuilder()) {
			buildDocument(root);
			root.close();
			MatchUtils.assertJsonEqual("", expected, root.getRoot().toString());
			assertEquals(expected, root.getRoot().toString());
		}
		
	}

	@Test
	public void testJsonHandlerDocumentBuilder() throws DocumentException {
		
		String expected = expectedJson;
		JsonWriter writer = new JsonWriter();
		try (JsonHandlerNodeBuilder root = new JsonHandlerNodeBuilder(writer)) {
			buildDocument(root);
		}
		//MatchUtils.assertJsonEqual("", expected, root.getRoot().toString());
		assertEquals(expected, writer.toString());
	}
	

}
