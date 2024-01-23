package org.frankframework.jdbc.dbms;

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
		info.append("removeAbOnBorrow [").append(pool.getRemoveAbandonedOnBorrow()).append(CLOSE);
		info.append("removeAbOnMaint [").append(pool.getRemoveAbandonedOnMaintenance()).append(CLOSE);
		info.append("removeAbOnTimeoutDur [").append(pool.getRemoveAbandonedTimeoutDuration()).append(CLOSE);
	}
}
