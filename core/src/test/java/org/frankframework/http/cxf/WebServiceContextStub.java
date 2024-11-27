package org.frankframework.http.cxf;

import java.security.Principal;

import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;

public class WebServiceContextStub implements WebServiceContext {

	private final MessageContextStub messageContext = new MessageContextStub();
	@Override
	public EndpointReference getEndpointReference(Element... arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends EndpointReference> T getEndpointReference(Class<T> arg0, Element... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageContext getMessageContext() {
		return messageContext;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
