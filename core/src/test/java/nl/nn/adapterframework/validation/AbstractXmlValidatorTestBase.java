package nl.nn.adapterframework.validation;


import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

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
            ,{JavaxXmlValidator.class}
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

    @Test(expected = SAXParseException.class)
    public void testEntityExpansion() throws Exception {
    	String schemaLocation=SCHEMA_LOCATION_BASIC_A_OK;
    	String inputfile=INPUT_FILE_BASIC_A_ENTITY_EXPANSION;
        AbstractXmlValidator instance = implementation.newInstance();
        instance.setSchemasProvider(getSchemasProvider(schemaLocation, false));
        instance.setThrowException(true);
        instance.setFullSchemaChecking(true);
        instance.configure("init");

        String testXml=inputfile!=null?getTestXml(inputfile+".xml"):null;
        PipeLineSessionBase session = new PipeLineSessionBase();
        ValidationContext context = instance.createValidationContext(session);
        XMLReader reader = instance.getValidatingParser(session, context);
        StringReader sr = new StringReader(testXml);
    	InputSource is = new InputSource(sr);
    	final StringBuffer sb=new StringBuffer();
    	
    	ContentHandler ch = new DefaultHandler() {

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				sb.append(ch, start, length);
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				sb.append("</"+localName+">");
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				sb.append("<"+localName+">");
			}

			@Override
			public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
				//ignore
			}
    		
    	};
    	
    	reader.setContentHandler(ch);

    	reader.parse(is);
    	
    	System.out.println("----> result"+ sb.toString());
    	
    	assertEquals("<A>    <B></B></A>", sb.toString());
    }



}
