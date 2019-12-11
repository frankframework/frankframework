package nl.nn.adapterframework.soap;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.pipes.XmlValidatorTest;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.JavaxXmlValidator;
import nl.nn.adapterframework.validation.XercesXmlValidator;


/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class WsdlTest {

    private final  Class<AbstractXmlValidator> implementation;

    public WsdlTest(Class<AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class},
            {JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }


	@Test
	public void basic() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, ConfigurationException, URISyntaxException, NamingException {
		PipeLine simple = mockPipeLine(
				getXmlValidatorInstance("a", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"),
				getXmlValidatorInstance("b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"), "urn:webservice1", "Test1");
		Wsdl wsdl = new Wsdl(simple);
		wsdl.init();
		test(wsdl, "WsdlTest/webservice1.test.wsdl");
	}

	@Test
	public void basicMixed() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, ConfigurationException, URISyntaxException, NamingException {
		XmlValidator inputValidator=getXmlValidatorInstance("a", "b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd");
		IPipe outputValidator=inputValidator.getResponseValidator();
		PipeLine simple = mockPipeLine(inputValidator, outputValidator, "urn:webservice1", "Test1");
		Wsdl wsdl = new Wsdl(simple);
		wsdl.init();
		test(wsdl, "WsdlTest/webservice1.test.wsdl");
	}

	@Test
	public void includeXsdInWsdl() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, ConfigurationException, URISyntaxException, NamingException {
		PipeLine simple = mockPipeLine(
				getXmlValidatorInstance("a", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"),
				getXmlValidatorInstance("b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"), "urn:webservice1", "IncludeXsds");

		Wsdl wsdl = new Wsdl(simple);
        wsdl.setUseIncludes(true);
        wsdl.init();
		test(wsdl, "WsdlTest/includexsds.test.wsdl");
	}


	@Test
	public void includeXsdInWsdlMixed() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, ConfigurationException, URISyntaxException, NamingException {
		XmlValidator inputValidator=getXmlValidatorInstance("a", "b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd");
		IPipe outputValidator=inputValidator.getResponseValidator();
		PipeLine simple = mockPipeLine(inputValidator, outputValidator, "urn:webservice1", "IncludeXsds");

		Wsdl wsdl = new Wsdl(simple);
        wsdl.setUseIncludes(true);
        wsdl.init();
		test(wsdl, "WsdlTest/includexsds.test.wsdl");
	}


	@Test
	@Ignore("not finished, but would fail, you must specify root tag now.")
	public void noroottagAndInclude() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
		PipeLine simple = mockPipeLine(
				getXmlValidatorInstance(null, "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"),
				getXmlValidatorInstance("b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"), "urn:webservice1", "TestRootTag");

		Wsdl wsdl = new Wsdl(simple);
		wsdl.setUseIncludes(true);
		test(wsdl, "WsdlTest/noroottag.test.wsdl");
	}

    @Test
    public void noroottag() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
        PipeLine simple = mockPipeLine(
            getXmlValidatorInstance(null, "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"),
            getXmlValidatorInstance("b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd"), "urn:webservice1", "TestRootTag");
        Wsdl wsdl = new Wsdl(simple);
        wsdl.init();
        test(wsdl, "WsdlTest/noroottag.test.wsdl");
    }

    @Test
    public void noroottagMixed() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
		XmlValidator inputValidator=getXmlValidatorInstance(null, "b", "WsdlTest/test.xsd", "urn:webservice1 WsdlTest/test.xsd");
		IPipe outputValidator=inputValidator.getResponseValidator();
		PipeLine simple = mockPipeLine(inputValidator, outputValidator, "urn:webservice1", "TestRootTag");
        Wsdl wsdl = new Wsdl(simple);
        wsdl.init();
        test(wsdl, "WsdlTest/noroottag.test.wsdl");
    }




    @Test
    public void wubCalculateQuoteAndPolicyValuesLifeRetail() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
        PipeLine pipe = mockPipeLine(
            getXmlValidatorInstance("CalculationRequest", null, null,
                "http://wub2nn.nn.nl/CalculateQuoteAndPolicyValuesLifeRetail " +
						"WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail/xsd/CalculationRequestv2.1.xsd"),
            getXmlValidatorInstance("CalculationResponse", null, null,
                "http://wub2nn.nn.nl/CalculateQuoteAndPolicyValuesLifeRetail_response " +
						"WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail/xsd/CalculationRespons.xsd"),
            "http://wub2nn.nn.nl/CalculateQuoteAndPolicyValuesLifeRetail", "WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail");
        Wsdl wsdl = new Wsdl(pipe);
        wsdl.init();
        wsdl.setUseIncludes(true);
        test(wsdl, "WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail.test.wsdl");
    }

    @Test
    public void wubCalculateQuoteAndPolicyValuesLifeRetailMixed() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
    	XmlValidator inputValidator=getXmlValidatorInstance("CalculationRequest", "CalculationResponse", null,
    			"http://wub2nn.nn.nl/CalculateQuoteAndPolicyValuesLifeRetail WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail/xsd/CalculationRequestv2.1.xsd "+
    			"http://wub2nn.nn.nl/CalculateQuoteAndPolicyValuesLifeRetail_response  WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail/xsd/CalculationRespons.xsd");
    	IPipe outputValidator = inputValidator.getResponseValidator();
    	PipeLine pipe = mockPipeLine(inputValidator, outputValidator, 
            "http://wub2nn.nn.nl/CalculateQuoteAndPolicyValuesLifeRetail", "WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail");
        Wsdl wsdl = new Wsdl(pipe);
        wsdl.init();
        wsdl.setUseIncludes(true);
        test(wsdl, "WsdlTest/CalculateQuoteAndPolicyValuesLifeRetail.test.wsdl");
    }

    @Test
    public void wubFindIntermediary() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
        PipeLine pipe = mockPipeLine(
            getXmlValidatorInstance("FindIntermediaryREQ", null, null,
                "http://wub2nn.nn.nl/FindIntermediary WsdlTest/FindIntermediary/xsd/XSD_FindIntermediary_v1.1_r1.0.xsd"),
            getXmlValidatorInstance("FindIntermediaryRLY", null, null,
                "http://wub2nn.nn.nl/FindIntermediary WsdlTest/FindIntermediary/xsd/XSD_FindIntermediary_v1.1_r1.0.xsd"),
            "http://wub2nn.nn.nl/FindIntermediary", "WsdlTest/FindIntermediary");
        Wsdl wsdl = new Wsdl(pipe);
        wsdl.init();
        wsdl.setUseIncludes(true);
        assertTrue(wsdl.isUseIncludes());
		test(wsdl, "WsdlTest/FindIntermediary.test.wsdl");
        zip(wsdl);
        // assertEquals(2, wsdl.getXSDs(true).size()); TODO?
    }

    @Test
    public void wubFindIntermediaryMixed() throws XMLStreamException, IOException, SAXException, ParserConfigurationException, URISyntaxException, ConfigurationException, NamingException {
    	XmlValidator inputValidator=getXmlValidatorInstance("FindIntermediaryREQ", "FindIntermediaryRLY", null,
                		"http://wub2nn.nn.nl/FindIntermediary WsdlTest/FindIntermediary/xsd/XSD_FindIntermediary_v1.1_r1.0.xsd");
    	IPipe outputValidator = inputValidator.getResponseValidator();
        PipeLine pipe = mockPipeLine(inputValidator, outputValidator, "http://wub2nn.nn.nl/FindIntermediary", "WsdlTest/FindIntermediary");
        Wsdl wsdl = new Wsdl(pipe);
        wsdl.setUseIncludes(true);
        wsdl.init();
        assertTrue(wsdl.isUseIncludes());
		test(wsdl, "WsdlTest/FindIntermediary.test.wsdl");
        zip(wsdl);
        // assertEquals(2, wsdl.getXSDs(true).size()); TODO?
    }

    protected void test(Wsdl wsdl, String testWsdl) throws IOException, SAXException, ParserConfigurationException, XMLStreamException, URISyntaxException, NamingException, ConfigurationException {
        wsdl.setDocumentation("test");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wsdl.wsdl(out, "Test");
        DocumentBuilder dbuilder = createDocumentBuilder();
        Document result = dbuilder.parse(new ByteArrayInputStream(out.toByteArray()));

        Document expected = dbuilder.parse(getClass().getClassLoader().getResourceAsStream(testWsdl));
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);

        assertXMLEqual("expected xml (" + testWsdl + ") not similar to result xml:\n" + new String(out.toByteArray()), expected, result);

        zip(wsdl);

    }
    protected void zip(Wsdl wsdl) throws IOException, XMLStreamException, URISyntaxException, NamingException, ConfigurationException {
        File dir = new File(System.getProperty("java.io.tmpdir") + File.separator + "zipfiles");
        File zipFile = new File(dir, wsdl.getName() + ".zip");
		zipFile.getParentFile().mkdirs();
        System.out.println("Creating " + zipFile.getAbsolutePath());
        wsdl.zip(new FileOutputStream(zipFile), "http://myserver/");
    }

    protected XmlValidator getXmlValidatorInstance(String rootTag, String schema, String schemaLocation) throws ConfigurationException {
    	return getXmlValidatorInstance(rootTag, null, schema, schemaLocation);
    }
    
    protected XmlValidator getXmlValidatorInstance(String rootTag, String responseRootTag, String schema, String schemaLocation) throws ConfigurationException {
        XmlValidator validator = XmlValidatorTest.getUnconfiguredValidator(schemaLocation, implementation);
        validator.setSchema(schema);
        validator.setRoot(rootTag);
        if (responseRootTag!=null) {
        	validator.setResponseRoot(responseRootTag);
        }
        return validator;
    }

    protected PipeLine mockPipeLine(IPipe inputValidator, IPipe outputValidator, String targetNamespace, String adapterName) {
        PipeLine simple = mock(PipeLine.class);
        when(simple.getInputValidator()).thenReturn(inputValidator);
        when(simple.getOutputValidator()).thenReturn(outputValidator);
        Adapter adp = mock(Adapter.class);
        when(simple.getAdapter()).thenReturn(adp);
        Configuration cfg = mock(Configuration.class);
        when(simple.getAdapter().getConfiguration()).thenReturn(cfg);
        final ReceiverBase receiverBase = mock(ReceiverBase.class);
        WebServiceListener listener = new WebServiceListener();
        listener.setServiceNamespaceURI(targetNamespace);
        when(receiverBase.getListener()).thenReturn(listener);
        when(adp.getReceiverIterator()).thenAnswer(new Answer<Iterator>() {
            public Iterator answer(InvocationOnMock invocation) throws Throwable {
                return Arrays.asList(receiverBase).iterator();
            }
        });
        when(adp.getName()).thenReturn(adapterName);
        when(cfg.getClassLoader()).thenReturn(this.getClass().getClassLoader());
        when(adp.getConfigurationClassLoader()).thenReturn(this.getClass().getClassLoader());
        return simple;

    }
    static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory docBuilderFactory = XmlUtils.getDocumentBuilderFactory();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        return docBuilder;
    }


}
