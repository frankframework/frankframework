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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public class EjbDelegatingIbisManager implements IbisManager, BeanFactoryAware {
    private final static Logger log = LogUtil.getLogger(EjbDelegatingIbisManager.class);
    
    private static final String FACTORY_BEAN_ID = "&ibisManagerEjb";
    
    private IbisManager ibisManager;
    private BeanFactory beanFactory;
    private PlatformTransactionManager transactionManager;
    
    protected synchronized IbisManager getIbisManager() {
        if (this.ibisManager == null) {
            // Look it up via EJB, using JNDI Name based on configuration name
            LocalStatelessSessionProxyFactoryBean factoryBean = 
                    (LocalStatelessSessionProxyFactoryBean) beanFactory.getBean(FACTORY_BEAN_ID);
            this.ibisManager = (IbisManager) factoryBean.getObject();
            log.info("Looked up IbisManagerEjb at JNDI location '" + factoryBean.getJndiName() + "'");
        }
        return this.ibisManager;
    }
    
    public Configuration getConfiguration() {
        IbisManager mngr = getIbisManager();
        if (mngr == null) {
            log.error("Cannot look up the configuration when the IbisManager is not set");
            return null;
        } else {
            Configuration cfg = mngr.getConfiguration();
            if (cfg == null) {
                log.error("Retrieved null configuration object from real IbisManager");
            } else {
                log.info("Configuration retrieved from real IbisManager: configuration-name '"
                        + cfg.getConfigurationName() + "', nr of adapters: "
                        + cfg.getRegisteredAdapters().size());
            }
            return cfg;
        }
    }

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        getIbisManager().handleAdapter(action, adapterName, receiverName, commandIssuedBy);
    }

    public void startIbis() {
        // Not implemented for this case, since the Ibis will be auto-started from EJB container
    }

    public void shutdownIbis() {
        getIbisManager().shutdownIbis();
    }
    
    public void startAdapters() {
        getIbisManager().startAdapters();
    }

    public void stopAdapters() {
        getIbisManager().stopAdapters();
    }

    public void startAdapter(IAdapter adapter) {
        getIbisManager().startAdapter(adapter);
    }

    public void stopAdapter(IAdapter adapter) {
        getIbisManager().stopAdapter(adapter);
    }

    public void loadConfigurationFile(String configurationFile) {
    	// Do not delegate to EJB; EJB initializes itself.
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public String getDeploymentModeString() {
        return IbisManager.DEPLOYMENT_MODE_EJB_STRING;
    }

    public int getDeploymentMode() {
        return IbisManager.DEPLOYMENT_MODE_EJB;
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
