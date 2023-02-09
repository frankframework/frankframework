package nl.nn.adapterframework.pipes;



import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.JavaxXmlValidator;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;
import nl.nn.adapterframework.validation.XmlValidatorTestBase;

/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class XmlValidatorTest extends XmlValidatorTestBase {

    private Class<AbstractXmlValidator> implementation;

    public XmlValidatorTest(Class<AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class}
            ,{JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

 
    protected static PipeForward createSuccessForward() {
        PipeForward forward = new PipeForward();
        forward.setName("success");
        return forward;
    }

    protected static PipeForward createFailureForward() {
        PipeForward forward = new PipeForward();
        forward.setName("failure");
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
    	XmlValidator validator=getUnconfiguredValidator(schemaLocation, addNamespaceToSchema, implementation);
    	validator.configure();
    	return validator;
    }
    
   public static XmlValidator getUnconfiguredValidator(String schemaLocation, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
        return getUnconfiguredValidator(schemaLocation, false, implementation);
    }

   public static XmlValidator getUnconfiguredValidator(String schemaLocation, boolean addNamespaceToSchema, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
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
        validator.registerForward(createSuccessForward());
        validator.setThrowException(true);
        validator.setFullSchemaChecking(true);
        return validator;
    }
    

    
   protected String runAndEvaluate(XmlValidator validator, String inputfile, String[] expectedFailureReasons) throws IOException  {
	   System.out.println("inputfile ["+inputfile+"]");
       String testXml=inputfile!=null?getTestXml(inputfile+".xml"):null;
  		PipeLineSession session=new PipeLineSession();
       try {
      		PipeRunResult result=validator.doPipe(new Message(testXml), session);
      		PipeForward forward=result.getPipeForward();
	        evaluateResult(forward.getName(), session, null, expectedFailureReasons);
       } catch (Exception e) {
	        evaluateResult(null, session, e, expectedFailureReasons);
	    	return "Invalid XML";
       }
       return null;
   }
   
    
    @Override
	public String validate(String rootelement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema,
			boolean ignoreUnknownNamespaces, String inputfile, String[] expectedFailureReasons) throws Exception,
			IllegalAccessException, XmlValidatorException, PipeRunException, IOException {
    	XmlValidator validator =getValidator(schemaLocation, addNamespaceToSchema, implementation);
    	if (rootelement!=null) validator.setRoot(rootelement);
   		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
   		validator.configure();
   		validator.start();
    	return runAndEvaluate(validator, inputfile, expectedFailureReasons);
    }


	/*
	 * <tr> <td>{@link #setSoapNamespace(String) soapNamespace}</td> <td>the
	 * namespace of the SOAP Envelope, when this property has a value and the
	 * input message is a SOAP Message the content of the SOAP Body is used for
	 * validation, hence the SOAP Envelope and SOAP Body elements are not
	 * considered part of the message to validate. Please note that this
	 * functionality is deprecated, using {@link
	 * nl.nn.adapterframework.soap.SoapValidator} is now the preferred solution
	 * in case a SOAP Message needs to be validated, in other cases give this
	 * property an empty value</td>
	 * <td>http://schemas.xmlsoap.org/soap/envelope/</td></tr>
	 * 
	 */
	public void testSoapNamespaceFeature(String schema, String root, String inputFile) throws ConfigurationException, IOException, PipeRunException, XmlValidatorException, PipeStartException {
        XmlValidator validator = new XmlValidator();
        try {
            validator.setImplementation(implementation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        validator.registerForward(createSuccessForward());
        validator.setThrowException(true);
        validator.setFullSchemaChecking(true);
		validator.setRoot(root);
		validator.setSoapNamespace("http://www.w3.org/2003/05/soap-envelope");
		validator.setSchema(schema);
		validator.configure();
		validator.start();

		assertNull(runAndEvaluate(validator, inputFile, null));
	}

	@Test
	public void testSoapNamespaceFeature() throws ConfigurationException, IOException, PipeRunException, XmlValidatorException, PipeStartException {
		testSoapNamespaceFeature(NO_NAMESPACE_SCHEMA,NO_NAMESPACE_SOAP_MSGROOT,NO_NAMESPACE_SOAP_FILE);
	}

//	@Test
//	public void straighforwardInEnvelope() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
//		validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_OK_IN_ENVELOPE,false,null);
//	 	validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_ERR_IN_ENVELOPE,false,MSG_INVALID_CONTENT);
//	 	validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_OK_IN_ENVELOPE,false,MSG_CANNOT_FIND_DECLARATION);
//		validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_ERR_IN_ENVELOPE,false,MSG_CANNOT_FIND_DECLARATION);
//	}

	
	public void testStoreRootElement(String schema, String root, String inputFile) throws Exception {
		XmlValidator validator = new XmlValidator();

		validator.registerForward(createSuccessForward());
		validator.setThrowException(true);
		validator.setFullSchemaChecking(true);
		validator.setRoot(root);
		validator.setRootElementSessionKey("rootElement");
		validator.setSchemaLocation(schema);
		validator.configure();
		validator.start();

		String testXml = inputFile != null ? getTestXml(inputFile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.doPipe(new Message(testXml), session);
		PipeForward forward = result.getPipeForward();

		assertEquals(root, (String)session.get("rootElement"));
		assertEquals("success", forward.getName());
	}

	@Test
	public void testStoreRootElement() throws Exception {
		testStoreRootElement(SCHEMA_LOCATION_BASIC_A_OK, "A", INPUT_FILE_BASIC_A_OK);
	}
	
	@Test
	public void testWrongRootElement() throws Exception {
		String schema = SCHEMA_LOCATION_BASIC_A_OK;
		String inputFile = INPUT_FILE_BASIC_A_OK;
		XmlValidator validator = new XmlValidator();

		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
		
		validator.setFullSchemaChecking(true);
		validator.setRoot("anotherElement");
		validator.setReasonSessionKey("reason");
		validator.setSchemaLocation(schema);
		validator.configure();
		validator.start();

		String testXml = inputFile != null ? getTestXml(inputFile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.doPipe(new Message(testXml), session);
		PipeForward forward = result.getPipeForward();

		assertEquals("failure", forward.getName());
		assertThat((String)session.get("reason"), containsString("Illegal element 'A'. Element(s) 'anotherElement' expected."));
	}


	@Test
	public void testMultipleRootElement() throws Exception {
		String schema = SCHEMA_LOCATION_BASIC_A_OK;
		String root = "A"; 
		String inputFile = INPUT_FILE_BASIC_A_OK;
		XmlValidator validator = new XmlValidator();

		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
		
		validator.setFullSchemaChecking(true);
		validator.setRoot(root+",anotherElement"); // if multiple root elements are specified, in a comma separated list, the validation succeeds if one of these root elements is found
		validator.setSchemaLocation(schema);
		validator.configure();
		validator.start();

		String testXml = inputFile != null ? getTestXml(inputFile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.doPipe(new Message(testXml), session);
		PipeForward forward = result.getPipeForward();

		assertEquals("success", forward.getName());
	}

	@Test
	public void testRuntimeRootElement() throws Exception {
		String schema = SCHEMA_LOCATION_BASIC_A_OK;
		String root = "A"; 
		String inputFile = INPUT_FILE_BASIC_A_OK;
		XmlValidator validator = new XmlValidator();

		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
		
		validator.setFullSchemaChecking(true);
		validator.setRoot("oneElement,anotherElement"); // if multiple root elements are specified, in a comma separated list, the validation succeeds if one of these root elements is found
		validator.setSchemaLocation(schema);
		validator.configure();
		validator.start();

		String testXml = inputFile != null ? getTestXml(inputFile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.validate(new Message(testXml), session, root);
		PipeForward forward = result.getPipeForward();

		assertEquals("success", forward.getName());
	}

	@Test
	public void testWrongRuntimeRootElement() throws Exception {
		String schema = SCHEMA_LOCATION_BASIC_A_OK;
		String root = "A"; 
		String inputFile = INPUT_FILE_BASIC_A_OK;
		XmlValidator validator = new XmlValidator();

		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
		
		validator.setFullSchemaChecking(true);
		validator.setRoot(root);
		validator.setReasonSessionKey("reason");
		validator.setSchemaLocation(schema);
		validator.configure();
		validator.start();

		String testXml = inputFile != null ? getTestXml(inputFile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.validate(new Message(testXml), session, "anotherElement");
		PipeForward forward = result.getPipeForward();

		assertEquals("failure", forward.getName());
		assertThat((String)session.get("reason"), containsString("Illegal element 'A'. Element(s) 'anotherElement' expected."));
	}


	@Test //copied from iaf-test /XmlValidator/scenario07a
	public void testImportIncludeOK() throws Exception {
		XmlValidator validator = new XmlValidator();
		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
		validator.setRoot("root");
		validator.setSchemaLocation("http://nn.nl/root /Validation/ImportInclude/xsd/root.xsd");
		validator.setThrowException(true);
		validator.configure();
		validator.start();

		String testXml = getTestXml("/Validation/ImportInclude/root-ok.xml");
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.validate(new Message(testXml), session, "root");
		PipeForward forward = result.getPipeForward();

		assertEquals("success", forward.getName());
	}

	@Test //copied from iaf-test /XmlValidator/scenario07b
	public void testImportIncludeError() throws Exception {
		XmlValidator validator = new XmlValidator();
		validator.registerForward(createSuccessForward());
		validator.registerForward(createFailureForward());
		validator.setRoot("root");
		validator.setSchemaLocation("http://nn.nl/root /Validation/ImportInclude/xsd/root.xsd");
		validator.configure();
		validator.start();

		String testXml = getTestXml("/Validation/ImportInclude/root-err.xml");
		PipeLineSession session = new PipeLineSession();
		PipeRunResult result = validator.validate(new Message(testXml), session, "root");
		PipeForward forward = result.getPipeForward();

		assertEquals("failure", forward.getName());
	}
}
