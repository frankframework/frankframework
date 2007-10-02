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
import javax.management.ReflectionException;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.JdkVersion;
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

    public static final String DFLT_AUTOSTART = "TRUE";
    public static final String DFLT_SPRING_CONTEXT = "/springContext.xml";
    
    protected ListableBeanFactory beanFactory;
    protected Configuration configuration;
    protected IbisManager ibisManager;
    
	public static void main(String[] args) {
        IbisMain im=new IbisMain();
        im.initConfig(IbisManager.DFLT_CONFIGURATION, DFLT_AUTOSTART);
	}
    
    public boolean initConfig() {
        return initConfig(IbisManager.DFLT_CONFIGURATION, 
            IbisMain.DFLT_AUTOSTART);
    }
    
    public boolean initConfig(
        String configurationFile,
        String autoStart) {
        log.info("* IBIS Startup: Running on JDK version '" 
                + System.getProperty("java.version")
                + "', Spring indicates JDK Major version: 1." + (JdkVersion.getMajorJavaVersion()+3));
        // This should be made conditional, somehow
        startJmxServer();
        
        // Reading in Spring Context
        log.info("* IBIS Startup: Creating Spring Bean Factory from file '"
            + DFLT_SPRING_CONTEXT + "'");
        Resource rs = new ClassPathResource(DFLT_SPRING_CONTEXT);
        beanFactory = new XmlBeanFactory(rs);
        ibisManager = (IbisManager) beanFactory.getBean("ibisManager");
        
        ibisManager.loadConfigurationFile(configurationFile);
        configuration = ibisManager.getConfiguration();
        
        if (autoStart.equalsIgnoreCase("TRUE")) {
            log.info("* IBIS Startup: Starting adapters");
            ibisManager.startIbis();
        }
        log.info("* IBIS Startup: Startup complete");
        return true;
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
