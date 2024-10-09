package org.frankframework.extensions.sap;

import org.frankframework.configuration.ConfigurationException;

import org.frankframework.parameters.Parameter;

import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SapSenderTest {

	private final String sapSystemName = "sap-system-1";
	
	private SapSender pipe;
	private SapSystem sapSystem;

	@BeforeEach
	public void setUp() {
		GlobalListItem.clear();

		pipe = new SapSender();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		
		sapSystem = new SapSystem();
		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(pipe);
	}

	@Test
	public void testNoFunctionName() {
		pipe.setFunctionName(null);
		pipe.setSapSystemName(sapSystemName);

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testFunctionNameAttributeAndParameter() {
		pipe.setFunctionName("function-name");
		pipe.setSapSystemName(sapSystemName);
		pipe.addParameter(new Parameter("functionName", "function-name"));

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemDoesntExist() {
		pipe.setFunctionName("function-name");

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemExists() {
		pipe.setFunctionName("function-name");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
	}

}
