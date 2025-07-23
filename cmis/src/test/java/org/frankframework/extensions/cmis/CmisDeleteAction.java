package org.frankframework.extensions.cmis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.core.SenderException;
import org.frankframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import org.frankframework.stream.Message;

public class CmisDeleteAction extends CmisSenderTestBase {
	private static final String ALLOWED_SELECTOR = "<cmis><id>ALLOWED</id></cmis>";
	private static final String NOT_ALLOWED_SELECTOR = "<cmis><id>NOT_ALLOWED</id></cmis>";
	private static final String NOT_FOUND_SELECTOR = "<cmis><id>NOT_FOUND</id></cmis>";

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
		sender.setResultOnNotFound("document-not-found");
		sender.configure();

		Message result = sendMessage(NOT_FOUND_SELECTOR);
		assertThat(result.asString(), Matchers.startsWith("document-not-found"));
	}
}
