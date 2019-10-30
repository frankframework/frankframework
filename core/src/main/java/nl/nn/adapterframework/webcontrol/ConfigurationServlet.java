/*
   Copyright 2013, 2016-2017 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.webcontrol;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;

/**
 * Legacy reload servlet, can be used to manually start the IBIS in case it did not start up succesfully.
 * The Application Context reload functionality has been moved to the web console.
 * <br/></br/>
 * This servlet mainly exists for legacy purposes.
 * 
 * @author  Johan Verrips
 * @author  Jaco de Groot
 */
public class ConfigurationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger log = LogUtil.getLogger(this);
	public static final String KEY_CONTEXT = "KEY_CONTEXT";
	private IbisContext ibisContext;

	@Override
	public void init() throws ServletException {
		String attributeKey = AppConstants.getInstance().getResolvedProperty(KEY_CONTEXT);
		ibisContext = (IbisContext) getServletContext().getAttribute(attributeKey);
		log.debug("retrieved IbisContext [" + ClassUtils.nameOf(ibisContext) + "]["+ ibisContext + "] in ServletContext under key ["+ attributeKey + "]");
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		noCache(response);
		PrintWriter out = response.getWriter();

		if(ibisContext.getIbisManager() != null) {
			out.println("<html>");
			out.println("<body>");
			out.println("Reload function moved to <a href=\"" + request.getContextPath() + "\">console</a>");
			out.println("</body>");
			out.println("</html>");
		}
		else {
			out.print("Attempting to start the IBIS Application... ");
			try {
				ibisContext.init(false);
				response.setStatus(201);
				out.println("success");
			}
			catch (Exception e) {
				response.setStatus(500);
				out.println("failed: " + e.getMessage());
			}
		}
	}

	public static void noCache(HttpServletResponse response) {
		response.setDateHeader("Expires",1);
		response.setDateHeader("Last-Modified",new Date().getTime());
		response.setHeader("Cache-Control","no-store, no-cache, must-revalidate");
		response.setHeader("Pragma","no-cache");
	}

}
