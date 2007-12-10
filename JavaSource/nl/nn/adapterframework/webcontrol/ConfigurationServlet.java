/*
 * $Log: ConfigurationServlet.java,v $
 * Revision 1.13  2007-12-10 10:24:16  europe\L190409
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisMain;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

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
 * @version Id
 */
public class ConfigurationServlet extends HttpServlet {
	public static final String version = "$RCSfile: ConfigurationServlet.java,v $ $Revision: 1.13 $ $Date: 2007-12-10 10:24:16 $";
    protected Logger log = LogUtil.getLogger(this);

	public static final String KEY_MANAGER = "KEY_MANAGER";

    static final String DFLT_SPRING_CONTEXT = "springContext.xml";
    static final String EJB_SPRING_CONTEXT = "springContextEjbWeb.xml";
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
            	log.error("Cannot find configuration to shutdown");
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
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String commandIssuedBy = " remoteHost [" + request.getRemoteHost() + "]";
        commandIssuedBy += " remoteAddress [" + request.getRemoteAddr() + "]";
        commandIssuedBy += " remoteUser [" + request.getRemoteUser() + "]";

        String configurationFile = request.getParameter("configurationFile");
        String autoStart = request.getParameter("autoStart");
        String springContext = request.getParameter("springContext");

        log.warn("ConfigurationServlet initiated by " + commandIssuedBy);
		noCache(response);
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");


        if (areAdaptersStopped()) {
            IbisMain im=new IbisMain();
            boolean success = im.initConfig(springContext, configurationFile, autoStart);
            if (success) {
                out.println("<p> Configuration successfully completed</p></body>");
            } else {
                out.println("<p> Errors occured during configuration. Please, examine logfiles</p>");
                if (StringUtils.isNotEmpty(lastErrorMessage)) {
					out.println("<p>"+lastErrorMessage+"</p>");
                }
            }
            ServletContext ctx = getServletContext();
            ctx.setAttribute(AppConstants.getInstance().getProperty(KEY_MANAGER), im.getIbisManager());
        } else {
            out.println(
                    "<p>Action cancelled: some adapters are still running.</p>");
        }
        out.println("Click <a href=\"" + request.getContextPath() + "\">" + "here</a> to return");
        out.println("</body>");
        out.println("</html>");

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
    
    public IbisManager getIbisManager() {
        ServletContext ctx = getServletContext();
        IbisManager manager = null;
        manager = (IbisManager) ctx.getAttribute(AppConstants.getInstance().getResolvedProperty(KEY_MANAGER));
        return manager;
    }
    
    /**
     *  Initialize Servlet.
     *
     */
    public void init() throws ServletException {
        super.init();
        IbisMain im=new IbisMain();
        String configurationFile = getInitParameter("configuration");
        String autoStart = getInitParameter("autoStart");
        String springContext = getInitParameter("springContext");
        
        if (areAdaptersStopped()) {
            boolean success = im.initConfig(springContext, configurationFile, autoStart);
            if (success) {
				log.info("Configuration succeeded");
            }
            else {
				log.warn("Configuration did not succeed, please examine log");
            }
            ServletContext ctx = getServletContext();
            String attributeKey=AppConstants.getInstance().getResolvedProperty(KEY_MANAGER);
            ctx.setAttribute(attributeKey, im.getIbisManager());
			log.debug("stored IbisManager ["+ClassUtils.nameOf(im.getIbisManager())+"]["+im.getIbisManager()+"] in ServletContext under key ["+attributeKey+"]");
            log.debug("Servlet init finished");
        } else
            log.warn("Not all adapters are stopped, cancelling ConfigurationServlet");

    }
    
}
