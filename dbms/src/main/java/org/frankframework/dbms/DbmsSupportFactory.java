/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

package org.frankframework.dbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;


/**
 * @author Gerrit van Brakel
 */
@Log4j2
public class DbmsSupportFactory {
	private final Map<DataSource, IDbmsSupport> dbmsSupport = new ConcurrentHashMap<>();

	private @Getter Properties dbmsSupportMap;

	public IDbmsSupport getDbmsSupport(DataSource datasource) {
		return dbmsSupport.computeIfAbsent(datasource, this::compute);
	}

	private IDbmsSupport compute(DataSource datasource) {
		try (Connection connection = datasource.getConnection()) {
			return getDbmsSupport(connection);
		} catch (SQLException e) {
			log.warn("SQL exception while trying to get a connection from datasource [{}]", datasource, e);
			return new GenericDbmsSupport();
		}
	}

	public IDbmsSupport getDbmsSupport(Connection connection) throws SQLException {
		try {
			DatabaseMetaData md = connection.getMetaData();
			String name = md.getDatabaseProductName();
			String version = md.getDatabaseProductVersion();
			return getDbmsSupport(name, version);
		} catch (SQLException | DbmsException e) {
			throw new RuntimeException("cannot obtain product from connection metadata", e);
		}
	}

	public IDbmsSupport getDbmsSupport(String product, String productVersion) throws DbmsException {
		if (StringUtils.isEmpty(product)) {
			log.warn("no product found from connection metadata");
		} else {
			Properties supportMap = getDbmsSupportMap();
			if (supportMap == null) {
				log.debug("no dbmsSupportMap specified, reverting to built-in types");
			} else {
				if (!supportMap.containsKey(product)) {
					log.debug("product [{}] not configured in dbmsSupportMap, will search in built-in types", product);
				} else {
					String dbmsSupportClass = supportMap.getProperty(product);
					if (StringUtils.isEmpty(dbmsSupportClass)) {
						log.warn("product [{}] configured empty in dbmsSupportMap, will search in built-in types", product);
					} else {
						try {
							if (log.isDebugEnabled())
								log.debug("creating dbmsSupportClass [{}] for product [{}] productVersion [{}]", dbmsSupportClass, product, productVersion);
							return ClassUtils.newInstance(dbmsSupportClass, IDbmsSupport.class);
						} catch (Exception e) {
							throw new DbmsException("Cannot create dbmsSupportClass [" + dbmsSupportClass + "] for product [" + product + "] productVersion [" + productVersion + "]", e);
						}
					}
				}
			}
			return Dbms.findDbmsSupportByProduct(product, productVersion);
		}
		log.debug("Setting databasetype to GENERIC, productName [{}]", product);
		return new GenericDbmsSupport();
	}

	public void setDbmsSupportMap(Properties dbmsSupportMap) {
		this.dbmsSupportMap = dbmsSupportMap;
	}
}
