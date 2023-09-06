package nl.nn.adapterframework.http.cxf;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import lombok.SneakyThrows;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class SoapProviderStub extends SOAPProviderBase {

	PipeLineSession session = null;

	final Map<String, Message> sessionCopy = new HashMap<>();

	@Override
	@SneakyThrows
	Message processRequest(Message message, PipeLineSession pipelineSession) throws ListenerException {
		if(session != null) {
			pipelineSession.putAll(session);
			session.getCloseables().clear();
		}
		session = pipelineSession;
		for (String key : session.keySet()) {
			if (!(session.get(key) instanceof InputStream)) {
				sessionCopy.put(key, session.getMessage(key).copyMessage());
			}
		}
		return message;
	}

	public void setSession(PipeLineSession session) {
		this.session = session;
	}

	public PipeLineSession getSession() {
		return session;
	}

	public Message getMessageFromSessionCopy(String key) {
		return sessionCopy.get(key);
	}
}
