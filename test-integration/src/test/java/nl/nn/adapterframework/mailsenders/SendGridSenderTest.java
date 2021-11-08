package nl.nn.adapterframework.mailsenders;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.core.SenderException;

public class SendGridSenderTest extends MailSenderTestBase<SendGridSenderWrapper> {
	private String password = "";

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
