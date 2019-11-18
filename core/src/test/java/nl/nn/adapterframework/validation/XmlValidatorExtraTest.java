package nl.nn.adapterframework.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XmlValidatorExtraTest extends PipeTestBase<XmlValidator> {

	@Override
	public XmlValidator createPipe() {
		return new XmlValidator();
	}


	@Test
	@Ignore("Override requires XmlSchema version 1.1")
	public void testOverride() throws Exception {
		pipe.setSchema("/Validation/OverrideAndRedefine/Override.xsd");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();
		
		PipeLineSessionBase session = new PipeLineSessionBase();
		String input = TestFileUtils.getTestFile("/Validation/OverrideAndRedefine/in_OK.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testRedefine() throws Exception {
		pipe.setSchema("/Validation/OverrideAndRedefine/Redefine.xsd");
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		PipeLineSessionBase session = new PipeLineSessionBase();
		String input = TestFileUtils.getTestFile("/Validation/OverrideAndRedefine/in_OK.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals("success", prr.getPipeForward().getName());	
	}
}
