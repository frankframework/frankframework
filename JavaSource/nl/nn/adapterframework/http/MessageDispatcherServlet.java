/*
 * $Log: MessageDispatcherServlet.java,v $
 * Revision 1.1  2005-01-13 16:29:57  L190409
 * first version
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.receivers.ServiceDispatcher;

/**
 * Servlet om te listenen naar messages
 * //TODO: versie maken die naar SOAP berichten luistert en die uitpakt en dan dispatcht
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class MessageDispatcherServlet extends HttpServlet {
	
	private ServiceDispatcher serviceDispatcher = ServiceDispatcher.getInstance();
	
	protected String dispatch(String serviceName, String requestMsg) { 
		return serviceDispatcher.dispatchRequest(serviceName,requestMsg);
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String serviceName = request.getContextPath();
		String responseMsg = dispatch(serviceName,request.getInputStream().toString());
		response.getOutputStream().print(responseMsg);
	}
}
