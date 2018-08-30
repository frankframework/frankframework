/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class CmisServletRouter extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger log = LogUtil.getLogger(this);
	private HttpServlet servlet;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String servletClass = config.getInitParameter("servlet-class");
		String version = config.getInitParameter("cmisVersion");

		try {
			servlet = (HttpServlet) ClassUtils.newInstance(servletClass);
			servlet.init(config);

			log.debug("initialize CMIS servlet class[" + servletClass + "] version[" + version + "] name[" +config.getServletName()+"]");
		} catch (ClassNotFoundException e) {
			log.debug("CMIS not found, skipping cmis servlet ["+servletClass+"]");
		} catch (UnsupportedClassVersionError e) {
			log.error("CMIS was found on the classpath but requires Java 1.7 or higher to run. Unabled to initialize servlet ["+servletClass+"]");
		} catch (Exception e) {
			log.error("unhandled exception occured while loading or initiating cmis servlet ["+servletClass+"]", e);
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		if(servlet != null) {
			servlet.service(req, res);
		}
		else {
			log.warn("unable to route to cmis servlet ["+getInitParameter("servlet-class")+"]");
			HttpServletResponse resp = (HttpServletResponse) res;
			resp.sendError(404, "cmis depencencies not installed");
		}
	}
}
