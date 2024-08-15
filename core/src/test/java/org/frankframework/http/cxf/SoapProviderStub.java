package org.frankframework.http.cxf;

import jakarta.xml.ws.WebServiceContext;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

import lombok.Getter;

public class SoapProviderStub extends SOAPProviderBase {

	final @Getter PipeLineSession session;

	public SoapProviderStub(WebServiceContext context) {
		this.webServiceContext = context;
		session = new PipeLineSession();
	}

	@Override
	Message processRequest(Message message, PipeLineSession pipelineSession) {
		pipelineSession.putAll(session);

		pipelineSession.mergeToParentSession("*", session);
		return message;
	}

	public void setSession(PipeLineSession session) {
		this.session.putAll(session);
	}

	public void setMultipartBackwardsCompatibilityMode(boolean legacyAttachmentNotation) {
		multipartBackwardsCompatibilityMode = legacyAttachmentNotation;
	}
}
