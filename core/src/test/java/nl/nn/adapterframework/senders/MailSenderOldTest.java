package nl.nn.adapterframework.senders;

import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.Provider.Type;

import org.junit.Test;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.mail.MailSenderTestBase;
import nl.nn.adapterframework.senders.mail.TransportMock;

public class MailSenderOldTest extends MailSenderTestBase<MailSenderOld> {

	@Override
	public MailSenderOld createSender() throws Exception {
		MailSenderOld mailSender = new MailSenderOld() {
			Session mailSession;
			@Override
			protected Session getSession() {
				try {
					mailSession = super.getSession();
					Provider provider = new Provider(Type.TRANSPORT, "smtp", TransportMock.class.getCanonicalName(), "IbisSource.org", "1.0");
					mailSession.setProvider(provider);
	
					return mailSession;
				} catch(Exception e) {
					e.printStackTrace();
					throw new IllegalStateException("no mail session", e);
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
		return "/MailSenderOld/";
	}

	@Test(expected = NullPointerException.class)
	public void noRecipient() throws Exception {
		super.noRecipient();
	}
}
