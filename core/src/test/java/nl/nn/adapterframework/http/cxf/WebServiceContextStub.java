package nl.nn.adapterframework.http.cxf;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;

public class WebServiceContextStub implements WebServiceContext {

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
		return new MessageContext() {
			
			@Override
			public Collection<Object> values() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int size() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public Object remove(Object arg0) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void putAll(Map<? extends String, ? extends Object> arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public Object put(String arg0, Object arg1) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Set<String> keySet() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean isEmpty() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public Object get(Object object) {
				if(MessageContext.HTTP_REQUEST_METHOD.equals(object.toString())) {
					return "POST";
				}
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Set<Entry<String, Object>> entrySet() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean containsValue(Object arg0) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean containsKey(Object arg0) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public void clear() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setScope(String arg0, Scope arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public Scope getScope(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}
		};
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
