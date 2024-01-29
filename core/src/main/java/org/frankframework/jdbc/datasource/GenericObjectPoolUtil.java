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

import org.apache.commons.pool2.impl.GenericObjectPool;

public class GenericObjectPoolUtil {

	private static final String CLOSE = "], ";

	static void addPoolMetadata(GenericObjectPool pool, StringBuilder info) {
		if (pool == null || info == null) {
			return;
		}
		info.append("DBCP2 Pool Info: ");
		info.append("maxIdle [").append(pool.getMaxIdle()).append(CLOSE);
		info.append("minIdle [").append(pool.getMinIdle()).append(CLOSE);
		info.append("maxTotal [").append(pool.getMaxTotal()).append(CLOSE);
		info.append("numActive [").append(pool.getNumActive()).append(CLOSE);
		info.append("numIdle [").append(pool.getNumIdle()).append(CLOSE);
		info.append("testOnBorrow [").append(pool.getTestOnBorrow()).append(CLOSE);
		info.append("testOnCreate [").append(pool.getTestOnCreate()).append(CLOSE);
		info.append("testOnReturn [").append(pool.getTestOnReturn()).append(CLOSE);
		info.append("testWhileIdle [").append(pool.getTestWhileIdle()).append(CLOSE);
		info.append("removeAbandonedOnBorrow [").append(pool.getRemoveAbandonedOnBorrow()).append(CLOSE);
		info.append("removeAbandonedOnMaintenance [").append(pool.getRemoveAbandonedOnMaintenance()).append(CLOSE);
		info.append("removeAbandonedTimeoutDuration [").append(pool.getRemoveAbandonedTimeoutDuration()).append(CLOSE);
	}
}
