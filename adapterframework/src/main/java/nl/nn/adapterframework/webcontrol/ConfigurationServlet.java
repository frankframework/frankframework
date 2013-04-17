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
