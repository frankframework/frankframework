/*
 * $Log: RestListenerServlet.java,v $
 * Revision 1.5  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.4  2011/11/30 13:52:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/05/23 13:12:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.1  2011/05/19 15:11:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of Rest-provider support
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
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Servlet that listens for REST requests, and handles them over to the RestServiceDispatcher.
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class RestListenerServlet extends HttpServlet {
	protected Logger log=LogUtil.getLogger(this);
	
	private RestServiceDispatcher sd=null;
	
	public void init() throws ServletException {
		super.init();
		if (sd==null) {
			sd= RestServiceDispatcher.getInstance();
		}
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String path=request.getPathInfo();
		String body=Misc.streamToString(request.getInputStream(),"\n",false);
		
		String etag=request.getHeader("etag");
		String contentType=request.getHeader("accept");

		if (log.isDebugEnabled()) log.debug("path ["+path+"] etag ["+etag+"] contentType ["+contentType+"]");
		
		ISecurityHandler securityHandler = new HttpSecurityHandler(request);
		Map messageContext= new HashMap();
		messageContext.put(IPipeLineSession.securityHandlerKey, securityHandler);

		Enumeration paramnames=request.getParameterNames();
		while (paramnames.hasMoreElements()) {
			String paramname = (String)paramnames.nextElement();
			String paramvalue = request.getParameter(paramname);
			if (log.isDebugEnabled()) log.debug("setting parameter ["+paramname+"] to ["+paramvalue+"]");
			messageContext.put(paramname, paramvalue);
		}
		try {
			log.debug("RestListenerServlet calling service ["+path+"]");
			String result=sd.dispatchRequest(path, request.getMethod(), etag, contentType, body, messageContext);
			etag=(String)messageContext.get("etag");
			contentType=(String)messageContext.get("contentType");
			if (StringUtils.isNotEmpty(contentType)) { 
				response.setHeader("Content-Type", contentType); 
			}
			if (StringUtils.isNotEmpty(etag)) { 
				response.setHeader("etag", etag); 
			}
			response.getWriter().print(result);
		} catch (ListenerException e) {
			log.warn("RestListenerServlet caught exception, will rethrow as ServletException",e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
		}
	}
	
}
