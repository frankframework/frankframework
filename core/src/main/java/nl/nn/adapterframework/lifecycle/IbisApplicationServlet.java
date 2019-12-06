/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.lifecycle;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.log4j.Logger;

/**
 * Start IAF with a servlet and register it in the Spring {@link nl.nn.adapterframework.lifecycle.IbisApplicationContext Application Context}.
 * This is important as we want to integrate CXF and run it on the existing {@link org.apache.cxf.bus.spring.SpringBus SpringBus}.
 * 
 * @author Niels Meijer
 * 
 */
public class IbisApplicationServlet extends CXFServlet {

	private static final long serialVersionUID = 1L;

	private Logger log = LogUtil.getLogger(this);
	public static final String KEY_CONTEXT = "KEY_CONTEXT";
	private IbisContext ibisContext;
	private ServletContext servletContext = null;
	private ServletConfig currentConfig = null;

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public ServletConfig getServletConfig() {
		return currentConfig;
	}

	/**
	 * Initializes the {@link nl.nn.adapterframework.lifecycle.IbisApplicationContext Ibis Application Context} 
	 * which will in turn call {@link #doInit()} to initialize the CXF servlet
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.currentConfig = config;
		servletContext = config.getServletContext();
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
		String attributeKey = appConstants.getResolvedProperty(KEY_CONTEXT);
		servletContext.setAttribute(attributeKey, ibisContext);
		log.debug("stored IbisContext [" + ClassUtils.nameOf(ibisContext) + "]["+ ibisContext + "] in ServletContext under key ["+ attributeKey + "]");

		//Create the Spring Application Context first before running the super init() method
		ibisContext.setServletConfig(this);

		//This is the point where we initialize the Spring Context and run super.init() on the CXFServlet, which is triggered by the refresh() method in this class
		//It is important that the servlet loads all busses before the ibis starts up.
		ibisContext.init();

		if(ibisContext.getIbisManager() == null)
			log.warn("Servlet init finished without successfully initializing the ibisContext");
		else
			log.debug("Servlet init finished");

		//Add console warning when security constraints have not been enabled
		String stage = appConstants.getString("otap.stage", "LOC");
		if(appConstants.getBoolean("security.constraint.warning", !"LOC".equalsIgnoreCase(stage))) {
			try {
				String web = "/WEB-INF"+File.separator+"web.xml";
				URL webXml = servletContext.getResource(web);
				if(webXml != null) {
					if(XmlUtils.buildDomDocument(webXml).getElementsByTagName("security-constraint").getLength() < 1)
						ConfigurationWarnings.getInstance().add(log, "unsecure IBIS application, enable the security constraints section in the web.xml in order to secure the application!");
				}
			} catch (Exception e) {
				ConfigurationWarnings.getInstance().add(log, "unable to determine whether security constraints have been enabled, is there a web.xml present?", e);
			}
		}
	}

	/**
	 * Should only be called by {@link nl.nn.adapterframework.lifecycle.IbisApplicationContext#createWebApplicationContext() createWebApplicationContext}
	 * <br/>
	 * Creates the CXF servlet context
	 * @throws ServletException
	 */
	public void doInit() throws ServletException {
		log.info("initalizing CXF Servlet");
		super.init(getServletConfig());
	}

	/**
	 * Destroy the {@link nl.nn.adapterframework.lifecycle.IbisApplicationContext Ibis Application Context}
	 * which will in turn call {@link #doDestroy()} to destroy the CXF servlet
	 */
	@Override
	public void destroy() {
		log.info("shutting down, destroying ibisContext");
		ibisContext.destroy(); //This will in turn call doDestroy()
	}

	/**
	 * Should only be called by {@link nl.nn.adapterframework.lifecycle.IbisApplicationContext#createWebApplicationContext() createWebApplicationContext}
	 * <br/>
	 * Destroys the CXF servlet context
	 */
	public void doDestroy() {
		log.info("destroying CXF Servlet");
		super.destroy();
		setBus(null);
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
}
