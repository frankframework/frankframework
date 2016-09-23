/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.util.List;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
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
    
    public List<Configuration> getConfigurations() {
        IbisManager mngr = getIbisManager();
        if (mngr == null) {
            log.error("Cannot look up the configuration when the IbisManager is not set");
            return null;
        } else {
        	List<Configuration> configurations = mngr.getConfigurations();
            if (configurations == null) {
                log.error("Retrieved null configuration object from real IbisManager");
            } else {
                for (Configuration configuration : configurations) {
                    log.info("Configuration retrieved from real IbisManager: configuration-name '"
                            + configuration.getConfigurationName() + "', nr of adapters: "
                            + configuration.getRegisteredAdapters().size());
                }
            }
            return configurations;
        }
    }

    public void handleAdapter(String action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin) {
        getIbisManager().handleAdapter(action, configurationName, adapterName, receiverName, commandIssuedBy, isAdmin);
    }

    public void unload(String configurationName) {
        getIbisManager().unload(configurationName);
    }

    public void shutdown() {
        getIbisManager().shutdown();
    }
    

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

	public void startConfiguration(Configuration configuration) {
		ibisManager.startConfiguration(configuration);
		
	}

	public void setIbisContext(IbisContext ibisContext) {
		ibisManager.setIbisContext(ibisContext);
	}

	public IbisContext getIbisContext() {
		return ibisManager.getIbisContext();
	}

	public void addConfiguration(Configuration configuration) {
		ibisManager.addConfiguration(configuration);
	}

	public Configuration getConfiguration(String configurationName) {
		return ibisManager.getConfiguration(configurationName);
	}

	public IAdapter getRegisteredAdapter(String name) {
		return ibisManager.getRegisteredAdapter(name);
	}

	public List<IAdapter> getRegisteredAdapters() {
		return ibisManager.getRegisteredAdapters();
	}

	public void dumpStatistics(int action) {
		ibisManager.dumpStatistics(action);
	}

	public List<String> getSortedStartedAdapterNames() {
		return ibisManager.getSortedStartedAdapterNames();
	}

}
