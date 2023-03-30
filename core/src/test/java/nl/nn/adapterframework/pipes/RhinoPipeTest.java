package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

/**
 * RhinoPipe Tester.
 *
 * @author <Sina Sen>
 */
public class RhinoPipeTest extends PipeTestBase<RhinoPipe> {

	private String fileName = "/Pipes/javascript/rhino-test.js";

	@Override
	public RhinoPipe createPipe() {
		return new RhinoPipe();
	}

	@Test
	public void testDoPipe() throws Exception {
		pipe.setFileName(fileName);
		pipe.setjsfunctionName("giveNumber");
		pipe.setjsfunctionArguments("3");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "3", session);
		assertEquals("9", res.getResult().asString());
	}

	@Test
	public void testDoPipeLookupAtRuntime() throws Exception {
		pipe.setFileName(fileName);
		pipe.setjsfunctionName("giveNumber");
		pipe.setjsfunctionArguments("2");
		pipe.setLookupAtRuntime(true);
		PipeRunResult res = doPipe(pipe, "3", session);
		assertEquals("9", res.getResult().asString());
	}

	@Test
	public void testDoPipeFailNoFilename() throws Exception {
		pipe.setjsfunctionName("giveNumber");
		pipe.setjsfunctionArguments("2");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("has neither fileName nor inputString specified"));
	}

	@Test
	public void testDoPipeAsFunctionNotSpecified() throws Exception {
		pipe.setFileName(fileName);
		pipe.setjsfunctionArguments("2");

		PipeStartException e = assertThrows(PipeStartException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("JavaScript functionname not specified!"));
	}

	@Test
	public void testDoPipeFailAsWrongFileName() throws Exception {
		pipe.setFileName("random");
		pipe.setjsfunctionName("giveNumber");
		pipe.setjsfunctionArguments("3");

		PipeStartException e = assertThrows(PipeStartException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("cannot find resource [random]"));
	}

	@Test
	public void testDoPipeFailAsWrongInputType() throws Exception {
		pipe.setFileName(fileName);
		pipe.setjsfunctionName("giveNumber");
		pipe.setjsfunctionArguments("3");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, 4 + "s", session);
		assertEquals("NaN", res.getResult().asString());
	}

}
