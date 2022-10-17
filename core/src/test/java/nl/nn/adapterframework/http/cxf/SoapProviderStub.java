package nl.nn.adapterframework.http.cxf;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.stream.Message;

public class SoapProviderStub extends SOAPProviderBase {

	PipeLineSession session = null;

	@Override
	Message processRequest(Message message, PipeLineSession pipelineSession) throws ListenerException {
		if(session != null)
			pipelineSession.putAll(session);

		session = pipelineSession;
		return message;
	}

	public void setSession(PipeLineSession session) {
		this.session = session;
	}

	public PipeLineSession getSession() {
		return session;
	}
}
