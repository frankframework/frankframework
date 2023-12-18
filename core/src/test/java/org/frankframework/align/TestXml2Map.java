package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.testutil.MatchUtils;

/**
 * @author Gerrit van Brakel
 */
public class TestXml2Map extends AlignTestBase {

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(inputFile+".xml");
		String mapString=getTestFile(inputFile+".properties");


		boolean expectValid=expectedFailureReason==null;
		// check the validity of the input XML
		assertEquals(expectValid, Utils.validate(schemaUrl, xmlString), "valid XML");

		LOG.debug("input xml [{}]", xmlString);
		Map<String,String> result;
		try {
			result= Xml2Map.translate(xmlString, schemaUrl);
			for (String key: result.keySet()) {
				String value=result.get(key);
				LOG.debug("key [{}] => [{}]", key, value);
			}
			if (!expectValid) {
				fail("expected to fail");
			}
			if (mapString==null) {
				fail("no .properties file for ["+inputFile+"]!");
			}
			assertEquals(mapString.trim(), MatchUtils.mapToString(result).trim());
		} catch (Exception e) {
			if (expectValid) {
				LOG.error("expected valid result", e);
				fail(e.getMessage());
			}
			return;
		}
	}


	@Override
	@Test
	@Disabled("only json input")
	public void testMixedContentUnknown() throws Exception {
		super.testMixedContentUnknown();
	}

	@Override
	@Test
	@Disabled("No content")
	public void testOptionalArray() throws Exception {
		super.testMixedContentUnknown();
	}

}
