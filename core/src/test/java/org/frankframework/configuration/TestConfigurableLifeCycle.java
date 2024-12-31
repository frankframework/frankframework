package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestConfigurableLifeCycle {

	@Test
	public void start() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		assertTrue(manager.isRunning());

		manager.close();
	}

	@Test
	public void canStop() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		manager.stop();
		assertFalse(manager.isRunning());

		manager.close();
	}

	@Test
	public void canStartAfterStopped() {
		AdapterManager manager = new AdapterManager();
		manager.configure();
		manager.start();
		manager.stop();

		manager.start();
		assertTrue(manager.isRunning());

		manager.close();
	}
}
