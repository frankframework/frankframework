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
package nl.nn.adapterframework.ejb;

import javax.ejb.EJBContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jndi.JndiLookupFailureException;

/**
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
abstract public class AbstractEJBBase {
    public static final String COMP_ENV_JNDI_PREFIX = "java:comp/env/";
    private final static Logger log = LogUtil.getLogger(AbstractEJBBase.class);
    
    protected static IbisContext ibisContext;
    protected static IbisManager ibisManager;
    protected static Configuration config;
    
    private Context context;
    
    static {
        // Do static initializations, including setting up Spring
        // NB: This MUST me done statically, not from an instance.
        
        log.info("<** - **> Starting initialization of IBIS Framework from EJB");
		ibisContext = new IbisContext();
        
        // TODO: Get the right parameters for initialization from somewhere,
        // most importantly the right Spring Context!
		ibisContext.initConfig();
		ibisManager = ibisContext.getIbisManager();
        config = ibisManager.getConfiguration();
		ibisManager.startIbis();
    }
    
    abstract protected EJBContext getEJBContext();

    /**
     * Let the Spring Bean Factory auto-write the EJB instance by name.
     */
    public AbstractEJBBase() {
        // Apply auto-wiring and initialization to self
		ibisContext.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		ibisContext.initializeBean(this, "IbisEJB");
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
