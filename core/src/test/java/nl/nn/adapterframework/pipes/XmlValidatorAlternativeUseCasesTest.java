package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;

import org.junit.jupiter.api.Test;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.ValidatorTestBase;

public class XmlValidatorAlternativeUseCasesTest extends PipeTestBase<XmlValidator> {

	@Override
	public XmlValidator createPipe() throws ConfigurationException {
		return new XmlValidator();
	}

	@Test
	void testBasic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String schemaLocation = ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK;
		String input = TestFileUtils.getTestFile(ValidatorTestBase.INPUT_FILE_BASIC_A_OK+".xml");

		pipe.setSchemaLocation(schemaLocation);
		pipe.setThrowException(true);

		configureAndStartPipe();

		PipeRunResult prr = doPipe(input);

		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	void testWitSessionKey() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String schemaLocation = ValidatorTestBase.SCHEMA_LOCATION_BASIC_A_OK;
		String input = TestFileUtils.getTestFile(ValidatorTestBase.INPUT_FILE_BASIC_A_OK+".xml");
		String schemaSessionKey = "schemaLocationSessionKey";
		String schema = schemaLocation.split(" ")[1];

		pipe.setSchemaSessionKey(schemaSessionKey);
		pipe.setThrowException(true);

		configureAndStartPipe();

		session.put(schemaSessionKey, schema);
		PipeRunResult prr = doPipe(input);

		assertEquals("success", prr.getPipeForward().getName());
	}
}
