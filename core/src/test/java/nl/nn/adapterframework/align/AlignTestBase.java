package nl.nn.adapterframework.align;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonStructure;

import org.junit.Test;

public abstract class AlignTestBase {

	public static String BASEDIR="/Align/";
	
	public abstract void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception;

	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, String expectedFailureReason) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, false, expectedFailureReason);
	}
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, potentialCompactionProblems, null);
	}
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, false, null);
	}

	public URL getSchemaURL(String schemaFile) {
		 return AlignTestBase.class.getResource(BASEDIR+schemaFile);
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

    public Map<String,Object> stringToMap(String mapInStr) throws IOException {
		Properties inProps=new Properties();
		inProps.load(new StringReader(mapInStr));
		Map<String,Object> mapIn=new HashMap<String,Object>();
		for (Object key:inProps.keySet()) {
			mapIn.put((String)key, inProps.getProperty((String)key));
		}
		return mapIn;
    }
    
	public String mapToString(Map<String,String> map) {
		StringBuffer buf=new StringBuffer();
		for (String key:map.keySet()) {
			buf.append(key).append('=');
			if (map.containsKey(key)) {
				buf.append(map.get(key));
			}
			buf.append("\n");
		}
		return buf.toString();
	}
	
	public void assertMapEquals(Map<String,String> exp, Map<String,String> act) {
		SortedMap<String,String> exps=new TreeMap<String,String>(exp);
		String expStr=mapToString(exps);
		SortedMap<String,String> acts=new TreeMap<String,String>(act);
		String actStr=mapToString(acts);
		assertEquals(expStr,actStr);
	}
 
	public static JsonStructure string2Json(String json) {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return jsonStructure;
	}

	public void assertJsonEqual(String description, String jsonExp, String jsonAct) {
		JsonStructure jExp=string2Json(jsonExp);
		JsonStructure jAct=string2Json(jsonAct);
		assertEquals(description,jExp.toString(),jAct.toString());
		//assertEquals(description,inputJson,jsonOut);
	}

	
	@Test
	public void testOK_abc() throws Exception {
		testFiles("Abc/abc.xsd","urn:test","a","Abc/abc");
	}


	
	@Test
	public void testArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","arrays","Arrays/arrays",true);
	}

	@Test
	public void testEmptyArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","arrays","Arrays/empty-arrays",true);
	}

	@Test
	public void testSingleComplexArray() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","array1","Arrays/single-complex-array",true);
	}

	@Test
	public void testSingleElementArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","arrays","Arrays/single-element-arrays",true);
	}

	@Test
	public void testSingleSimpleArray() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","singleSimpleRepeatedElement","Arrays/single-simple-array",true);
	}


	
	@Test
    public void testAttributes() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Attributes");
    }

	@Test
    public void testBooleans() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Booleans",true);
    }

    @Test
    public void testDateTime() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/DateTime");
    }
    
	@Test
    public void testDiacritics() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Diacritics",true);
    }

    @Test
    public void testNull() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Null");
    }

    @Test
    public void testNumbers() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Numbers");
    }

    @Test
    public void testSpecialChars() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/SpecialChars",true);
    }

	@Test
    public void testStrings() throws Exception {
    	testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Strings",true);
    }

	

	@Test
	public void test_hcda() throws Exception {
		testFiles("HCDA/HandleCollectionDisbursementAccount3_v3.0.xsd","","HandleCollectionDisbursementAccount","HCDA/HandleCollectionDisbursementAccount");
	}

	

    @Test
    public void testMixedContent() throws Exception {
    	testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-simple");
    	testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-complex");
    	testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-empty");
    }

    @Test
    public void testMixedContentUnknown() throws Exception {
    	testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-unknown","Cannot find the declaration of element");
    }


 	@Test
    public void testRepeatedElements() throws Exception {
    	testFiles("RepeatedElements/sprint.xsd","","sprint","/RepeatedElements/sprint-withRepeatedElement");
    	testFiles("RepeatedElements/sprint.xsd","","sprint","/RepeatedElements/sprint-withoutRepeatedElement");
    }

    @Test
    public void testSimple() throws Exception {
    	testFiles("Simple/simple.xsd","urn:simple","simple","/Simple/simple");
    }


    @Test
	public void testChoiceOfSequence() throws Exception {
		testFiles("Transaction/transaction.xsd","","transaction","Transaction/order");
		testFiles("Transaction/transaction.xsd","","transaction","Transaction/invoice");
	}


}
