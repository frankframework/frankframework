package org.frankframework.validation;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.frankframework.core.PipeLineSession;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;

/**
 * @author Gerrit van Brakel
 */
public class XmlValidatorTest extends XmlValidatorTestBase {

	protected static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of(XercesXmlValidator.class),
				Arguments.of(JavaxXmlValidator.class)
		);
	}

	@Override
	public ValidationResult validate(String rootElement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputfile, String[] expectedFailureReasons) throws Exception {
		AbstractXmlValidator xmlValidator = ClassUtils.newInstance(implementation);
		xmlValidator.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
		xmlValidator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
		xmlValidator.setThrowException(true);
		xmlValidator.setFullSchemaChecking(true);

		String testXml = inputfile != null ? TestFileUtils.getTestFile(inputfile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();

		try {
			xmlValidator.configure(null);
			xmlValidator.start();

			RootValidations rootvalidations = null;
			if(rootElement != null) {
				rootvalidations = new RootValidations("Envelope", "Body", rootElement);
			}
			ValidationResult result = xmlValidator.validate(testXml, session, rootvalidations, null);
			evaluateResult(result, session, null, expectedFailureReasons);
			return result;
		} catch (Exception e) {
			evaluateResult(null, session, e, expectedFailureReasons);
			return ValidationResult.INVALID;
		}
	}
}
