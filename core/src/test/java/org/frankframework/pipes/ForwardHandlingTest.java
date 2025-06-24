package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.PipeForward;
import org.frankframework.testutil.TestAppender;

class ForwardHandlingTest extends ConfiguredTestBase {


	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		ApplicationWarnings.removeInstance();
	}

	@Test
	void testFindForwardToPipeExplicit() throws ConfigurationException {
		SwitchPipe pipe1 = new SwitchPipe();
		pipe1.setName("pipe1");
		pipe1.addForward(new PipeForward("fakeForward", "pipe3"));
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		EchoPipe pipe3 = new EchoPipe();
		pipe3.setName("pipe3");
		pipeline.addPipe(pipe3);

		configureAdapter();

		PipeForward forward = pipe1.findForward("fakeForward");
		assertEquals("pipe3", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}

	@Test
	void testFindForwardToPipeImplicit() throws ConfigurationException {
		SwitchPipe pipe1 = new SwitchPipe();
		pipe1.setName("pipe1");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		EchoPipe pipe3 = new EchoPipe();
		pipe3.setName("pipe3");
		pipeline.addPipe(pipe3);

		configureAdapter();

		PipeForward forward = pipe1.findForward("pipe2");
		assertEquals("pipe2", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}

	@Test
	void testFindForwardToNextPipeImplicit() throws ConfigurationException {
		SwitchPipe pipe1 = new SwitchPipe();
		pipe1.setName("pipe1");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		EchoPipe pipe3 = new EchoPipe();
		pipe3.setName("pipe3");
		pipeline.addPipe(pipe3);

		configureAdapter();

		PipeForward forward = pipe2.findForward("success");
		assertEquals("pipe3", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}


	@Test
	void testFindForwardToExitExplicit() throws ConfigurationException {
		SwitchPipe pipe1 = new SwitchPipe();
		pipe1.setName("pipe1");
		pipe1.addForward(new PipeForward("ready", "READY"));
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		configureAdapter();

		PipeForward forward = pipe1.findForward("ready");
		assertEquals("READY", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}


	@Test
	void testFindForwardToExitImplicit() throws ConfigurationException {
		SwitchPipe pipe1 = new SwitchPipe();
		pipe1.setName("pipe1");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = new EchoPipe();
		pipe2.setName("pipe2");
		pipeline.addPipe(pipe2);

		configureAdapter();

		PipeForward forward = pipe2.findForward("success");
		assertEquals("READY", forward.getPath());
		assertEquals(0, getConfigurationWarnings().size());
		assertEquals(0, ApplicationWarnings.getSize());
	}

	@Test
	public void testRegisterUnknownForward() throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", null)));
		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("thisForwardDoesntExist", null)));

		assertDoesNotThrow(pipe::configure);
		List<String> warnings = getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("the forward [thisForwardDoesntExist] does not exist and cannot be used in this pipe"));
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	public void testForwardWithoutName(String forwardName) throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", null))); // FixedForwardPipe requires a SUCCESS forward
		assertDoesNotThrow(() -> pipe.addForward(new PipeForward(forwardName, null)));

		assertDoesNotThrow(pipe::configure);
		List<String> warnings = getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("pipe contains a forward without a name"));
	}

	@Test
	public void testRegisterSameForwardTwice() throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", "Sergi")));
		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", "Sergi")));

		assertDoesNotThrow(pipe::configure);
		List<String> warnings = getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("the forward [success] is already registered on this pipe"));
	}

	@Test
	public void testRegisterSameForwardDifferentPath() throws ConfigurationException {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			var pipe = new EchoPipe();
			pipe.setName("Echo Pipe");
			pipeline.addPipe(pipe);
			autowireByType(pipe);

			assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", "Sergi1")));
			assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", "Sergi2")));
			assertDoesNotThrow(pipe::configure);

			assertTrue(appender.contains("INFO - PipeForward [success] already registered, pointing to [Sergi1]. Ignoring new one, that points to [Sergi2]"), "Log messages: "+appender.getLogLines());
		}
	}

	@Test
	public void testRegisterKnownForward() throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", null)));
	}

	@Test
	public void testRegisterUnknownWildcardForward() throws ConfigurationException {
		var pipe = new XmlIf();
		pipe.setName("Xml If");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("thisForwardDoesntExist", null)));
	}

	@Test
	public void testRegisterKnownWildcardForward() throws ConfigurationException {
		var pipe = new XmlIf();
		pipe.setName("Xml If");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.addForward(new PipeForward("success", null)));
	}

}
