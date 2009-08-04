/*
 * $Log: Download.java,v $
 * Revision 1.1  2009-08-04 11:42:51  L190409
 * introduced servlet to work around IE 6 issue, that prevented downloading files from a POST.
 *
 */
package nl.nn.adapterframework.webcontrol;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Servlet that redirects a POST that should result in a download to a GET without parameters.
 * This is to work around an issue in Internet Explorer 6 that does not allow downloading from https POSTS.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.8
 * @version Id
 */
public class Download extends HttpServlet {
	protected static Logger log = LogUtil.getLogger(Download.class);

	
	public static final String MY_OWN_URL="/Download";
	public static final String URL_KEY="downloadurl";
	public static final String PARAMETERS_KEY="downloadparameters";
	public static final String FILENAME_KEY="downloadfilename";
	public static final String CONTENTTYPE_KEY="downloadcontenttype";

	public static boolean redirectForDownload(HttpServletRequest request, HttpServletResponse response, String contentType, String filename) throws IOException {
		if (!(request.getMethod().equalsIgnoreCase("POST") && request.getScheme().equals("https")) ) {
			// TODO could test for GET with paramters, too
			return false;
		}

		if (log.isDebugEnabled()) log.debug("redirecting to GET Download, to avoid IE 6 problems with download via https POST");
		String url=request.getRequestURI();
		Map params=new LinkedHashMap();
		
		for (Enumeration enum=request.getParameterNames();enum.hasMoreElements();) {
			String name=(String)enum.nextElement();
			String values[]=request.getParameterValues(name);
			params.put(name,values);
		}
		HttpSession session=request.getSession(true);
		session.setAttribute(URL_KEY,url);
		session.setAttribute(PARAMETERS_KEY,params);
		session.setAttribute(FILENAME_KEY,filename);
		session.setAttribute(CONTENTTYPE_KEY,contentType);
		response.sendRedirect(request.getContextPath()+MY_OWN_URL);
		return true;
	}


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session=request.getSession();
		if (session!=null) {
			String url = (String)session.getAttribute(URL_KEY);
			if (url!=null) {
				String parameters=null;
				Map params=(Map)session.getAttribute(PARAMETERS_KEY);
				for (Iterator it=params.keySet().iterator(); it.hasNext();) {
					String name=(String)it.next();
					String values[]=(String[])params.get(name);
					for (int i=0; i<values.length; i++) {
						if (parameters==null) {
							parameters="?";
						} else {
							parameters+="&";
						}
						parameters+=name+"="+URLEncoder.encode(values[i],response.getCharacterEncoding());
					}
				}
				if (parameters!=null) {
					url+=parameters;
				}
				String context=request.getContextPath();
				if (url.startsWith(context)) {
					url=url.substring(context.length());
				}
				url=response.encodeURL(url);
				if (log.isDebugEnabled()) log.debug("dispatching to ["+url+"]");
				String contenttype=(String)session.getAttribute(CONTENTTYPE_KEY);
				String filename=(String)session.getAttribute(FILENAME_KEY);
				
				session.removeAttribute(URL_KEY);
				session.removeAttribute(PARAMETERS_KEY);
				session.removeAttribute(CONTENTTYPE_KEY);
				session.removeAttribute(FILENAME_KEY);
				
				response.setContentType(contenttype);
				response.setHeader("Content-Disposition","attachment; filename=\""+filename+"\"");
				RequestDispatcher rd=request.getRequestDispatcher(url);
				rd.include(request,response);
			}
		}
	}

}
