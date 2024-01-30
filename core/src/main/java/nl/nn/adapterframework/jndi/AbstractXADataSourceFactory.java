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
package nl.nn.adapterframework.jndi;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

public abstract class AbstractXADataSourceFactory extends PoolingJndiDataSourceFactory {

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if (dataSource instanceof XADataSource) {
			log.info("DataSource [{}] is XA enabled, registering with a Transaction Manager", dataSourceName);
			return createXADataSource((XADataSource) dataSource, dataSourceName);
		}

		if(maxPoolSize > 1) {
			log.info("DataSource [{}] is not XA enabled, creating connection pool for the datasource", dataSourceName);
			return createPool((DataSource)dataSource);
		}
		log.info("DataSource [{}] is not XA enabled and pooling not configured, used without augmentation", dataSourceName);
		return (DataSource) dataSource;
	}

	protected abstract DataSource createXADataSource(XADataSource xaDataSource, String dataSourceName);
}
