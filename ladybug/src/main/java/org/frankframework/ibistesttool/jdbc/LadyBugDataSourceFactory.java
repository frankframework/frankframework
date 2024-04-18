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

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.jndi.DataSourceFactory;
import org.frankframework.jta.narayana.NarayanaDataSource;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class LadyBugDataSourceFactory extends DataSourceFactory implements ApplicationContextAware {
	private String txManagerType;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		String txManager = applicationContext.getEnvironment().getProperty("application.server.type.custom");
		if(StringUtils.isNotEmpty(txManager)) {
			txManagerType = txManager.toUpperCase();
		}
	}

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if("NARAYANA".equals(txManagerType) && dataSource instanceof XADataSource source) {
			return new NarayanaDataSource(source, dataSourceName);
		}

		return super.augmentDatasource(dataSource, dataSourceName);
	}

}
