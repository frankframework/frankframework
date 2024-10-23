package org.frankframework.extensions.sap.jco3;

import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class IDocSenderTest {

	private final String sapSystemName = "sap-system";

	private IdocSender sender;
	private SapSystem sapSystem;

	@BeforeEach
	public void setUp() {
		GlobalListItem.clear();

		sender = new IdocSender();
		sender.setName(sender.getClass().getSimpleName()+" under test");

		sapSystem = new SapSystem();
		sapSystem.setName(sapSystemName);
		GlobalListItem.registerItem(sapSystem);
	}

	@Test
	public void testSendMessage() {
		sender.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> sender.configure());
	}

}
