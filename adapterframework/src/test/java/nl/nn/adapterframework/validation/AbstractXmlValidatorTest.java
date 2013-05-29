package nl.nn.adapterframework.validation;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.XmlValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;

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
            {XercesXmlValidator.class},
            {JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

    @Test
    public void straighforward() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
        AbstractXmlValidator instance = implementation.newInstance();
        // TODO It seems that setSchemaLocation was renamed
        /*instance.setSchemaLocation("http://www.ing.com/testxmlns " +
            "/GetIntermediaryAgreementDetails/xsd/A_correct.xsd");
        */instance.validate("intermediaryagreementdetails.xml", new PipeLineSessionBase(), "test");
    }


    @Test
    public void addTargetNamespace() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
        AbstractXmlValidator instance = implementation.newInstance();
        instance.setSchemasProvider(
                new SchemasProviderImpl(
                        "http://www.ing.com/testxmlns",
                        "/GetIntermediaryAgreementDetails/xsd/A.xsd"));
        instance.setAddNamespaceToSchema(true);
        instance.validate("intermediaryagreementdetails.xml", new PipeLineSessionBase(), "test");
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
