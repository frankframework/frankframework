package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.PipeForward;
import org.frankframework.testutil.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

class ForwardHandlingTest extends ConfiguredTestBase {


	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		ApplicationWarnings.removeInstance();
	}

	@Test
	void testFindForwardToPipeExplicit() throws ConfigurationException {
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipe1.registerForward(new PipeForward("fakeForward", "pipe3"));
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
		XmlSwitch pipe1 = new XmlSwitch();
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
		XmlSwitch pipe1 = new XmlSwitch();
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
		XmlSwitch pipe1 = new XmlSwitch();
		pipe1.setName("pipe1");
		pipe1.registerForward(new PipeForward("ready", "READY"));
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
		XmlSwitch pipe1 = new XmlSwitch();
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

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", null)));
		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("thisForwardDoesntExist", null)));

		ConfigurationException ex = assertThrows(ConfigurationException.class, pipe::configure);
		assertEquals("The forward [thisForwardDoesntExist] does not exist and cannot be used in this pipe", ex.getMessage());
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	public void testForwardWithoutName(String forwardName) throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward(forwardName, null))); // FixedForwardPipe requires a SUCCESS forward
		ConfigurationException ex = assertThrows(ConfigurationException.class, pipe::configure);
		assertEquals("forward without a name", ex.getMessage());
	}

	@Test
	public void testRegisterSameForwardTwice() throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", "Sergi")));
		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", "Sergi")));

		ConfigurationException ex = assertThrows(ConfigurationException.class, pipe::configure);
		assertEquals("The forward [success] is already registered on this pipe", ex.getMessage());
	}

	@Test
	public void testRegisterSameForwardDifferentPath() throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", "Sergi1")));
		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", "Sergi2")));

		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
		TestAppender.addToRootLogger(appender);
		try {
			assertDoesNotThrow(pipe::configure);
		} finally {
			TestAppender.removeAppender(appender);
		}
		assertTrue(appender.contains("INFO - PipeForward [success] already registered, pointing to [Sergi1]. Ignoring new one, that points to [Sergi2]"), "Log messages: "+appender.getLogLines());
	}

	@Test
	public void testRegisterKnownForward() throws ConfigurationException {
		var pipe = new EchoPipe();
		pipe.setName("Echo Pipe");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", null)));
	}

	@Test
	public void testRegisterUnknownWildcardForward() throws ConfigurationException {
		var pipe = new XmlIf();
		pipe.setName("Xml If");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("thisForwardDoesntExist", null)));
	}

	@Test
	public void testRegisterKnownWildcardForward() throws ConfigurationException {
		var pipe = new XmlIf();
		pipe.setName("Xml If");
		pipeline.addPipe(pipe);
		autowireByType(pipe);

		assertDoesNotThrow(() -> pipe.registerForward(new PipeForward("success", null)));
	}

}
