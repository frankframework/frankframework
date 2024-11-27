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
package org.frankframework.jdbc.datasource;

import java.sql.Connection;

import org.apache.tomcat.dbcp.dbcp2.PoolingDataSource;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;


/**
 * Extension of {@link PoolingDataSource} that exposes an extra method to fetch pool statistics.
 *
 */
public class OpenPoolingDataSource<C extends Connection> extends PoolingDataSource<C> {
	public OpenPoolingDataSource(final GenericObjectPool<C> pool) {
		super(pool);
	}

	@Override
	protected GenericObjectPool<C> getPool() {
		return (GenericObjectPool<C>) super.getPool();
	}
}
