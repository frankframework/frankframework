package nl.nn.adapterframework.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.pipes.JsonPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * @author Gerrit van Brakel
 */
@RunWith(value = Parameterized.class)
public class Json2XmlValidatorTest extends XmlValidatorTestBase {

    private Class<? extends AbstractXmlValidator> implementation;
    private AbstractXmlValidator validator;

	Json2XmlValidator instance;
	JsonPipe jsonPipe;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class}
            ,{JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

    public Json2XmlValidatorTest(Class<? extends AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

	protected void init() throws ConfigurationException  {
		jsonPipe=new JsonPipe();
		jsonPipe.setName("xml2json");
		jsonPipe.registerForward(new PipeForward("success",null));
		jsonPipe.setDirection("xml2json");
		jsonPipe.configure();
		try {
			validator = implementation.newInstance();
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e);
		} catch (InstantiationException e) {
			throw new ConfigurationException(e);
		}
		validator.setThrowException(true);
		validator.setFullSchemaChecking(true);

		instance=new Json2XmlValidator();
		instance.registerForward(new PipeForward("success",null));
		instance.setSoapNamespace(null);
		instance.setFailOnWildcards(false);
	}

	@Override
	public String validate(String rootelement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws Exception {
		init();
        PipeLineSession session = new PipeLineSession();
        //instance.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
        instance.setSchemaLocation(schemaLocation);
        instance.setAddNamespaceToSchema(addNamespaceToSchema);
    	instance.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
//        instance.registerForward("success");
        instance.setThrowException(true);
        instance.setFullSchemaChecking(true);
        instance.setTargetNamespace(rootNamespace);
        instance.registerForward(new PipeForward("failure",null));
        instance.registerForward(new PipeForward("parserError",null));
        if (rootelement!=null) { 
        	instance.setRoot(rootelement);
        }
        instance.configure();
        instance.start();
        validator.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
        validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
        validator.configure("setup");
        validator.start();

        String testXml=inputFile!=null?TestFileUtils.getTestFile(inputFile+".xml"):null;
        System.out.println("testXml ["+inputFile+".xml] contents ["+testXml+"]");
        String xml2json=null; 
        try {
        	xml2json = jsonPipe.doPipe(new Message(testXml),session).getResult().asString();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        System.out.println("testXml ["+inputFile+".xml] to json ["+xml2json+"]");
        String testJson=inputFile!=null?TestFileUtils.getTestFile(inputFile+".json"):null;
        System.out.println("testJson ["+testJson+"]");
         
        try {
        	PipeRunResult prr = instance.doPipe(new Message(testJson), session);
        	String result = prr.getResult().asString();
        	System.out.println("result ["+ToStringBuilder.reflectionToString(prr)+"]");
        	String event;
        	if (prr.isSuccessful()) {
        		event="valid XML";
        	} else {
            	if (prr.getPipeForward().getName().equals("failure")) {
            		event="Invalid XML";
	        	} else {
	        		event=prr.getPipeForward().getName();
	        	}
        	}
        	evaluateResult(event, session, null, expectedFailureReasons);
            try {
    	        Set<List<String>> rootvalidations=null;
    	        if (rootelement!=null) {
    	        	List<String> rootvalidation=new ArrayList<String>();
    	        	rootvalidation.add("Envelope");
    	        	rootvalidation.add("Body");
    	        	rootvalidation.add(rootelement);
    	        	rootvalidations=new HashSet<List<String>>();
    	        	rootvalidations.add(rootvalidation);
    	        }
    	        String validationResult=validator.validate(result, session, "check result", rootvalidations, null);
    	        evaluateResult(validationResult, session, null, expectedFailureReasons);
    	        return result;
            } catch (Exception e) {
            	fail("result XML must be valid: "+ e.getMessage());
            }

    		return result;
        } catch (PipeRunException pre) {
        	evaluateResult("Invalid XML", session, pre, expectedFailureReasons);
        }
		return null;
	}
	
    @Override
	public String getExpectedErrorForPlainText() {
    	return "Message is not XML or JSON";
    }

    @Test
    public void jsonStructs() throws Exception {
        validate(null,SCHEMA_LOCATION_ARRAYS, true ,INPUT_FILE_SCHEMA_LOCATION_ARRAYS_COMPACT_JSON,null);
        validate(null,SCHEMA_LOCATION_ARRAYS, true ,INPUT_FILE_SCHEMA_LOCATION_ARRAYS_FULL_JSON,null);
    }
	
    @Override
    @Ignore // check this later...
	public void unresolvableSchema() throws Exception {
    }
	

    @Override
    @Ignore // no such thing as unknown namespace, align() determines it from the schema
    public void step5ValidationErrorUnknownNamespace() throws Exception {
    }

    @Override
    @Ignore // no such thing as unknown namespace, align() determines it from the schema
    public void validationUnknownNamespaceSwitchedOff() throws Exception {
    }
    @Override
    @Ignore // no such thing as unknown namespace, align() determines it from the schema
    public void validationUnknownNamespaceSwitchedOn() throws Exception {
    }
    
    @Override
    @Ignore // no such thing as unknown namespace, align() determines it from the schema
    public void step5ValidationUnknownNamespaces() throws Exception {
    }
}
