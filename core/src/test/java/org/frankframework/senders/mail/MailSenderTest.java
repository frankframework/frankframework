package org.frankframework.senders.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Properties;

import jakarta.annotation.Nonnull;
import jakarta.mail.Provider;
import jakarta.mail.Provider.Type;
import jakarta.mail.Session;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.MailSender;
import org.frankframework.stream.Message;

public class MailSenderTest extends MailSenderTestBase<MailSender> {

	private MailSender createBareMailSender(int port, boolean useSsl) throws Exception {
		MailSender mailSender = new MailSender();
		mailSender.setSmtpHost("localhost");
		mailSender.setSmtpPort(port);
		mailSender.setUseSsl(useSsl);
		getConfiguration().autowireByType(mailSender);
		mailSender.configure();
		return mailSender;
	}

	@Test
	public void testSslPort465EnablesImplicitSsl() throws Exception {
		MailSender mailSender = createBareMailSender(465, true);

		Properties properties = mailSender.getProperties();
		assertEquals("true", properties.get("mail.smtp.ssl.enable"));
		assertNull(properties.get("mail.smtp.starttls.enable"));
		assertNull(properties.get("mail.smtp.starttls.required"));
	}

	@Test
	public void testSslPort587EnablesStartTls() throws Exception {
		MailSender mailSender = createBareMailSender(587, true);

		Properties properties = mailSender.getProperties();
		assertEquals("true", properties.get("mail.smtp.starttls.enable"));
		assertEquals("true", properties.get("mail.smtp.starttls.required"));
		assertNull(properties.get("mail.smtp.ssl.enable"));
	}

	@Test
	public void testSslDisabledSetsNoSslProperties() throws Exception {
		MailSender mailSender = createBareMailSender(465, false);

		Properties properties = mailSender.getProperties();
		assertNull(properties.get("mail.smtp.ssl.enable"));
		assertNull(properties.get("mail.smtp.starttls.enable"));
		assertNull(properties.get("mail.smtp.starttls.required"));
	}

	@Test
	public void testSslEnabledOnUnrecognizedPortSetsNoSslProperties() throws Exception {
		MailSender mailSender = createBareMailSender(25, true);

		Properties properties = mailSender.getProperties();
		assertNull(properties.get("mail.smtp.ssl.enable"));
		assertNull(properties.get("mail.smtp.starttls.enable"));
		assertNull(properties.get("mail.smtp.starttls.required"));
	}

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
