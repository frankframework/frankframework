package nl.nn.adapterframework.validation;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

    public AbstractXmlValidatorTest(Class<AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class}
            //,{JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

    protected void validationTest(String namespace, String systemId, String inputfile, boolean addNamespaceToSchema, boolean expectedValid) throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
        AbstractXmlValidator instance = implementation.newInstance();
        instance.setSchemasProvider(new SchemasProviderImpl(namespace, systemId));
        instance.setIgnoreUnknownNamespaces(false);
        instance.setAddNamespaceToSchema(addNamespaceToSchema);
        String result=instance.validate(getTestXml(inputfile), new PipeLineSessionBase(), "test");
        if (expectedValid) {
            Assert.assertEquals("valid XML",result);
        } else {
            Assert.assertEquals("Invalid XML: does not comply to XSD",result);
        }
    }
    
    @Test
    public void straighforward() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_correct.xsd","/Basic/in/ok.xml",false,true);
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_correct.xsd","/Basic/in/with_errors.xml",false,false);
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_without_targetnamespace.xsd","/Basic/in/ok.xml",false,false);
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_without_targetnamespace.xsd","/Basic/in/with_errors.xml",false,false);
    }


    @Test
    public void addTargetNamespace() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
//        AbstractXmlValidator instance = implementation.newInstance();
//        instance.setSchemasProvider(
//                new SchemasProviderImpl(
//                        "http://www.ing.com/testxmlns",
//                        "/GetIntermediaryAgreementDetails/xsd/A_without_targetnamespace.xsd"));
//        instance.setAddNamespaceToSchema(true);
//        instance.setIgnoreUnknownNamespaces(false);
//        instance.validate(getTestXml("/intermediaryagreementdetails.xml"), new PipeLineSessionBase(), "test");
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_correct.xsd","/Basic/in/ok.xml",true,true);
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_correct.xsd","/Basic/in/with_errors.xml",true,false);
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_without_targetnamespace.xsd","/Basic/in/ok.xml",true,false);
    	validationTest("http://www.ing.com/testxmlns","/Basic/xsd/A_without_targetnamespace.xsd","/Basic/in/with_errors.xml",true,false);
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
