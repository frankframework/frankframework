package nl.nn.adapterframework.senders.mail;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;

public class TransportMock extends Transport {
	private final Session session;

	public TransportMock(Session session, URLName urlname) {
		super(session, urlname);
		this.session = session;
	}

	@Override
	public void connect(String host, String username, String password) throws MessagingException {
		session.getProperties().put("login.user", username);
		session.getProperties().put("login.pass", password);
	}

	@Override
	public synchronized void connect(String host, int port, String username, String password) throws MessagingException {
		session.getProperties().put("login.user", username);
		session.getProperties().put("login.pass", password);
	}

	@Override
	public void sendMessage(Message msg, Address[] addresses) throws MessagingException {
		session.getProperties().put("MimeMessage", msg);
	}

	@Override
	public synchronized void close() throws MessagingException {
	}
}
