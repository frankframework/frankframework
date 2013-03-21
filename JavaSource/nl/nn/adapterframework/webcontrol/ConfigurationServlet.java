/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: ConfigurationServlet.java,v $
 * Revision 1.20  2011-11-30 13:51:58  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.18  2010/09/07 15:55:14  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.17  2009/12/22 16:42:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set Struts upload-directory in init
 *
 * Revision 1.16  2009/10/30 15:31:07  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Run IBIS on Tomcat
 *
 * Revision 1.15  2008/02/13 13:37:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed IbisMain to IbisContext
 *
 * Revision 1.14  2008/02/08 09:50:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * loadConfig in a separate method
 *
 * Revision 1.13  2007/12/10 10:24:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * style fix
 *
 * Revision 1.12  2007/11/22 09:18:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extended shutdown
 *
 * Revision 1.11.2.1  2007/10/25 08:36:58  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add shutdown method for IBIS which shuts down the scheduler too, and which unregisters all EjbJmsConfigurators from the ListenerPortPoller.
 * Unregister JmsListener from ListenerPortPoller during ejbRemove method.
 * Both changes are to facilitate more proper shutdown of the IBIS adapters.
 *
 * Revision 1.11  2007/10/10 09:43:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * version using IbisManager
 *
 * Revision 1.10.2.6  2007/10/05 13:09:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix NPE which happened when IbisManager was not yet set in the ServletContext, as happens during init
 *
 * Revision 1.10.2.5  2007/10/05 12:57:44  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor so that spring-context to be used can come from servlet Init or Request parameters
 *
 * Revision 1.10.2.4  2007/10/05 09:09:57  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Update web front-end to retrieve Configuration-object via the IbisManager, not via the servlet-context
 *
 * Revision 1.10.2.3  2007/10/02 14:15:29  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Further refactoring code further for adding in EJBs and enabling communication of web-front end with EJB-driven back-end.
 *
 * Revision 1.10.2.2  2007/09/26 06:05:19  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add exception-propagation to new JMS Listener; increase robustness of JMS configuration
 *
 * Revision 1.10.2.1  2007/09/13 13:27:18  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * First commit of work to use Spring for creating objects
 *
 * Revision 1.10  2007/07/19 15:20:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configure adapters in order of declaration
 *
 * Revision 1.9  2007/06/26 06:58:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improve logging
 *
 * Revision 1.8  2007/02/12 14:40:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.StringResolver;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * Servlet that attempts to load the <code>Configuration</code>from
 * the configuration file specified in web.xml (default: Configuration.xml).
 *
 * Parameters in the application descriptor 'web.xml':<br/>
 * <ul>
 *   <li>configuration - the name of the configuration file</li>
 *   <li>digester-rules - the name of the digester-rules file</li>
 *   <li>autoStart - when TRUE : automatically start the adapters</li>
 * </ul>
 * 
 * @author  Johan Verrips
 * @version $Id$
 */
public class ConfigurationServlet extends HttpServlet {
	public static final String version = "$RCSfile: ConfigurationServlet.java,v $ $Revision: 1.20 $ $Date: 2011-11-30 13:51:58 $";
    protected Logger log = LogUtil.getLogger(this);

	public static final String KEY_CONTEXT = "KEY_CONTEXT";

    //static final String DFLT_SPRING_CONTEXT = "springContext.xml";
    //static final String EJB_SPRING_CONTEXT = "springContextEjbWeb.xml";
    static final String DFLT_CONFIGURATION = "Configuration.xml";
    static final String DFLT_AUTOSTART = "TRUE";
    
    public String lastErrorMessage=null;

    public boolean areAdaptersStopped() {
        Configuration config = getConfiguration();

        if (null != config) {

			//check for adapters that are started
            boolean startedAdaptersPresent = false;
			for(int i=0; i<config.getRegisteredAdapters().size(); i++) {
				IAdapter adapter = config.getRegisteredAdapter(i);

				RunStateEnum currentRunState = adapter.getRunState();
				if (currentRunState.equals(RunStateEnum.STARTED)) {
					log.warn("Adapter [" + adapter.getName() + "] is running");
					startedAdaptersPresent = true;
				}
			}
            if (startedAdaptersPresent) {
                log.warn(
                        "Reload of configuration aborted because there are started adapters present");
                return false;
            }
            return true;

        }
        return true;
    }
    
    /**
     * Shuts down the configuration, meaning that all adapters are stopped.
     * @since 4.0
     */
    public void destroy() {
        log.info("************** Configuration shutting down **************");
        try {
            IbisManager ibisManager = getIbisManager();
            if (ibisManager != null) {
				ibisManager.shutdownIbis();
            } else {
            	log.error("Cannot find manager to shutdown");
            }
			IbisContext ibisContext = getIbisContext();
			if (ibisContext != null) {
				ibisContext.destroyConfig();
			} else {
				log.error("Cannot find configuration to destroy");
			}
         } catch (Exception e) {
            log("Error stopping adapters on closing", e);
        }
        log.info("************** Configuration shut down successfully **************");
    }
    
    public static void noCache(HttpServletResponse response) {
		response.setDateHeader("Expires",1);
		response.setDateHeader("Last-Modified",new Date().getTime());
		response.setHeader("Cache-Control","no-store, no-cache, must-revalidate");
		response.setHeader("Pragma","no-cache");
    }

	public void init() throws ServletException {
		super.init();
		setUploadPathInServletContext();
		loadConfig();
		log.debug("Servlet init finished");
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

    /**
     * Initializes the configuration. Request parameters are used.
     * Request parameters:
     * <ul>
     * <li>configurationFile: the name of the configuration file</li>
     * <li>digesterRulesFile: the name of the digester rules file</li>
     * <li>autoStart: true or false, indicating to start the adapters on startup.</li>
     * </ul>
     * @since 4.0
     * @param  request  the request
     * @param  response  the response
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)  throws IOException, ServletException {
        String commandIssuedBy = " remoteHost [" + request.getRemoteHost() + "]";
        commandIssuedBy += " remoteAddress [" + request.getRemoteAddr() + "]";
        commandIssuedBy += " remoteUser [" + request.getRemoteUser() + "]";

        log.warn("ConfigurationServlet initiated by " + commandIssuedBy);
		noCache(response);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");

		if (loadConfig()) {
			out.println("<p> Configuration successfully completed</p></body>");
		} else {
			out.println("<p> Errors occured during configuration. Please, examine logfiles</p>");
			if (StringUtils.isNotEmpty(lastErrorMessage)) {
				out.println("<p>"+lastErrorMessage+"</p>");
			}
		}
        out.println("Click <a href=\"" + request.getContextPath() + "\">" + "here</a> to return");
        out.println("</body>");
        out.println("</html>");

    }
 
	private boolean loadConfig() {
		if (areAdaptersStopped()) {
			IbisContext ibisContext = new IbisContext();
			String configurationFile = getInitParameter("configuration");
			String autoStart = getInitParameter("autoStart");
			String springContext = getInitParameter("springContext");
			boolean success = ibisContext.initConfig(springContext, configurationFile, autoStart);
			if (success) {
				log.info("Configuration succeeded");
			} else {
				log.warn("Configuration did not succeed, please examine log");
			}
			ServletContext ctx = getServletContext();
			String attributeKey = AppConstants.getInstance().getResolvedProperty(KEY_CONTEXT);
			ctx.setAttribute(attributeKey, ibisContext);
			log.debug("stored IbisContext [" + ClassUtils.nameOf(ibisContext) + "]["+ ibisContext + "] in ServletContext under key ["+ attributeKey	+ "]");
			return success;
		} else {
			log.warn("Not all adapters are stopped, cancelling ConfigurationServlet");
			lastErrorMessage= "Action cancelled: some adapters are still running.";
			return false;
		}
	}
  

    
    public Configuration getConfiguration() {
        ServletContext ctx = getServletContext();
        Configuration config = null;
        IbisManager ibisManager = getIbisManager();
        if (ibisManager != null) {
            config = ibisManager.getConfiguration();
        }
        return config;
    }
    
	public IbisContext getIbisContext() {
		ServletContext ctx = getServletContext();
		IbisContext context = null;
		context = (IbisContext) ctx.getAttribute(AppConstants.getInstance().getResolvedProperty(KEY_CONTEXT));
		return context;
	}
    
    public IbisManager getIbisManager() {
        IbisContext ibisContext = getIbisContext();
        IbisManager manager = null;
        if (ibisContext != null) {
			manager = ibisContext.getIbisManager();
        }
        return manager;
    }
    
}
