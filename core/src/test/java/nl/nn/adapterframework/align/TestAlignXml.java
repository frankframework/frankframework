package nl.nn.adapterframework.align;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.w3c.dom.Document;

import nl.nn.adapterframework.util.XmlUtils;

public class TestAlignXml {
	
	public static String BASEDIR="/Align/";

	public void testXml(String xml, URL schemaUrl, String expectedFailureReason, String description) throws Exception {
		Document dom = XmlUtils.buildDomDocument(xml, true);
 
		// check the validity of the input XML
    	assertTrue("valid input XML", Utils.validate(schemaUrl, xml));
 
		try {
			String xmlAct = DomTreeAligner.translate(dom, schemaUrl);
	    	System.out.println("xml out="+xmlAct);
	    	if (expectedFailureReason!=null) {
	    		fail("Expected to fail: "+description);
	    	}
	    	if (xmlAct==null) {
	    		fail("could not convert to xml: "+description);
	    	}
	       	assertTrue("converted XML is not aligned: "+description,  Utils.validate(schemaUrl, xmlAct));
//	       	assertEquals("round tripp",xml,xmlAct);
		} catch (Exception e) {
			if (expectedFailureReason==null) {
				e.printStackTrace();
				fail("Expected conversion to succeed: "+description);
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
	
	public void testStrings(String xmlIn,URL schemaUrl, String targetNamespace, String rootElement, String expectedFailureReason) throws Exception {
		System.out.println("schemaUrl ["+schemaUrl+"]");
		if (xmlIn!=null) assertTrue("validated input",Utils.validate(schemaUrl, xmlIn));

		testXml(xmlIn, schemaUrl, expectedFailureReason,"");
	}

    protected String getTestFile(String file) throws IOException, TimeoutException {
		URL url=AlignTestBase.class.getResource(BASEDIR+file);
		if (url==null) {
			return null;
		}
        BufferedReader buf = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line);
            line = buf.readLine();
            if (line!=null) {
            	string.append("\n");
            }
        }
        return string.toString();
    }

    public void testFiles(String inputFile, String namespace, String schemaFile, String rootElement) throws Exception {
		testFiles(inputFile, namespace, schemaFile, rootElement, false, true, null);
	}
	public void testFiles(String file, String namespace, String schemaFile, String rootElement, boolean potentialCompactionProblems, boolean checkRountTrip, String expectedFailureReason) throws Exception {
		URL schemaUrl=Utils.class.getResource(BASEDIR+schemaFile);
		String xmlString=getTestFile(file+".xml");
		testStrings(xmlString, schemaUrl,namespace, rootElement,expectedFailureReason);
	}
	
	@Test
	public void testOK_abc() throws Exception {
		//testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
		testFiles("Abc/abc","urn:test","Abc/abc.xsd","a");
	}

	@Test
	public void testOK_hcda() throws Exception {
		testFiles("HCDA/HandleCollectionDisbursementAccount","","HCDA/HandleCollectionDisbursementAccount3_v3.0.xsd","HandleCollectionDisbursementAccount");
	}

	
	@Test
	public void testArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays","urn:arrays","Arrays/arrays.xsd","arrays",true,true, null);
	}

	@Test
	public void testEmptyArrays() throws Exception {
		// straight test
		testFiles("Arrays/empty-arrays","urn:arrays","Arrays/arrays.xsd","arrays",true,true, null);
	}

	@Test
	public void testSingleElementArrays() throws Exception {
		// straight test
		testFiles("Arrays/single-element-arrays","urn:arrays","Arrays/arrays.xsd","arrays",true,true, null);
	}

	@Test
	public void testSingleComplexArray() throws Exception {
		// straight test
		testFiles("Arrays/single-complex-array","urn:arrays","Arrays/arrays.xsd","array1",true,true, null);
	}

	@Test
	public void testSingleSimpleArray() throws Exception {
		// straight test
		testFiles("Arrays/single-simple-array","urn:arrays","Arrays/arrays.xsd","singleSimpleRepeatedElement",true,true, null);
	}

	@Test
    public void testAttributes() throws Exception {
    	testFiles("/DataTypes/Attributes","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes");
    }

	@Test
    public void testStrings() throws Exception {
    	testFiles("/DataTypes/Strings","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes",true,true, null);
    }

	@Test
    public void testSpecialChars() throws Exception {
    	testFiles("/DataTypes/SpecialChars","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes",true,true, null);
    }

	@Test
    public void testDiacritics() throws Exception {
    	testFiles("/DataTypes/Diacritics","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes",true,true, null);
    }

	@Test
    public void testBooleans() throws Exception {
    	testFiles("/DataTypes/Booleans","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes",true,true, null);
    }

    @Test
    public void testNumbers() throws Exception {
    	testFiles("/DataTypes/Numbers","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes");
    }

    @Test
    public void testDateTime() throws Exception {
    	testFiles("/DataTypes/DateTime","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes");
    }
    
    @Test
    public void testNull() throws Exception {
    	testFiles("/DataTypes/Null","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes");
    }

//    @Test
//    public void testNullError1() throws Exception {
//    	testFiles("/DataTypes/Null-illegal1","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes", false, true, "nillable");
//    	testFiles("/DataTypes/Null-illegal2","urn:datatypes","/DataTypes/DataTypes.xsd","DataTypes", false, true, "nillable");
//    }
    

 	@Test
	public void testChoiceOfSequence() throws Exception {
		//testStrings("<a><b></b><c></c></a>","{\"a\":{\"b\":\"\",\"c\":\"\"}}");
		testFiles("Transaction/order","","Transaction/transaction.xsd","transaction");
		testFiles("Transaction/invoice","","Transaction/transaction.xsd","transaction");
	}

 	@Test
    public void testRepeatedElements() throws Exception {
//    	testFiles("/RepeatedElements/sprint-withRepeatedElement","","/RepeatedElements/sprint.xsd","sprint");
//    	testFiles("/RepeatedElements/sprint-withoutRepeatedElement","","/RepeatedElements/sprint.xsd","sprint");
    	testFiles("/RepeatedElements/sprint-emptyRepeatedElement","","/RepeatedElements/sprint.xsd","sprint",false,false,null);
    }

    @Test
    public void testSimple() throws Exception {
    	testFiles("/Simple/simple","urn:simple","/Simple/simple.xsd","simple");
    }

}
