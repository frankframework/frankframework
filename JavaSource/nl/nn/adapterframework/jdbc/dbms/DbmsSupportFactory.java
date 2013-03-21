/*
   Copyright 2013 Nationale-Nederlanden

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
 * @version $Id$
 */
public class DbmsSupportFactory implements IDbmsSupportFactory {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private final static String PRODUCT_NAME_ORACLE_="Oracle";
	private final static String PRODUCT_NAME_MSSQLSERVER="Microsoft SQL Server";

	private Properties dbmsSupportMap; 
	
	public IDbmsSupport getDbmsSupport(Connection conn) {
		String product;
		try {
			DatabaseMetaData md = conn.getMetaData();
			product=md.getDatabaseProductName();
		} catch (SQLException e1) {
			throw new RuntimeException("cannot obtain product from connection metadata", e1);
		}
		Properties supportMap=getDbmsSupportMap();
		if (supportMap!=null) {
			if (StringUtils.isEmpty(product)) {
				log.warn("no product found from connection metadata");
			} else {
				if (!supportMap.containsKey(product)) {
					log.warn("product ["+product+"] not configured in dbmsSupportMap");
				} else {
					String dbmsSupportClass=supportMap.getProperty(product);
					if (StringUtils.isEmpty(dbmsSupportClass)) {
						log.warn("product ["+product+"] configured empty in dbmsSupportMap");
					} else {
						try {
							if (log.isDebugEnabled()) log.debug("creating dbmsSupportClass ["+dbmsSupportClass+"] for product ["+product+"]");
							return (IDbmsSupport)ClassUtils.newInstance(dbmsSupportClass);
						} catch (Exception e) {
							throw new RuntimeException("Cannot create dbmsSupportClass ["+dbmsSupportClass+"] for product ["+product+"]",e);
						} 
					}
				}
			}				
		} else {
			log.warn("no dbmsSupportMap specified, reverting to built in types");
			if (PRODUCT_NAME_ORACLE_.equals(product)) {
				log.debug("Setting databasetype to ORACLE");
				return new OracleDbmsSupport();
			}
			if (PRODUCT_NAME_MSSQLSERVER.equals(product)) {
				log.debug("Setting databasetype to MSSQLSERVER");
				return new MsSqlServerDbmsSupport();
			}
		}
		log.debug("Setting databasetype to GENERIC, productName ["+product+"]");
		return new GenericDbmsSupport();
	}

	public Properties getDbmsSupportMap() {
		return dbmsSupportMap;
	}
	public void setDbmsSupportMap(Properties dbmsSupportMap) {
		this.dbmsSupportMap = dbmsSupportMap;
	}

}
