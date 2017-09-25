package nl.nn.adapterframework.validation;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.EntityResolvingTest;

@RunWith(value = Parameterized.class)
public class ValidatorEntityExpansionTest extends EntityResolvingTest {

	
    private Class<? extends AbstractXmlValidator> implementation;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class}
            ,{JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

    public ValidatorEntityExpansionTest(Class<? extends AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

	@Override
	public String parseAndRenderString(String xsd, String xmlIn) throws Exception {
//        AbstractXmlValidator instance = implementation.newInstance();
//        instance.setSchemasProvider(new SchemasProviderImpl(SCHEMA_NAMESPACE, xsd));
//        instance.setIgnoreUnknownNamespaces(false);
//        instance.setAddNamespaceToSchema(false);
//        instance.configure("Setup");
//        String result=instance.validate(xmlIn, new PipeLineSessionBase(), "test");
//        System.out.println("Validation Result:"+result);
//        return instance.get
//		return result;
        AbstractXmlValidator instance = implementation.newInstance();
        System.out.println("Created instance ["+instance.getClass().getName()+"]");
        instance.setSchemasProvider(new SchemasProviderImpl(SCHEMA_NAMESPACE, xsd));
        instance.setThrowException(true);
        instance.setFullSchemaChecking(true);
        instance.configure("init");

        PipeLineSessionBase session = new PipeLineSessionBase();
        ValidationContext context = instance.createValidationContext(session, null, null);
        XMLReader reader = instance.getValidatingParser(session, context, false);
        StringReader sr = new StringReader(xmlIn);
    	InputSource is = new InputSource(sr);
    	final StringBuffer sb=new StringBuffer();
    	
    	ContentHandler ch = new DefaultHandler() {

    		boolean elementOpen;
    		
			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				if (elementOpen) {
					sb.append(">");
					elementOpen=false;
				}
				sb.append(ch, start, length);
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				sb.append("<"+localName);
				sb.append(" xmlns=\"").append(uri).append("\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
				for (int i=0;i<atts.getLength();i++) {
					sb.append(' ').append(atts.getLocalName(i)).append("=\"").append(atts.getValue(i)).append('"');
				}
				elementOpen=true;
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if (elementOpen) {
					sb.append("/>");
					elementOpen=false;
				} else {
					sb.append("</"+localName+">");
				}
			}


			@Override
			public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
				//ignore
			}
    		
    	};
    	
    	
//		String testString;
//		try {
//			testString = XmlUtils.identityTransform((String) xmlIn);
//		} catch (DomBuilderException e) {
//			throw new XmlValidatorException("caught DomBuilderException", e);
//		}
//		System.out.println("TestStr:"+testString);
    	
    	reader.setContentHandler(ch);

    	reader.parse(is);
    	
    	instance.finalizeValidation(context, session, null);
    	
    	XmlValidatorErrorHandler errorHandler = context.getErrorHandler();
    	if (errorHandler.hasErrorOccured()) {
    		throw new SAXException(errorHandler.getReasons());
    	}
    	return sb.toString();
	}
	
}
