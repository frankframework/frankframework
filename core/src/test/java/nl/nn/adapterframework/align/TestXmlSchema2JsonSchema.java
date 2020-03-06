package nl.nn.adapterframework.align;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Set;

import javax.json.JsonStructure;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.pipes.Json2XmlValidator;

/*
 * @see: https://github.com/networknt/json-schema-validator
 * @see: https://swagger.io/specification
 * @see: https://json-schema.org/understanding-json-schema/reference/
 */
public class TestXmlSchema2JsonSchema extends AlignTestBase{

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		boolean  expectValidRoundTrip=false;
		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, false, false, expectValidRoundTrip, expectedFailureReason);
//		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, true, false, expectValidRoundTrip, expectedFailureReason);
//		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, false, true, expectValidRoundTrip, expectedFailureReason);
		testXml2JsonSchema(schemaFile, namespace, inputFile, rootElement, true, true, expectValidRoundTrip, expectedFailureReason);
	}

	public void testXml2JsonSchema(String schemaFile, String namespace, String xml, String rootElement, boolean compactArrays, boolean skipJsonRootElements, boolean expectValidRoundTrip, String expectedFailureReason) throws Exception {
		
		URL schemaUrl=getSchemaURL(schemaFile);
    	String xmlString=getTestFile(xml+".xml");
    	String jsonString=getTestFile(xml+(skipJsonRootElements?"-compact":"-full")+".json");
 
    	Json2XmlValidator validator = new Json2XmlValidator();
    	validator.registerForward(new PipeForward("success",null));
    	validator.setThrowException(true);
    	if (StringUtils.isNotEmpty(namespace)) {
        	validator.setSchemaLocation(namespace+" "+BASEDIR+schemaFile);
    	} else {
    		validator.setSchema(BASEDIR+schemaFile);
    	}
    	validator.setTargetNamespace(namespace);
    	validator.setJsonWithRootElements(!skipJsonRootElements);
    	validator.setRoot(rootElement);
    	validator.configure();
    	validator.start();
    	
    	boolean expectValid=expectedFailureReason==null;

//    	// check the validity of the input XML
//    	if (expectValid) {
//    		assertEquals("valid XML", expectValid, Utils.validate(schemaUrl, xmlString));
//    	}
//    

    	JsonStructure jsonschema = validator.createJsonSchema(rootElement);
    	if (jsonschema==null) {
    		fail("no schema generated for ["+rootElement+"]");
    	}
    		String jsonSchemaContent=jsonschema.toString();
	    	System.out.println("result compactArrays ["+compactArrays+"] skipJsonRootElements ["+skipJsonRootElements+"] json:\n" +jsonSchemaContent);
	    	if (StringUtils.isEmpty(jsonSchemaContent)) {
	    		fail("json schema is empty");
	    	}
//	    	if (!expectValid) {
//				fail("expected to fail with reason ["+ expectedFailureReason +"]");
//			}
	    	JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
	    	JsonSchema schema = factory.getSchema(jsonSchemaContent);
	    	
	    	if (StringUtils.isNotEmpty(jsonString)) {
		        ObjectMapper mapper = new ObjectMapper();
		        JsonNode node = mapper.readTree(jsonString);
		    	
		        Set<ValidationMessage> errors = schema.validate(node);

		        System.out.println(jsonString);
		        System.out.println(errors);
		        assertEquals(errors.toString(),0,errors.size());
	    	}
	    	
//		} catch (Exception e) {
//			if (expectValid) {
//				e.printStackTrace();
//		    	System.out.println("exception compactArrays ["+compactArrays+"] skipJsonRootElements ["+skipJsonRootElements+"]");
//				fail(e.getMessage());
//			}
//			return;
//		}
//    	if (expectValidRoundTrip) {
//			String backToXml1=Json2Xml.translate(json, schemaUrl, compactArrays, skipJsonRootElements?rootElement:null, null);
//	    	//System.out.println("back to xml compactArrays ["+compactArrays+"] xml:\n" +backToXml1);
//			String backToXml2=Json2Xml.translate(json, schemaUrl, !compactArrays, skipJsonRootElements?rootElement:null, null);
//	    	//System.out.println("back to xml compactArrays ["+!compactArrays+"] xml:\n" +backToXml2);
//	
//	    	String jsonCompactExpected=getTestFile(xml+"-compact.json");
//	    	String jsonFullExpected=getTestFile(xml+"-full.json");
//			
//			if (jsonCompactExpected!=null && compactArrays && skipJsonRootElements) {
//		    	assertEquals(jsonCompactExpected,jsonOut);
//			}
//			if (jsonFullExpected!=null && !compactArrays && !skipJsonRootElements) {
//		    	assertEquals(jsonFullExpected,jsonOut);
//			}
//    	}		
	}

}
