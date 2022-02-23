package nl.nn.adapterframework.align;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.testutil.MatchUtils;

public class TestMap2Xml extends AlignTestBase {
	
	public void testStrings(String xmlIn, String mapInStr, URL schemaUrl, String targetNamespace, String rootElement, boolean checkRoundTrip, String expectedFailureReason) throws Exception {
		System.out.println("schemaUrl ["+schemaUrl+"]");
		if (xmlIn!=null) assertTrue("validated input",Utils.validate(schemaUrl, xmlIn));
		if (mapInStr==null || mapInStr.isEmpty()) {
			fail("no input map");
		}
		try {
			Properties inProps=new Properties();
			inProps.load(new StringReader(mapInStr));
			Map<String,String> mapIn=new HashMap<String,String>();
			for (Object key:inProps.keySet()) {
				mapIn.put((String)key, inProps.getProperty((String)key));
			}

			System.out.println("mapIn ["+mapInStr+"]");

			String xmlAct = Properties2Xml.translate(mapIn, schemaUrl, rootElement, targetNamespace);
			System.out.println("xml out="+xmlAct);
			if (expectedFailureReason!=null) {
				fail("Expected to fail");
			}
			if (xmlAct==null) {
				fail("could not convert to xml");
			}
			assertTrue("converted XML is not aligned",  Utils.validate(schemaUrl, xmlAct));
			MatchUtils.assertXmlEquals(null, xmlIn, xmlAct, true);
			if (checkRoundTrip) {
				Map<String,String> roundTrippedmap=Xml2Map.translate(xmlAct, schemaUrl);
				System.out.println("mapIn:\n"+mapInStr);
				System.out.println("roundTrippedmap:\n"+MatchUtils.mapToString(roundTrippedmap));
				assertEquals(mapInStr.trim(),MatchUtils.mapToString(roundTrippedmap).trim());
//				assertMapEquals(mapIn,roundTrippedmap);
			}
		} catch (Exception e) {
			if (expectedFailureReason==null) {
				e.printStackTrace();
				fail("Expected conversion to succeed");
			}
			String msg=e.getMessage();
			if (msg==null) {
				e.printStackTrace();
				fail("msg==null ("+e.getClass().getSimpleName()+")");
			}
			if (!msg.contains(expectedFailureReason)) {
				e.printStackTrace();
				fail("expected reason ["+expectedFailureReason+"] in msg ["+msg+"]");
			}
		}
	}


	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(inputFile+".xml");
		String mapString=getTestFile(inputFile+".properties");
		if (mapString==null) {
			fail("no map input files found for ["+inputFile+"]");
		}
		testStrings(xmlString,mapString, schemaUrl,namespace, rootElement,  true, expectedFailureReason);
	}

	@Test
	public void testNestedValue() throws Exception {
		testFiles("NestedValue/nestedValue.xsd","urn:gbpd","NestedValue","/NestedValue/nestedValue");
	}



	@Override
	@Test
	@Ignore("Map2XML does not support mixed content")
	public void testMixedContent() throws Exception {
		super.testMixedContent();
	}

	@Override
	@Test
	@Ignore("Map2XML does not support null values")
	public void testNull() throws Exception {
		super.testNull();
	}

	@Override
	@Test
	@Ignore("Map2XML does not support attributes")
	public void testAttributes() throws Exception {
		super.testAttributes();
	}

	@Override
	@Test
	@Ignore("Map2XML does not support arrays of complex values")
	public void testArrays() throws Exception {
		super.testArrays();
	}

	@Override
	@Test
	@Ignore("Map2XML does not support arrays of complex values")
	public void testEmptyArrays() throws Exception {
		super.testEmptyArrays();
	}

	@Override
	@Test
	@Ignore("input file is empty")
	public void testOK_abc() throws Exception {
		super.testOK_abc();
	}
	
	@Override
	@Test
	@Ignore("Map2XML does not support arrays of complex values")
	public void test_hcda() throws Exception {
		super.test_hcda();
	}

	@Override
	@Test
	@Ignore("Map2XML does not support arrays of complex values")
	public void testSingleComplexArray() throws Exception {
		super.testSingleComplexArray();
	}

	@Override
	@Test
	@Ignore("Map2XML does not support reporting 'unknown' elements")
	public void testMixedContentUnknown() throws Exception {
		super.testMixedContentUnknown();
	}

	@Test
	@Ignore("Id is ambigous, special test in Json2XmlValidatorTest tests with fully specified Id")
	public void testDoubleId() throws Exception {
		testFiles("DoubleId/Party.xsd","","Party","DoubleId/Party");
	}

	
	@Override
	@Test
	@Ignore("No content")
	public void testOptionalArray() throws Exception {
		super.testMixedContentUnknown();
	}

	@Override
	@Test
	@Ignore("Generates stackoverflow, known issue")
	public void testFamilyTree() throws Exception {
		testFiles("FamilyTree/family.xsd", "urn:family", "family", "FamilyTree/family", true);
	}

	@Test
	@Ignore("Problem with converting property array into xml")
	public void testTextAndAttributes() throws Exception {
		super.testTextAndAttributes();
	}

	@Test
	@Ignore("Does not work, not priority to solve it now")
	public void testAnyAttribute() throws Exception {
		super.testAnyAttribute();
	}

	@Test
	@Ignore("Does not work, not priority to solve it now")
	public void testLabelValue() throws Exception {
		super.testLabelValue();
	}

}
