package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

public class IsXmlPipeTest extends PipeTestBase<IsXmlPipe> {

	String pipeForwardThen = "then";
	String pipeForwardElse = "else";

	@Override
	public IsXmlPipe createPipe() throws ConfigurationException {
		IsXmlPipe isXmlPipe = new IsXmlPipe();

		//Add default pipes
		isXmlPipe.addForward(new PipeForward(pipeForwardThen, null));
		isXmlPipe.addForward(new PipeForward(pipeForwardElse, null));
		return isXmlPipe;
	}

	@Test
	void validInputOnInvalidElsePipeTestUnRegistered() throws Exception {
		String pipeName = "test123";
		pipe.setElseForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	void emptySpaceInputOnValidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.addForward(new PipeForward(pipeName, null));
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, " <test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	void emptySpaceInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, " <test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	void tabSpaceInputOnValidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.addForward(new PipeForward(pipeName, null));
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "	<test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	void tabSpaceInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "	 <test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	void validInputOnInvalidThenPipeTestUnRegistered() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "<test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	void validInputOnInvalidElsePipeTest() throws Exception {
		String pipeName = "test123";

		pipe.setElseForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	void validInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "<test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	void validInputOnElsePipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "test", session);
		assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	void validInputOnThenPipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	void emptyInputOnElsePipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "", session);
		assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	void emptyInputOnThenPipeTest() throws Exception {
		pipe.setElseForwardOnEmptyInput(false);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, "", session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	void nullInputOnElsePipeTest() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, null, session);
		assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	void nullInputOnThenPipeTest() throws Exception {
		pipe.setElseForwardOnEmptyInput(false);
		configureAndStartPipe();

		PipeRunResult prr  = doPipe(pipe, null, session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}
}
