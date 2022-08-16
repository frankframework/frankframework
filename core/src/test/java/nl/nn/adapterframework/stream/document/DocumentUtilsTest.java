package nl.nn.adapterframework.stream.document;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;
import org.xml.sax.SAXException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import nl.nn.adapterframework.testutil.MatchUtils;

public class DocumentUtilsTest {

	protected void testBuild(String json, String expected) throws SAXException {
		try(JsonReader jr = Json.createReader(new StringReader(json))) {
			JsonValue jValue=jr.read();
			testBuild(jValue, expected);
		}
	}

	protected void testBuild(JsonValue jValue, String expected) throws SAXException {
		StringWriter writer = new StringWriter();
		try (XmlDocumentBuilder documentBuilder = new XmlDocumentBuilder("root",writer)) {
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
	public void testStringArrayDocument() throws SAXException {
		testBuild("[ \"aap\", \"noot\", \"mies\" ]", "<root><array>aap</array><array>noot</array><array>mies</array></root>");
	}

	@Test
	public void testStringObjectDocument() throws SAXException {
		testBuild("{ \"naam\":\"aap\" }", "<root><naam>aap</naam></root>");
	}

	@Test
	public void testStringDocument() throws SAXException {;
		testBuild(getJsonValue("\"waarde\""), "<root>waarde</root>");
	}

	@Test
	public void testNumberDocument() throws SAXException {;
		testBuild(getJsonValue("100"), "<root>100</root>");
	}

	@Test
	public void testTrueDocument() throws SAXException {;
		testBuild(getJsonValue("true"), "<root>true</root>");
	}

	@Test
	public void testFalseDocument() throws SAXException {;
		testBuild(getJsonValue("false"), "<root>false</root>");
	}

	@Test
	public void testNullDocument() throws SAXException {;
		testBuild(getJsonValue("null"), "<root nil=\"true\"/>");
	}

	@Test
	public void testNestedObjectDocument() throws SAXException {
		testBuild("{ \"items\":{ \"numeric\":1, \"chars\":\"waarde\", \"welles\":true, \"nietes\":false, \"niks\":null, \"rij\":[\"a\",2,true,false,null,{\"a\":1,\"b\":7},[1,2,3]]}}",
				"<root>"+
					"<items>"+
						"<numeric>1</numeric>"+
						"<chars>waarde</chars>"+
						"<welles>true</welles>"+
						"<nietes>false</nietes>"+
						"<niks nil=\"true\"/>"+
						"<rij>a</rij>"+
						"<rij>2</rij>"+
						"<rij>true</rij>"+
						"<rij>false</rij>"+
						"<rij nil=\"true\"/>"+
						"<rij><a>1</a><b>7</b></rij>"+
						"<rij>"+
							"<item>1</item>"+
							"<item>2</item>"+
							"<item>3</item>"+
						"</rij>"+
					"</items>"+
				"</root>");
	}

}
