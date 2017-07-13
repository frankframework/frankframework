package nl.nn.adapterframework.pipes;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;
import nl.nn.adapterframework.validation.XmlValidatorTestBase;

/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class XmlValidatorTest2 extends XmlValidatorTestBase {

    private Class<AbstractXmlValidator> implementation;

    public XmlValidatorTest2(Class<AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class}/*,
            {JavaxXmlValidator.class} Not fully implemented yet/anymore */
        };
        return Arrays.asList(data);
    }

 
    static PipeForward getSuccess() {
        PipeForward forward = new PipeForward();
        forward.setName("success");
        return forward;
    }

    XmlValidator getValidator(String schemaLocation) throws ConfigurationException {
        return getValidator(schemaLocation, false);
    }
    XmlValidator getValidator(String schemaLocation, boolean addNamespaceToSchema) throws ConfigurationException {
        return getValidator(schemaLocation, addNamespaceToSchema, implementation);
    }

    public static XmlValidator getValidator(String schemaLocation, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
        return getValidator(schemaLocation, false, implementation);
    }
    public static XmlValidator getValidator(String schemaLocation, boolean addNamespaceToSchema, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
        XmlValidator validator = new XmlValidator();
        try {
            validator.setImplementation(implementation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        validator.setSchemaLocation(schemaLocation);
        if (addNamespaceToSchema) {
            validator.setAddNamespaceToSchema(addNamespaceToSchema);
        }
        validator.registerForward(getSuccess());
        validator.setThrowException(true);
        validator.setFullSchemaChecking(true);
        validator.configure();
        return validator;
    }
    
    @Override
	public String validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema,
			boolean ignoreUnknownNamespaces, String inputfile, String expectedFailureReason) throws ConfigurationException, InstantiationException,
			IllegalAccessException, XmlValidatorException, PipeRunException, IOException {
        String testXml=inputfile!=null?getTestXml(inputfile+".xml"):null;
   		IPipeLineSession session=new PipeLineSessionBase();
        try {
        	XmlValidator validator =getValidator(schemaLocation, addNamespaceToSchema, implementation);
       		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
       		PipeForward forward=validator.validate(testXml, session);
	        evaluateResult(forward.getName(), session, null, expectedFailureReason);
        } catch (Exception e) {
	        evaluateResult(null, session, e, expectedFailureReason);
	    	return "Invalid XML";
        }
        return null;
    }


}
