package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.pipes.JsonPipe;

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
	}

	@Override
	public String validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema,
			boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws IOException, ConfigurationException, PipeRunException {
		init();
        PipeLineSessionBase session = new PipeLineSessionBase();
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
        instance.configure(null);
        validator.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
        validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
        validator.configure("setup");

        String testXml=inputFile!=null?getTestXml(inputFile+".xml"):null;
        System.out.println("testXml ["+inputFile+".xml] contents ["+testXml+"]");
        String xml2json = (String)jsonPipe.doPipe(testXml,session).getResult();
        System.out.println("testXml ["+inputFile+".xml] to json ["+xml2json+"]");
        String testJson=inputFile!=null?getTestXml(inputFile+".json"):null;
        System.out.println("testJson ["+testJson+"]");
         
        try {
        	PipeRunResult prr = instance.doPipe(testJson, session);
        	String result = (String)prr.getResult();
        	System.out.println("result ["+ToStringBuilder.reflectionToString(prr)+"]");
        	String event;
        	if (prr.getPipeForward().getName().equals("success")) {
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
    	        String validationResult=validator.validate(result, session, "check result", null, null, false);
    	        evaluateResult(validationResult, session, null, expectedFailureReasons);
    	        return result;
            } catch (Exception e) {
            	fail("result XML must be valid");
            }

    		return result;
        } catch (PipeRunException pre) {
        	evaluateResult("Invalid XML", session, pre, expectedFailureReasons);
        }
		return null;
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
