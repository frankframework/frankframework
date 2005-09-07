package nl.nn.adapterframework.webcontrol;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;


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
 * @version Id
 * @author    Johan Verrips
 */
public class ConfigurationServlet extends HttpServlet {
	public static final String version = "$RCSfile: ConfigurationServlet.java,v $ $Revision: 1.6 $ $Date: 2005-09-07 15:33:21 $";
	
    // logging category for this class
    protected Logger log = Logger.getLogger(this.getClass());

    static final String DFLT_DIGESTER_RULES = "digester-rules.xml";
    static final String DFLT_CONFIGURATION = "Configuration.xml";
    static final String DFLT_AUTOSTART = "TRUE";
    
    public String lastErrorMessage=null;

    public boolean areAdaptersStopped() {
        Configuration config = getConfiguration();

        if (null != config) {

//check for adapters that are started
            boolean startedAdaptersPresent = false;
            Iterator registeredAdapters = config.getRegisteredAdapterNames();
            while (registeredAdapters.hasNext()) {
                String adapterName = (String) registeredAdapters.next();
                IAdapter adapter = config.getRegisteredAdapter(adapterName);
                RunStateEnum currentRunState = adapter.getRunState();
                if (currentRunState.equals(RunStateEnum.STARTED)) {
                    log.warn("Adapter [" + adapterName + "] is running");
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
            Configuration config = getConfiguration();
            if (config != null)
                config.stopAdapters();
        } catch (Exception e) {
            log("Error stopping adapters on closing", e);
        }
        log.info("************** Configuration shut down successfully **************");
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
        String digesterRulesFile = request.getParameter("digesterRulesFile");
        String autoStart = request.getParameter("autoStart");

        log.warn("ConfigurationServlet initiated by " + commandIssuedBy);
		response.setDateHeader("Expires",1);
		response.setDateHeader("Last-Modified",new Date().getTime());
        response.setHeader("Cache-Control","no-store, no-cache, must-revalidate");
		response.setHeader("Pragma","no-cache");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<body>");


        if (areAdaptersStopped()) {
            boolean success = initConfig(configurationFile, digesterRulesFile, autoStart);
            if (success) {
                out.println("<p> Configuration successfully completed</p></body>");
            } else {
                out.println("<p> Errors occured during configuration. Please, examine logfiles</p>");
                if (StringUtils.isNotEmpty(lastErrorMessage)) {
					out.println("<p>"+lastErrorMessage+"</p>");
                }
            }
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
        config = (Configuration) ctx.getAttribute(AppConstants.getInstance().getProperty("KEY_CONFIGURATION"));
        return config;
    }
    /**
     *  Initialize Servlet.
     *
     */
    public void init() throws ServletException {
        super.init();
        String configurationFile = getInitParameter("configuration");
        String digesterRulesFile = getInitParameter("digester-rules");
        String autoStart = getInitParameter("autoStart");
        if (areAdaptersStopped()) {
            boolean success = initConfig(configurationFile, digesterRulesFile, autoStart);
            if (success)
                log.info("Configuration succeeded");
            else
                log.warn("Configuration did not succeed, please examine log");
        } else
            log.warn("Not all adapters are stopped, cancelling ConfigurationServlet");

    }
    /**
     * Does the actual confguration work. <br/>
     * For parameters that are null, the default values are used.
     * @return true if no errors occured
     */
    public boolean initConfig(
            String configurationFile,
            String digesterRulesFile,
            String autoStart) {
        Configuration config = null;
        ServletContext ctx = getServletContext();

        if (null == configurationFile)
            configurationFile = DFLT_CONFIGURATION;
        if (null == digesterRulesFile)
            digesterRulesFile = DFLT_DIGESTER_RULES;
        if (null == autoStart)
            autoStart = DFLT_AUTOSTART;

        log.info(
                "ConfigurationServlet starting with configurationFile ["
                + configurationFile
                + "] digesterRulesFile ["
                + digesterRulesFile
                + "] autoStart["
                + autoStart
                + "]");
        ConfigurationDigester cd = new ConfigurationDigester();

        System.out.println(
                "servlet starting with configurationfile ["
                + configurationFile
                + "]"
                + " digesterRulesFile ["
                + digesterRulesFile
                + "]"
                + " autoStart ["
                + autoStart
                + "]");

        try {
            config =
                    cd.unmarshalConfiguration(
                            ClassUtils.getResourceURL(this, digesterRulesFile),
                            ClassUtils.getResourceURL(this, configurationFile));
        } catch (Throwable e) {
            log.error("Error occured unmarshalling configuration:", e);
			lastErrorMessage=e.getMessage();
        }

        // if configuration did not succeed, log and return.
        if (null == config) {
            log.error("Error occured unmarshalling configuration. See previous messages.");
            return false;
        }
        // store configuration under key config

        ctx.setAttribute(
                AppConstants.getInstance().getProperty("KEY_CONFIGURATION"),
                config);

        if (autoStart.equalsIgnoreCase("TRUE")) {
            log.info("Starting configuration");
            config.startAdapters();
        }
        log.info("************** ConfigurationServlet complete **************");
        return true;
    }
}
