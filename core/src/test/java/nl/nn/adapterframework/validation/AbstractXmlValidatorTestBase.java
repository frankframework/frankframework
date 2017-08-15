package nl.nn.adapterframework.validation;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * @author Gerrit van Brakel
 */
public abstract class AbstractXmlValidatorTestBase extends XmlValidatorTestBase {
	
    private Class<? extends AbstractXmlValidator> implementation;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class}
            ,{XercesJavaxXmlValidator.class}
            //,{JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

    public AbstractXmlValidatorTestBase(Class<? extends AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

   
	@Override
	public String validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputfile, String expectedFailureReason) throws ConfigurationException, InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, IOException {
        AbstractXmlValidator instance = implementation.newInstance();
        instance.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
    	instance.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
//        instance.registerForward("success");
        instance.setThrowException(true);
        instance.setFullSchemaChecking(true);

        String testXml=inputfile!=null?getTestXml(inputfile+".xml"):null;
        PipeLineSessionBase session = new PipeLineSessionBase();

        try {
	        instance.configure("init");
	        String result=instance.validate(testXml, session, "test");
	        evaluateResult(result, session, null, expectedFailureReason);
	        return result;
        } catch (Exception e) {
	        evaluateResult(null, session, e, expectedFailureReason);
	    	return "Invalid XML";
        }
	}


}
