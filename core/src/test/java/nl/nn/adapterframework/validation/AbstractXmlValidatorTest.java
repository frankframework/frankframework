package nl.nn.adapterframework.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import fi.iki.elonen.NanoHTTPD;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.XmlValidator;

/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class AbstractXmlValidatorTest {
    private Class<AbstractXmlValidator> implementation;
    private AtomicBoolean hitExternalSystem;
    private NanoHTTPD httpd;

    public AbstractXmlValidatorTest(Class<AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }
    
    @Before
    public void startServer() throws IOException {
        hitExternalSystem = new AtomicBoolean();
        httpd = new NanoHTTPD(8090) {
            @Override
            public Response serve(IHTTPSession session) {
                hitExternalSystem.set(true);
                return newFixedLengthResponse("hello");
            }
        };
        httpd.start();        
    }
    
    @After
    public void stopServer() {
        httpd.stop();
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
    public void straighforward() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
        AbstractXmlValidator instance = implementation.newInstance();
        instance.setSchemasProvider(new SchemasProviderImpl("http://www.ing.com/testxmlns",
                "/GetIntermediaryAgreementDetails/xsd/A_correct.xsd"));
        instance.setIgnoreUnknownNamespaces(false);
        instance.validate(getTestXml("/intermediaryagreementdetails.xml"), new PipeLineSessionBase(), "test");
    }


    @Test
    public void addTargetNamespace() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
        AbstractXmlValidator instance = implementation.newInstance();
        instance.setSchemasProvider(
                new SchemasProviderImpl(
                        "http://www.ing.com/testxmlns",
                        "/GetIntermediaryAgreementDetails/xsd/A.xsd"));
        instance.setAddNamespaceToSchema(true);
        instance.setIgnoreUnknownNamespaces(false);
        instance.validate(getTestXml("/intermediaryagreementdetails.xml"), new PipeLineSessionBase(), "test");
    }
    
    @Test
    public void xxeInjection_gotit() throws Exception {
        System.clearProperty(XmlValidator.XML_IGNORE_EXTERNAL_ENTITIES);
        validateXXE();
        assertTrue(hitExternalSystem.get());
    }

    @Test
    public void xxeInjection_ignoring_entities() throws Exception {
        assumeThat(implementation, Is.is(XercesXmlValidator.class));
        System.setProperty(XmlValidator.XML_IGNORE_EXTERNAL_ENTITIES, "true");
        validateXXE();
        assertFalse(hitExternalSystem.get());
    }

    private void validateXXE()
            throws InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, ConfigurationException {
        AbstractXmlValidator validator = implementation.newInstance();

        validator.setSchemasProvider(
                new SchemasProviderImpl(
                        "http://www.ing.com/testxmlns",
                        "/GetIntermediaryAgreementDetails/xsd/A.xsd"));
        validator.setAddNamespaceToSchema(true);
        validator.setIgnoreUnknownNamespaces(false);
        validator.validate("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + 
                "<!DOCTYPE foo [ <!ELEMENT foo ANY >\n" + 
                "<!ENTITY xxe SYSTEM \"http://localhost:8090\" >]>\n" + 
                "<foo>\n" + 
                "    <giveme>&xxe;</giveme>\n" + 
                "    <yourpasses>mypass</yourpasses>\n" + 
                "</foo>", new PipeLineSessionBase(), "test");
    }


    private String getTestXml(String testxml) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(XmlValidator.class.getResourceAsStream(testxml)));
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line);
            line = buf.readLine();
        }
        return string.toString();

    }
}
