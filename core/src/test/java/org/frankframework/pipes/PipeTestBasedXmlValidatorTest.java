package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;

public class PipeTestBasedXmlValidatorTest extends PipeTestBase<XmlValidator> {
	public static String BASE_DIR_VALIDATION="/Validation";
	public String ROOT_NAMESPACE_BASIC="http://www.ing.com/testxmlns";
	public String SCHEMA_LOCATION_FACET_ERROR           =ROOT_NAMESPACE_BASIC+"_facetError "+BASE_DIR_VALIDATION+"/Basic/xsd/facet_error.xsd";

	@Override
	public XmlValidator createPipe() {
		return new XmlValidator();
	}

	@Test
	public void testFacetErrorDetected() throws Exception {
		pipe.setFullSchemaChecking(true);
		pipe.setRoot("Root");
		pipe.setReasonSessionKey("reason");
		pipe.setThrowException(true);
		pipe.addForward(new PipeForward("success", null));
		pipe.setSchemaLocation(SCHEMA_LOCATION_FACET_ERROR);
		configureAndStartPipe();
		assertEquals(1, getConfigurationWarnings().size());
		String configurationWarning = getConfigurationWarnings().get(0);
		assertThat(configurationWarning, containsString("cos-applicable-facets"));
		assertThat(configurationWarning, containsString("maxLength"));
	}
}
