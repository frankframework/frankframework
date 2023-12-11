package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.junit.jupiter.api.Test;

public class IsXmlIfPipeTest extends PipeTestBase<IsXmlPipe> {

	String pipeForwardThen = "then";
	String pipeForwardElse = "else";

	@Override
	public IsXmlPipe createPipe() throws ConfigurationException {
		IsXmlPipe isXmlIfPipe = new IsXmlPipe();

		//Add default pipes
		isXmlIfPipe.registerForward(new PipeForward(pipeForwardThen,null));
		isXmlIfPipe.registerForward(new PipeForward(pipeForwardElse,null));

		return isXmlIfPipe;
	}

	@Test
	public void validInputOnInvalidElsePipeTestUnRegistered() throws Exception {
		String pipeName = "test123";
		pipe.setElseForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	public void emptySpaceInputOnValidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.registerForward(new PipeForward(pipeName, null));
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, " <test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void emptySpaceInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, " <test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	public void tabSpaceInputOnValidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.registerForward(new PipeForward(pipeName, null));
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "	<test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void tabSpaceInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "	 <test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	public void validInputOnInvalidThenPipeTestUnRegistered() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "<test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	public void validInputOnInvalidElsePipeTest() throws Exception {
		String pipeName = "test123";

		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "<test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnElsePipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "test", session);
		assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnThenPipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void emptyInputOnElsePipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "", session);
		assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	public void emptyInputOnThenPipeTest() throws Exception {
		pipe.setElseForwardOnEmptyInput(false);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "", session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void nullInputOnElsePipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, null, session);
		assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	public void nullInputOnThenPipeTest() throws Exception {
		pipe.setElseForwardOnEmptyInput(false);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, null, session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}
}
