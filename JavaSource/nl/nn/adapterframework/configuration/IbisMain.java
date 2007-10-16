/*
 * $Log: IbisMain.java,v $
 * Revision 1.5  2007-10-16 13:07:39  europe\M00035F
 * Refactor so that creation of Spring factory is in seperate method, which can be called externally
 *
 * Revision 1.4  2007/10/16 09:12:28  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.3  2007/10/15 13:06:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configuration field (TvdL)
 * Revision 1.1.2.10  2007/10/15 13:01:00  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Suppress message that Ibis internal MBean server is running when it isn't
 *
 * Revision 1.1.2.9  2007/10/15 08:35:29  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix NPE in ConfigurationServlet
 *
 * Revision 1.1.2.8  2007/10/12 14:29:31  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Several fixes and improvements to get EJB deployment mode running
 *
 * Revision 1.1.2.7  2007/10/10 14:30:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.JdkVersion;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Main entry point for creating and starting Ibis instances from
 * the configuration file.
 * 
 * This class can not be created from the Spring context, because it
 * is the place where the Spring context is created.
 * 
 * 
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class IbisMain {
    protected Logger log = LogUtil.getLogger(this);

    public static final String DFLT_AUTOSTART = "TRUE";
    public static final String DFLT_SPRING_CONTEXT = "/springContext.xml";
    
    protected ListableBeanFactory beanFactory;
    protected IbisManager ibisManager;
    
	public static void main(String[] args) {
        IbisMain im=new IbisMain();
        im.initConfig(
                IbisMain.DFLT_SPRING_CONTEXT,
                IbisManager.DFLT_CONFIGURATION, 
                IbisMain.DFLT_AUTOSTART);
	}
    
    /**
     * Initialize Ibis with all default parameters
     * 
     * @return
     */
    public boolean initConfig() {
        return initConfig(
                IbisMain.DFLT_SPRING_CONTEXT,
                IbisManager.DFLT_CONFIGURATION,
                IbisMain.DFLT_AUTOSTART);
    }
    
    /**
     * Initalize Ibis with the given parameters, substituting default
     * values when <code>null</code> is passed in.
     * 
     * This method creates the Spring context, and loads the configuration
     * file. After executing this method, the BeanFactory, IbisManager and Configuration
     * properties are available and the Ibis instance can be started and
     * stopped.
     * 
     * @param springContext
     * @param configurationFile
     * @param autoStart
     * @return
     */
    public boolean initConfig(
            String springContext,
            String configurationFile,
            String autoStart) {
        log.info("* IBIS Startup: Running on JDK version '" 
                + System.getProperty("java.version")
                + "', Spring indicates JDK Major version: 1." + (JdkVersion.getMajorJavaVersion()+3));
        // This should be made conditional, somehow
        startJmxServer();
        
		beanFactory = createBeanFactory(springContext);
        ibisManager = (IbisManager) beanFactory.getBean("ibisManager");
        
        ibisManager.loadConfigurationFile(configurationFile);
        
        if ("TRUE".equalsIgnoreCase(autoStart)) {
            log.info("* IBIS Startup: Starting adapters");
            ibisManager.startIbis();
        }
        log.info("* IBIS Startup: Startup complete");
        return true;
    }

	/**
	 * Create Spring Bean factory. Parameter 'springContext' can be null.
	 * 
	 * Create the Spring Bean Factory using the supplied <code>springContext</code>,
	 * if not <code>null</code>.
	 * 
	 * @param springContext Spring Context to create. If <code>null</code>,
	 * use the default spring context.
	 * The spring context is loaded as a spring ClassPathResource from
	 * the class path.
	 * 
	 * @return The Spring XML Bean Factory.
	 * @throws BeansException If the Factory can not be created.
	 * 
	 */
	public XmlBeanFactory createBeanFactory(String springContext)
		throws BeansException {
		// Reading in Spring Context
		if (springContext == null) {
		    springContext = DFLT_SPRING_CONTEXT;
		}
		log.info("* IBIS Startup: Creating Spring Bean Factory from file '"
		    + springContext + "'");
		Resource rs = new ClassPathResource(springContext);
		XmlBeanFactory bf = new XmlBeanFactory(rs);
		return bf;
	}

	private void startJmxServer() {
		//Start MBean server
        
        // It seems that no reference to the server is required anymore,
        // anywhere later? So no reference is returned from
        // this method.
        log.info("* IBIS Startup: Attempting to start MBean server");
		MBeanServer server=MBeanServerFactory.createMBeanServer();
		try {
		  ObjectInstance html = server.createMBean("com.sun.jdmk.comm.HtmlAdaptorServer", 
		  null);
		    
		  server.invoke(html.getObjectName(), "start", new Object[0], new String[0]);
        } catch (ReflectionException e ) {
            log.error("Requested JMX Server MBean can not be created; JMX not available.");
            return;
        } catch (Exception e) {
		    log.error("Error with jmx:",e);
            return;
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
		return ibisManager.getConfiguration();
	}

	/**
	 * @param factory
	 */
	public void setBeanFactory(ListableBeanFactory factory) {
		beanFactory = factory;
	}

	/**
	 * @return
	 */
	public IbisManager getIbisManager() {
		return ibisManager;
	}

}
