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
package org.frankframework.jdbc.dbms;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Extension of {@link PoolingDataSource} that exposes the underlying {@link GenericObjectPool}, so it's possible to fetch more statistics out.
 * @param <C>
 */
public class OpenPoolingDataSource<C> extends PoolingDataSource {
	public OpenPoolingDataSource(final ObjectPool<C> pool) {
		super(pool);
	}

	/**
	 * Only available to gather statistics. Don't use this to get a connection.
	 */
	public GenericObjectPool<C> getPool() {
		ObjectPool<C> objectPool = super.getPool();
		if (objectPool instanceof GenericObjectPool) {
			return (GenericObjectPool<C>) objectPool;
		}
		return null;
	}
}
