package nl.nn.adapterframework.senders.mail;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

public class TransportMock extends Transport {
	private Session session;

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
