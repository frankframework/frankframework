package nl.nn.adapterframework.align;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Map;

import javax.json.JsonStructure;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.testutil.MatchUtils;

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
		assertEquals("valid XML", expectValid, Utils.validate(schemaUrl, xmlString));

		System.out.println("input xml:"+xmlString);
		JsonStructure json;
		Map<String,String> result;
		try {
			result=Xml2Map.translate(xmlString, schemaUrl);
			for (String key: result.keySet()) {
				String value=result.get(key);
				System.out.println(key+"="+value);
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
				e.printStackTrace();
				fail(e.getMessage());
			}
			return;
		}
	}


	@Override
	@Test
	@Ignore("only json input")
	public void testMixedContentUnknown() throws Exception {
		super.testMixedContentUnknown();
	}

	@Override
	@Test
	@Ignore("No content")
	public void testOptionalArray() throws Exception {
		super.testMixedContentUnknown();
	}

}
