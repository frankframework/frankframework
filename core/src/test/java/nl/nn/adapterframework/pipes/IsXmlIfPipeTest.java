package nl.nn.adapterframework.pipes;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public class IsXmlIfPipeTest extends PipeTestBase<IsXmlIfPipe> {

	String pipeForwardThen = "then";
	String pipeForwardElse = "else";

	@Override
	public IsXmlIfPipe createPipe() throws ConfigurationException {
		IsXmlIfPipe isXmlIfPipe = new IsXmlIfPipe();

		//Add default pipes
		isXmlIfPipe.registerForward(new PipeForward(pipeForwardThen,null));
		isXmlIfPipe.registerForward(new PipeForward(pipeForwardElse,null));

		return isXmlIfPipe;
	}

	@Test
	public void validInputOnInvalidElsePipeTestUnRegistered() throws PipeRunException, ConfigurationException, PipeStartException{
		exception.expect(PipeRunException.class);

		String pipeName = "test123";
		pipe.setElseForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void EmptySpaceInputOnValidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		String pipeName = "test123";
		pipe.registerForward(new PipeForward(pipeName, null));
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, " <test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void EmptySpaceInputOnInvalidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		exception.expect(PipeRunException.class);

		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, " <test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void TabSpaceInputOnValidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		String pipeName = "test123";
		pipe.registerForward(new PipeForward(pipeName, null));
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "	<test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void TabSpaceInputOnInvalidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		exception.expect(PipeRunException.class);
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "	<test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnInvalidThenPipeTestUnRegistered() throws PipeRunException, ConfigurationException, PipeStartException{
		exception.expect(PipeRunException.class);

		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "<test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnInvalidElsePipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		String pipeName = "test123";

		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));		
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnInvalidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "<test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnElsePipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "test", session);
		Assert.assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	public void validInputOnThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "<test", session);
		Assert.assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void emptyInputOnElsePipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "", session);
		Assert.assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}

	@Test
	public void emptyInputOnThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.setElseForwardOnEmptyInput(false);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, "", session);
		Assert.assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void nullInputOnElsePipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, null, session);
		Assert.assertEquals(pipeForwardElse, prr.getPipeForward().getName());
	}
	
	@Test
	public void nullInputOnThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.setElseForwardOnEmptyInput(false);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = doPipe(pipe, null, session);
		Assert.assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}
}