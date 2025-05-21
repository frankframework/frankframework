package org.frankframework.http.cxf;

import jakarta.xml.soap.SOAPMessage;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

public class SoapProviderStub extends AbstractSOAPProvider {

	final @Getter PipeLineSession session;

	public SoapProviderStub() {
		session = new PipeLineSession();
	}

	@Override
	Message processRequest(SOAPMessage message, PipeLineSession pipelineSession) {
		pipelineSession.putAll(session);

		pipelineSession.mergeToParentSession("*", session);
		return parseSOAPMessage(message);
	}

	public void setSession(PipeLineSession session) {
		this.session.putAll(session);
	}

	public void setMultipartBackwardsCompatibilityMode(boolean legacyAttachmentNotation) {
		multipartBackwardsCompatibilityMode = legacyAttachmentNotation;
	}
}
