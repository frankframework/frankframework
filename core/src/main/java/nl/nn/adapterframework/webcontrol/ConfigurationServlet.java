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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Start IAF with a servlet.
 * 
 * @author  Johan Verrips
 * @author  Jaco de Groot
 */
public class ConfigurationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger log = LogUtil.getLogger(this);
	public static final String KEY_CONTEXT = "KEY_CONTEXT";
	IbisContext ibisContext;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletContext servletContext = getServletContext();
		AppConstants appConstants = AppConstants.getInstance();
		String realPath = servletContext.getRealPath("/");
		if (realPath != null) {
			appConstants.put("webapp.realpath", realPath);
		} else {
			log.warn("Could not determine webapp.realpath");
		}
		String projectBaseDir = Misc.getProjectBaseDir();
		if (projectBaseDir != null) {
			appConstants.put("project.basedir", projectBaseDir);
		} else {
			log.info("Could not determine project.basedir");
		}
		setUploadPathInServletContext();
		ibisContext = new IbisContext();
		setDefaultApplicationServerType(ibisContext);
		String attributeKey = appConstants.getResolvedProperty(KEY_CONTEXT);
		servletContext.setAttribute(attributeKey, ibisContext);
		log.debug("stored IbisContext [" + ClassUtils.nameOf(ibisContext) + "]["+ ibisContext + "] in ServletContext under key ["+ attributeKey	+ "]");
		ibisContext.init();
		if(ibisContext.getIbisManager() == null)
			log.warn("Servlet init finished without successfully initializing the ibisContext");
		else
			log.debug("Servlet init finished");
	}

	@Override
	public void destroy() {
		ibisContext.destroy();
		super.destroy();
	}

	private void setUploadPathInServletContext() {
		try {
			// set the directory for struts upload, that is used for instance in 'test a pipeline'
			ServletContext context = getServletContext();
			String path=AppConstants.getInstance().getResolvedProperty("upload.dir");
			// if the path is not found
			if (StringUtils.isEmpty(path)) {
				path="/tmp";
			}
			log.debug("setting path for Struts file-upload to ["+path+"]");
			File tempDirFile = new File(path);
			context.setAttribute("javax.servlet.context.tempdir",tempDirFile);
		} catch (Exception e) {
			log.error("Could not set servlet context attribute 'javax.servlet.context.tempdir' to value of ${upload.dir}",e);
		}
	}

	private void setDefaultApplicationServerType(IbisContext ibisContext) {
		ServletContext context = getServletContext();
		String serverInfo = context.getServerInfo();
		String defaultApplicationServerType = null;
		if (StringUtils.containsIgnoreCase(serverInfo, "WebSphere Liberty")) {
			defaultApplicationServerType = "WLP";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "WebSphere")) {
			defaultApplicationServerType = "WAS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "Tomcat")) {
			defaultApplicationServerType = "TOMCAT";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "JBoss")) {
			defaultApplicationServerType = "JBOSS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "jetty")) {
			String javaHome = AppConstants.getInstance().getString("java.home",
					"");
			if (StringUtils.containsIgnoreCase(javaHome, "tibco")) {
				defaultApplicationServerType = "TIBCOAMX";
			} else {
				defaultApplicationServerType = "JETTYMVN";
			}
		} else {
			ConfigurationWarnings configWarnings = ConfigurationWarnings
					.getInstance();
			configWarnings.add(log, "Unknown server info [" + serverInfo
					+ "] default application server type could not be determined, TOMCAT will be used as default value");
			defaultApplicationServerType = "TOMCAT";
		}
		ibisContext.setDefaultApplicationServerType(defaultApplicationServerType);
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
			ibisContext.init(false);
			if(ibisContext.getIbisManager() == null) {
				response.setStatus(500);
				out.println("failed");
			}
			else {
				response.setStatus(201);
				out.println("success");
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
