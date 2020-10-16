package nl.nn.adapterframework.http.cxf;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;

public class SoapProviderStub extends SOAPProviderBase {

	IPipeLineSession session = null;

	@Override
	String processRequest(String correlationId, String message, IPipeLineSession pipelineSession) throws ListenerException {
		if(session != null)
			pipelineSession.putAll(session);

		session = pipelineSession;
		return message;
	}

	public void setSession(IPipeLineSession session) {
		this.session = session;
	}

	public IPipeLineSession getSession() {
		return session;
	}
}
