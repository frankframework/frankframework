/*
 * $Log: HttpListenerServlet.java,v $
 * Revision 1.7  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2011/11/30 13:52:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/05/19 15:10:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use simplified ServiceDispatcher
 *
 * Revision 1.3  2007/10/08 12:18:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.2  2007/02/12 13:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.1  2006/02/09 07:54:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial versions
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.Logger;

/**
 * Servlet that listens for HTTP GET or POSTS, and handles them over to the ServiceDispatcher
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.x (still experimental)
 * @version Id
 */
public class HttpListenerServlet extends HttpServlet {
	protected Logger log=LogUtil.getLogger(this);
	
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
		Map messageContext= new HashMap();
		messageContext.put(IPipeLineSession.securityHandlerKey, securityHandler);
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
			String result=sd.dispatchRequest(service, null, message, messageContext);
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
