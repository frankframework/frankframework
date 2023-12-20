package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.validation.ValidatorTestBase;
import org.junit.jupiter.api.Test;

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
