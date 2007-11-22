/*
 * $Log: AbstractEJBBase.java,v $
 * Revision 1.4  2007-11-22 08:47:43  europe\L190409
 * update from ejb-branch
 *
 * Revision 1.3.2.3  2007/11/15 10:24:32  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add JavaDoc
 * * Add AutoWiring by name of EJB Bean instances via the Spring Bean Factory
 *
 * Revision 1.3.2.2  2007/10/29 10:37:25  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix method visibility error
 *
 * Revision 1.3.2.1  2007/10/29 10:29:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Refactor: pullup a number of methods to abstract base class so they can be shared with new IFSA Session EJBs
 *
 * Revision 1.3  2007/10/15 13:08:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * EJB updates
 *
 * Revision 1.1.2.4  2007/10/15 08:37:29  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Use LogUtil for retrieving logging-instance
 *
 * Revision 1.1.2.3  2007/10/12 11:53:42  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add variable to indicate to MDB if it's transactions are container-managed, or bean-managed
 *
 * Revision 1.1.2.2  2007/10/10 14:30:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 16:07:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import javax.ejb.EJBContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisMain;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.jndi.JndiLookupFailureException;

/**
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
abstract public class AbstractEJBBase {
    public static final String COMP_ENV_JNDI_PREFIX = "java:comp/env/";
    private final static Logger log = LogUtil.getLogger(AbstractEJBBase.class);
    
    protected static IbisMain main;
    protected static IbisManager manager;
    protected static Configuration config;
    protected static ListableBeanFactory beanFactory;
    
    private Context context;
    
    static {
        // Do static initializations, including setting up Spring
        // NB: This MUST me done statically, not from an instance.
        
        log.info("<** - **> Starting initialization of IBIS Framework from EJB");
        main = new IbisMain();
        
        // TODO: Get the right parameters for initialization from somewhere,
        // most importantly the right Spring Context!
        main.initConfig();
        manager = main.getIbisManager();
        config = main.getConfiguration();
        manager.startIbis();
        beanFactory = main.getBeanFactory();
    }
    
    abstract protected EJBContext getEJBContext();

    /**
     * Let the Spring Bean Factory auto-write the EJB instance by name.
     */
    public AbstractEJBBase() {
        // Apply auto-wiring and initialization to self
        ((AutowireCapableBeanFactory)beanFactory)
            .autowireBeanProperties(
                this, 
                AutowireCapableBeanFactory.AUTOWIRE_BY_NAME,
                false);
        ((AutowireCapableBeanFactory)beanFactory).initializeBean(this, "IbisEJB");
    }
    
    /**
     * Get the JNDI Naming Context
     * @return Default JNDI Naming Context
     */
    protected Context getContext() {
        synchronized (this) {
            if (context == null) {
                try {
                    context = new InitialContext();
                } catch (NamingException ex) {
                    throw new JndiLookupFailureException("Couldn't create InitialContext - oh bugger", ex);
                }
            }
        }
        return context;
    }
    
    /**
     * Get variable from Bean Environment. Not allowed to call before EJB Create.
     * 
     * @param varName Name of variable to retrieve. Will be prefixed with 
     * "java:comp/env/" is needed.
     * @return Value of the variable, as <code>java.lang.Object</code>.
     * @throws org.springframework.jndi.JndiLookupFailureException If the lookup
     * in the JNDI throws a NamingException, it will be wrapped in a JndiLookupFailureException
     * (which is derived from RuntimeException).
     */
    protected Object getContextVariable(String varName) throws JndiLookupFailureException {
        try {
            if (!varName.startsWith(COMP_ENV_JNDI_PREFIX)) {
                varName = COMP_ENV_JNDI_PREFIX + varName;
            }
            return getContext().lookup(varName);
        } catch (NamingException ex) {
            throw new JndiLookupFailureException(varName, ex);
        }
    }
}
