/*
 * Created on 5-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.configuration;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class IbisMain {
    protected Logger log = LogUtil.getLogger(this);

    public static final String DFLT_DIGESTER_RULES = "digester-rules.xml";
    public static final String DFLT_CONFIGURATION = "Configuration.xml";
    public static final String DFLT_AUTOSTART = "TRUE";
    public static final String DFLT_SPRING_CONTEXT = "/springContext.xml";
    
    protected ListableBeanFactory beanFactory;
    protected Configuration configuration;
    protected IbisManager ibisManager;
    
	public static void main(String[] args) {
        IbisMain im=new IbisMain();
        im.initConfig(DFLT_CONFIGURATION, DFLT_DIGESTER_RULES, DFLT_AUTOSTART);
	}
    
    public boolean initConfig() {
        return initConfig(IbisMain.DFLT_CONFIGURATION, 
            IbisMain.DFLT_DIGESTER_RULES, IbisMain.
            DFLT_AUTOSTART);
    }
    
    public boolean initConfig(
        String configurationFile,
        String digesterRulesFile,
        String autoStart) {
        // This should be made conditional, somehow
        startJmxServer();
        
        // Reading in Spring Context
        Resource rs = new ClassPathResource(DFLT_SPRING_CONTEXT);
        beanFactory = new XmlBeanFactory(rs);
        
        ConfigurationDigester cd = (ConfigurationDigester) beanFactory.getBean("configurationDigester");
        ibisManager = (IbisManager) beanFactory.getBean("ibisManager");
        
        // Reading in Apache Digester configuration file
        if (null == configurationFile)
            configurationFile = DFLT_CONFIGURATION;
        if (null == digesterRulesFile)
            digesterRulesFile = DFLT_DIGESTER_RULES;
        if (null == autoStart)
            autoStart = DFLT_AUTOSTART;
        
        log.info("* IBIS Startup: Reading IBIS configuration from file '"
            + configurationFile + "'" + (configurationFile == DFLT_CONFIGURATION ?
            " (default configuration file)" : ""));
        try {
            configuration =
                cd.unmarshalConfiguration(
                    ClassUtils.getResourceURL(cd, digesterRulesFile),
                    ClassUtils.getResourceURL(cd, configurationFile));
        } catch (Throwable e) {
            log.error("Error occured unmarshalling configuration:", e);
        }
        // if configuration did not succeed, log and return.
        if (null == configuration) {
            log.error(
                "Error occured unmarshalling configuration. See previous messages.");
            return false;
        }

        if (autoStart.equalsIgnoreCase("TRUE")) {
            log.info("Starting configuration");
            ibisManager.startAdapters();
        }
        log.info(
            "****" + "********** Configuration complete **************");
        return true;
    }

	private void startJmxServer() {
		//Start MBean server
        
        // It seems that no reference to the server is required anymore,
        // anywhere later? So no reference is returned from
        // this method.
		MBeanServer server=MBeanServerFactory.createMBeanServer();
		try {
		  ObjectInstance html = server.createMBean("com.sun.jdmk.comm.HtmlAdaptorServer", 
		  null);
		    
		  server.invoke(html.getObjectName(), "start", new Object[0], new String[0]);
		} catch (Exception e) {
		    log.error("Error with jmx:",e);
		}
		log.info("MBean server up and running. Monitor your application by pointing your browser to http://localhost:8082");
	}
    
	/**
	 * @return
	 */
	public ListableBeanFactory getBeanFactory() {
		return beanFactory;
	}

	/**
	 * @return
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * @param factory
	 */
	public void setBeanFactory(ListableBeanFactory factory) {
		beanFactory = factory;
	}

	/**
	 * @param configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * @return
	 */
	public IbisManager getIbisManager() {
		return ibisManager;
	}

}
