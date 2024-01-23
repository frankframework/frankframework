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

import java.sql.Connection;

import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.dbcp2.managed.TransactionRegistry;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Extension of {@link ManagedDataSource} that exposes an extra method to fetch pool statistics.
 *
 * @param <C>
 */
public class OpenManagedDataSource<C extends Connection> extends ManagedDataSource<C> {

	public OpenManagedDataSource(ObjectPool pool, TransactionRegistry transactionRegistry) {
		super(pool, transactionRegistry);
	}

	public void addPoolMetadata(StringBuilder info) {
		ObjectPool<C> objectPool = super.getPool();
		if (objectPool instanceof GenericObjectPool) {
			GenericObjectPoolUtil.addPoolMetadata((GenericObjectPool<C>) objectPool, info);
		}
	}
}
