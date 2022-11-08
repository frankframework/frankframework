package nl.nn.adapterframework.senders;

import org.junit.Test;

import jakarta.mail.Provider;
import jakarta.mail.Provider.Type;
import jakarta.mail.Session;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.mail.MailSenderTestBase;
import nl.nn.adapterframework.senders.mail.TransportMock;
import nl.nn.adapterframework.stream.Message;

public class MailSenderTest extends MailSenderTestBase<MailSender> {

	@Override
	public MailSender createSender() throws Exception {
		MailSender mailSender = new MailSender() {
			Session mailSession;
			@Override
			protected Session createSession() throws SenderException {
				try {
					mailSession = super.createSession();
					Provider provider = new Provider(Type.TRANSPORT, "smtp", TransportMock.class.getCanonicalName(), "IbisSource.org", "1.0");
					mailSession.setProvider(provider);

					return mailSession;
				} catch(Exception e) {
					e.printStackTrace();
					throw new SenderException(e);
				}
			}

			@Override
			public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
				super.sendMessage(message, session);
				session.put("mailSession", mailSession);
				String messageID = session.getMessageId();
				return new SenderResult(messageID);
			}
		};
		mailSender.setSmtpHost("localhost");
		mailSender.setSmtpUserid("user123");
		mailSender.setSmtpPassword("secret321");
		return mailSender;
	}

	@Override
	@Test
	public void mailWithoutBase64Message() throws Exception {
		super.mailWithoutBase64Message();
	}
}
