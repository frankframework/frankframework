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
package org.frankframework.ladybug.jdbc;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.frankframework.jdbc.datasource.PoolingDataSourceFactory;

public class LadyBugDataSourceFactory extends PoolingDataSourceFactory {

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if(dataSource instanceof DataSource && isNotOracleDataSource(dataSource)) {
			// Augment the traditional way, with an optional pool.
			return super.augmentDatasource(dataSource, dataSourceName);
		}

		// If the DataSource is only XA capable, throw an exception
		throw new IllegalStateException("DataSource is XA capable and this is not allowed!");
	}

	// Newer Oracle driver implements both XA and normal DataSource.
	private boolean isNotOracleDataSource(CommonDataSource dataSource) {
		return !dataSource.getClass().getName().startsWith("oracle.jdbc.xa");
	}
}