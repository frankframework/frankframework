/*
 * AbstractEJBBase.java
 * 
 * Created on 28-sep-2007, 14:01:14
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * @author m00035f
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
