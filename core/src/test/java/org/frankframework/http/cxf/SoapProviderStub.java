package org.frankframework.http.cxf;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

import lombok.SneakyThrows;

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
				// Do not copy input-streams b/c then they are no longer readable
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
