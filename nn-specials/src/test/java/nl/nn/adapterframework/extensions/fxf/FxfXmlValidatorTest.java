package nl.nn.adapterframework.extensions.fxf;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.extensions.fxf.FxfXmlValidator.Direction;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.XmlValidatorException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FxfXmlValidatorTest extends PipeTestBase<FxfXmlValidator> {

	@Override
	public FxfXmlValidator createPipe() {
		return new FxfXmlValidator();
	}

	@Test
	public void testReceiveOk() throws Exception {
		pipe.setDirection(Direction.RECEIVE);
		pipe.setSoapBody("OnCompletedTransferNotify_Action");
		pipe.setThrowException(true);
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Fxf/OnCompletedTransferNotify-soap.xml");

		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testReceiveNoFilename() throws Exception {
		pipe.setDirection(Direction.RECEIVE);
		pipe.setSoapBody("OnCompletedTransferNotify_Action");

		configureAndStartPipe();

		String input = TestFileUtils.getTestFile("/Fxf/OnCompletedTransferNotify-nofiles-soap.xml");

		String expectedErrorMessage = "Validation using FxfXmlValidator with 'xml/wsdl/OnCompletedTransferNotify_FxF3_1.1.4_abstract.wsdl' failed:";

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(input));
		assertTrue(e.getCause() instanceof XmlValidatorException);
		assertTrue(e.getCause().getMessage().startsWith(expectedErrorMessage));
	}

}
