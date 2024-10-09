package org.frankframework.extensions.sap;

import org.frankframework.configuration.ConfigurationException;

import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SapListenerTest {

	private final String sapSystemName = "sap-system-1";

	private SapListener pipe;
	private SapSystem sapSystem;

	@BeforeEach
	public void setUp() {
		GlobalListItem.clear();

		pipe = new SapListener();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");

		sapSystem = new SapSystem();
		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(pipe);
	}

	@Test
	public void testNoFunctionName() {
		pipe.setProgid(null);

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemDoesntExist() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName("doesnt-exist");

		assertThrows(NullPointerException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemExists() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
	}

}
