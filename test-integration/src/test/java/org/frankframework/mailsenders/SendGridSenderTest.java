package org.frankframework.mailsenders;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
