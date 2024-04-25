package org.frankframework.http.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.WebServiceContext;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

import lombok.Getter;
import lombok.SneakyThrows;

public class SoapProviderStub extends SOAPProviderBase {

	public SoapProviderStub(WebServiceContext context) {
		this.webServiceContext = context;
	}

	@Getter PipeLineSession session = null;

	final Map<String, Message> sessionCopy = new HashMap<>();

	@Override
	@SneakyThrows(IOException.class)
	Message processRequest(Message message, PipeLineSession pipelineSession) {
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

	public Message getMessageFromSessionCopy(String key) {
		return sessionCopy.get(key);
	}
}
