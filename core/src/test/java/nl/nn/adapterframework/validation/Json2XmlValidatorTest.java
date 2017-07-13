package nl.nn.adapterframework.validation;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
@RunWith(value=JUnit4.class)
public class Json2XmlValidatorTest extends XmlValidatorTestBase {


	Json2XmlValidator instance;
	XercesXmlValidator validator;
	JsonPipe jsonPipe;

	protected void init() throws ConfigurationException  {
		jsonPipe=new JsonPipe();
		jsonPipe.setName("xml2json");
		jsonPipe.registerForward(new PipeForward("success",null));
		jsonPipe.setDirection("xml2json");
		jsonPipe.configure();
		validator=new XercesXmlValidator();
    	validator.setThrowException(true);
    	validator.setFullSchemaChecking(true);

		instance=new Json2XmlValidator();
		instance.registerForward(new PipeForward("success",null));
		instance.setSoapNamespace(null);
	}

	@Override
	public String validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema,
			boolean ignoreUnknownNamespaces, String inputFile, String expectedFailureReason) throws IOException, ConfigurationException, PipeRunException {
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
        instance.configure(null);
        validator.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
        validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
        validator.init();

        String testXml=inputFile!=null?getTestXml(inputFile+".xml"):null;
        System.out.println("testXml ["+inputFile+".xml] contents ["+testXml+"]");
        String xml2json = (String)jsonPipe.doPipe(testXml,session).getResult();
        System.out.println("testXml ["+inputFile+".xml] to json ["+xml2json+"]");
        String testJson=inputFile!=null?getTestXml(inputFile+".json"):null;
        System.out.println("testJson ["+testJson+"]");
         
        try {
        	PipeRunResult prr = instance.doPipe(testJson, session);
        	String result = (String)prr.getResult();
        	System.out.println("result ["+result+"]");
        	evaluateResult("valid XML", session, null, expectedFailureReason);
            try {
    	        String validationResult=validator.validate(result, session, "check result");
    	        evaluateResult(validationResult, session, null, expectedFailureReason);
    	        return result;
            } catch (Exception e) {
            	fail("result XML must be valid");
            }

    		return result;
        } catch (PipeRunException pre) {
        	evaluateResult("Invalid XML", session, pre, expectedFailureReason);
        }
		return null;
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
