package nl.nn.adapterframework.validation;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class XSDTest {

	public void testAddNamespacesToSchema(String schemaLocation, String expectedSchemaLocation) throws ConfigurationException, IOException {

		String expectedSchema = TestFileUtils.getTestFile(expectedSchemaLocation.trim().split("\\s+")[1]);

		XSD xsd = getXSD(schemaLocation);
		xsd.setAddNamespaceToSchema(true);

		xsd.addTargetNamespace();
		String actual = StreamUtil.streamToString(xsd.getInputStream(), null, null);
		MatchUtils.assertXmlEquals(expectedSchema, actual);
	}

	@Test
	public void testAddNamespacesToSchemaNoop() throws ConfigurationException, IOException {
		testAddNamespacesToSchema(ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK, ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK+"-after-adding-namespace.xsd");
	}

	@Test
	public void testAddNamespacesToSchema() throws ConfigurationException, IOException {
		testAddNamespacesToSchema(ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE+"-after-adding-namespace.xsd");
	}


	public XSD getXSD(String schemaLocation) throws ConfigurationException {
		String[] split =  schemaLocation.trim().split("\\s+");
		XSD xsd = new XSD();
		xsd.initNamespace(split[0], getTestScopeProvider(), split[1]);
		return xsd;
	}

	private IScopeProvider getTestScopeProvider() {
		return new IScopeProvider() {

			@Override
			public ClassLoader getConfigurationClassLoader() {
				return getClass().getClassLoader();
			}

		};
	}
}
