/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.jdbc;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.beans.factory.DisposableBean;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class BtmDataSourceFactory extends JndiDataSourceFactory implements DisposableBean {

	@Override
	protected DataSource augment(CommonDataSource dataSource, String dataSourceName) {
		PoolingDataSource result = new PoolingDataSource();
		result.setUniqueName(dataSourceName);
		result.setMaxPoolSize(100);
		result.setAllowLocalTransactions(true);
		result.setXaDataSource((XADataSource)dataSource);
		result.init();
		return result;
	}

	@Override
	public void destroy() throws Exception {
		objects.values().forEach(ds -> ((PoolingDataSource)ds).close());
	}
}
