package org.frankframework.senders.mail;

import jakarta.annotation.Nonnull;
import jakarta.mail.Provider;
import jakarta.mail.Provider.Type;
import jakarta.mail.Session;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.MailSender;
import org.frankframework.stream.Message;

public class MailSenderTest extends MailSenderTestBase<MailSender> {

	@Override
	public MailSender createSender() throws Exception {
		MailSender mailSender = new MailSender() {
			Session mailSession;
			@Override
			protected Session createSession() {
				try {
					mailSession = super.createSession();
					Provider provider = new Provider(Type.TRANSPORT, "smtp", TransportMock.class.getCanonicalName(), "frankframework.org", "1.0");
					mailSession.setProvider(provider);

					return mailSession;
				} catch(Exception e) {
					log.error("unable to create mail Session", e);
					throw new LifecycleException(e);
				}
			}

			@Override
			public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
				super.sendMessage(message, session);
				session.put("mailSession", mailSession);
				String messageID = session.getMessageId();
				return new SenderResult(messageID);
			}
		};

		mailSender.setSmtpHost("localhost");
		mailSender.setUserId("user123");
		mailSender.setPassword("secret321");
		return mailSender;
	}
}
