/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.ibistesttool.jdbc;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.frankframework.configuration.Configuration;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.util.AppConstants;
import org.frankframework.util.SpringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.PlatformTransactionManager;

public class ProxyDataSourceFactory implements IDataSourceFactory, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {
	private ApplicationContext applicationContext;
	private IDataSourceFactory delegate;
	private String instanceName = AppConstants.getInstance().getProperty("instance.name");

	@Override
	public DataSource getDataSource(String dataSourceName) throws IllegalStateException {
		if(delegate == null) throw new IllegalStateException("No delegate wired (yet)!");

		return delegate.getDataSource(dataSourceName);
	}

	@Override
	public DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws IllegalStateException {
		if(delegate == null) throw new IllegalStateException("No delegate wired (yet)!");

		return delegate.getDataSource(dataSourceName, jndiEnvironment);
	}

	@Override
	public List<String> getDataSourceNames() {
		throw new IllegalStateException("not implemented!");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(event.getSource() != applicationContext &&
				!(event.getSource() instanceof Configuration) &&
				instanceName.equals(event.getApplicationContext().getId())) {

			ApplicationContext ac = event.getApplicationContext();
			delegate = ac.getBean("dataSourceFactory", IDataSourceFactory.class);

			PlatformTransactionManager txManager = ac.getBean("txManager", PlatformTransactionManager.class);
			SpringUtils.registerSingleton(applicationContext, "ladybugTransactionManager", txManager);
		}
	}

}
