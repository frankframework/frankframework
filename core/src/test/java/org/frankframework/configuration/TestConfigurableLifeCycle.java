package org.frankframework.configuration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.RunState;

@Log4j2
public class TestConfigurableLifeCycle {

	private Adapter adapter;
	private TestConfiguration configuration;

	@BeforeEach
	public void setUp() throws Exception {
		configuration = new TestConfiguration();
		adapter = configuration.createBean(Adapter.class);
		adapter.setName("testAdapter");
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		IPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName("echo");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);
		configuration.addAdapter(adapter);
	}

	@AfterEach
	public void tearDown() {
		configuration.close();
	}

	@Test
	public void start() throws Exception {
		// Arrange
		adapter.configure();

		// Act
		adapter.start();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.until(()-> adapter.getRunState() == RunState.STARTED);

		// Assert
		assertTrue(adapter.isRunning());
	}

	@Test
	public void cantStartWithoutConfigure() {
		adapter.start();
		assertFalse(adapter.isRunning());
	}

	@Test
	public void canStartAfterStopped() throws Exception {
		adapter.configure();
		adapter.start();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.until(()-> adapter.getRunState() == RunState.STARTED);
		log.debug("!> started");
		assertTrue(adapter.isRunning());

		// Act
		adapter.stop();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.until(()-> adapter.getRunState() == RunState.STOPPED);
		log.debug("!> stopped");
		// Assert
		assertFalse(adapter.isRunning());

		// Act
		adapter.start();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.until(()-> adapter.getRunState() == RunState.STARTED);
		log.debug("!> started again");

		// Assert
		assertTrue(adapter.isRunning());
	}

	@Test
	public void verifyAdapterIsStoppedAfterConfigurationStop() throws Exception {
		configuration.configure();
		assertTrue(configuration.isConfigured());
		configuration.start();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.until(()-> adapter.getRunState() == RunState.STARTED);
		log.debug("!> started");
		assertTrue(configuration.isRunning());
		assertTrue(adapter.configurationSucceeded());
		assertTrue(adapter.isRunning());

		// Act
		configuration.stop();
		await()
			.atMost(10, TimeUnit.SECONDS)
			.until(()-> adapter.getRunState() == RunState.STOPPED);
		log.debug("!> stopped");
		// Assert
		assertFalse(adapter.isRunning());
		assertFalse(configuration.isRunning());
	}
}
