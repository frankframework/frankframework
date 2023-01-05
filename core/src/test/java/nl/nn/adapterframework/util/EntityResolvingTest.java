package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;

import nl.nn.adapterframework.testutil.TestFileUtils;

public class EntityResolvingTest {

	public String INPUT_FILE_SMALL_ENTITIES                ="/Entities/SmallEntity.xml";
	public String INPUT_FILE_TOO_LARGE_ENTITIES            ="/Entities/TooLargeEntity.xml";
	public String INPUT_FILE_FILE_EXTERNAL_ENTITIES        ="/Entities/FileExternalEntity.xml";
	public String INPUT_FILE_HTTP_EXTERNAL_ENTITIES        ="/Entities/HttpExternalEntity.xml";
	public String INPUT_FILE_SMALL_ENTITIES_RESULT         ="/Entities/SmallEntityResult.xml";
	public String INPUT_FILE_TOO_LARGE_ENTITIES_RESULT     ="/Entities/TooLargeEntityResult.xml";
	public String INPUT_FILE_FILE_EXTERNAL_ENTITIES_RESULT ="/Entities/FileExternalEntityResult.xml";
	public String INPUT_FILE_HTTP_EXTERNAL_ENTITIES_RESULT ="/Entities/HttpExternalEntityResult.xml";
	public String TOO_MANY_ENTITIES_ERROR_MESSAGE_PATTERN ="The parser has encountered more than \"100.000\" entity expansions in this document";

	public String SCHEMA_NAMESPACE="urn:entities";
	public String SCHEMA_LOCATION_ENTITIES         ="/Entities/schema.xsd";


	public String parseAndRenderString(String xsd, String xmlIn) throws Exception {
		Document doc=XmlUtils.buildDomDocument(xmlIn);
		String actual=XmlUtils.nodeToString(doc);
		return actual;
	}

	public void testEntityExpansion(String xsd, String inputFile, boolean expectValid, String expectedResult) throws TransformerException, IOException, DomBuilderException {
		String xmlIn=TestFileUtils.getTestFile(inputFile);
		try {
			String actual=parseAndRenderString(xsd, xmlIn);
			if (!expectValid) {
				fail("expected to fail with message: "+expectedResult);
			}
			String expected=TestFileUtils.getTestFile(expectedResult);
			assertEquals(expected, actual);
		} catch (Exception e) {
			LogUtil.getLogger(this).error("error message: "+e.getMessage());
			if (expectValid) {
				fail("expected to be valid with result: "+expectedResult);
			}
			if (e.getMessage().indexOf(expectedResult)<0) {
				LogUtil.getLogger(this).error("error message does not contain ["+expectedResult+"], but is ["+e.getMessage()+"]");
			}
		}
	}

	@Test
	public void testSmallEntityExpansion() throws DomBuilderException, TransformerException, IOException {
		// a small number of internal entities are allowed
		testEntityExpansion(SCHEMA_LOCATION_ENTITIES, INPUT_FILE_SMALL_ENTITIES, true, INPUT_FILE_SMALL_ENTITIES_RESULT);
	}

	@Test
	public void testTooLargeEntityExpansion() throws DomBuilderException, TransformerException, IOException {
		// if the number of internal entities exceeds 100.000, and error must be raised
		testEntityExpansion(SCHEMA_LOCATION_ENTITIES, INPUT_FILE_TOO_LARGE_ENTITIES, false, TOO_MANY_ENTITIES_ERROR_MESSAGE_PATTERN);
	}

	@Test
	public void testFileExternalEntityExpansion() throws DomBuilderException, TransformerException, IOException {
		// external entities are not allowed by default
		testEntityExpansion(SCHEMA_LOCATION_ENTITIES, INPUT_FILE_FILE_EXTERNAL_ENTITIES, true, INPUT_FILE_FILE_EXTERNAL_ENTITIES_RESULT);
	}

	@Test
	public void testHttpExternalEntityExpansion() throws DomBuilderException, TransformerException, IOException {
		// external entities are not allowed by default
		testEntityExpansion(SCHEMA_LOCATION_ENTITIES, INPUT_FILE_HTTP_EXTERNAL_ENTITIES, true, INPUT_FILE_HTTP_EXTERNAL_ENTITIES_RESULT);
	}
}
