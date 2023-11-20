package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;

/**
 * @author Gerrit van Brakel
 */
public abstract class AbstractXmlValidatorTestBase extends XmlValidatorTestBase {

	private final Class<? extends AbstractXmlValidator> implementation;

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { XercesXmlValidator.class }, { JavaxXmlValidator.class } };
		return Arrays.asList(data);
	}

	public AbstractXmlValidatorTestBase(Class<? extends AbstractXmlValidator> implementation) {
		this.implementation = implementation;
	}

	@Override
	public ValidationResult validate(String rootElement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputfile, String[] expectedFailureReasons) throws ConfigurationException, InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, IOException {
		AbstractXmlValidator instance = implementation.newInstance();
		instance.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
		instance.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
//        instance.registerForward("success");
		instance.setThrowException(true);
		instance.setFullSchemaChecking(true);

		String testXml = inputfile != null ? TestFileUtils.getTestFile(inputfile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();

		try {
			instance.configure(null);
			instance.start();

			RootValidations rootvalidations = null;
			if(rootElement != null) {
				rootvalidations = new RootValidations("Envelope", "Body", rootElement);
			}
			ValidationResult result = instance.validate(testXml, session, "test", rootvalidations, null);
			evaluateResult(result, session, null, expectedFailureReasons);
			return result;
		} catch (Exception e) {
			evaluateResult(null, session, e, expectedFailureReasons);
			return ValidationResult.INVALID;
		}
	}
}
