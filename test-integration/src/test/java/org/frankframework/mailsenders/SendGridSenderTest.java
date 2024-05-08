package org.frankframework.mailsenders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.core.SenderException;

org.frankframework.core.SenderException;

public class SendGridSenderTest extends MailSenderTestBase<SendGridSenderWrapper> {
	private final String password = "";

	@Override
	public SendGridSenderWrapper createSender() {
		SendGridSenderWrapper ms = new SendGridSenderWrapper();
		ms.setPassword(password);
		return ms;
	}

	@Test
	@Override
	public void testEmptySubject() throws Exception {
		SenderException thrown = assertThrows(SenderException.class, () -> super.testEmptySubject());
		assertTrue(thrown.getMessage().contains("subject is required"));
	}

}
