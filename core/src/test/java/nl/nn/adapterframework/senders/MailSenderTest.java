package nl.nn.adapterframework.senders;

import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.Provider.Type;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.mail.MailSenderTestBase;
import nl.nn.adapterframework.senders.mail.TransportMock;

import org.junit.Test;

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
			public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
				super.sendMessage(correlationID, message, prc);
				prc.getSession().put("mailSession", mailSession);
				return correlationID;
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
