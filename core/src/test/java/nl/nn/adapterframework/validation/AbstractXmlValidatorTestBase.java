package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.runners.Parameterized;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * @author Gerrit van Brakel
 */
public abstract class AbstractXmlValidatorTestBase extends XmlValidatorTestBase {

	private Class<? extends AbstractXmlValidator> implementation;

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { XercesXmlValidator.class }, { JavaxXmlValidator.class } };
		return Arrays.asList(data);
	}

	public AbstractXmlValidatorTestBase(Class<? extends AbstractXmlValidator> implementation) {
		this.implementation = implementation;
	}

	@Override
	public String validate(String rootElement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputfile, String[] expectedFailureReasons) throws ConfigurationException, InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, IOException {
		AbstractXmlValidator instance = implementation.newInstance();
		instance.setSchemasProvider(getSchemasProvider(schemaLocation, addNamespaceToSchema));
		instance.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
//        instance.registerForward("success");
		instance.setThrowException(true);
		instance.setFullSchemaChecking(true);

		String testXml = inputfile != null ? TestFileUtils.getTestFile(inputfile + ".xml") : null;
		PipeLineSession session = new PipeLineSession();

		try {
			instance.configure("init");
			instance.start();

			Set<List<String>> rootvalidations = null;
			if(rootElement != null) {
				List<String> rootvalidation = new ArrayList<String>();
				rootvalidation.add("Envelope");
				rootvalidation.add("Body");
				rootvalidation.add(rootElement);
				rootvalidations = new HashSet<List<String>>();
				rootvalidations.add(rootvalidation);
			}
			String result = instance.validate(testXml, session, "test", rootvalidations, null);
			evaluateResult(result, session, null, expectedFailureReasons);
			return result;
		} catch (Exception e) {
			evaluateResult(null, session, e, expectedFailureReasons);
			return "Invalid XML";
		}
	}
}
