/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.lifecycle;

import java.sql.Connection;

import javax.annotation.Nonnull;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.Setter;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.jndi.JndiDataSourceFactory;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * Verifies if a (valid) connection can be made.
 * This class uses JDBC4 isValid which may not always work properly.
 * As an additional step it not only verifies the socket is open but also usable.
 * 
 * In addition it verifies if the TransactionManager can see/use the database and if the connections are using a TX isolation level
 * 
 * @author Niels Meijer
 */
public class VerifyDatabaseConnectionBean implements ApplicationContextAware, InitializingBean {

	private final Logger log = LogUtil.getLogger(this);
	private final String defaultDatasource = AppConstants.getInstance().getProperty(JndiDataSourceFactory.DEFAULT_DATASOURCE_NAME_PROPERTY);
	private final boolean requiresDatabase = AppConstants.getInstance().getBoolean("jdbc.required", true);
	private @Setter ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(requiresDatabase) {
			PlatformTransactionManager transactionManager = getTransactionManager();

			//Try to create a new transaction to check if there is a connection to the database
			TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

			try (Connection connection = getDefaultDataSource().getConnection()) {
				if(!connection.isValid(5)) {
					throw new CannotGetJdbcConnectionException("Database was unable to validate the connection within 5 seconds");
				}

				int isolationLevel = connection.getTransactionIsolation();
				if(isolationLevel == Connection.TRANSACTION_NONE) {
					log.info("expected a transacted connection got isolation level [{}]", isolationLevel);
				}
			}

			if(status != null) { //If there is a transaction close it!
				transactionManager.commit(status);
			}
		}
	}

	private @Nonnull PlatformTransactionManager getTransactionManager() {
		try {
			PlatformTransactionManager txManager;
			txManager = applicationContext.getBean("txManager", PlatformTransactionManager.class);
			log.info("found transaction manager to [{}]", txManager);
			return txManager;
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			throw new IllegalStateException("no TransactionManager found or configured", e);
		}
	}

	private @Nonnull DataSource getDefaultDataSource() {
		try {
			IDataSourceFactory dsf = applicationContext.getBean(IDataSourceFactory.class);
			return dsf.getDataSource(defaultDatasource);
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			throw new IllegalStateException("no DataSourceFactory found or configured", e);
		} catch (NamingException e) {
			throw new IllegalStateException("no default datasource found", e);
		}
	}
}
