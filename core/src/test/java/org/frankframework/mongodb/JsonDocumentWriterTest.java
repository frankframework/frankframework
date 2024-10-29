package org.frankframework.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.documentbuilder.IDocumentBuilder;

class JsonDocumentWriterTest {

	@Test
	void testArrayListOfIntegers() throws SAXException {
		List<Integer> classes = new ArrayList<>(List.of(4, 5, 6));
		Document classes1 = new Document().append("classes", classes);
		assertEquals("""
			<FindOneResult>
				<classes>
					<item>4</item>
					<item>5</item>
					<item>6</item>
				</classes>
			</FindOneResult>""", convertDocumentToString(classes1, DocumentFormat.XML));
	}

	@Test
	void testArrayListOfObjects() throws SAXException {
		Document grade1 = new Document().append("grade", 5).append("topic", "math");
		Document grade2 = new Document().append("grade", 5).append("topic", "science");
		List<Document> scores = new ArrayList<>(List.of(grade1, grade2));
		Document root = new Document().append("scores", scores);
		assertEquals("""
				<FindOneResult>
					<scores>
						<item>
							<grade>5</grade>
							<topic>math</topic>
						</item>
						<item>
							<grade>5</grade>
							<topic>science</topic>
						</item>
					</scores>
				</FindOneResult>""", convertDocumentToString(root, DocumentFormat.XML));
		assertEquals("{\"scores\":[{\"grade\":5,\"topic\":\"math\"},{\"grade\":5,\"topic\":\"science\"}]}", convertDocumentToString(root, DocumentFormat.JSON));
	}

	@Test
	void testNumberBooleanNull() throws SAXException {
		Document root = new Document().append("number", 5).append("boolean", true).append("null", null);
		assertEquals("""
				<FindOneResult>
					<number>5</number>
					<boolean>true</boolean>
					<null nil="true"/>
				</FindOneResult>""", convertDocumentToString(root, DocumentFormat.XML));
		assertEquals("{\"number\":5,\"boolean\":true,\"null\":null}", convertDocumentToString(root, DocumentFormat.JSON));
	}

	@Test
	void testDouble() throws SAXException {
		Document root = new Document().append("double", 5.5);
		assertEquals("""
				<FindOneResult>
					<double>5.5</double>
				</FindOneResult>""", convertDocumentToString(root, DocumentFormat.XML));
		assertEquals("{\"double\":5.5}", convertDocumentToString(root, DocumentFormat.JSON));
	}

	private static String convertDocumentToString(Document inputDocument, DocumentFormat documentFormat) throws SAXException {
		try (IDocumentBuilder builder = DocumentBuilderFactory.startDocument(documentFormat, "FindOneResult", new StringWriter())) {
			JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
			Encoder<Document> encoder = new DocumentCodec();
			JsonDocumentWriter jsonWriter = new JsonDocumentWriter(builder, writerSettings);
			encoder.encode(jsonWriter, inputDocument, EncoderContext.builder().build());
			return builder.toString();
		}
	}

}
