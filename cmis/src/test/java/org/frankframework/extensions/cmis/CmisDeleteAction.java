package org.frankframework.extensions.cmis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import org.frankframework.stream.Message;

public class CmisDeleteAction extends CmisSenderTestBase {
	private static final String ALLOWED_SELECTOR = "<cmis><id>ALLOWED</id></cmis>";
	private static final String NOT_ALLOWED_SELECTOR = "<cmis><id>NOT_ALLOWED</id></cmis>";
	private static final Message NOT_FOUND_SELECTOR = new Message("<cmis><id>NOT_FOUND</id></cmis>");

	@Test
	public void canConfigure() {
		sender.setBindingType(BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DELETE);

		assertDoesNotThrow(sender::configure);
	}

	@Test
	public void notAllowedToDelete() {
		sender.setBindingType(BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DELETE);
		assertDoesNotThrow(sender::configure);

		SenderException exception = assertThrows(
			SenderException.class, () -> {
				sendMessage(NOT_ALLOWED_SELECTOR);
			}
		);
		assertThat(exception.getMessage(), Matchers.endsWith("Document cannot be deleted"));
	}

	@Test
	public void deleteObject() throws Exception {
		sender.setBindingType(BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DELETE);
		sender.configure();

		Message result = sendMessage(ALLOWED_SELECTOR);
		assertThat(result.asString(), Matchers.startsWith("testmessage"));
	}

	@Test
	public void objectNotFound() throws Exception {
		sender.setBindingType(BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DELETE);
		sender.configure();

		SenderResult result = sender.sendMessage(NOT_FOUND_SELECTOR, session);

		assertTrue(Message.isNull(result.getResult()));
		assertThat(result.getErrorMessage(), Matchers.startsWith("document with id ["));
		assertThat(result.getErrorMessage(), Matchers.endsWith("] not found"));
	}
}
