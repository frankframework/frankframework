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

import org.frankframework.jdbc.datasource.PoolingDataSourceFactory;

/**
 * The Ladybug uses it's own factory so it can be in control over it's connections.
 * The DataSource is wrapped in a TransactionAwareDataSourceProxy.
 */
public class LadyBugDataSourceFactory extends PoolingDataSourceFactory {

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if(dataSource instanceof XADataSource) {
			throw new IllegalStateException("The Ladybug only supports non-xa-datasources");
		}

		return super.augmentDatasource(dataSource, dataSourceName);
	}

}
