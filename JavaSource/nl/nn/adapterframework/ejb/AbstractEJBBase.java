/*
 * $Log: AbstractEJBBase.java,v $
 * Revision 1.2  2007-10-09 16:07:37  europe\L190409
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisMain;
import nl.nn.adapterframework.configuration.IbisManager;
import org.apache.log4j.Logger;
import org.springframework.jndi.JndiLookupFailureException;

/**
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
abstract public class AbstractEJBBase {
    public static final String COMP_ENV_JNDI_PREFIX = "java:comp/env/";
    private final static Logger log = Logger.getLogger(AbstractEJBBase.class);
    
    protected static IbisMain main;
    protected static IbisManager manager;
    protected static Configuration config;

    private Context context;
    
    static {
        // Do static initializations, including setting up Spring
        log.info("<** - **> Starting initialization of IBIS Framework from EJB");
        main = new IbisMain();
        
        // TODO: Get the right parameters for initialization from somewhere,
        // most importantly the right Spring Context!
        main.initConfig();
        manager = main.getIbisManager();
        config = main.getConfiguration();
        manager.startIbis();
    }
    
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
    
    protected String getContextVariable(String varName) {
        try {
            if (!varName.startsWith(COMP_ENV_JNDI_PREFIX)) {
                varName = COMP_ENV_JNDI_PREFIX + varName;
            }
            return (String) getContext().lookup(varName);
        } catch (NamingException ex) {
            throw new JndiLookupFailureException(varName, ex);
        }
    }
}
