package nl.nn.adapterframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.stream.Message;

public class PipeTestBasedXmlValidatorTest extends PipeTestBase<XmlValidator> {
	public static String BASE_DIR_VALIDATION="/Validation";
	public String ROOT_NAMESPACE_BASIC="http://www.ing.com/testxmlns";
	public String SCHEMA_LOCATION_FACET_ERROR           =ROOT_NAMESPACE_BASIC+"_facetError "+BASE_DIR_VALIDATION+"/Basic/xsd/facet_error.xsd";	
	public String INPUT_FILE_FOR_FACET_ERROR            =BASE_DIR_VALIDATION+"/Basic/in/for_wrong_facet_unqualified";

	@Override
	public XmlValidator createPipe() {
		return new XmlValidator();
	}

	@Test
	public void testFacetErrorDetected() throws Exception {
		String inputFile = INPUT_FILE_FOR_FACET_ERROR;
		pipe.setFullSchemaChecking(true);
		pipe.setRoot("Root");
		pipe.setReasonSessionKey("reason");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		pipe.setSchemaLocation(SCHEMA_LOCATION_FACET_ERROR);
		configureAndStartPipe();
		doPipe(getTestXml(inputFile + ".xml"));
		assertEquals(1, getConfigurationWarnings().size());
		String configurationWarning = getConfigurationWarnings().get(0);
		assertThat(configurationWarning, containsString("cos-applicable-facets"));
		assertThat(configurationWarning, containsString("maxLength"));
	}

    // TODO: Put this function in PipeTestBase.
	protected Message getTestXml(String testxml) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(XmlValidator.class.getResourceAsStream(testxml)));
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line);
            line = buf.readLine();
        }
        return new Message(new StringReader(string.toString()));
    }
}
