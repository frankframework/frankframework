/*
 * $Log: IbisContext.java,v $
 * Revision 1.3  2009-10-29 13:32:07  m168309
 * Run IBIS on Tomcat
 *
 * Revision 1.2  2008/02/25 10:10:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed JMX server starting
 *
 * Revision 1.1  2008/02/13 12:52:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed IbisMain to IbisContext
 *
 * Revision 1.8  2008/02/08 09:47:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.7  2007/12/28 08:54:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made fields private
 *
 * Revision 1.6  2007/10/16 13:18:03  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Allow creating of Spring Factory without creating instance of IbisMain
 *
 * Revision 1.5  2007/10/16 13:07:39  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
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

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
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
public class IbisContext {
    private final static Logger log = LogUtil.getLogger(IbisContext.class);

    public static final String DFLT_AUTOSTART = "TRUE";
    public static final String DFLT_SPRING_CONTEXT = "/springContext.xml";
    
    private ListableBeanFactory beanFactory;
    private IbisManager ibisManager;
    
	/**
	 * Initialize Ibis with all default parameters.
	 * 
	 * @return
	 */
	public boolean initConfig() {
	    return initConfig(IbisContext.DFLT_SPRING_CONTEXT, IbisManager.DFLT_CONFIGURATION, IbisContext.DFLT_AUTOSTART);
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
    public boolean initConfig(String springContext, String configurationFile, String autoStart) {
		initContext(springContext);        
        ibisManager.loadConfigurationFile(configurationFile);
        
        if ("TRUE".equalsIgnoreCase(autoStart)) {
            log.info("* IBIS Startup: Starting adapters");
            ibisManager.startIbis();
        }
        log.info("* IBIS Startup: Startup complete");
        return true;
    }

	public void initContext(String springContext) {
		log.info("* IBIS Startup: Running on JDK version [" + System.getProperty("java.version")
				+ "], Spring indicates JDK Major version: 1." + (JdkVersion.getMajorJavaVersion()+3));
		// This should be made conditional, somehow
//		startJmxServer();

		// TODO: replace the setting of the default value for the property below (it should be done in the spring context file)
		String ts_name = "transaction.strategy";
		String ts_value = System.getProperty(ts_name);
		if (ts_value!=null) {
			log.info("* system property [" + ts_name + "] has value [" + ts_value + "]");
		} else {
			System.setProperty(ts_name,ts_value);
			log.info("* system property [" + ts_name + "] set to value [" + ts_value + "]");
		}
		
		beanFactory = createBeanFactory(springContext);
		ibisManager = getIbisManager(beanFactory);
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
	static public XmlBeanFactory createBeanFactory(String springContext) throws BeansException {
		// Reading in Spring Context
		if (springContext == null) {
		    springContext = DFLT_SPRING_CONTEXT;
		}
		log.info("* IBIS Startup: Creating Spring Bean Factory from file [" + springContext + "]");
		Resource rs = new ClassPathResource(springContext);
		XmlBeanFactory bf = new XmlBeanFactory(rs);
		return bf;
	}

	static private IbisManager getIbisManager(ListableBeanFactory beanFactory) throws BeansException {
		return (IbisManager) beanFactory.getBean("ibisManager");
	}

	public Object getAutoWiredObject(Class clazz) throws ConfigurationException {
		return getAutoWiredObject(clazz, null);
	}
	
	public Object getAutoWiredObject(Class clazz, String prototypeName) throws ConfigurationException {
		
		String beanName;
		
		prototypeName="proto-"+prototypeName;
		// No explicit classname given; get bean from Spring Factory
		if (clazz == null) {
			beanName = prototypeName;
		} else {
			// Get all beans matching the classname given
			String[] matchingBeans = getBeanFactory().getBeanNamesForType(clazz);
			if (matchingBeans.length == 1) {
				// Only 1 bean of this type, so create it
				beanName = matchingBeans[0];
			} else if (matchingBeans.length > 1) {
				// multiple beans; find if there's one with the
				// same name as from 'getBeanName'.
				beanName = prototypeName;
			} else {
				// No beans matching the type.
				// Create instance, and if the instance implements
				// Spring's BeanFactoryAware interface, use it to
				// set BeanFactory attribute on this Bean.
				try {
					return createBeanAndAutoWire(clazz, prototypeName);
				} catch (Exception e) {
					throw new ConfigurationException(e);
				}
			}
		}
        
		// Only accept prototype-beans!
		if (!getBeanFactory().isPrototype(beanName)) {
			throw new ConfigurationException("Beans created from the BeanFactory must be prototype-beans, bean ["
				+ beanName + "] of class [" + clazz.getName() + "] is not.");
		}
		if (log.isDebugEnabled()) {
			log.debug("Creating bean with actual bean-name [" + beanName + "], bean-class [" + (clazz != null ? clazz.getName() : "null") + "] from Spring Bean Factory.");
		}
		return getBeanFactory().getBean(beanName, clazz);
	}

	protected Object createBeanAndAutoWire(Class beanClass, String prototype) throws InstantiationException, IllegalAccessException {
		if (log.isDebugEnabled()) {
			log.debug("Bean class [" + beanClass.getName() + "] not found in Spring Bean Factory, instantiating directly and using Spring Factory for auto-wiring support.");
		}
		Object o = beanClass.newInstance();
		if (getBeanFactory() instanceof AutowireCapableBeanFactory) {
			((AutowireCapableBeanFactory)getBeanFactory()).autowireBeanProperties(o,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME,false);
			o = ((AutowireCapableBeanFactory)getBeanFactory()).initializeBean(o, prototype);
		} else if (o instanceof BeanFactoryAware) {
			((BeanFactoryAware)o).setBeanFactory(getBeanFactory());
		}
		return o;
	}

//	private void startJmxServer() {
//		//Start MBean server
//        
//        // It seems that no reference to the server is required anymore,
//        // anywhere later? So no reference is returned from
//        // this method.
//        log.info("* IBIS Startup: Attempting to start MBean server");
//		MBeanServer server=MBeanServerFactory.createMBeanServer();
//		try {
//		  ObjectInstance html = server.createMBean("com.sun.jdmk.comm.HtmlAdaptorServer", null);
//		    
//		  server.invoke(html.getObjectName(), "start", new Object[0], new String[0]);
//        } catch (ReflectionException e ) {
//            log.error("Requested JMX Server MBean can not be created; JMX not available.");
//            return;
//        } catch (Exception e) {
//		    log.error("Error with jmx:",e);
//            return;
//		}
//		log.info("MBean server up and running. Monitor your application by pointing your browser to http://localhost:8082");
//	}
    

	public void setBeanFactory(ListableBeanFactory factory) {
		beanFactory = factory;
	}
	public ListableBeanFactory getBeanFactory() {
		if (beanFactory==null) {
			initContext(DFLT_SPRING_CONTEXT);
		}
		return beanFactory;
	}


	public IbisManager getIbisManager() {
		return ibisManager;
	}

	public static void main(String[] args) {
		IbisContext im=new IbisContext();
		im.initConfig(IbisContext.DFLT_SPRING_CONTEXT, IbisManager.DFLT_CONFIGURATION, IbisContext.DFLT_AUTOSTART);
	}

}
