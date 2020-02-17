package nl.nn.adapterframework.senders;

import java.io.IOException;

import javax.mail.Provider;
import javax.mail.Provider.Type;
import javax.mail.Session;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
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
			public Message sendMessage(String correlationID, Message message, IPipeLineSession session) throws SenderException, TimeOutException, IOException {
				super.sendMessage(correlationID, message, session);
				session.put("mailSession", mailSession);
				return new Message(correlationID);
			}
		};
		mailSender.setSmtpHost("localhost");
		mailSender.setSmtpUserid("user123");
		mailSender.setSmtpPassword("secret321");
		return mailSender;
	}

	@Override
	protected String getTestRootFolder() {
		return "/MailSender/";
	}

	@Override
	@Test
	public void mailWithoutBase64Message() throws Exception {
		super.mailWithoutBase64Message();
	}
}
