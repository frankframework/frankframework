/*
   Copyright 2013, 2018, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since  
 */
public class DbmsSupportFactory implements IDbmsSupportFactory {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private final static String PRODUCT_NAME_ORACLE_="Oracle";
	private final static String PRODUCT_NAME_MSSQLSERVER="Microsoft SQL Server";
	private final static String PRODUCT_NAME_MYSQL="MySQL";
	private final static String PRODUCT_NAME_MARIADB="MariaDB";

	private Properties dbmsSupportMap; 

	public IDbmsSupport getDbmsSupport(Connection conn) {
		String productName;
		String productVersion;
		try {
			DatabaseMetaData md = conn.getMetaData();
			productName = md.getDatabaseProductName();
			productVersion = md.getDatabaseProductVersion();
			if (PRODUCT_NAME_MYSQL.equals(productName) && StringUtils.contains(productVersion, PRODUCT_NAME_MARIADB)) {
				productName = PRODUCT_NAME_MARIADB;
			}
		} catch (SQLException e1) {
			throw new RuntimeException("cannot obtain product from connection metadata", e1);
		}
		Properties supportMap=getDbmsSupportMap();
		if (supportMap!=null) {
			if (StringUtils.isEmpty(productName)) {
				log.warn("no product found from connection metadata");
			} else {
				if (!supportMap.containsKey(productName)) {
					log.warn("product ["+productName+"] not configured in dbmsSupportMap");
				} else {
					String dbmsSupportClass=supportMap.getProperty(productName);
					if (StringUtils.isEmpty(dbmsSupportClass)) {
						log.warn("product ["+productName+"] configured empty in dbmsSupportMap");
					} else {
						try {
							if (log.isDebugEnabled()) log.debug("creating dbmsSupportClass ["+dbmsSupportClass+"] for product ["+productName+"]");
							return (IDbmsSupport)ClassUtils.newInstance(dbmsSupportClass);
						} catch (Exception e) {
							throw new RuntimeException("Cannot create dbmsSupportClass ["+dbmsSupportClass+"] for product ["+productName+"]",e);
						} 
					}
				}
			}				
		} else {
			log.warn("no dbmsSupportMap specified, reverting to built in types");
			if (PRODUCT_NAME_ORACLE_.equals(productName)) {
				log.debug("Setting databasetype to ORACLE");
				return new OracleDbmsSupport();
			}
			if (PRODUCT_NAME_MSSQLSERVER.equals(productName)) {
				log.debug("Setting databasetype to MSSQLSERVER");
				return new MsSqlServerDbmsSupport();
			}
			if (PRODUCT_NAME_MARIADB.equals(productName)) {
				log.debug("Setting databasetype to MariaDB");
				return new MariaDbDbmsSupport();
			}
		}
		log.debug("Setting databasetype to GENERIC, productName ["+productName+"]");
		return new GenericDbmsSupport();
	}

	public Properties getDbmsSupportMap() {
		return dbmsSupportMap;
	}
	public void setDbmsSupportMap(Properties dbmsSupportMap) {
		this.dbmsSupportMap = dbmsSupportMap;
	}

}
