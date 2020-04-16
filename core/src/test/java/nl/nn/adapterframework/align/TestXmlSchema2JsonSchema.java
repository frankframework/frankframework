package nl.nn.adapterframework.align;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.util.StreamUtil;

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
		String jsonSchemaFile = schemaFile.replace(".xsd", "-"+rootElement+".jsd");
		URL jsonSchemaUrl=getSchemaURL(jsonSchemaFile);
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

	    	if (skipJsonRootElements && compactArrays) {
		    	String expectedJsonSchema = jsonSchemaUrl==null?null:StreamUtil.streamToString(jsonSchemaUrl.openStream(), "\n", "utf-8");
		    	
		    	
		    	String schemaPretty = jsonPrettyPrint("{"+schema.toString()+"}");
		    	schemaPretty=schemaPretty.substring(1,schemaPretty.length()-1).trim();
		    	
		    	System.out.println("expected ["+expectedJsonSchema+"]");
		    	System.out.println("actual   ["+schemaPretty+"]");
		    	assertEquals("generated does not match ["+jsonSchemaFile+"]", expectedJsonSchema, schemaPretty);
	    	}
	    	
	}
	
	private String jsonPrettyPrint(String json) {
		StringWriter sw = new StringWriter();
		JsonReader jr = Json.createReader(new StringReader(json));
		JsonObject jobj = jr.readObject();

		Map<String, Object> properties = new HashMap<>(1);
		properties.put(JsonGenerator.PRETTY_PRINTING, true);

		JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
		try (JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
			jsonWriter.writeObject(jobj);
		}

		return sw.toString().trim();
	}

}
