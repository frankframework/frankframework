/*
 * $Log: HttpListenerServlet.java,v $
 * Revision 1.1  2006-02-09 07:54:04  europe\L190409
 * initial versions
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.Misc;

/**
 * Servlet that listens for HTTP GET or POSTS, and handles them over to the ServiceDispatcher
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.x (still experimental)
 * @version Id
 */
public class HttpListenerServlet extends HttpServlet {
	public static final String version = "$RCSfile: HttpListenerServlet.java,v $ $Revision: 1.1 $ $Date: 2006-02-09 07:54:04 $";
	protected Logger log=Logger.getLogger(this.getClass());
	
	public final String SERVICE_ID_PARAM = "service";
	public final String MESSAGE_PARAM = "message";

	private ServiceDispatcher sd=null;
	
	public void init() throws ServletException {
		super.init();
		if (sd==null) {
			sd= ServiceDispatcher.getInstance();
		}
	}
	

	public void invoke(String message, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		ISecurityHandler securityHandler = new HttpSecurityHandler(request);
		HashMap messageContext= new HashMap();
		messageContext.put(PipeLineSession.securityHandlerKey, securityHandler);
		String service=request.getParameter(SERVICE_ID_PARAM);
		Enumeration paramnames=request.getParameterNames();
		while (paramnames.hasMoreElements()) {
			String paramname = (String)paramnames.nextElement();
			String paramvalue = request.getParameter(paramname);
			if (log.isDebugEnabled()) {
				log.debug("HttpListenerServlet setting parameter ["+paramname+"] to ["+paramvalue+"]");
			}
			messageContext.put(paramname, paramvalue);
		}
		try {
			log.debug("HttpListenerServlet calling service ["+service+"]");
			String result=sd.dispatchRequestWithExceptions(service, null, message, messageContext);
			response.getWriter().print(result);
		} catch (ListenerException e) {
			log.warn("HttpListenerServlet caught exception, will rethrow as ServletException",e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String message=request.getParameter(MESSAGE_PARAM);
		invoke(message,request,response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String message=Misc.streamToString(request.getInputStream(),"\n",false);
		invoke(message,request,response);
	}
	
}
