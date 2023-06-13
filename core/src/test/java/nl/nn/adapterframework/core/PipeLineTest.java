package nl.nn.adapterframework.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hamcrest.core.StringEndsWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.extensions.esb.DirectWrapperPipe;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;

public class PipeLineTest {

	TestConfiguration configuration;

	@Before
	public void setUp() {
		configuration = new TestConfiguration();
	}

	@After
	public void tearDown() {
		configuration.close();
	}

	@Test
	public void testDuplicateExits() throws ConfigurationException {
		Adapter adapter = new Adapter();
		PipeLine pipeline = new PipeLine();
		pipeline.setApplicationContext(configuration);
		PipeLineExit exit = new PipeLineExit();
		exit.setPath("success");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.registerPipeLineExit(exit);
		adapter.setPipeLine(pipeline);

		List<String> warnings = configuration.getConfigurationWarnings().getWarnings();
		assertEquals(warnings.size(), 1);
		String lastWarning = warnings.get(warnings.size()-1);
		assertThat(lastWarning,StringEndsWith.endsWith("PipeLine exit named [success] already exists"));
	}

	@Test
	public void testFixedForwardPipesWithNoForwardShouldDefaultToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertTrue("pipe should not cause any configuration warnings", configuration.getConfigurationWarnings().getWarnings().isEmpty());
		assertEquals("pipe1 should only have 1 pipe-forward", 1, pipe.getForwards().size());
		assertEquals("pipe1 forward should default to next pipe", pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());

		assertEquals("pipe2 should only have 1 pipe-forward", 1, pipe2.getForwards().size());
		assertEquals("pipe2 forward should default to pipeline-exit", "exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());
	}

	@Test
	public void testFixedForwardAsLastPipeForwardsToFirstSuccessfulExit() throws ConfigurationException {
		// Arrange
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IExtendedPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IExtendedPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit errorExit = new PipeLineExit();
		errorExit.setPath("error");
		errorExit.setState(ExitState.ERROR);
		pipeline.registerPipeLineExit(errorExit);
		PipeLineExit successExit = new PipeLineExit();
		successExit.setPath("exit");
		successExit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(successExit);
		PipeLineExit successExit2 = new PipeLineExit();
		successExit2.setPath("exit2");
		successExit2.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(successExit2);

		// Act
		pipeline.configure();

		// Assert
		assertTrue("pipe should not cause any configuration warnings", configuration.getConfigurationWarnings().getWarnings().isEmpty());
		assertEquals("pipe1 should only have 1 pipe-forward", 1, pipe.getForwards().size());
		assertEquals("pipe1 forward should default to next pipe", pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());

		assertEquals("pipe2 should only have 1 pipe-forward", 1, pipe2.getForwards().size());
		assertEquals("pipe2 forward should default to successful pipeline-exit", "exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());
	}

	@Test
	public void testPipesWithNormalForwardToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IExtendedPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IExtendedPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertTrue("pipe should not cause any configuration warnings", configuration.getConfigurationWarnings().getWarnings().isEmpty());
		assertEquals("pipe1 should only have 1 pipe-forward", 1, pipe.getForwards().size());
		assertEquals("pipe1 forward should default to next pipe", pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());

		assertEquals("pipe2 should only have 1 pipe-forward", 1, pipe2.getForwards().size());
		assertEquals("pipe2 forward should default to pipeline-exit", "exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());
	}

	@Test
	public void giveWarningWhenForwardIsAlreadyDefined() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.registerForward(new PipeForward("success", "exit"));//Surprisingly this doesn't cause any warnings
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertEquals("pipes should cause a configuration warning", 1, configuration.getConfigurationWarnings().getWarnings().size());
		assertThat(configuration.getConfigWarning(0), StringEndsWith.endsWith("] has forward [success] which is already registered"));
		assertEquals("pipe1 should only have 1 pipe-forward", 1, pipe.getForwards().size());
		assertEquals("pipe1 forward should default to next pipe", pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());

		assertEquals("pipe2 should only have 1 pipe-forward", 1, pipe2.getForwards().size());
		assertEquals("pipe2 forward should default to pipeline-exit", "exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());
	}

	@Test
	public void giveWarningWhenForwardToNonExistingPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", "the next pipe"));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("special exit name");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertEquals("pipes should cause a configuration warning", 1, configuration.getConfigurationWarnings().getWarnings().size());
		assertThat(configuration.getConfigWarning(0), StringEndsWith.endsWith("] has a forward of which the pipe to execute [the next pipe] is not defined"));
		assertEquals("pipe1 should only have 1 pipe-forward", 1, pipe.getForwards().size());
		assertEquals("pipe1 forward should exist even though next pipe doesn't exist", "the next pipe", pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());

		assertEquals("pipe2 should only have 1 pipe-forward", 1, pipe2.getForwards().size());
		assertEquals("pipe2 forward should default to pipeline-exit", "special exit name", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());
	}

	private static class NonFixedForwardPipe extends AbstractPipe {
		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			return new PipeRunResult(findForward(PipeForward.SUCCESS_FORWARD_NAME), message);
		}
	}

	@Test
	public void testNonFixedForwardPipesWithNoForwardToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IExtendedPipe pipe = configuration.createBean(NonFixedForwardPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IExtendedPipe pipe2 = configuration.createBean(NonFixedForwardPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertEquals("pipe should cause 1 configuration warnings", 0, configuration.getConfigurationWarnings().getWarnings().size());

		assertEquals("pipe1 should only have 1 pipe-forward", 1, pipe.getForwards().size());
		assertEquals("pipe1 forward should default to next pipe", pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath());

		assertEquals("pipe2 should not have a pipe-forward", 0, pipe2.getForwards().size());
	}

	//Should add tests to assertThat(configuration.getConfigWarning(0), StringEndsWith.endsWith("] has no pipe forwards defined"));


	@Test
	public void testDirectWrapperPipeSuccessForward() throws ConfigurationException, PipeRunException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);

		PipeForward pf = configuration.createBean(PipeForward.class);
		pf.setName("success");
		pf.setPath("nextPipe");

		PipeForward toExit = configuration.createBean(PipeForward.class);
		toExit.setName("success");
		toExit.setPath("EXIT");

		DirectWrapperPipe pipe = configuration.createBean(DirectWrapperPipe.class);
		pipe.setName("DirectWrapperPipe");
		pipe.registerForward(pf);
		pipeline.addPipe(pipe);

		EchoPipe echoPipe = configuration.createBean(EchoPipe.class);
		echoPipe.setName("nextPipe");
		echoPipe.setPipeLine(pipeline);
		echoPipe.registerForward(toExit);
		pipeline.addPipe(echoPipe);

		PipeLineExit exit = configuration.createBean(PipeLineExit.class);
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);

		pipeline.setOwner(pipe);
		pipeline.configure();

		CorePipeProcessor cpp = configuration.createBean(CorePipeProcessor.class);
		PipeLineSession ps = configuration.createBean(PipeLineSession.class);

		PipeRunResult pipeRunResult=cpp.processPipe(pipeline, pipe, new Message("<dummy/>"), ps);

		PipeForward pipeForward=pipeRunResult.getPipeForward();

		IForwardTarget target = pipeline.resolveForward(pipe, pipeForward);

		assertNotNull(target);
	}
}
